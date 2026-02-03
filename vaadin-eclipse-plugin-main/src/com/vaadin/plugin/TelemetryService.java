package com.vaadin.plugin;

import java.io.IOException;
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
import com.vaadin.plugin.preferences.VaadinPreferencePage;
import com.vaadin.plugin.util.VaadinHomeUtil;
import com.vaadin.plugin.util.VaadinPluginLog;

public class TelemetryService {

    private static final String AMPLITUDE_API_URL = "https://api2.amplitude.com/2/httpapi";
    private static final String API_KEY = "5332f8777b8ce7f12dcbf6c9d749488d";
    private static final String SESSION_ID_KEY = "com.vaadin.plugin.sessionId";

    private static TelemetryService instance;
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final Gson gson;
    private final long sessionId;

    private static volatile String userId = null;
    private static volatile Boolean vaadiner = null;

    private TelemetryService() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Amplitude-Telemetry");
            t.setDaemon(true);
            return t;
        });
        this.gson = new Gson();
        this.sessionId = System.currentTimeMillis();
    }

    public static synchronized TelemetryService getInstance() {
        if (instance == null) {
            instance = new TelemetryService();
        }
        return instance;
    }

    private String getUserId() {
        if (userId == null) {
            synchronized (TelemetryService.class) {
                if (userId == null) {
                    try {
                        userId = VaadinHomeUtil.getUserKey();
                    } catch (IOException e) {
                        userId = "user-" + UUID.randomUUID();
                    }
                }
            }
        }
        return userId;
    }

    private boolean isVaadiner() {
        if (vaadiner == null) {
            synchronized (TelemetryService.class) {
                try {
                    String username = VaadinHomeUtil.getProUsername();
                    vaadiner = username != null && username.endsWith("@vaadin.com");
                } catch (IOException e) {
                    vaadiner = false;
                }
            }
        }
        return vaadiner;
    }

    private String getProKey() {
        return System.getProperty("vaadin.prokey", "");
    }

    public void trackEvent(String eventName, Map<String, Object> properties) {
        if (!isTelemetryEnabled()) {
            return;
        }

        executor.submit(() -> {
            try {
                sendEvent(eventName, properties);
            } catch (Exception e) {
                VaadinPluginLog.info("Failed to send telemetry event: " + e.getMessage());
            }
        });
    }

    private boolean isTelemetryEnabled() {
        try {
            return Activator.getDefault().getPreferenceStore().getBoolean(VaadinPreferencePage.PREF_ENABLE_TELEMETRY);
        } catch (Exception e) {
            return true;
        }
    }

    private void sendEvent(String eventName, Map<String, Object> properties) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("user_id", getUserId());
            eventData.put("event_type", eventName);
            eventData.put("time", System.currentTimeMillis());
            eventData.put("session_id", sessionId);
            eventData.put("platform", "Eclipse");
            eventData.put("os_name", System.getProperty("os.name"));
            eventData.put("os_version", System.getProperty("os.version"));
            eventData.put("eclipse_version", Platform.getProduct().getDefiningBundle().getVersion().toString());
            eventData.put("app_version", Platform.getBundle("vaadin-eclipse-plugin").getVersion().toString());
            eventData.put("device_family", "eclipse");
            Map<String, Object> eventProperties = new HashMap<>();
            eventProperties.put("java_version", System.getProperty("java.version"));
            eventProperties.put("Vaadiner", isVaadiner());
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
