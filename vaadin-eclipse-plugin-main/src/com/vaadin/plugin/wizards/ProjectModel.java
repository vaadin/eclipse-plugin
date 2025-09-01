package com.vaadin.plugin.wizards;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Model for Vaadin project creation options.
 */
public class ProjectModel {

    public enum ProjectType {
        STARTER, HELLO_WORLD
    }

    private ProjectType projectType = ProjectType.STARTER;
    private String projectName;
    private String location;

    // Starter project options
    private boolean prerelease = false;
    private boolean includeFlow = true;
    private boolean includeHilla = false;

    // Hello World options
    private String framework = "flow"; // flow or hilla
    private String language = "java"; // java or kotlin
    private String buildTool = "maven"; // maven or gradle
    private String architecture = "springboot"; // springboot, quarkus, jakartaee, servlet

    public ProjectModel() {
    }

    public String getDownloadUrl() {
        if (projectType == ProjectType.STARTER) {
            return buildStarterUrl();
        } else {
            return buildHelloWorldUrl();
        }
    }

    private String buildStarterUrl() {
        StringBuilder url = new StringBuilder("https://start.vaadin.com/skeleton?");

        // Add project name as group and artifact ID
        String artifactId = toArtifactId(projectName);
        url.append("artifactId=").append(encode(artifactId));

        // Add framework selection using the 'frameworks' parameter
        if (includeFlow && includeHilla) {
            url.append("&frameworks=flow,hilla");
        } else if (includeHilla) {
            url.append("&frameworks=hilla");
        } else {
            url.append("&frameworks=flow");
        }

        // Add version selection
        if (prerelease) {
            url.append("&platformVersion=pre");
        } else {
            url.append("&platformVersion=latest");
        }

        // Add reference for tracking
        url.append("&ref=eclipse-plugin");

        return url.toString();
    }

    private String buildHelloWorldUrl() {
        StringBuilder url = new StringBuilder("https://start.vaadin.com/helloworld?");

        // Add framework
        url.append("framework=").append(framework);

        // Add language
        url.append("&language=").append(language);

        // Add build tool (note: parameter name is 'buildtool' not 'buildTool')
        url.append("&buildtool=").append(buildTool);

        // Add architecture (note: parameter name is 'stack' not 'architecture')
        url.append("&stack=").append(architecture);

        // Add reference for tracking
        url.append("&ref=eclipse-plugin");

        return url.toString();
    }

    private String toArtifactId(String projectName) {
        // Convert project name to valid Maven artifact ID
        return projectName.toLowerCase().replaceAll("[^a-z0-9-]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    // Getters and setters

    public ProjectType getProjectType() {
        return projectType;
    }

    public void setProjectType(ProjectType projectType) {
        this.projectType = projectType;
    }

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

    public boolean isPrerelease() {
        return prerelease;
    }

    public void setPrerelease(boolean prerelease) {
        this.prerelease = prerelease;
    }

    public boolean isIncludeFlow() {
        return includeFlow;
    }

    public void setIncludeFlow(boolean includeFlow) {
        this.includeFlow = includeFlow;
    }

    public boolean isIncludeHilla() {
        return includeHilla;
    }

    public void setIncludeHilla(boolean includeHilla) {
        this.includeHilla = includeHilla;
    }

    public String getFramework() {
        return framework;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getBuildTool() {
        return buildTool;
    }

    public void setBuildTool(String buildTool) {
        this.buildTool = buildTool;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }
}
