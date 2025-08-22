package com.vaadin.plugin.hotswap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/**
 * Manages the Hotswap Agent installation and updates. Handles downloading, installing, and version management of
 * hotswap-agent.jar.
 */
public class HotswapAgentManager {

    private static final String HOTSWAP_AGENT_JAR = "hotswap-agent.jar";
    private static final String VAADIN_HOME = ".vaadin";
    private static final String ECLIPSE_PLUGIN_DIR = "eclipse-plugin";

    private static HotswapAgentManager instance;

    private Path vaadinHomePath;
    private Path hotswapAgentPath;

    public static HotswapAgentManager getInstance() {
        if (instance == null) {
            instance = new HotswapAgentManager();
        }
        return instance;
    }

    private HotswapAgentManager() {
        initializePaths();
    }

    private void initializePaths() {
        String userHome = System.getProperty("user.home");
        vaadinHomePath = Paths.get(userHome, VAADIN_HOME, ECLIPSE_PLUGIN_DIR);
        hotswapAgentPath = vaadinHomePath.resolve(HOTSWAP_AGENT_JAR);

        // Create directories if they don't exist
        try {
            Files.createDirectories(vaadinHomePath);
        } catch (IOException e) {
            System.err.println("Failed to create Vaadin home directory: " + e.getMessage());
        }
    }

    /**
     * Get the Hotswap Agent JAR file, installing it if necessary.
     *
     * @return The Hotswap Agent JAR file
     * @throws IOException
     *             if installation fails
     */
    public File getHotswapAgentJar() throws IOException {
        if (!Files.exists(hotswapAgentPath)) {
            installHotswapAgent();
        }
        return hotswapAgentPath.toFile();
    }

    /**
     * Install or update the Hotswap Agent JAR.
     *
     * @return The version of the installed agent, or null if installation failed
     */
    public String installHotswapAgent() {
        try {
            // Get the bundled hotswap-agent.jar from plugin resources
            Bundle bundle = Platform.getBundle("vaadin-eclipse-plugin");
            if (bundle == null) {
                throw new IOException("Could not find vaadin-eclipse-plugin bundle");
            }

            URL resourceUrl = bundle.getEntry("resources/" + HOTSWAP_AGENT_JAR);
            if (resourceUrl == null) {
                throw new IOException("Could not find bundled hotswap-agent.jar");
            }

            // Resolve the URL to get actual file URL
            URL fileUrl = FileLocator.toFileURL(resourceUrl);

            // Check if we need to update
            String bundledVersion = getJarVersion(fileUrl);
            String installedVersion = null;

            if (Files.exists(hotswapAgentPath)) {
                installedVersion = getJarVersion(hotswapAgentPath.toUri().toURL());
            }

            if (installedVersion == null || !installedVersion.equals(bundledVersion)) {
                // Copy the bundled JAR to the installation location
                try (InputStream in = fileUrl.openStream()) {
                    Files.copy(in, hotswapAgentPath, StandardCopyOption.REPLACE_EXISTING);
                }
                System.out.println("Installed Hotswap Agent version: " + bundledVersion);
                return bundledVersion;
            } else {
                System.out.println("Hotswap Agent is up to date: " + installedVersion);
                return installedVersion;
            }

        } catch (Exception e) {
            System.err.println("Failed to install Hotswap Agent: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Check if Hotswap Agent is installed.
     *
     * @return true if the agent JAR exists
     */
    public boolean isInstalled() {
        return Files.exists(hotswapAgentPath);
    }

    /**
     * Get the installation path of Hotswap Agent.
     *
     * @return The path to the hotswap-agent.jar
     */
    public Path getHotswapAgentPath() {
        return hotswapAgentPath;
    }

    /**
     * Get the version of a JAR file from its manifest.
     *
     * @param jarUrl
     *            URL to the JAR file
     * @return The version string, or "unknown" if not found
     */
    private String getJarVersion(URL jarUrl) {
        try {
            // Create a temporary file to read the JAR
            Path tempFile = Files.createTempFile("temp", ".jar");
            try (InputStream in = jarUrl.openStream()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            try (JarFile jarFile = new JarFile(tempFile.toFile())) {
                Manifest manifest = jarFile.getManifest();
                if (manifest != null) {
                    Attributes attrs = manifest.getMainAttributes();
                    String version = attrs.getValue("Implementation-Version");
                    if (version != null) {
                        return version;
                    }
                    version = attrs.getValue("Bundle-Version");
                    if (version != null) {
                        return version;
                    }
                }
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (Exception e) {
            // Ignore and return unknown
        }
        return "unknown";
    }

    /**
     * Get the JVM arguments needed for Hotswap Agent. Returns a formatted string ready for Eclipse VM arguments.
     *
     * @return VM arguments as a single formatted string
     */
    public String getHotswapJvmArgsString() throws IOException {
        File agentJar = getHotswapAgentJar();

        StringBuilder args = new StringBuilder();

        // Add javaagent
        args.append("-javaagent:").append(agentJar.getAbsolutePath()).append(" ");

        // Add JBR-specific flags
        args.append("-XX:+AllowEnhancedClassRedefinition ");
        args.append("-XX:+ClassUnloading ");
        args.append("-XX:HotswapAgent=external ");

        // Add module opens for Java 9+ using space-separated format
        // Eclipse handles this format better than the equals syntax
        args.append("--add-opens").append("java.base/java.lang=ALL-UNNAMED ");
        args.append("--add-opens").append("java.base/java.lang.reflect=ALL-UNNAMED ");
        args.append("--add-opens").append("java.base/java.util=ALL-UNNAMED ");
        args.append("--add-opens").append("java.base/java.util.concurrent=ALL-UNNAMED ");
        args.append("--add-opens").append("java.base/java.util.concurrent.atomic=ALL-UNNAMED ");
        args.append("--add-opens").append("java.base/java.io=ALL-UNNAMED ");
        args.append("--add-opens").append("java.base/java.nio=ALL-UNNAMED ");
        args.append("--add-opens").append("java.base/java.nio.file=ALL-UNNAMED ");
        args.append("--add-opens").append("java.base/sun.nio.ch=ALL-UNNAMED ");
        args.append("--add-opens").append("java.base/sun.nio.fs=ALL-UNNAMED ");
        args.append("--add-opens").append("java.base/sun.net.www.protocol.http=ALL-UNNAMED ");
        args.append("--add-opens").append("java.base/sun.net.www.protocol.https=ALL-UNNAMED ");
        args.append("--add-opens").append("java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED ");
        args.append("--add-opens").append("java.base/java.time=ALL-UNNAMED ");
        args.append("--add-opens").append("java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED ");
        args.append("--add-opens").append("java.management/sun.management=ALL-UNNAMED ");
        args.append("--add-opens").append("jdk.management/com.sun.management.internal=ALL-UNNAMED ");

        // Spring Boot specific
        args.append("-Dspring.devtools.restart.enabled=false ");
        args.append("-Dspring.devtools.restart.quiet-period=0 ");
        args.append("-Dspring.context.lazy-init.enabled=false");

        return args.toString().trim();
    }

    /**
     * Get the JVM arguments needed for Hotswap Agent.
     *
     * @return Array of JVM arguments
     * @deprecated Use getHotswapJvmArgsString() instead for better Eclipse compatibility
     */
    @Deprecated
    public String[] getHotswapJvmArgs() throws IOException {
        File agentJar = getHotswapAgentJar();

        return new String[] { "-javaagent:" + agentJar.getAbsolutePath(), "-XX:+AllowEnhancedClassRedefinition",
                "-XX:+ClassUnloading", "-XX:HotswapAgent=external",
                // Add module opens for Java 9+ - use = syntax to keep as single arguments
                "--add-opens=java.base/java.lang=ALL-UNNAMED", "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED", "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
                "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED", "--add-opens=java.base/java.nio=ALL-UNNAMED",
                "--add-opens=java.base/java.nio.file=ALL-UNNAMED", "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
                "--add-opens=java.base/sun.nio.fs=ALL-UNNAMED",
                "--add-opens=java.base/sun.net.www.protocol.http=ALL-UNNAMED",
                "--add-opens=java.base/sun.net.www.protocol.https=ALL-UNNAMED",
                "--add-opens=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED",
                "--add-opens=java.base/java.time=ALL-UNNAMED",
                "--add-opens=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED",
                "--add-opens=java.management/sun.management=ALL-UNNAMED",
                "--add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED",
                // Spring Boot specific
                "-Dspring.devtools.restart.enabled=false", "-Dspring.devtools.restart.quiet-period=0",
                "-Dspring.context.lazy-init.enabled=false" };
    }
}
