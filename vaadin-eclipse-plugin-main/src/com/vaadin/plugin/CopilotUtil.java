package com.vaadin.plugin;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import com.vaadin.plugin.util.VaadinPluginLog;

/**
 * Utility class for Copilot integration.
 */
public class CopilotUtil {

    private static final String serviceName = "copilot-" + UUID.randomUUID();

    public static String getServiceName() {
        return serviceName;
    }

    public static String getEndpoint(int port) {
        return "http://127.0.0.1:" + port + "/vaadin/" + getServiceName();
    }

    public static String getSupportedActions() {
        String[] actions = { "write", "writeBase64", "delete", "undo", "redo", "refresh", "showInIde", "getModulePaths",
                "compileFiles", "restartApplication", "getVaadinRoutes", "getVaadinVersion", "getVaadinComponents",
                "getVaadinEntities", "getVaadinSecurity", "reloadMavenModule", "heartbeat" };
        return Arrays.stream(actions).collect(Collectors.joining(","));
    }

    public static void saveDotFile(String projectBasePath, int port) {
        try {
            java.io.File dotFile = new java.io.File(projectBasePath, ".vaadin/copilot/vaadin-copilot.properties");
            dotFile.getParentFile().mkdirs();

            java.util.Properties props = new java.util.Properties();
            props.setProperty("endpoint", getEndpoint(port));
            props.setProperty("ide", "eclipse");
            props.setProperty("version", "1.0.0");
            props.setProperty("supportedActions", getSupportedActions());

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dotFile)) {
                props.store(fos, "Vaadin Copilot Integration Runtime Properties");
            }

            VaadinPluginLog.info("Created copilot dotfile at: " + dotFile.getAbsolutePath());
        } catch (Exception e) {
            VaadinPluginLog.error("Failed to create copilot dotfile: " + e.getMessage(), e);
        }
    }
}
