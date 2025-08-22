package com.vaadin.plugin.hotswap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMStandin;

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

    // Known broken JBR version
    private static final String BROKEN_JBR_VERSION = "21.0.4+13-b509.17";

    private static JetBrainsRuntimeManager instance;

    private Path vaadinHomePath;
    private Path jbrInstallPath;

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
        vaadinHomePath = Paths.get(userHome, VAADIN_HOME, ECLIPSE_PLUGIN_DIR);
        jbrInstallPath = vaadinHomePath.resolve(JBR_DIR);

        // Create directories if they don't exist
        try {
            Files.createDirectories(jbrInstallPath);
        } catch (IOException e) {
            System.err.println("Failed to create JBR directory: " + e.getMessage());
        }
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

        // Check by running java -version
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
        if (!isJetBrainsRuntime(vmInstall)) {
            return false;
        }

        String version = getJavaVersion(vmInstall);
        return BROKEN_JBR_VERSION.equals(version);
    }

    /**
     * Find an installed JetBrains Runtime.
     *
     * @return The JBR installation, or null if not found
     */
    public IVMInstall findInstalledJBR() {
        IVMInstallType[] vmTypes = JavaRuntime.getVMInstallTypes();

        for (IVMInstallType vmType : vmTypes) {
            IVMInstall[] vms = vmType.getVMInstalls();
            for (IVMInstall vm : vms) {
                if (isJetBrainsRuntime(vm) && !isBrokenJBR(vm)) {
                    return vm;
                }
            }
        }

        // Check if JBR is installed in our directory
        File[] jbrDirs = jbrInstallPath.toFile().listFiles(File::isDirectory);
        if (jbrDirs != null) {
            for (File jbrDir : jbrDirs) {
                File javaHome = findJavaHome(jbrDir);
                if (javaHome != null && isValidJavaHome(javaHome)) {
                    // Register this JBR with Eclipse
                    IVMInstall jbr = registerJBR(javaHome);
                    if (jbr != null && !isBrokenJBR(jbr)) {
                        return jbr;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Get a compatible JetBrains Runtime for the given Java version.
     *
     * @param requiredJavaVersion
     *            The required Java version (e.g., "17", "21")
     * @return The compatible JBR, or null if none found
     */
    public IVMInstall getCompatibleJBR(String requiredJavaVersion) {
        IVMInstall jbr = findInstalledJBR();

        if (jbr != null) {
            String jbrVersion = getJavaMajorVersion(jbr);
            if (jbrVersion != null && jbrVersion.equals(requiredJavaVersion)) {
                return jbr;
            }
        }

        return null;
    }

    /**
     * Download and install JetBrains Runtime. This should be called in a background job.
     *
     * @param javaVersion
     *            The Java version to download (e.g., "17", "21")
     * @param monitor
     *            Progress monitor
     * @return The installed JBR, or null if installation failed
     */
    public IVMInstall downloadAndInstallJBR(String javaVersion, IProgressMonitor monitor) {
        try {
            monitor.beginTask("Downloading JetBrains Runtime " + javaVersion, 100);

            // Determine platform
            String os = System.getProperty("os.name").toLowerCase();
            String arch = System.getProperty("os.arch");
            String platform = getPlatformString(os, arch);

            // Construct download URL (this is a simplified version)
            // In reality, you'd need to fetch the actual download URL from JetBrains
            String downloadUrl = getJBRDownloadUrl(javaVersion, platform);

            if (downloadUrl == null) {
                throw new IOException("Could not determine JBR download URL");
            }

            monitor.subTask("Downloading JBR...");
            // Download logic would go here
            // For now, we'll just print a message
            System.out.println("Would download JBR from: " + downloadUrl);

            monitor.worked(50);

            monitor.subTask("Extracting JBR...");
            // Extraction logic would go here

            monitor.worked(40);

            monitor.subTask("Registering JBR with Eclipse...");
            // Registration logic

            monitor.worked(10);

            return null; // Would return the installed JBR

        } catch (Exception e) {
            System.err.println("Failed to download JBR: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            monitor.done();
        }
    }

    /**
     * Register a JBR installation with Eclipse.
     *
     * @param javaHome
     *            The Java home directory
     * @return The registered JVM installation
     */
    private IVMInstall registerJBR(File javaHome) {
        try {
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

            return vm;

        } catch (Exception e) {
            System.err.println("Failed to register JBR: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
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
        } catch (Exception e) {
            // Ignore
        }

        return null;
    }

    /**
     * Get the major Java version (e.g., "17" from "17.0.1").
     *
     * @param vmInstall
     *            The VM installation
     * @return The major version string
     */
    private String getJavaMajorVersion(IVMInstall vmInstall) {
        String fullVersion = getJavaVersion(vmInstall);
        if (fullVersion == null) {
            return null;
        }

        // Extract major version
        String[] parts = fullVersion.split("\\.");
        if (parts.length > 0) {
            // Handle both "1.8.0" and "17.0.1" formats
            if (parts[0].equals("1") && parts.length > 1) {
                return parts[1]; // Java 8 or earlier
            } else {
                return parts[0]; // Java 9+
            }
        }

        return null;
    }

    /**
     * Find the Java home directory within a JBR installation.
     *
     * @param jbrDir
     *            The JBR installation directory
     * @return The Java home directory, or null if not found
     */
    private File findJavaHome(File jbrDir) {
        // Check if it's already a Java home
        if (isValidJavaHome(jbrDir)) {
            return jbrDir;
        }

        // Check Contents/Home on macOS
        File contentsHome = new File(jbrDir, "Contents/Home");
        if (contentsHome.exists() && isValidJavaHome(contentsHome)) {
            return contentsHome;
        }

        // Check jbr subdirectory
        File jbrSubdir = new File(jbrDir, "jbr");
        if (jbrSubdir.exists() && isValidJavaHome(jbrSubdir)) {
            return jbrSubdir;
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

    /**
     * Get the platform string for downloading JBR.
     *
     * @param os
     *            Operating system name
     * @param arch
     *            Architecture
     * @return The platform string
     */
    private String getPlatformString(String os, String arch) {
        String platform = "";

        if (os.contains("win")) {
            platform = "windows";
        } else if (os.contains("mac")) {
            platform = "osx";
        } else if (os.contains("linux")) {
            platform = "linux";
        } else {
            return null;
        }

        if (arch.contains("64")) {
            platform += "-x64";
        } else if (arch.contains("aarch64") || arch.contains("arm64")) {
            platform += "-aarch64";
        } else {
            platform += "-x86";
        }

        return platform;
    }

    /**
     * Get the JBR download URL for a specific version and platform. This is a placeholder - actual implementation would
     * need to fetch the real URL from JetBrains or use a hardcoded mapping.
     *
     * @param javaVersion
     *            The Java version
     * @param platform
     *            The platform string
     * @return The download URL
     */
    private String getJBRDownloadUrl(String javaVersion, String platform) {
        // This would need to be implemented with actual JBR download URLs
        // For now, return a placeholder
        return "https://github.com/JetBrains/JetBrainsRuntime/releases/download/...";
    }
}
