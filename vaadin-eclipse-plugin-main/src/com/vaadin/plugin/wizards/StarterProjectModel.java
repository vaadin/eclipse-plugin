package com.vaadin.plugin.wizards;

/**
 * Model for Vaadin Starter projects from start.vaadin.com/skeleton endpoint.
 */
public class StarterProjectModel extends AbstractProjectModel {

    private boolean prerelease = false;
    private boolean includeFlow = true;

    @Override
    public String getDownloadUrl() {
        StringBuilder url = new StringBuilder("https://start.vaadin.com/skeleton?");

        // Add project name as group and artifact ID
        String artifactId = toArtifactId(projectName);
        url.append("name=").append(encode(artifactId));
        url.append("&artifactId=").append(encode(artifactId));
        url.append("&groupId=").append(encode(groupId));

        // Add framework selection using the 'frameworks' parameter
        if (includeFlow) {
            url.append("&frameworks=flow");
        }

        // Add platform version selection (always include, defaults to "latest")
        String platformVersion = prerelease ? "pre" : "latest";
        url.append("&platformVersion=").append(platformVersion);

        // Add download parameter
        url.append("&download=true");

        // Add reference for tracking
        url.append("&ref=eclipse-plugin");

        return url.toString();
    }

    // Getters and setters specific to Starter projects

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
}
