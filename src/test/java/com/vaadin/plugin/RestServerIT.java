package com.vaadin.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RestServerIT {
    private RestServer server;

    @BeforeEach
    public void setUp() throws IOException {
        server = new RestServer();
        server.start();
    }

    @AfterEach
    public void tearDown() {
        server.stop();
    }

    @Test
    public void testServerResponds() throws Exception {
        URL url = new URL("http://localhost:" + server.getPort() + "/api/echo");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        try (OutputStream os = connection.getOutputStream()) {
            os.write("{}".getBytes(StandardCharsets.UTF_8));
        }
        int code = connection.getResponseCode();
        assertEquals(200, code);
        String body = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(body.contains("ok"));
    }
}
