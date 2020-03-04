package com.vaadin.integration.eclipse.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

public class EclipsePluginDownloader {
    private static final String STABLE_ECLIPSE_SITE_URL = "https://vaadin.com/eclipse";

    public static void main(String[] args) throws IOException {
        downloadEclipsePlugin(args[0]);
    }

    public static void downloadEclipsePlugin(String baseDir) throws IOException {
        System.out.println("Downloading Eclipse plugins and features from "
                + STABLE_ECLIPSE_SITE_URL + " to " + baseDir);
        URL url = new URL(STABLE_ECLIPSE_SITE_URL + "/site.xml");

        String sitexml = IOUtils.toString(url.openStream());
        // Remove all comments
        Matcher n = Pattern.compile("(\r|\n)", Pattern.MULTILINE).matcher(
                sitexml);
        sitexml = n.replaceAll("");
        sitexml = sitexml.replaceAll("<!--(.*?)-->", "");
        // Find the version for id="com.vaadin.integration.eclipse"
        Pattern p = Pattern
                .compile("<feature.*?url=\"(.*?)\" id=\"(.*?)\" version=\"(.*?)\"");
        Matcher m = p.matcher(sitexml);
        // System.out.println("site.xml:");
        // System.out.println(sitexml);

        int found = 0;

        while (m.find()) {
            found++;

            String relativeFeatureUrl = m.group(1);
            String relativePluginUrl = relativeFeatureUrl.replace("features/",
                    "plugins/");
            String pluginId = m.group(2);
            String version = m.group(3);

            if (pluginId.equals("com.vaadin.integration.eclipse.manual")) {
                // Manual feature and plugin names do not match...
                relativePluginUrl = relativePluginUrl.replace(
                        "com.vaadin.integration.eclipse.manual",
                        "com.vaadin.manual");
            }

            if (pluginId.equals("com.vaadin.integration.eclipse")) {
                System.out.println("Latest stable Eclipse plugin version is "
                        + version);
                if (!version.startsWith("1")) {
                    // Only download for versions 2 and up.
                    downloadVisualDesignerPlugin(baseDir,
                            getRelativeBaseUrl(relativePluginUrl), version);
                }
            }

            downloadFeature(baseDir, relativeFeatureUrl);
            downloadPlugin(baseDir, relativePluginUrl);
        }

        if (found == 0) {
            throw new IOException(
                    "Unable to find out latest stable plugin version");
        }

    }

    /**
     * Takes a String representing a URL and removes everything following the
     * last '/'
     * 
     * @param relativePluginUrl
     *            the url to get the base for
     * @return the base url
     */
    private static String getRelativeBaseUrl(String relativePluginUrl) {
        return relativePluginUrl.substring(0,
                relativePluginUrl.lastIndexOf('/'));
    }

    private static void downloadVisualDesignerPlugin(String baseDir,
            String relativePluginBaseUri, String version) throws IOException {
        downloadPlugin(baseDir, String.format(
                "%s/com.vaadin.wysiwyg.eclipse_%s.jar", relativePluginBaseUri,
                version));
    }

    private static void downloadPlugin(String baseDir, String relativePluginUrl)
            throws IOException {
        downloadFile(STABLE_ECLIPSE_SITE_URL + "/" + relativePluginUrl, baseDir
                + File.separator + relativePluginUrl);

    }

    /**
     * Downloads the file if it does not already exist.
     * 
     * @param baseDir
     * @param relativeFeatureUrl
     * @throws IOException
     */
    private static void downloadFile(String url, String targetFilename)
            throws IOException {
        File targetFile = new File(targetFilename);
        if (targetFile.exists()) {
            return;
        }

        System.out.println("Downloading " + url);

        IOUtils.copy(new URL(url).openStream(), new FileOutputStream(
                targetFilename));
    }

    private static void downloadFeature(String baseDir,
            String relativeFeatureUrl) throws IOException {

        downloadFile(STABLE_ECLIPSE_SITE_URL + "/" + relativeFeatureUrl,
                baseDir + File.separator + relativeFeatureUrl);
    }
}
