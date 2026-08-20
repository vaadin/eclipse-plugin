package com.vaadin.plugin.hotswap;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import com.vaadin.plugin.util.VaadinPluginLog;

/**
 * Minimal extractor for gzip compressed tar archives.
 *
 * The JDK has no tar support and the Eclipse target platform does not provide commons-compress, so the JetBrains
 * Runtime archives are unpacked with this reader. It understands the ustar format together with the GNU long name and
 * PAX extended header entries that GNU tar emits for the JBR archives.
 */
public class TarGzExtractor {

    private static final int BLOCK_SIZE = 512;

    // ustar header field offsets
    private static final int NAME_OFFSET = 0;
    private static final int NAME_LENGTH = 100;
    private static final int MODE_OFFSET = 100;
    private static final int MODE_LENGTH = 8;
    private static final int SIZE_OFFSET = 124;
    private static final int SIZE_LENGTH = 12;
    private static final int TYPE_OFFSET = 156;
    private static final int LINK_NAME_OFFSET = 157;
    private static final int LINK_NAME_LENGTH = 100;
    private static final int PREFIX_OFFSET = 345;
    private static final int PREFIX_LENGTH = 155;

    // Entry types
    private static final byte TYPE_FILE_OLD = '\0';
    private static final byte TYPE_FILE = '0';
    private static final byte TYPE_HARD_LINK = '1';
    private static final byte TYPE_SYMLINK = '2';
    private static final byte TYPE_DIRECTORY = '5';
    private static final byte TYPE_GNU_LONG_NAME = 'L';
    private static final byte TYPE_GNU_LONG_LINK_NAME = 'K';
    private static final byte TYPE_PAX_EXTENDED = 'x';
    private static final byte TYPE_PAX_GLOBAL = 'g';

    private TarGzExtractor() {
    }

    /**
     * Extract a .tar.gz archive into the given directory.
     *
     * @param archive
     *            The archive to extract
     * @param targetDirectory
     *            The directory to extract into, created when missing
     * @param monitor
     *            Progress monitor, may be null
     * @throws IOException
     *             if the archive cannot be read or written
     */
    public static void extract(Path archive, Path targetDirectory, IProgressMonitor monitor) throws IOException {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(archive))) {
            extract(in, targetDirectory, monitor);
        }
    }

    /**
     * Extract a gzip compressed tar stream into the given directory.
     *
     * @param compressed
     *            The gzip compressed tar stream, closed by the caller
     * @param targetDirectory
     *            The directory to extract into, created when missing
     * @param monitor
     *            Progress monitor, may be null
     * @throws IOException
     *             if the archive cannot be read or written
     */
    public static void extract(InputStream compressed, Path targetDirectory, IProgressMonitor monitor)
            throws IOException {
        Path root = targetDirectory.toAbsolutePath().normalize();
        Files.createDirectories(root);
        // Not closed on purpose, the caller owns the stream it passed in
        extractTar(new GZIPInputStream(compressed, 65536), root, monitor);
    }

    private static void extractTar(InputStream tar, Path root, IProgressMonitor monitor) throws IOException {
        byte[] header = new byte[BLOCK_SIZE];
        String longName = null;
        String longLinkName = null;
        Map<String, String> paxHeaders = new HashMap<>();

        while (true) {
            checkCanceled(monitor);

            if (!readFully(tar, header)) {
                // Truncated archives are treated as complete, GNU tar pads with
                // empty blocks that may be dropped by the producer
                break;
            }
            if (isEmptyBlock(header)) {
                break;
            }

            long size = readOctal(header, SIZE_OFFSET, SIZE_LENGTH);
            byte type = header[TYPE_OFFSET];

            if (type == TYPE_GNU_LONG_NAME) {
                longName = readString(tar, size);
                continue;
            }
            if (type == TYPE_GNU_LONG_LINK_NAME) {
                longLinkName = readString(tar, size);
                continue;
            }
            if (type == TYPE_PAX_EXTENDED || type == TYPE_PAX_GLOBAL) {
                paxHeaders.putAll(parsePaxHeaders(readString(tar, size)));
                continue;
            }

            String name = resolveName(header, longName, paxHeaders.get("path"));
            String linkName = longLinkName != null
                    ? longLinkName
                    : paxHeaders.getOrDefault("linkpath", readString(header, LINK_NAME_OFFSET, LINK_NAME_LENGTH));
            int mode = (int) readOctal(header, MODE_OFFSET, MODE_LENGTH);

            longName = null;
            longLinkName = null;
            paxHeaders.clear();

            if (name.isEmpty()) {
                skipEntry(tar, size);
                continue;
            }

            subTask(monitor, name);
            Path target = resolveTarget(root, name);

            switch (type) {
            case TYPE_DIRECTORY:
                Files.createDirectories(target);
                applyMode(target, mode);
                break;
            case TYPE_SYMLINK:
                createSymbolicLink(target, linkName);
                break;
            case TYPE_HARD_LINK:
                createHardLink(root, target, linkName);
                break;
            case TYPE_FILE:
            case TYPE_FILE_OLD:
                writeFile(tar, target, size);
                applyMode(target, mode);
                break;
            default:
                // Character devices, FIFOs and similar are not expected in a JDK
                // archive and are silently ignored
                VaadinPluginLog.debug("Skipping unsupported tar entry type '" + (char) type + "' for " + name);
                break;
            }

            if (type != TYPE_FILE && type != TYPE_FILE_OLD) {
                skipEntry(tar, size);
            }
        }
    }

    private static String resolveName(byte[] header, String longName, String paxPath) {
        if (longName != null) {
            return longName;
        }
        if (paxPath != null) {
            return paxPath;
        }
        String name = readString(header, NAME_OFFSET, NAME_LENGTH);
        String prefix = readString(header, PREFIX_OFFSET, PREFIX_LENGTH);
        return prefix.isEmpty() ? name : prefix + "/" + name;
    }

    /**
     * Resolve an archive entry name against the extraction root, rejecting entries that would escape it.
     */
    private static Path resolveTarget(Path root, String name) throws IOException {
        Path target = root.resolve(name).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("Tar entry points outside of the target directory: " + name);
        }
        return target;
    }

    private static void writeFile(InputStream in, Path target, long size) throws IOException {
        Files.createDirectories(target.getParent());
        Files.deleteIfExists(target);

        byte[] buffer = new byte[8192];
        long remaining = size;
        try (OutputStream out = Files.newOutputStream(target)) {
            while (remaining > 0) {
                int read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read < 0) {
                    throw new EOFException("Unexpected end of tar entry " + target.getFileName());
                }
                out.write(buffer, 0, read);
                remaining -= read;
            }
        }
        skipPadding(in, size);
    }

    private static void createSymbolicLink(Path target, String linkName) throws IOException {
        if (linkName.isEmpty()) {
            return;
        }
        Files.createDirectories(target.getParent());
        Files.deleteIfExists(target);
        try {
            Files.createSymbolicLink(target, Paths.get(linkName));
        } catch (IOException | UnsupportedOperationException e) {
            // Creating symbolic links requires elevated privileges on Windows.
            // The JDK archives do not rely on them there, so keep going.
            VaadinPluginLog.debug("Could not create symbolic link " + target + " -> " + linkName);
        }
    }

    private static void createHardLink(Path root, Path target, String linkName) throws IOException {
        if (linkName.isEmpty()) {
            return;
        }
        Path source = resolveTarget(root, linkName);
        if (!Files.exists(source)) {
            VaadinPluginLog.debug("Skipping hard link " + target + ", target " + linkName + " does not exist");
            return;
        }
        Files.createDirectories(target.getParent());
        Files.deleteIfExists(target);
        try {
            Files.createLink(target, source);
        } catch (IOException | UnsupportedOperationException e) {
            Files.copy(source, target);
        }
    }

    /**
     * Apply the permissions stored in the tar header. The executable bit matters, a JDK is unusable when bin/java is
     * not executable.
     */
    private static void applyMode(Path target, int mode) {
        if (mode == 0) {
            return;
        }
        PosixFileAttributeView posix = Files.getFileAttributeView(target, PosixFileAttributeView.class,
                LinkOption.NOFOLLOW_LINKS);
        if (posix != null) {
            try {
                posix.setPermissions(toPermissions(mode));
                return;
            } catch (IOException e) {
                VaadinPluginLog.debug("Could not set permissions on " + target + ": " + e.getMessage());
            }
        }
        target.toFile().setExecutable((mode & 0111) != 0, false);
    }

    private static Set<PosixFilePermission> toPermissions(int mode) {
        Set<PosixFilePermission> permissions = new HashSet<>();
        if ((mode & 0400) != 0) {
            permissions.add(PosixFilePermission.OWNER_READ);
        }
        if ((mode & 0200) != 0) {
            permissions.add(PosixFilePermission.OWNER_WRITE);
        }
        if ((mode & 0100) != 0) {
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
        }
        if ((mode & 0040) != 0) {
            permissions.add(PosixFilePermission.GROUP_READ);
        }
        if ((mode & 0020) != 0) {
            permissions.add(PosixFilePermission.GROUP_WRITE);
        }
        if ((mode & 0010) != 0) {
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
        }
        if ((mode & 0004) != 0) {
            permissions.add(PosixFilePermission.OTHERS_READ);
        }
        if ((mode & 0002) != 0) {
            permissions.add(PosixFilePermission.OTHERS_WRITE);
        }
        if ((mode & 0001) != 0) {
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);
        }
        return permissions;
    }

    /**
     * Parse PAX extended header records, each formatted as "length key=value\n".
     */
    private static Map<String, String> parsePaxHeaders(String content) {
        Map<String, String> headers = new HashMap<>();
        for (String record : content.split("\n")) {
            int space = record.indexOf(' ');
            int equals = record.indexOf('=');
            if (space < 0 || equals < space) {
                continue;
            }
            headers.put(record.substring(space + 1, equals), record.substring(equals + 1));
        }
        return headers;
    }

    private static void skipEntry(InputStream in, long size) throws IOException {
        skipFully(in, size);
        skipPadding(in, size);
    }

    private static void skipPadding(InputStream in, long size) throws IOException {
        skipFully(in, (BLOCK_SIZE - (size % BLOCK_SIZE)) % BLOCK_SIZE);
    }

    private static void skipFully(InputStream in, long count) throws IOException {
        long remaining = count;
        while (remaining > 0) {
            long skipped = in.skip(remaining);
            if (skipped <= 0) {
                if (in.read() < 0) {
                    throw new EOFException("Unexpected end of tar archive");
                }
                skipped = 1;
            }
            remaining -= skipped;
        }
    }

    private static String readString(InputStream in, long size) throws IOException {
        byte[] content = new byte[(int) size];
        if (!readFully(in, content)) {
            throw new EOFException("Unexpected end of tar archive");
        }
        skipPadding(in, size);
        return trimToNull(content, 0, content.length);
    }

    private static String readString(byte[] header, int offset, int length) {
        return trimToNull(header, offset, length);
    }

    private static String trimToNull(byte[] bytes, int offset, int length) {
        int end = offset;
        while (end < offset + length && bytes[end] != 0) {
            end++;
        }
        return new String(bytes, offset, end - offset, StandardCharsets.UTF_8).trim();
    }

    private static long readOctal(byte[] header, int offset, int length) {
        String value = trimToNull(header, offset, length);
        if (value.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(value, 8);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean isEmptyBlock(byte[] block) {
        for (byte b : block) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean readFully(InputStream in, byte[] buffer) throws IOException {
        int offset = 0;
        while (offset < buffer.length) {
            int read = in.read(buffer, offset, buffer.length - offset);
            if (read < 0) {
                return false;
            }
            offset += read;
        }
        return true;
    }

    private static void subTask(IProgressMonitor monitor, String name) {
        if (monitor != null) {
            monitor.subTask(name);
        }
    }

    private static void checkCanceled(IProgressMonitor monitor) {
        if (monitor != null && monitor.isCanceled()) {
            throw new OperationCanceledException();
        }
    }
}
