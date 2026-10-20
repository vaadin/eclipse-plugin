package com.vaadin.plugin;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

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
}
