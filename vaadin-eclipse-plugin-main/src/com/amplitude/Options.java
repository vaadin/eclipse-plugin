package com.amplitude;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

public class Options {

    /**
     * Minimum length for user ID or device ID value.
     */
    public Integer minIdLength;
    public Map<String, String> headers;

    public Options setMinIdLength(Integer minIdLength) {
        this.minIdLength = minIdLength;

        return this;
    }

    public Options setHeaders(Map<String, String> headers) {
        this.headers = headers;

        return this;
    }

    public Options addHeader(String key, String value) {
        if (this.headers == null) {
            this.headers = new HashMap<>();
        }

        this.headers.put(key, value);

        return this;
    }

    public JsonObject toJsonObject() {
        JsonObject eventOptions = new JsonObject();
        if (minIdLength != null)
            eventOptions.addProperty("min_id_length", minIdLength);
        return eventOptions;
    }
}
