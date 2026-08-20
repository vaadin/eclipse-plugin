package com.vaadin.plugin.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.plugin.hotswap.TarGzExtractor;

/**
 * Tests for extracting the .tar.gz archives the JetBrains Runtime is distributed as.
 */
public class TarGzExtractorTest {

	private static final String SAMPLE_ARCHIVE = "testdata/jbrsdk-sample.tar.gz";

	private static final String ROOT_DIRECTORY = "jbrsdk-21.0.7-linux-x64-b1038.58";

	private static final boolean POSIX = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

	private Path targetDirectory;

	@Before
	public void createTargetDirectory() throws IOException {
		targetDirectory = Files.createTempDirectory("jbr-extract-test");
	}

	@After
	public void deleteTargetDirectory() throws IOException {
		if (targetDirectory == null || !Files.exists(targetDirectory)) {
			return;
		}
		try (Stream<Path> paths = Files.walk(targetDirectory)) {
			paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
		}
	}

	@Test
	public void extractsArchiveContent() throws IOException {
		extractSampleArchive();

		Path javaHome = targetDirectory.resolve(ROOT_DIRECTORY);
		assertTrue("Archive root directory should be created", Files.isDirectory(javaHome));

		Path release = javaHome.resolve("release");
		assertTrue("release file should be extracted", Files.isRegularFile(release));
		assertTrue("release file content should be preserved",
				Files.readString(release).contains("IMPLEMENTOR=\"JetBrains s.r.o.\""));

		Path modules = javaHome.resolve("lib").resolve("modules");
		assertEquals("File content should be preserved", "not really a module image", Files.readString(modules));
	}

	@Test
	public void keepsJavaExecutable() throws IOException {
		if (!POSIX) {
			return;
		}
		extractSampleArchive();

		Path java = targetDirectory.resolve(ROOT_DIRECTORY).resolve("bin").resolve("java");
		assertTrue("bin/java should be extracted", Files.isRegularFile(java));
		assertTrue("bin/java should stay executable, a runtime is useless otherwise", Files.isExecutable(java));
	}

	@Test
	public void extractsSymbolicLinks() throws IOException {
		if (!POSIX) {
			return;
		}
		extractSampleArchive();

		Path link = targetDirectory.resolve(ROOT_DIRECTORY).resolve("lib").resolve("modules-link");
		assertTrue("Symbolic link should be extracted", Files.isSymbolicLink(link));
		assertEquals("Symbolic link should point at its target", "modules", Files.readSymbolicLink(link).toString());
	}

	@Test
	public void rejectsEntriesOutsideOfTargetDirectory() throws IOException {
		byte[] archive = tarGz(header("../escaped.txt", (byte) '0', 0), new byte[512], new byte[512]);

		try {
			TarGzExtractor.extract(new ByteArrayInputStream(archive), targetDirectory, null);
			fail("Extracting an entry outside of the target directory should fail");
		} catch (IOException e) {
			assertTrue("Should report the offending entry, was: " + e.getMessage(),
					e.getMessage().contains("escaped.txt"));
		}

		assertTrue("Nothing should be written outside of the target directory",
				!Files.exists(targetDirectory.getParent().resolve("escaped.txt")));
	}

	@Test
	public void rejectsSymbolicLinksPointingOutsideOfTargetDirectory() throws IOException {
		// A link out of the root would otherwise let the following entry be written
		// through it, landing outside the extraction directory
		Path escaped = Files.createTempDirectory("jbr-extract-escape");
		byte[] archive = tarGz(symlinkHeader("escape", escaped.toString()),
				header("escape/pwned.txt", (byte) '0', 0), new byte[512], new byte[512]);

		try {
			TarGzExtractor.extract(new ByteArrayInputStream(archive), targetDirectory, null);
			fail("Extracting a symbolic link outside of the target directory should fail");
		} catch (IOException e) {
			assertTrue("Should report the offending link, was: " + e.getMessage(), e.getMessage().contains("escape"));
		}

		assertFalse("Nothing should be written through the link", Files.exists(escaped.resolve("pwned.txt")));
		Files.deleteIfExists(escaped);
	}

	private void extractSampleArchive() throws IOException {
		try (InputStream in = getClass().getResourceAsStream(SAMPLE_ARCHIVE)) {
			assertNotNull("Test archive " + SAMPLE_ARCHIVE + " should be available", in);
			TarGzExtractor.extract(in, targetDirectory, null);
		}
	}

	private static byte[] tarGz(byte[]... blocks) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
			for (byte[] block : blocks) {
				gzip.write(block);
			}
		}
		return out.toByteArray();
	}

	/**
	 * Build a ustar header block, used for cases GNU tar refuses to create.
	 */
	private static byte[] header(String name, byte type, long size) {
		byte[] block = new byte[512];
		write(block, 0, name);
		write(block, 100, "0000644");
		write(block, 124, String.format("%011o", size));
		write(block, 136, String.format("%011o", 0));
		block[156] = type;
		write(block, 257, "ustar");
		block[263] = '0';
		block[264] = '0';
		return recalculateChecksum(block);
	}

	/**
	 * The checksum is calculated over the block with the checksum field filled with spaces.
	 */
	private static byte[] recalculateChecksum(byte[] block) {
		for (int i = 148; i < 156; i++) {
			block[i] = ' ';
		}
		int checksum = 0;
		for (byte b : block) {
			checksum += b & 0xFF;
		}
		write(block, 148, String.format("%06o", checksum));
		return block;
	}

	/**
	 * Build a ustar header block for a symbolic link entry.
	 */
	private static byte[] symlinkHeader(String name, String linkName) {
		byte[] block = header(name, (byte) '2', 0);
		write(block, 157, linkName);
		return recalculateChecksum(block);
	}

	private static void write(byte[] block, int offset, String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		System.arraycopy(bytes, 0, block, offset, bytes.length);
	}
}
