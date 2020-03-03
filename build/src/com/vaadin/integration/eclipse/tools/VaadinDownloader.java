package com.vaadin.integration.eclipse.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;

public class VaadinDownloader {

    private static final String VAADIN_BASE_URL = "https://vaadin.com/download";
    private static final String LATEST_URL = VAADIN_BASE_URL + "/LATEST";
    private static final String VAADIN_VERSION_ATTRIBUTE = "Bundle-version";
    private static final String GWT_VERSION_ATTRIBUTE = "GWT-Version";
    private static final String GWT_DEPENDENCIES_ATTRIBUTE = "GWT-Version-Dependencies";

    public static void main(String[] args) throws IOException {
        // Download the latest jar available and gwt-dev and gwt-user. Place
        // them in the directory given as the first parameter, in a version sub
        // directory in "vaadin","gwt-user","gwt-dev" respectively

        String baseDir = args[0];
        System.out.println("Downloading Vaadin jar files to base directory: "
                + baseDir);
        clean(baseDir);
        downloadJars(baseDir);

    }

    private static void clean(String baseDir) throws IOException {
        System.out.println("Cleaning directory " + baseDir);
        File f = new File(baseDir);
        if (!f.exists()) {
            f.mkdirs();
            return;
        }

        if (f.exists() && !f.isDirectory()) {
            throw new IOException("Base directory (" + baseDir
                    + ") is not a directory");
        }

        // Remove the directory and recreate it
        for (File sub : f.listFiles()) {
            removeRecursively(sub);
        }
        f.mkdir();

    }

    /**
     * Remove the file/directory f and all its contents.
     * 
     * @param f
     */
    public static void removeRecursively(File f) {
        if (f.isDirectory()) {
            for (File sub : f.listFiles()) {
                removeRecursively(sub);
            }
        }
        f.delete();

    }

    private static void downloadJars(String baseDir) throws IOException {
        File vaadinJar = downloadLatestVaadinJar(baseDir);
        Attributes attributes = new JarFile(vaadinJar).getManifest()
                .getMainAttributes();
        String vaadinVersion = attributes.getValue(VAADIN_VERSION_ATTRIBUTE);
        String gwtVersion = attributes.getValue(GWT_VERSION_ATTRIBUTE);
        String gwtDependencies = attributes
                .getValue(GWT_DEPENDENCIES_ATTRIBUTE);
        if (gwtVersion == null) {
            if (vaadinVersion.startsWith("6.4.")) {
                // Make it work with 6.4 also. 6.5 and newer contains the
                // GWT_VERISON_ATTRIBUTE
                gwtVersion = "2.0.4";
            }
        }

        if (vaadinVersion == null || gwtVersion == null) {
            throw new IOException("Downloaded jar "
                    + vaadinJar.getAbsolutePath() + " is not a Vaadin jar file");
        }

        System.out.println("Downloaded Vaadin jar version " + vaadinVersion);
        System.err.print(vaadinVersion);
        downloadGWTJars(baseDir, gwtVersion);
        downloadGWTDependencies(baseDir, gwtVersion, gwtDependencies);
        System.out.println(String.format(
                "Downloaded GWT version %s and dependencies (%s)", gwtVersion,
                gwtDependencies));

    }

    private static void downloadGWTJars(String baseDir, String gwtVersion)
            throws IOException {
        downloadGWTJar(baseDir, "gwt-user", gwtVersion, "gwt-user.jar");
        downloadGWTJar(baseDir, "gwt-dev", gwtVersion, "gwt-dev.jar");

    }

    private static void downloadGWTDependencies(String baseDir,
            String gwtVersion, String dependencies)
            throws MalformedURLException, IOException {
        if (dependencies == null) {
            return;
        }

        String[] deps = dependencies.split(",\\s*");
        for (String dep : deps) {
            downloadGWTJar(baseDir, "gwt-dependencies", gwtVersion, dep.trim());
        }
    }

    private static void downloadGWTJar(String baseDir, String type,
            String gwtVersion, String jarName) throws MalformedURLException,
            IOException {
        URLConnection userUrl = new URL(VAADIN_BASE_URL + "/external/gwt/"
                + gwtVersion + "/" + jarName).openConnection();
        File userTargetDir = getDownloadDir(baseDir, type, gwtVersion);
        File targetFile = new File(userTargetDir, jarName);

        // Only download if it has not already been downloaded
        if (!targetFile.exists()) {
            IOUtils.copy(userUrl.getInputStream(), new FileOutputStream(
                    targetFile));
        }
    }

    private static File downloadLatestVaadinJar(String baseDirectory)
            throws IOException {
        URLConnection conn = new URL(LATEST_URL).openConnection();
        StringWriter sw = new StringWriter();
        IOUtils.copy(conn.getInputStream(), sw);
        String[] latestParts = sw.toString().split("\n");
        String version = latestParts[0];
        String relativeUrl = latestParts[1];

        String vaadinJarFilename = "vaadin-" + version + ".jar";

        File targetDir = getDownloadDir(baseDirectory, "vaadin", version);

        File targetFile = new File(targetDir, vaadinJarFilename);
        URL jarURL = new URL(VAADIN_BASE_URL + "/" + relativeUrl + "/"
                + vaadinJarFilename);

        if (!targetFile.exists()) {
            IOUtils.copy(jarURL.openStream(), new FileOutputStream(targetFile));
        }

        return targetFile;
    }

    private static File getDownloadDir(String baseDirectory, String identifier,
            String version) {
        File targetDir = new File(baseDirectory + File.separator + identifier
                + File.separator + version);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        return targetDir;
    }
}
