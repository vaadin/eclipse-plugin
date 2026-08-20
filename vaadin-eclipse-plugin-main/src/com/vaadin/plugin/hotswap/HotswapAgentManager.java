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

import com.vaadin.plugin.util.VaadinPluginLog;

/**
 * Manages the Hotswap Agent installation and updates. Handles downloading, installing, and version management of
 * hotswap-agent.jar.
 */
public class HotswapAgentManager {

    private static final String HOTSWAP_AGENT_JAR = "hotswap-agent.jar";
    private static final String VAADIN_HOME = ".vaadin";
    private static final String ECLIPSE_PLUGIN_DIR = "eclipse-plugin";

    // Single source of truth for module/package opens. Used to render --add-opens=<value>
    // tokens. Must stay single-token (equals form) so Eclipse's whitespace-based argument
    // tokenizer cannot re-glue them with adjacent text.
    private static final String[] ADD_OPENS = { "java.base/java.lang", "java.base/java.lang.reflect",
            "java.base/java.util", "java.base/java.util.concurrent", "java.base/java.util.concurrent.atomic",
            "java.base/java.io", "java.base/java.nio", "java.base/java.nio.file", "java.base/sun.nio.ch",
            "java.base/sun.nio.fs", "java.base/sun.net.www.protocol.http", "java.base/sun.net.www.protocol.https",
            "java.base/sun.reflect.generics.reflectiveObjects", "java.base/java.time",
            "java.management/com.sun.jmx.mbeanserver", "java.management/sun.management",
            "jdk.management/com.sun.management.internal" };

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
        installHotswapAgent();
    }

    private void initializePaths() {
        String userHome = System.getProperty("user.home");
        vaadinHomePath = Paths.get(userHome, VAADIN_HOME, ECLIPSE_PLUGIN_DIR);
        hotswapAgentPath = vaadinHomePath.resolve(HOTSWAP_AGENT_JAR);

        // Create directories if they don't exist
        try {
            Files.createDirectories(vaadinHomePath);
        } catch (IOException e) {
            VaadinPluginLog.error("Failed to create Vaadin home directory: " + e.getMessage());
        }
    }

    /**
     * Get the Hotswap Agent JAR file
     *
     * @return The Hotswap Agent JAR file
     */
    public File getHotswapAgentJar() {
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
                VaadinPluginLog.info("Installed Hotswap Agent version: " + bundledVersion);
                return bundledVersion;
            } else {
                VaadinPluginLog.info("Hotswap Agent is up to date: " + installedVersion);
                return installedVersion;
            }

        } catch (Exception e) {
            VaadinPluginLog.error("Failed to install Hotswap Agent: " + e.getMessage(), e);
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
     * @param withJbrFlags
     *            include JBR-only flags (-XX:+AllowEnhancedClassRedefinition, -XX:+ClassUnloading,
     *            -XX:HotswapAgent=external). Stock OpenJDK rejects these at startup, so pass false when launching
     *            without JBR.
     * @return VM arguments as a single formatted string
     */
    public String getHotswapJvmArgsString(boolean withJbrFlags) throws IOException {
        File agentJar = getHotswapAgentJar();

        StringBuilder args = new StringBuilder();

        // Quote the agent path so paths containing spaces (e.g. "C:\Program Files\...") survive
        // Eclipse's whitespace-based argument tokenization.
        args.append("-javaagent:\"").append(agentJar.getAbsolutePath()).append("\" ");

        if (withJbrFlags) {
            args.append("-XX:+AllowEnhancedClassRedefinition ");
            args.append("-XX:+ClassUnloading ");
            args.append("-XX:HotswapAgent=external ");
        }

        // Single-token equals form: --add-opens=<module>/<package>=ALL-UNNAMED. The
        // space-separated form would be re-glued with the next token by Eclipse's tokenizer.
        for (String open : ADD_OPENS) {
            args.append("--add-opens=").append(open).append("=ALL-UNNAMED ");
        }

        // Spring Boot specific
        args.append("-Dspring.devtools.restart.enabled=false ");
        args.append("-Dspring.devtools.restart.quiet-period=0 ");
        args.append("-Dspring.context.lazy-init.enabled=false");

        return args.toString().trim();
    }
}
