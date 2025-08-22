package com.vaadin.plugin.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Utility class to demonstrate how deployed applications can read the hello.txt resource from the classpath.
 *
 * The hello.txt file is automatically injected into WEB-INF/classes during deployment, making it available as a
 * classpath resource.
 */
public class ResourceReader {

    private static final String HELLO_RESOURCE = "/hello.txt";

    /**
     * Reads the content of hello.txt from the classpath.
     *
     * @return The content of hello.txt, or null if not found
     */
    public static String readHelloResource() {
        try (InputStream is = ResourceReader.class.getResourceAsStream(HELLO_RESOURCE)) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    return reader.lines().collect(Collectors.joining("\n"));
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read hello.txt resource: " + e.getMessage());
        }
        return null;
    }

    /**
     * Example usage from within a deployed application:
     *
     * <pre>
     * String projectPath = ResourceReader.readHelloResource();
     * if (projectPath != null) {
     *     System.out.println("Project deployed from: " + projectPath);
     * }
     * </pre>
     */
    public static void exampleUsage() {
        String content = readHelloResource();
        if (content != null) {
            System.out.println("hello.txt content (project path): " + content);
        } else {
            System.out.println("hello.txt resource not found in classpath");
        }
    }
}
