package com.vaadin.plugin.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public final class VaadinHomeUtil {

    private static final String PROPERTY_USER_HOME = "user.home";
    private static final String VAADIN_FOLDER_NAME = ".vaadin";

    private VaadinHomeUtil() {
        /* no instances */ }

    /**
     * Get Vaadin home directory.
     *
     * @return File instance for Vaadin home folder. Does not check if the folder exists.
     */
    public static File resolveVaadinHomeDirectory() {
        String userHome = System.getProperty(PROPERTY_USER_HOME);
        return new File(userHome, VAADIN_FOLDER_NAME);
    }

    public static String getUserKey() throws IOException {
        File vaadinHome = resolveVaadinHomeDirectory();
        File userKeyFile = new File(vaadinHome, "userKey");
        if (userKeyFile.exists()) {
            try {
                String content = Files.readString(userKeyFile.toPath());
                JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();
                return jsonObject.get("key").getAsString();
            } catch (JsonSyntaxException ex) {
                // fix for invalid JSON regression
                // fall through to regenerate
                // noinspection ResultOfMethodCallIgnored
                userKeyFile.delete();
            }
        }

        String key = "user-" + UUID.randomUUID();
        JsonObject keyObject = new JsonObject();
        keyObject.addProperty("key", key);
        Gson gson = new Gson();
        Files.createDirectories(vaadinHome.toPath());
        Files.write(userKeyFile.toPath(), gson.toJson(keyObject).getBytes(Charset.defaultCharset()));
        return key;
    }

    public static String getProKey() throws IOException {
        File vaadinHome = resolveVaadinHomeDirectory();
        File proKeyFile = new File(vaadinHome, "proKey");
        if (proKeyFile.exists()) {
            try {
                String content = Files.readString(proKeyFile.toPath());
                JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();
                return jsonObject.get("key").getAsString();
            } catch (JsonSyntaxException ex) {
                throw new IOException(ex);
            }
        }
        return null;
    }

    public static String getProUsername() throws IOException {
        File vaadinHome = resolveVaadinHomeDirectory();
        File proKeyFile = new File(vaadinHome, "proKey");
        if (proKeyFile.exists()) {
            try {
                String content = Files.readString(proKeyFile.toPath());
                JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();
                return jsonObject.get("username").getAsString();
            } catch (JsonSyntaxException ex) {
                throw new IOException(ex);
            }
        }
        return null;
    }

}
