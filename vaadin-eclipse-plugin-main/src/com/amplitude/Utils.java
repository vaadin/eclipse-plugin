package com.amplitude;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Utils {
    public static String getStringValueWithKey(JsonObject json, String key) {
        JsonElement element = json.get(key);
        return (element != null && !element.isJsonNull() && element.isJsonPrimitive()) ? element.getAsString() : "";
    }

    public static JsonObject getJsonObjectValueWithKey(JsonObject json, String key) {
        JsonElement element = json.get(key);
        return (element != null && !element.isJsonNull() && element.isJsonObject())
                ? element.getAsJsonObject()
                : new JsonObject();
    }

    public static int[] jsonArrayToIntArray(JsonArray jsonArray) {
        int[] intArray = new int[jsonArray.size()];
        for (int i = 0; i < intArray.length; i++) {
            JsonElement element = jsonArray.get(i);
            intArray[i] = element.isJsonPrimitive() ? element.getAsInt() : 0;
        }
        return intArray;
    }

    public static int[] convertJsonArrayToIntArray(JsonObject json, String key) {
        JsonElement element = json.get(key);
        boolean hasKey = element != null && !element.isJsonNull() && element.isJsonArray();
        if (!hasKey)
            return new int[] {};
        else {
            JsonArray jsonArray = element.getAsJsonArray();
            return jsonArrayToIntArray(jsonArray);
        }
    }

    public static boolean isEmptyString(String s) {
        return (s == null || s.length() == 0);
    }

    public static String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
