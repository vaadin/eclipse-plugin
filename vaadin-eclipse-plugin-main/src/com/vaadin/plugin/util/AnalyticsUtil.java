package com.vaadin.plugin.util;

import com.amplitude.ampli.*;
import com.vaadin.pro.licensechecker.LocalProKey;
import com.vaadin.pro.licensechecker.ProKey;

import elemental.json.Json;
import elemental.json.JsonObject;

import java.io.IOException;
import java.util.UUID;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

public final class AnalyticsUtil {

    private static final EventOptions eventOptions = buildEventOptions();

    private static volatile String userId = null;
    private static volatile Boolean vaadiner = null;

    private AnalyticsUtil() {
        // no instances
    }

    private static EventOptions buildEventOptions() {
        EventOptions opts = new EventOptions();
        opts.setPlatform(getEclipseVersion());
        opts.setLanguage(System.getProperty("user.language"));
        opts.setCountry(System.getProperty("user.country"));
        opts.setRegion(System.getProperty("user.region"));
        opts.setOsName(System.getProperty("os.name"));
        opts.setOsVersion(System.getProperty("os.version"));
		opts.setAppVersion(getPluginVersion());
        return opts;
    }

    private static String getUserId() {
        if (userId == null) {
            synchronized (AnalyticsUtil.class) {
                if (userId == null) {
                    try {
                        userId = VaadinHomeUtil.getUserKey();
                    } catch (IOException e) {
                        userId = "user-" + UUID.randomUUID();
                    }
                    LoadOptions loadOptions = new LoadOptions();
                    loadOptions.setEnvironment(Ampli.Environment.IDEPLUGINS);
                    Ampli.getInstance().load(loadOptions);
                    Ampli.getInstance().identify(userId, eventOptions);
                }
            }
        }
        return userId;
    }

    private static boolean isVaadiner() {
        if (vaadiner == null) {
            synchronized (AnalyticsUtil.class) {
                if (vaadiner == null) {
                    ProKey proKey = LocalProKey.get();
                    if (proKey != null) {
                        JsonObject json = (JsonObject) Json.parse(proKey.toJson());
                        if (json.hasKey("username")) {
                            String username = json.getString("username");
                            vaadiner = username != null && username.endsWith("@vaadin.com");
                        } else {
                            vaadiner = false;
                        }
                    } else {
                        vaadiner = false;
                    }
                }
            }
        }
        return vaadiner;
    }

    private static String getProKey() {
        ProKey proKey = LocalProKey.get();
        if (proKey != null) {
        	return proKey.getProKey();
        }
        return null;
    }

    private static boolean isEnabled() {
        // Adjust getters if VaadinSettings exposes them differently
        return true; //VaadinSettings.getInstance().getState().getSendUsageStatistics();
    }
    
    private static String getPluginVersion() {
    	Bundle bundle = Platform.getBundle("com.vaadin.plugin");
    	return bundle.getVersion().toString();
	}
    
    private static String getEclipseVersion() {
		return Platform.getProduct().getDefiningBundle().getVersion().toString();
	}

    // --- Public tracking API ---

    public static void trackPluginInitialized() {
        if (isEnabled()) {
            Ampli.getInstance().pluginInitialized(
                    getUserId(),
                    PluginInitialized.builder().vaadiner(isVaadiner()).proKey(getProKey()).build()
            );
        }
    }

    public static void trackProjectCreated(String downloadUrl) {
        if (isEnabled()) {
            Ampli.getInstance().projectCreated(
                    getUserId(),
                    ProjectCreated.builder().vaadiner(isVaadiner()).downloadUrl(downloadUrl).build()
            );
        }
    }

    public static void trackManualCopilotRestart() {
        if (isEnabled()) {
            Ampli.getInstance().manualCopilotRestart(
                    getUserId(),
                    ManualCopilotRestart.builder().vaadiner(isVaadiner()).build()
            );
        }
    }

    public static void trackDebugWithHotswap() {
        if (isEnabled()) {
            Ampli.getInstance().debugWithHotswap(
                    getUserId(),
                    DebugWithHotswap.builder().vaadiner(isVaadiner()).build()
            );
        }
    }
}
