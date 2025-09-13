package com.vaadin.plugin.wizards;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Abstract base class for Vaadin project models. Contains common functionality for all project types.
 */
public abstract class AbstractProjectModel {

    protected String projectName = "NewProject";
    protected String location;
    protected String groupId = "com.example.application";

    /**
     * Get the download URL for this project configuration.
     *
     * @return the complete download URL
     */
    public abstract String getDownloadUrl();

    /**
     * Convert project name to a valid Maven artifact ID.
     *
     * @param projectName
     *            the project name to convert
     * @return a valid Maven artifact ID
     */
    protected String toArtifactId(String projectName) {
        if (projectName == null || projectName.isEmpty()) {
            return "my-app";
        }
        // Convert project name to valid Maven artifact ID
        return projectName.toLowerCase().replaceAll("[^a-z0-9-]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
    }

    /**
     * URL encode a value using UTF-8.
     *
     * @param value
     *            the value to encode
     * @return the URL encoded value
     */
    protected String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    // Common getters and setters

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}