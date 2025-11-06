package com.vaadin.plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.core.runtime.Platform;

import com.google.gson.Gson;
import com.vaadin.plugin.util.VaadinPluginLog;

public class TelemetryService {

    private static final String AMPLITUDE_API_URL = "https://api2.amplitude.com/2/httpapi";
    private static final String API_KEY = "5332f8777b8ce7f12dcbf6c9d749488d";
    private static final String SESSION_ID_KEY = "com.vaadin.plugin.sessionId";

    private static TelemetryService instance;
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final Gson gson;
    private final String userId;
    private final long sessionId;

    private TelemetryService() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Amplitude-Telemetry");
            t.setDaemon(true);
            return t;
        });
        this.gson = new Gson();
        this.userId = getUserId();
        this.sessionId = System.currentTimeMillis();
    }

    public static synchronized TelemetryService getInstance() {
        if (instance == null) {
            instance = new TelemetryService();
        }
        return instance;
    }

    private String getUserId() {
        String workspaceId = System.getProperty("user.name", "unknown");
        String instanceId = System.getProperty(SESSION_ID_KEY);
        if (instanceId == null) {
            instanceId = UUID.randomUUID().toString();
            System.setProperty(SESSION_ID_KEY, instanceId);
        }
        return workspaceId + "-" + instanceId;
    }

    public void trackEvent(String eventName, Map<String, Object> properties) {
        executor.submit(() -> {
            try {
                sendEvent(eventName, properties);
            } catch (Exception e) {
                VaadinPluginLog.info("Failed to send telemetry event: " + e.getMessage());
            }
        });
    }

    private void sendEvent(String eventName, Map<String, Object> properties) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("user_id", userId);
            eventData.put("event_type", eventName);
            eventData.put("time", System.currentTimeMillis());
            eventData.put("session_id", sessionId);
            eventData.put("platform", "Eclipse");

            Map<String, Object> eventProperties = new HashMap<>();
            eventProperties.put("eclipse_version", Platform.getProduct().getDefiningBundle().getVersion().toString());
            eventProperties.put("java_version", System.getProperty("java.version"));
            eventProperties.put("os", System.getProperty("os.name"));
            eventProperties.put("os_version", System.getProperty("os.version"));
            if (properties != null) {
                eventProperties.putAll(properties);
            }
            eventData.put("event_properties", eventProperties);

            Map<String, Object> payload = new HashMap<>();
            payload.put("api_key", API_KEY);
            payload.put("events", new Object[] { eventData });

            String jsonPayload = gson.toJson(payload);

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(AMPLITUDE_API_URL))
                    .header("Content-Type", "application/json").header("Accept", "*/*").timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload)).build();

            CompletableFuture<HttpResponse<String>> responseFuture = httpClient.sendAsync(request,
                    HttpResponse.BodyHandlers.ofString());

            responseFuture.thenAccept(response -> {
                if (response.statusCode() != 200) {
                    VaadinPluginLog.info("Amplitude API returned status: " + response.statusCode());
                }
            }).exceptionally(ex -> {
                VaadinPluginLog.info("Failed to send telemetry: " + ex.getMessage());
                return null;
            });

        } catch (Exception e) {
            VaadinPluginLog.info("Error creating telemetry event: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}
