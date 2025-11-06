package com.amplitude;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.amplitude.exception.AmplitudeInvalidAPIKeyException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Response {
    protected int code;
    protected Status status;
    protected String error;
    protected JsonObject successBody;
    protected JsonObject invalidRequestBody;
    protected JsonObject rateLimitBody;

    private static boolean hasInvalidAPIKey(String errorMsg) {
        String invalidAPIKeyError = "Invalid API key: .*";
        return errorMsg.matches(invalidAPIKeyError);
    }

    protected static Response populateResponse(JsonObject json) throws AmplitudeInvalidAPIKeyException {
        Response res = new Response();
        int code = json.get("code").getAsInt();
        Status status = Status.getCodeStatus(code);
        res.code = code;
        res.status = status;
        if (status == Status.SUCCESS) {
            res.successBody = new JsonObject();
            res.successBody.addProperty("eventsIngested", json.get("events_ingested").getAsInt());
            res.successBody.addProperty("payloadSizeBytes", json.get("payload_size_bytes").getAsInt());
            res.successBody.addProperty("serverUploadTime", json.get("server_upload_time").getAsLong());
        } else if (status == Status.INVALID) {
            res.error = Utils.getStringValueWithKey(json, "error");
            if (hasInvalidAPIKey(res.error))
                throw new AmplitudeInvalidAPIKeyException();
            res.invalidRequestBody = new JsonObject();
            res.invalidRequestBody.addProperty("missingField", Utils.getStringValueWithKey(json, "missing_field"));
            JsonObject eventsWithInvalidFields = Utils.getJsonObjectValueWithKey(json, "events_with_invalid_fields");
            res.invalidRequestBody.add("eventsWithInvalidFields", eventsWithInvalidFields);
            JsonObject eventsWithMissingFields = Utils.getJsonObjectValueWithKey(json, "events_with_missing_fields");
            res.invalidRequestBody.add("eventsWithMissingFields", eventsWithMissingFields);
        } else if (status == Status.PAYLOAD_TOO_LARGE) {
            res.error = Utils.getStringValueWithKey(json, "error");
        } else if (status == Status.RATELIMIT) {
            res.error = Utils.getStringValueWithKey(json, "error");
            res.rateLimitBody = new JsonObject();
            res.rateLimitBody.addProperty("epsThreshold", json.get("eps_threshold").getAsInt());
            JsonObject throttledDevices = Utils.getJsonObjectValueWithKey(json, "throttled_devices");
            res.rateLimitBody.add("throttledDevices", throttledDevices);
            JsonObject throttledUsers = Utils.getJsonObjectValueWithKey(json, "throttled_users");
            res.rateLimitBody.add("throttledUsers", throttledUsers);
            int[] throttledEvents = Utils.convertJsonArrayToIntArray(json, "throttled_events");
            // Convert int array to JsonArray for storage
            JsonObject throttledEventsObj = new JsonObject();
            for (int i = 0; i < throttledEvents.length; i++) {
                throttledEventsObj.addProperty(String.valueOf(i), throttledEvents[i]);
            }
            res.rateLimitBody.add("throttledEvents", throttledEventsObj);
            JsonObject exceededDailyQuotaDevices = Utils.getJsonObjectValueWithKey(json,
                    "exceeded_daily_quota_devices");
            res.rateLimitBody.add("exceededDailyQuotaDevices", exceededDailyQuotaDevices);
            JsonObject exceededDailyQuotaUsers = Utils.getJsonObjectValueWithKey(json, "exceeded_daily_quota_users");
            res.rateLimitBody.add("exceededDailyQuotaUsers", exceededDailyQuotaUsers);
        }
        return res;
    }

    protected boolean isUserOrDeviceExceedQuote(String userId, String deviceId) {
        if (status == Status.RATELIMIT && rateLimitBody != null) {
            JsonObject exceededDailyQuotaUsers = rateLimitBody.getAsJsonObject("exceededDailyQuotaUsers");
            JsonObject exceededDailyQuotaDevices = rateLimitBody.getAsJsonObject("exceededDailyQuotaDevices");
            if ((userId != null && userId.length() > 0 && exceededDailyQuotaUsers.has(userId))
                    || (deviceId != null && deviceId.length() > 0 && exceededDailyQuotaDevices.has(deviceId))) {
                return true;
            }
        }
        return false;
    }

    protected int[] collectInvalidEventIndices() {
        if (status == Status.INVALID && invalidRequestBody != null) {
            List<Integer> invalidFieldsIndices = collectIndicesWithRequestBody(invalidRequestBody,
                    "eventsWithInvalidFields");
            List<Integer> missingFieldsIndices = collectIndicesWithRequestBody(invalidRequestBody,
                    "eventsWithMissingFields");
            invalidFieldsIndices.addAll(missingFieldsIndices);
            Collections.sort(invalidFieldsIndices);
            return invalidFieldsIndices.stream().distinct().mapToInt(i -> i).toArray();
        }
        return new int[] {};
    }

    private List<Integer> collectIndicesWithRequestBody(JsonObject requestBody, String key) {
        List<Integer> invalidIndices = new ArrayList<>();
        JsonObject fields = requestBody.getAsJsonObject(key);
        for (Map.Entry<String, JsonElement> entry : fields.entrySet()) {
            if (entry.getValue().isJsonArray()) {
                int[] eventIndices = Utils.jsonArrayToIntArray(entry.getValue().getAsJsonArray());
                for (int eventIndex : eventIndices) {
                    invalidIndices.add(eventIndex);
                }
            }
        }
        Collections.sort(invalidIndices);
        return invalidIndices;
    }

    public String toString() {
        JsonObject json = new JsonObject();
        json.addProperty("code", this.code);
        json.addProperty("status", this.status.name());
        if (this.error != null) {
            json.addProperty("error", this.error);
        }
        if (this.successBody != null) {
            json.add("successBody", this.successBody);
        }
        if (this.invalidRequestBody != null) {
            json.add("invalidRequestBody", this.invalidRequestBody);
        }
        if (this.rateLimitBody != null) {
            json.add("rateLimitBody", this.rateLimitBody);
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(json);
    }
}
