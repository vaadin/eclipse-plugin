package com.vaadin.plugin.hotswap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMStandin;

import com.vaadin.plugin.hotswap.JbrSelector.JbrCandidate;
import com.vaadin.plugin.util.VaadinPluginLog;

/**
 * Manages JetBrains Runtime (JBR) installation and configuration. JBR is required for enhanced class redefinition
 * support with Hotswap Agent.
 */
public class JetBrainsRuntimeManager {

    private static final String JBR_VENDOR = "JetBrains";
    private static final String JBR_NAME_PREFIX = "JetBrains Runtime";
    private static final String VAADIN_HOME = ".vaadin";
    private static final String ECLIPSE_PLUGIN_DIR = "eclipse-plugin";
    private static final String JBR_DIR = "jbr";

    /** Shared with the Vaadin plugins for other IDEs so a runtime is downloaded only once. */
    private static final String JDK_DIR = "jdk";

    /** Maximum directory depth searched for a Java home inside a downloaded runtime. */
    private static final int MAX_JAVA_HOME_DEPTH = 3;

    // Known broken JBR version, enhanced class redefinition crashes the VM
    private static final String BROKEN_JBR_VERSION = "21.0.4+13-b509.17";

    private static JetBrainsRuntimeManager instance;

    private Path vaadinHomePath;
    private Path jdkInstallPath;
    private Path jbrInstallPath;

    // Cache to avoid forking `java -version` once per registered VM per launch click. Keyed by
    // install location since that uniquely identifies a JVM on disk.
    private final Map<File, JbrCandidate> candidateCache = new HashMap<>();

    public static JetBrainsRuntimeManager getInstance() {
        if (instance == null) {
            instance = new JetBrainsRuntimeManager();
        }
        return instance;
    }

    private JetBrainsRuntimeManager() {
        initializePaths();
    }

    private void initializePaths() {
        String userHome = System.getProperty("user.home");
        vaadinHomePath = Paths.get(userHome, VAADIN_HOME);
        jdkInstallPath = vaadinHomePath.resolve(JDK_DIR);
        jbrInstallPath = vaadinHomePath.resolve(ECLIPSE_PLUGIN_DIR).resolve(JBR_DIR);

        // Create directories if they don't exist
        try {
            Files.createDirectories(jdkInstallPath);
            Files.createDirectories(jbrInstallPath);
        } catch (IOException e) {
            VaadinPluginLog.error("Failed to create JBR directory: " + e.getMessage());
        }
    }

    /**
     * Get the directory downloaded JetBrains Runtimes are installed into.
     *
     * @return The JDK installation directory
     */
    public Path getJdkInstallPath() {
        return jdkInstallPath;
    }

    /**
     * Check if a JVM is JetBrains Runtime.
     *
     * @param vmInstall
     *            The JVM installation to check
     * @return true if it's JBR
     */
    public boolean isJetBrainsRuntime(IVMInstall vmInstall) {
        if (vmInstall == null) {
            return false;
        }

        String name = vmInstall.getName();
        if (name != null && name.contains("JetBrains")) {
            return true;
        }

        // The release file identifies the vendor without starting a process
        String implementor = getReleaseProperty(vmInstall.getInstallLocation(), "IMPLEMENTOR");
        if (implementor != null) {
            return implementor.contains(JBR_VENDOR);
        }

        // Fall back to running java -version
        File javaExecutable = getJavaExecutable(vmInstall);
        if (javaExecutable != null && javaExecutable.exists()) {
            try {
                ProcessBuilder pb = new ProcessBuilder(javaExecutable.getAbsolutePath(), "-version");
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("JBR") || line.contains("JetBrains")) {
                            return true;
                        }
                    }
                }

                process.waitFor();
            } catch (Exception e) {
                // Ignore
            }
        }

        return false;
    }

    /**
     * Check if a JBR version is the known broken version.
     *
     * @param vmInstall
     *            The JVM installation to check
     * @return true if it's the broken version
     */
    public boolean isBrokenJBR(IVMInstall vmInstall) {
        if (vmInstall == null || vmInstall.getInstallLocation() == null) {
            return false;
        }

        if (!isJetBrainsRuntime(vmInstall)) {
            return false;
        }

        String runtimeVersion = getReleaseProperty(vmInstall.getInstallLocation(), "JAVA_RUNTIME_VERSION");
        return runtimeVersion != null && runtimeVersion.contains(BROKEN_JBR_VERSION);
    }

    /**
     * Find any compatible JBR (or fall-back JDK) without a version constraint. Equivalent to
     * {@link #findCompatibleJBR(int)} with required major == 0.
     *
     * @return The selected installation, or null if none is compatible
     */
    public IVMInstall findInstalledJBR() {
        return findCompatibleJBR(0);
    }

    /**
     * Find the best compatible JBR (or fall-back JDK) for a given required Java major version. See {@link JbrSelector}
     * for the selection rules.
     *
     * @param requiredMajor
     *            the project's required Java major version (e.g. 21, 25). Pass 0 to disable version filtering.
     * @return The selected installation, or null if none is compatible
     */
    public IVMInstall findCompatibleJBR(int requiredMajor) {
        return findCompatibleCandidate(requiredMajor).map(JbrCandidate::vm).orElse(null);
    }

    /**
     * Same selection as {@link #findCompatibleJBR(int)}, but returns the full candidate so callers can tell a real JBR
     * from a non-JBR fall-back JDK. {@link JbrSelector} falls back to plain JDKs when no compatible JBR exists, so a
     * non-empty result does <em>not</em> imply JBR — callers that emit JBR-only JVM flags must check
     * {@link JbrCandidate#isJbr()}.
     *
     * @param requiredMajor
     *            the project's required Java major version (e.g. 21, 25). Pass 0 to disable version filtering.
     * @return the selected candidate, or empty if none is compatible
     */
    public Optional<JbrCandidate> findCompatibleCandidate(int requiredMajor) {
        return JbrSelector.select(collectCandidates(), requiredMajor);
    }

    private List<JbrCandidate> collectCandidates() {
        List<JbrCandidate> candidates = new ArrayList<>();
        for (IVMInstallType type : JavaRuntime.getVMInstallTypes()) {
            for (IVMInstall vm : type.getVMInstalls()) {
                JbrCandidate c = toCandidate(vm);
                if (c != null) {
                    candidates.add(c);
                }
            }
        }

        // Scan ~/.vaadin/eclipse-plugin/jbr/ (legacy) and ~/.vaadin/jdk/ (shared with VS Code) for
        // installations not yet registered with Eclipse, register them, and add them as candidates.
        for (Path scanDir : List.of(jbrInstallPath, jdkInstallPath)) {
            File[] jbrDirs = scanDir.toFile().listFiles(File::isDirectory);
            if (jbrDirs == null) {
                continue;
            }
            for (File jbrDir : jbrDirs) {
                File javaHome = findJavaHome(jbrDir);
                if (javaHome == null || !isValidJavaHome(javaHome)) {
                    continue;
                }
                boolean alreadyKnown = candidates.stream()
                        .anyMatch(c -> c.vm() != null && javaHome.equals(c.vm().getInstallLocation()));
                if (alreadyKnown) {
                    continue;
                }
                IVMInstall jbr = registerJBR(javaHome);
                if (jbr != null) {
                    JbrCandidate c = toCandidate(jbr);
                    if (c != null) {
                        candidates.add(c);
                    }
                }
            }
        }

        return candidates;
    }

    private JbrCandidate toCandidate(IVMInstall vm) {
        File location = vm.getInstallLocation();
        if (location == null) {
            return null;
        }
        JbrCandidate cached = candidateCache.get(location);
        if (cached != null && cached.vm() == vm) {
            return cached;
        }
        String fullVersion = getFullVersion(vm);
        int major = JbrSelector.parseMajor(fullVersion);
        boolean isJbr = isJetBrainsRuntime(vm);
        boolean broken = isBrokenJBR(vm);
        JbrCandidate candidate = new JbrCandidate(vm, major, isJbr, broken, fullVersion);
        candidateCache.put(location, candidate);
        return candidate;
    }

    /**
     * Resolve the Java version of a VM, preferring Eclipse's cached value over a fresh fork of {@code java -version}.
     */
    private String getFullVersion(IVMInstall vm) {
        if (vm instanceof IVMInstall2 vm2) {
            String v = vm2.getJavaVersion();
            if (v != null && !v.isEmpty()) {
                return v;
            }
        }
        return getJavaVersion(vm);
    }

    /**
     * Download the latest JetBrains Runtime for this platform, install it into the Vaadin home directory and register
     * it with Eclipse. This must be called from a background job or a progress dialog, it blocks while downloading a
     * few hundred megabytes.
     *
     * @param monitor
     *            Progress monitor, may be null
     * @return The installed JBR
     * @throws IOException
     *             if the runtime cannot be downloaded, extracted or registered
     * @throws InterruptedException
     *             if the download is interrupted
     */
    public IVMInstall downloadAndInstallJBR(IProgressMonitor monitor) throws IOException, InterruptedException {
        Path installed = JetBrainsRuntimeDownloader.downloadLatestJBR(jdkInstallPath, monitor);

        File javaHome = findJavaHome(installed.toFile());
        if (javaHome == null) {
            throw new IOException("Downloaded JetBrains Runtime does not contain a Java home: " + installed);
        }

        IVMInstall jbr = registerJBR(javaHome);
        if (jbr == null) {
            throw new IOException("Failed to register the downloaded JetBrains Runtime with Eclipse");
        }
        return jbr;
    }

    /**
     * Register a JBR installation with Eclipse, reusing an existing entry for the same location.
     *
     * @param javaHome
     *            The Java home directory
     * @return The registered JVM installation
     */
    private IVMInstall registerJBR(File javaHome) {
        try {
            IVMInstall existing = findRegisteredVM(javaHome);
            if (existing != null) {
                return existing;
            }

            IVMInstallType vmType = JavaRuntime
                    .getVMInstallType("org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType");
            if (vmType == null) {
                return null;
            }

            // Generate a unique ID
            String id = "jbr_" + System.currentTimeMillis();

            // Create VM standin
            VMStandin standin = new VMStandin(vmType, id);
            standin.setName(JBR_NAME_PREFIX + " " + getJavaVersion(javaHome));
            standin.setInstallLocation(javaHome);

            // Convert standin to real VM
            IVMInstall vm = standin.convertToRealVM();

            // Save the VM configuration
            JavaRuntime.saveVMConfiguration();

            VaadinPluginLog.info("Registered JetBrains Runtime at " + javaHome);
            return vm;

        } catch (Exception e) {
            VaadinPluginLog.error("Failed to register JBR: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Find a JVM already registered with Eclipse for the given install location.
     *
     * @param installLocation
     *            The Java home directory
     * @return The registered JVM installation, or null if there is none
     */
    private IVMInstall findRegisteredVM(File installLocation) {
        for (IVMInstallType vmType : JavaRuntime.getVMInstallTypes()) {
            for (IVMInstall vm : vmType.getVMInstalls()) {
                if (installLocation.equals(vm.getInstallLocation())) {
                    return vm;
                }
            }
        }
        return null;
    }

    /**
     * Get the Java executable for a VM installation.
     *
     * @param vmInstall
     *            The VM installation
     * @return The Java executable file
     */
    private File getJavaExecutable(IVMInstall vmInstall) {
        if (vmInstall == null) {
            return null;
        }

        File installLocation = vmInstall.getInstallLocation();
        if (installLocation == null) {
            return null;
        }

        // Try standard locations
        File javaExe = new File(installLocation, "bin/java");
        if (!javaExe.exists()) {
            javaExe = new File(installLocation, "bin/java.exe");
        }

        return javaExe.exists() ? javaExe : null;
    }

    /**
     * Get the Java version string for a VM installation.
     *
     * @param vmInstall
     *            The VM installation
     * @return The version string
     */
    private String getJavaVersion(IVMInstall vmInstall) {
        File javaExe = getJavaExecutable(vmInstall);
        if (javaExe == null) {
            return null;
        }

        return getJavaVersion(javaExe.getParentFile().getParentFile());
    }

    /**
     * Get the Java version from a Java home directory.
     *
     * @param javaHome
     *            The Java home directory
     * @return The version string
     */
    private String getJavaVersion(File javaHome) {
        String version = getReleaseProperty(javaHome, "JAVA_VERSION");
        if (version != null) {
            return version;
        }

        try {
            File javaExe = new File(javaHome, "bin/java");
            if (!javaExe.exists()) {
                javaExe = new File(javaHome, "bin/java.exe");
            }

            if (!javaExe.exists()) {
                return null;
            }

            ProcessBuilder pb = new ProcessBuilder(javaExe.getAbsolutePath(), "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Parse version from output like: openjdk version "21.0.1" 2023-10-17 LTS
                    Pattern pattern = Pattern.compile("version \"([^\"]+)\"");
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                }
            }

            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Ignore
        }

        return null;
    }

    /**
     * Read a property from the release file of a Java home.
     *
     * @param javaHome
     *            The Java home directory
     * @param key
     *            The property name
     * @return The property value without surrounding quotes, or null if unavailable
     */
    private String getReleaseProperty(File javaHome, String key) {
        if (javaHome == null) {
            return null;
        }

        Path release = javaHome.toPath().resolve("release");
        if (!Files.isRegularFile(release)) {
            return null;
        }

        try {
            for (String line : Files.readAllLines(release, StandardCharsets.UTF_8)) {
                if (line.startsWith(key + "=")) {
                    return line.substring(key.length() + 1).replace("\"", "").trim();
                }
            }
        } catch (IOException e) {
            VaadinPluginLog.debug("Could not read " + release + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Find the Java home directory within a JBR installation. The runtime archives unpack into a directory of their own
     * and put the Java home below Contents/Home on macOS, so the layout differs per platform and release.
     *
     * @param jbrDir
     *            The JBR installation directory
     * @return The Java home directory, or null if not found
     */
    public File findJavaHome(File jbrDir) {
        return findJavaHome(jbrDir, MAX_JAVA_HOME_DEPTH);
    }

    private File findJavaHome(File jbrDir, int remainingDepth) {
        if (jbrDir == null || remainingDepth < 0) {
            return null;
        }

        // Check if it's already a Java home
        if (isValidJavaHome(jbrDir)) {
            return jbrDir;
        }

        // Check Contents/Home on macOS
        File contentsHome = new File(jbrDir, "Contents/Home");
        if (isValidJavaHome(contentsHome)) {
            return contentsHome;
        }

        // Check jbr subdirectory
        File jbrSubdir = new File(jbrDir, "jbr");
        if (isValidJavaHome(jbrSubdir)) {
            return jbrSubdir;
        }

        // Archives unpack into a directory of their own, descend into it
        File[] children = jbrDir.listFiles(File::isDirectory);
        if (children != null && children.length == 1) {
            return findJavaHome(children[0], remainingDepth - 1);
        }

        return null;
    }

    /**
     * Check if a directory is a valid Java home.
     *
     * @param dir
     *            The directory to check
     * @return true if it's a valid Java home
     */
    private boolean isValidJavaHome(File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            return false;
        }

        File binDir = new File(dir, "bin");
        File javaExe = new File(binDir, "java");
        if (!javaExe.exists()) {
            javaExe = new File(binDir, "java.exe");
        }

        return javaExe.exists();
    }

}
