package com.vaadin.integration.eclipse.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.ElementIterator;
import javax.swing.text.NumberFormatter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

public class DownloadUtils {

    private static final String VAADIN_BRANCH_PATTERN = "(5\\.[3-9])|([6-9]\\.[0-9]+)|([1-9][0-9]+\\.[0-9]+)|([6-9]\\.[0-9]+\\.[0-9]+)|([1-9][0-9]+\\.[0-9]+\\.[0-9]+)";
    // the last group of a Vaadin version can contain anything but a dot
    // this pattern also filters out old versions
    private static final String VAADIN_VERSION_PATTERN = "("
            + VAADIN_BRANCH_PATTERN + ")\\.[^.]+";

    // this is used to extract version number parts for sorting
    // this pattern does not filter out old versions
    private static final String VAADIN_VERSION_PART_PATTERN = "([0-9]*)\\.([0-9])\\.([^.]+)";

    private static final String LATEST_BASE_URL = "https://vaadin.com/download";
    private static final String LATEST_FILENAME = "LATEST";
    private static final String VAADIN_DOWNLOAD_BASE_URL = "https://vaadin.com/download/";
    private static final String GWT_DOWNLOAD_URL = "https://vaadin.com/download/external/gwt";

    /**
     * A file version for a particular type of files (e.g. Vaadin JAR) and the
     * download information for obtaining the file.
     */
    public static class Version implements Comparable<Version> {
        private final String version;
        private DownloadInformation downloadInformation;

        private Version(String version, DownloadInformation downloadInformation) {
            this.version = version;
            this.downloadInformation = downloadInformation;
        }

        public String getVersionString() {
            return version;
        }

        @Override
        public String toString() {
            return version;
        }

        public int compareTo(Version o) {
            return version.compareTo(o.version);
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof Version)
                    && version.equals(((Version) obj).version);
        }

        @Override
        public int hashCode() {
            return version.hashCode();
        }

        // branch is the two first parts of the version string
        private String getBranch() {
            return version.replaceAll("\\.[^.]+$", "");
        }

        // the first numeric parts of the version string
        private String getNumericVersion() {
            return version.replaceAll("[^0-9.].*$", "");
        }

        public String getJarFileName() {
            return downloadInformation.getDownloadableFilename(this);
        }
    }

    // Information about a type of files and the related download site.
    // Each DownloadInformation should be able to map a version string to a
    // file name unambiguously.
    private static abstract class DownloadInformation {
        protected final String id;
        protected final String baseUrl;
        protected final String filenamePrefix;
        protected final String filenamePostfix;
        // how many levels of directories above those of interest
        protected final int branchDepth;

        private DownloadInformation(String id, String baseUrl,
                String filenamePrefix, String filenamePostfix, int branchDepth) {
            this.id = id;
            this.baseUrl = baseUrl;
            this.filenamePrefix = filenamePrefix;
            this.filenamePostfix = filenamePostfix;
            this.branchDepth = branchDepth;
        }

        public String getDownloadableFilename(Version version) {
            return filenamePrefix + filenamePostfix;
        }

        public String getDownloadUrl(Version version) {
            // by default, there is a version directory
            return baseUrl + "/" + version + "/"
                    + getDownloadableFilename(version);
        }

        public String getFileListUrl() {
            return baseUrl + "/";
        }

        public Version fileToVersion(String filename) {
            if (filename.startsWith(filenamePrefix)
                    && filename.endsWith(filenamePostfix)) {
                return new Version(filename
                        .replaceAll("^" + filenamePrefix, "").replaceAll(
                                filenamePostfix + "$", ""), this);
            } else {
                return null;
            }
        }

        // convert a directory or file name under the branch directory to a
        // version
        public Version branchEntryToVersion(String dirname) {
            // by default, not supported
            return null;
        }
    }

    private static abstract class VaadinDownloadInformation extends
            DownloadInformation {
        private VaadinDownloadInformation(String id, String baseUrl,
                String filenamePrefix, int branchDepth) {
            super(id, baseUrl, filenamePrefix, ".jar", branchDepth);
        }

        @Override
        public String getDownloadableFilename(Version version) {
            return filenamePrefix + version + filenamePostfix;
        }
    }

    // New (vaadin.com) format download site
    private static class VaadinNewDownloadInformation extends
            VaadinDownloadInformation {
        private VaadinNewDownloadInformation(String id, String baseUrl,
                String filenamePrefix, int branchDepth) {
            super(id, baseUrl, filenamePrefix, branchDepth);
        }

        @Override
        public String getDownloadUrl(Version version) {
            // branch is the two first parts of the version string
            return baseUrl + "/" + version.getBranch() + "/" + version + "/"
                    + getDownloadableFilename(version);
        }

        @Override
        public Version branchEntryToVersion(String dirname) {
            // exclude 5.x
            if (dirname.matches(VAADIN_VERSION_PATTERN)
                    && !dirname.startsWith("5")) {
                return new Version(dirname, this);
            } else {
                return null;
            }
        }
    }

    // IT Mill Toolkit downloads from Vaadin.com
    private static class ToolkitDownloadInformation extends
            VaadinNewDownloadInformation {
        private ToolkitDownloadInformation(String id, String baseUrl,
                String filenamePrefix) {
            super(id, baseUrl, filenamePrefix, 1);
        }

        @Override
        public Version branchEntryToVersion(String dirname) {
            // 5.x only
            if (dirname.matches(VAADIN_VERSION_PATTERN)
                    && dirname.startsWith("5")) {
                return new Version(dirname, this);
            } else {
                return null;
            }
        }
    }

    private static class VaadinPrereleaseDownloadInformation extends
            VaadinNewDownloadInformation {
        private VaadinPrereleaseDownloadInformation(String id, String baseUrl,
                String filenamePrefix) {
            super(id, baseUrl, filenamePrefix, 2);
        }

        @Override
        public String getDownloadUrl(Version version) {
            // branch is the two first parts of the version string
            // numeric version is the numeric part (x.y.z, numbers only) of the
            // version string
            return baseUrl + "/" + version.getBranch() + "/"
                    + version.getNumericVersion() + "/" + version + "/"
                    + getDownloadableFilename(version);
        }
    }

    private static class VaadinNightlyDownloadInformation extends
            VaadinNewDownloadInformation {
        private VaadinNightlyDownloadInformation(String id, String baseUrl,
                String filenamePrefix) {
            super(id, baseUrl, filenamePrefix, 1);
        }

        @Override
        public String getDownloadUrl(Version version) {
            // branch is the two first parts of the version string
            return baseUrl + "/" + version.getBranch() + "/"
                    + getDownloadableFilename(version);
        }

        @Override
        public Version branchEntryToVersion(String filename) {
            return fileToVersion(filename);
        }
    }

    private static class GwtDownloadInformation extends DownloadInformation {
        private GwtDownloadInformation(String id, String baseUrl,
                String filenamePrefix) {
            super(id, baseUrl, filenamePrefix, ".jar", 0);
        }
    }

    // Old IT Mill Toolkit downloads
    private static DownloadInformation TOOLKIT_JAR_DOWNLOAD = new ToolkitDownloadInformation(
            "itmill-toolkit", "https://vaadin.com/download/release",
            "itmill-toolkit-");

    // Vaadin download sites
    private static DownloadInformation VAADIN_JAR_DOWNLOAD = new VaadinNewDownloadInformation(
            "vaadin", VAADIN_DOWNLOAD_BASE_URL + "release", "vaadin-", 1);
    private static DownloadInformation VAADIN_PRERELEASE_JAR_DOWNLOAD = new VaadinPrereleaseDownloadInformation(
            "vaadin-prerelease", VAADIN_DOWNLOAD_BASE_URL + "prerelease",
            "vaadin-");
    private static DownloadInformation VAADIN_NIGHTLY_JAR_DOWNLOAD = new VaadinNightlyDownloadInformation(
            "vaadin-nightly", VAADIN_DOWNLOAD_BASE_URL + "nightly", "vaadin-");

    // GWT download information
    private static DownloadInformation GWT_USER_JAR_DOWNLOAD = new GwtDownloadInformation(
            "gwt-user", GWT_DOWNLOAD_URL, "gwt-user");
    private static DownloadInformation GWT_DEV_JAR_DOWNLOAD = new GwtDownloadInformation(
            "gwt-dev", GWT_DOWNLOAD_URL, "gwt-dev-"
                    + VaadinPluginUtil.getPlatform());

    // (ordered) map from download site ID to the corresponding
    // DownloadInformation instance
    private static Map<String, DownloadInformation> vaadinDownloadInformation = new LinkedHashMap<String, DownloadInformation>();

    static {
        vaadinDownloadInformation.put(VAADIN_JAR_DOWNLOAD.id,
                VAADIN_JAR_DOWNLOAD);
        vaadinDownloadInformation.put(TOOLKIT_JAR_DOWNLOAD.id,
                TOOLKIT_JAR_DOWNLOAD);
        vaadinDownloadInformation.put(VAADIN_PRERELEASE_JAR_DOWNLOAD.id,
                VAADIN_PRERELEASE_JAR_DOWNLOAD);
        vaadinDownloadInformation.put(VAADIN_NIGHTLY_JAR_DOWNLOAD.id,
                VAADIN_NIGHTLY_JAR_DOWNLOAD);
    }

    /**
     * Extracts a Vaadin version from a filename, returns null if not a valid
     * Vaadin JAR file name.
     *
     * @param filename
     *            JAR file name
     * @return Version or null if not a Vaadin JAR
     */
    public static Version getVaadinJarVersion(String filename) {
        // iterate over downloadInformationMap and find the first match
        Version version = null;
        for (DownloadInformation dli : vaadinDownloadInformation.values()) {
            version = dli.fileToVersion(filename);
            if (version != null) {
                break;
            }
        }
        return version;
    }

    public static boolean isVaadinVersionString(Version version) {
        return version.getVersionString().matches(VAADIN_VERSION_PATTERN);
    }

    // returns true if the fetch succeeded, false if not
    // TODO error cases, when to throw an exception?
    private static boolean fetch(DownloadInformation information,
            Version version) {
        try {
            File targetFile = getLocalFile(information, version).toFile();
            if (targetFile.exists()) {
                throw new IOException("Target file already exists");
            }

            String urlString = information.getDownloadUrl(version);
            URL url = new URL(urlString);

            // it is normal for this to fail when trying to fetch a file from
            // the wrong directory (wrong DownloadInformation)
            InputStream urlStream = url.openStream();

            // Create the directory if it does not exist
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }

            IOUtils.copy(urlStream, new FileOutputStream(targetFile));

            // TODO could improve logging
            System.out.println("Fetched " + urlString + " to "
                    + targetFile.toString());
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    private static List<String> listHttpLinks(String urlString)
            throws CoreException {
        try {
            // fetch the HTML page and parse out links
            URL url = new URL(urlString);

            EditorKit kit = new HTMLEditorKit();
            Document doc = kit.createDefaultDocument();

            // Create a reader on the HTML content.
            InputStream inputStream = url.openStream();
            Reader rd = new InputStreamReader(inputStream);

            // Parse the HTML.
            kit.read(rd, doc, 0);

            List<String> dirs = new ArrayList<String>();
            // Iterate through the elements of the HTML document.
            ElementIterator it = new ElementIterator(doc);
            javax.swing.text.Element elem;
            while ((elem = it.next()) != null) {
                SimpleAttributeSet s = (SimpleAttributeSet) elem
                        .getAttributes().getAttribute(HTML.Tag.A);
                if (s != null) {
                    Object attr = s.getAttribute(HTML.Attribute.HREF);
                    if (attr != null) {
                        String str = attr.toString();
                        dirs.add(str);
                    }
                }
            }

            return dirs;
        } catch (Exception e) {
            throw VaadinPluginUtil.newCoreException(
                    "Failed to fetch directory listing for " + urlString, e);
        }

    }

    private static IPath getLocalFile(DownloadInformation information,
            Version version) throws CoreException {
        return VaadinPluginUtil.getVersionedDownloadDirectory(information.id,
                version.getVersionString()).append(
                IPath.SEPARATOR + information.getDownloadableFilename(version));
    }

    private static File getLocalDirectory(DownloadInformation information)
            throws CoreException {
        return VaadinPluginUtil.getDownloadDirectory(information.id).toFile();
    }

    public static void fetchVaadinJar(Version version, IProgressMonitor monitor)
            throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask("Downloading Vaadin JAR",
                    IProgressMonitor.UNKNOWN);

            // try to download the version from all available download sites
            boolean success = false;
            for (DownloadInformation dli : vaadinDownloadInformation.values()) {
                if (fetch(dli, version)) {
                    success = true;
                    break;
                }
            }
            if (!success) {
                throw VaadinPluginUtil.newCoreException(
                        "Failed to fetch Vaadin version " + version, null);
            }
        } finally {
            monitor.done();
        }
    }

    // official versions only
    public static Version getLatestVaadinVersion() throws CoreException {
        try {
            // TODO currently ignoring the relative path in the LATEST file
            URL url = new URL(LATEST_BASE_URL + "/" + LATEST_FILENAME);
            InputStream inputStream = url.openStream();
            String version = IOUtils.toString(inputStream).replaceAll(
                    "[\r\n].*", "");
            if (version.startsWith("5.")) {
                return new Version(version, TOOLKIT_JAR_DOWNLOAD);
            } else {
                return new Version(version, VAADIN_JAR_DOWNLOAD);
            }
        } catch (Exception e) {
            throw VaadinPluginUtil.newCoreException(
                    "Checking latest available version of Vaadin failed", e);
        }
    }

    public static List<Version> listDownloadableVaadinVersions(
            boolean includeDevelopment) throws CoreException {
        List<Version> versions = listDownloadableVaadinVersions(VAADIN_JAR_DOWNLOAD);
        // check the Vaadin site only, not the old IT Mill site
        versions.addAll(listDownloadableVaadinVersions(TOOLKIT_JAR_DOWNLOAD));
        if (includeDevelopment) {
            versions
                    .addAll(listDownloadableVaadinVersions(VAADIN_PRERELEASE_JAR_DOWNLOAD));
            versions
                    .addAll(listDownloadableVaadinVersions(VAADIN_NIGHTLY_JAR_DOWNLOAD));
        } else {
            // Check LATEST and include that if not already in the list
            Version latestVersion = getLatestVaadinVersion();
            if (!versions.contains(latestVersion)) {
                versions.add(latestVersion);
            }

        }
        return versions;
    }

    private static List<Version> listDownloadableVaadinVersions(
            DownloadInformation information) throws CoreException {
        List<Version> versions = new ArrayList<Version>();
        try {
            // updates the versions list

            // branch directories inside which are version directories
            // e.g. 6.0/6.0.0/vaadin-6.0.0.jar
            // for pre-releases, one more level of indirection:
            // e.g. 6.1/6.1.0/6.1.0-pre1/vaadin-6.1.0-pre1.jar
            // nightly builds use a flatter structure:
            // e.g. 6.1/vaadin-6.1.nightly-20090901-c8626.jar
            listDownloadableVaadinVersions(information, versions,
                    information.branchDepth, "/");

            Collections.reverse(versions);
            return versions;
        } catch (Exception e) {
            throw VaadinPluginUtil.newCoreException(
                    "Retrieving list of available Vaadin versions failed", e);
        }
    }

    private static void listDownloadableVaadinVersions(
            DownloadInformation information,
            List<Version> versions, int branchDepth, String relativeDir)
            throws CoreException {
        // fetch version list from the server
        List<String> dirs = listHttpLinks(information.getFileListUrl()
                + relativeDir);

        // use the branch-depth parameter in recursion, supporting both the IT
        // Mill Toolkit site and the new site directory structures
        if (branchDepth > 0) {
            Pattern branchPattern = Pattern.compile(VAADIN_BRANCH_PATTERN);
            for (String dir : dirs) {
                // remove trailing slash if any
                dir = dir.replaceAll("/$", "");
                if (branchPattern.matcher(dir).matches()) {
                    // list version directories (releases) or files (nightly
                    // builds etc.)
                    listDownloadableVaadinVersions(information, versions,
                            branchDepth - 1, relativeDir + dir + "/");
                }
            }
        } else {
            // version directories are under this directory
            versions.addAll(filterVaadinVersions(information, dirs));
        }
    }

    private static List<Version> filterVaadinVersions(
            DownloadInformation information, List<String> branchEntries) {
        List<Version> versions = new ArrayList<Version>();
        // handle files or subdirectories, whatever comes under branch
        for (String dir : branchEntries) {
            // remove trailing slash if any
            dir = dir.replaceAll("/$", "");
            Version version = information.branchEntryToVersion(dir);
            if (version != null) {
                versions.add(version);
            }
        }
        return versions;
    }

    /**
     * Returns the versions for all the locally available Vaadin jars, sorted
     * with the latest version first.
     *
     * If no local jar is found, an empty List is returned.
     *
     * @return
     * @throws CoreException
     */
    public static List<Version> getLocalVaadinJarVersions()
            throws CoreException {
        NumberFormatter nf = new NumberFormatter(new DecimalFormat("000"));

        // the key is only used internally for sorting
        SortedMap<String, Version> versions = new TreeMap<String, Version>();

        try {
            for (DownloadInformation dli : vaadinDownloadInformation.values()) {
                File downloadDirectory = getLocalDirectory(dli);
                if (!downloadDirectory.exists()) {
                    // ignore and continue with next directory
                    continue;
                }

                File[] files = downloadDirectory.listFiles();
                Pattern pattern = Pattern.compile("^"
                        + VAADIN_VERSION_PART_PATTERN + "$");
                for (File file : files) {
                    String name = file.getName();

                    Matcher m = pattern.matcher(name);
                    if (m.matches()) {
                        try {
                            int major = Integer.parseInt(m.group(1));
                            int minor = Integer.parseInt(m.group(2));
                            // the third component may be other than an int
                            String revision = m.group(3);
                            String key = nf.valueToString(major)
                                    + nf.valueToString(minor);
                            // make sure integers are sorted before other
                            // strings
                            if (revision.matches("[0-9]+")) {
                                key += "1" + revision;
                            } else {
                                key += "0" + revision;
                            }
                            versions.put(key, new Version(name, dli));
                        } catch (ParseException pe) {
                            // log and ignore
                            VaadinPluginUtil.handleBackgroundException(
                                    IStatus.INFO,
                                    "Failed to parse the Vaadin version number "
                                            + name, pe);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw VaadinPluginUtil.newCoreException(
                    "Failed to list local Vaadin versions", e);
        }

        List<Version> versionList = new ArrayList<Version>(versions.values());

        // return latest version first
        Collections.reverse(versionList);

        return versionList;
    }

    /**
     * Get the local (downloaded) Vaadin JAR version object for a given version
     * number.
     *
     * @param versionString
     *            Vaadin version number string
     * @return Version object or null if none found
     * @throws CoreException
     */
    public static Version getLocalVaadinVersion(String versionString)
            throws CoreException {
        if (versionString == null) {
            // optimization - no need to get the list
            return null;
        }
        List<Version> versions = getLocalVaadinJarVersions();
        for (Version version : versions) {
            if (version.getVersionString().equals(versionString)) {
                return version;
            }
        }
        // not found
        return null;
    }

    /**
     * Returns the version for the newest, locally available Vaadin jar. If no
     * local jar is found, null is returned.
     *
     * @return
     * @throws CoreException
     */
    // TODO official versions only?
    public static Version getLatestLocalVaadinJarVersion() throws CoreException {
        try {
            List<Version> versions = getLocalVaadinJarVersions();
            if (versions.size() > 0) {
                return versions.get(0);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw VaadinPluginUtil.newCoreException(
                    "Failed to get the latest local Vaadin version", e);
        }
    }

    public static IPath getLocalVaadinJar(Version version) throws CoreException {
        // find the first local Vaadin JAR for the version
        IPath jar = null;
        for (DownloadInformation dli : vaadinDownloadInformation.values()) {
            IPath path = getLocalFile(dli, version);
            if (path != null && path.toFile().exists()) {
                jar = path;
                break;
            }
        }
        return jar;
    }

    public static IPath getLocalGwtDevJar(String version) throws CoreException {
        return getLocalFile(GWT_DEV_JAR_DOWNLOAD, new Version(version,
                GWT_DEV_JAR_DOWNLOAD));
    }

    public static IPath getLocalGwtUserJar(String version) throws CoreException {
        return getLocalFile(GWT_USER_JAR_DOWNLOAD, new Version(version,
                GWT_USER_JAR_DOWNLOAD));
    }

    public static void ensureVaadinJarExists(Version version,
            IProgressMonitor monitor)
            throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask("Downloading Vaadin JAR if necessary", 1);
            if (getLocalVaadinJar(version) == null) {
                fetchVaadinJar(version, new SubProgressMonitor(monitor, 1));
            }
        } finally {
            monitor.done();
        }

    }

    public static void ensureGwtUserJarExists(String version,
            IProgressMonitor monitor)
            throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask("Downloading GWT JARs",
                    IProgressMonitor.UNKNOWN);
            if (!getLocalGwtUserJar(version).toFile().exists()) {
                fetch(GWT_USER_JAR_DOWNLOAD, new Version(version,
                        GWT_USER_JAR_DOWNLOAD));
            }
        } finally {
            monitor.done();
        }

    }

    public static void ensureGwtDevJarExists(String version,
            IProgressMonitor monitor)
            throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask("Downloading GWT JARs",
                    IProgressMonitor.UNKNOWN);
            if (!getLocalGwtDevJar(version).toFile().exists()) {
                fetch(GWT_DEV_JAR_DOWNLOAD, new Version(version,
                        GWT_DEV_JAR_DOWNLOAD));
            }
        } finally {
            monitor.done();
        }

    }

}
