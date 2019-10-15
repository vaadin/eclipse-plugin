package com.vaadin.integration.eclipse.flow.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.vaadin.integration.eclipse.flow.FlowPlugin;
import com.vaadin.integration.eclipse.flow.pref.JavaPreferenceHandler;
import com.vaadin.integration.eclipse.flow.pref.JavaPreferenceKey;
import com.vaadin.integration.eclipse.flow.util.LogUtil;
import com.vaadin.pro.licensechecker.LocalProKey;
import com.vaadin.pro.licensechecker.ProKey;

public class AmplitudeService {
    private static final String url = "https://api.amplitude.com/httpapi";

    private static final String devKey = "1f0ca7c3a9b8e1b171631eaa30eef10a";
    private static final String prodKey = "87bd4e7f802835b87ef07ad7bd763a87";

    private static final String platform = "Desktop, Eclipse";

    private static final String apiKeyParam = "api_key";
    private static final String eventParam = "event";
    private static final String userIdParam = "user_id";
    private static final String deviceIdParam = "device_id";
    private static final String eventTypeParam = "event_type";

    private final static String eventPropsParam = "event_properties";

    private final static String appVersionParam = "app_version";
    private final static String platformParam = "platform";
    private final static String osNameParam = "os_name";
    private final static String osVersionParam = "os_version";

    public static boolean sendTracking(String eventData) {
        if (eventData == null) {
            return false;
        }

        try {
            List<NameValuePair> body = createBody(eventData);
            HttpResponse response = Request.Post(url).bodyForm(body).execute()
                    .returnResponse();
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException(
                        EntityUtils.toString(response.getEntity()));
            }
            return true;
        } catch (Exception e) {
            LogUtil.handleBackgroundException(
                    "Error happened while sending analytics to Amplitude", e);
            return false;
        }
    }

    public static String generateEventData(String eventType,
            JsonObject eventProps) {
        try {
            JsonObject event = new JsonObject();

            String idName = userIdParam;
            String idVal = getUserId();
            if (idVal == null) {
                idName = deviceIdParam;
                idVal = getDeviceId();
            }

            event.addProperty(idName, encode(idVal));

            event.addProperty(appVersionParam, FlowPlugin.getVersion());
            event.addProperty(platformParam, platform);
            event.addProperty(osNameParam, System.getProperty("os.name"));
            event.addProperty(osVersionParam,
                    ", " + System.getProperty("os.version") + ", "
                            + System.getProperty("os.arch"));
            event.addProperty(eventTypeParam, eventType);

            if (eventProps != null) {
                event.add(eventPropsParam, eventProps);
            }

            JsonArray events = new JsonArray();
            events.add(event);
            return events.toString();
        } catch (NoSuchAlgorithmException e) {
            LogUtil.handleBackgroundException(
                    "Error happened while hashing user proKey", e);
            return null;
        }
    }

    private static List<NameValuePair> createBody(String eventData)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(apiKeyParam,
                FlowPlugin.prodMode() ? prodKey : devKey));
        params.add(new BasicNameValuePair(eventParam, eventData));
        return params;
    }

    private static String getDeviceId() {
        String id = JavaPreferenceHandler.getStringValue(JavaPreferenceKey.ID);
        if (id.isEmpty()) {
            id = UUID.randomUUID().toString();
            JavaPreferenceHandler.saveStringValue(JavaPreferenceKey.ID, id);
        }
        return id;
    }

    private static String getUserId() {
        ProKey proKey = LocalProKey.get();
        return proKey != null ? proKey.getProKey() : null;
    }

    private static String encode(String id) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        StringBuilder hexString = new StringBuilder();
        for (byte b : digest.digest(id.getBytes(StandardCharsets.UTF_8))) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                hexString.append("0");
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
