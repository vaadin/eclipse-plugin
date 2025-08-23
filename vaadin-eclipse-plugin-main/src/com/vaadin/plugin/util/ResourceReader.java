package com.vaadin.plugin.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Utility class to demonstrate how deployed applications can read the flow-build-info.json resource from the classpath.
 *
 * The flow-build-info.json file is automatically updated in META-INF/VAADIN/config during build, making it available as
 * a classpath resource.
 */
public class ResourceReader {

    private static final String FLOW_BUILD_INFO_RESOURCE = "/META-INF/VAADIN/config/flow-build-info.json";

    /**
     * Reads the content of flow-build-info.json from the classpath.
     *
     * @return The content of flow-build-info.json, or null if not found
     */
    public static String readFlowBuildInfo() {
        try (InputStream is = ResourceReader.class.getResourceAsStream(FLOW_BUILD_INFO_RESOURCE)) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    return reader.lines().collect(Collectors.joining("\n"));
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read flow-build-info.json resource: " + e.getMessage());
        }
        return null;
    }

    /**
     * Example usage from within a deployed application:
     *
     * <pre>
     * String flowBuildInfo = ResourceReader.readFlowBuildInfo();
     * if (flowBuildInfo != null) {
     *     System.out.println("Flow build info: " + flowBuildInfo);
     *     // Parse JSON to get npmFolder value
     * }
     * </pre>
     */
    public static void exampleUsage() {
        String content = readFlowBuildInfo();
        if (content != null) {
            System.out.println("flow-build-info.json content: " + content);
        } else {
            System.out.println("flow-build-info.json resource not found in classpath");
        }
    }
}
