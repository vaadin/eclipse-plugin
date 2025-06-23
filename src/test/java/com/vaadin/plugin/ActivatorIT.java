package com.vaadin.plugin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Integration test that verifies the plugin Activator starts the REST server
 * when invoked directly without an OSGi container.
 */
public class ActivatorIT {

    @Test
    public void activatorStartsServer() throws Exception {
        Activator activator = new Activator();
        activator.start(null);
        int port = activator.getPort();
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

        activator.stop(null);
    }
}
