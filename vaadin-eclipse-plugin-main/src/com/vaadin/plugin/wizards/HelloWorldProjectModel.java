package com.vaadin.plugin.wizards;

/**
 * Model for Vaadin Hello World projects from start.vaadin.com/helloworld endpoint.
 */
public class HelloWorldProjectModel extends AbstractProjectModel {

    private String framework = "flow"; // flow or hilla
    private String language = "java"; // java or kotlin
    private String buildTool = "maven"; // maven or gradle
    private String architecture = "springboot"; // springboot, quarkus, jakartaee, servlet

    @Override
    public String getDownloadUrl() {
        StringBuilder url = new StringBuilder("https://start.vaadin.com/helloworld?");

        // Add project name and IDs
        String artifactId = toArtifactId(projectName);
        url.append("name=").append(encode(artifactId));
        url.append("&artifactId=").append(encode(artifactId));
        url.append("&groupId=").append(encode(getGroupId()));

        // Add framework
        url.append("&framework=").append(framework);

        // Add language
        url.append("&language=").append(language);

        // Add build tool (note: parameter name is 'buildtool' not 'buildTool')
        url.append("&buildtool=").append(buildTool);

        // Add architecture (note: parameter name is 'stack' not 'architecture')
        url.append("&stack=").append(architecture);

        // Add download parameter
        url.append("&download=true");

        // Add reference for tracking
        url.append("&ref=eclipse-plugin");

        return url.toString();
    }

    // Getters and setters specific to Hello World projects

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