package com.vaadin.plugin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Starts a small HTTP server for Copilot integration. The server exposes a
 * single <code>/api/copilot</code> endpoint that accepts POST requests.
 */
public class CopilotRestService {
    private HttpServer server;
    private String endpoint;

    /** Start the embedded HTTP server on a random port. */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/copilot", new Handler());
        server.start();
        endpoint = "http://localhost:" + server.getAddress().getPort() + "/api/copilot";
        System.out.println("Copilot REST service started at " + endpoint);
    }

    /** Stop the server if it is running. */
    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    /** Returns the full endpoint URL. */
    public String getEndpoint() {
        return endpoint;
    }

    private static class Handler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("Received Copilot request: " + body);
            byte[] resp = "OK".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        }
    }
}
