package com.vaadin.plugin.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import elemental.json.Json;
import elemental.json.JsonException;
import elemental.json.JsonObject;

public final class VaadinHomeUtil {

    private static final String PROPERTY_USER_HOME = "user.home";
    private static final String VAADIN_FOLDER_NAME = ".vaadin";
    private static final String HOTSWAP_AGENT_JAR_FILE_NAME = "hotswap-agent.jar";

    private VaadinHomeUtil() {
        /* no instances */ }

    /**
     * Get Vaadin home directory.
     *
     * @return File instance for Vaadin home folder. Does not check if the folder exists.
     */
    public static File resolveVaadinHomeDirectory() {
        String userHome = System.getProperty(PROPERTY_USER_HOME);
        return new File(userHome, VAADIN_FOLDER_NAME);
    }

    public static String getUserKey() throws IOException {
        File vaadinHome = resolveVaadinHomeDirectory();
        File userKeyFile = new File(vaadinHome, "userKey");
        if (userKeyFile.exists()) {
            try {
                String content = Files.readString(userKeyFile.toPath());
                return Json.parse(content).getString("key");
            } catch (JsonException ex) {
                // fix for invalid JSON regression
                // fall through to regenerate
                // noinspection ResultOfMethodCallIgnored
                userKeyFile.delete();
            }
        }

        String key = "user-" + UUID.randomUUID();
        JsonObject keyObject = Json.createObject();
        keyObject.put("key", key);
        Files.createDirectories(vaadinHome.toPath());
        Files.write(userKeyFile.toPath(), keyObject.toJson().getBytes(Charset.defaultCharset()));
        return key;
    }

    /**
     * Gets the hotswap-agent.jar location in ~/.vaadin.
     *
     * @return the hotswap-agent.jar file
     */
    public static File getHotSwapAgentJar() {
        File jar = getHotSwapAgentJarFile();
        // might only happen if user removes hotswap-agent.jar manually after plugin is
        // already installed
        if (!jar.exists()) {
            updateOrInstallHotSwapJar();
        }
        return jar;
    }

    /**
     * Installs or updates hotswap-agent.jar in ~/.vaadin
     *
     * @return version of installed hotswap-agent.jar or null in case of error
     */
    public static String updateOrInstallHotSwapJar() {
        try {
            InputStream bundledHotswap = VaadinHomeUtil.class.getClassLoader()
                    .getResourceAsStream(HOTSWAP_AGENT_JAR_FILE_NAME);

            if (bundledHotswap == null) {
                throw new IllegalStateException("The plugin package is broken: no hotswap-agent.jar found");
            }

            File target = getHotSwapAgentJarFile();
            Path targetPath = target.toPath();

            if (!target.exists()) {
                try {
                    // Create parent directories if they don't exist
                    Files.createDirectories(targetPath.getParent());

                    try (InputStream in = bundledHotswap) {
                        Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    String version = getHotswapAgentVersion(target);
                    VaadinPluginLog.info("Installed hotswap-agent.jar version: " + version);
                    return version;
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to copy hotswap-agent.jar to " + target.getAbsolutePath(),
                            e);
                }
            } else if (isBundledVersionNewer()) {
                try (InputStream in = VaadinHomeUtil.class.getClassLoader()
                        .getResourceAsStream(HOTSWAP_AGENT_JAR_FILE_NAME)) {
                    if (in == null) {
                        throw new IllegalStateException("Unable to update hotswap-agent.jar: resource not found");
                    }
                    Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    String version = getHotswapAgentVersion(target);
                    VaadinPluginLog.info("Updated hotswap-agent.jar to version " + version);
                    return version;
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to update hotswap-agent.jar", e);
                }
            } else {
                String version = getHotswapAgentVersion(target);
                VaadinPluginLog.info("Using existing hotswap-agent.jar version " + version);
                return version;
            }
        } catch (Exception e) {
            VaadinPluginLog.error(e.getMessage(), e);
            return null;
        }
    }

    private static boolean isBundledVersionNewer() {
        String current = getHotswapAgentVersion(getHotSwapAgentJarFile());
        String bundled = getBundledHotswapAgentVersion();
        if (bundled != null && current != null) {
            return bundled.compareTo(current) == 1;
        }
        return false;
    }

    private static String getBundledHotswapAgentVersion() {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("bundled-hotswap-agent", ".jar");
            InputStream in = VaadinHomeUtil.class.getClassLoader().getResourceAsStream(HOTSWAP_AGENT_JAR_FILE_NAME);
            if (in == null) {
                throw new IllegalStateException("Unable to copy hotswap-agent.jar to temporary file ");
            }
            try (InputStream src = in) {
                Files.copy(src, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return getHotswapAgentVersion(tempFile.toFile());
        } catch (IOException e) {
            VaadinPluginLog.error(e.getMessage(), e);
            return null;
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    VaadinPluginLog.error("Failed to delete temporary file: " + tempFile, e);
                }
            }
        }
    }

    private static String getHotswapAgentVersion(File file) {
        try (JarFile jarFile = new JarFile(file)) {
            var entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if ("version.properties".equals(entry.getName())) {
                    Properties properties = new Properties();
                    try (var is = jarFile.getInputStream(entry)) {
                        properties.load(is);
                    }
                    String version = properties.getProperty("version");
                    if (version != null) {
                        int dash = version.indexOf('-');
                        if (dash != -1) {
                            version = version.substring(0, dash);
                        }
                    }
                    return version;
                }
            }
            return null;
        } catch (IOException e) {
            VaadinPluginLog.error(e.getMessage(), e);
            return null;
        }
    }

    private static File getIntellijFolder() {
        return new File(resolveVaadinHomeDirectory(), "intellij-plugin");
    }

    private static File getHotSwapAgentJarFile() {
        return new File(getIntellijFolder(), HOTSWAP_AGENT_JAR_FILE_NAME);
    }
}
