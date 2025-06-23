package com.vaadin.plugin;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Integration test that verifies the plugin Activator is started when running
 * within an OSGi container provided by Tycho.
 */
public class ActivatorIT {

    @Test
    public void activatorStartsServer() throws Exception {
        Bundle bundle = FrameworkUtil.getBundle(Activator.class);
        assertNotNull(bundle, "Bundle should be available");
        assertTrue((bundle.getState() & Bundle.ACTIVE) != 0, "Bundle not active");

        int port = Activator.getDefault().getPort();
        assertTrue(port > 0, "Server port should be positive");

        URL url = new URL("http://localhost:" + port + "/api/echo");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        try (OutputStream os = connection.getOutputStream()) {
            os.write("{}".getBytes(StandardCharsets.UTF_8));
        }
        int code = connection.getResponseCode();
        assertTrue(code == 200, "Server did not respond with 200");
    }
}
