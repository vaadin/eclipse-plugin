package com.vaadin.plugin.hotswap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vaadin.plugin.util.VaadinPluginLog;

/**
 * Downloads the latest JetBrains Runtime (JBR) release for the current platform.
 *
 * JetBrains does not attach the runtimes as GitHub release assets, the download links are listed in a Markdown table in
 * the release description. The release is located and the table is parsed the same way the Vaadin plugins for IntelliJ
 * IDEA and VS Code do it.
 */
public class JetBrainsRuntimeDownloader {

    public static final String JETBRAINS_GITHUB_RELEASES_API = "https://api.github.com/repos/JetBrains/JetBrainsRuntime/releases";

    public static final String TAR_GZ = ".tar.gz";

    private static final String SDK_TYPE = "JBRSDK";

    private static final Duration TIMEOUT = Duration.ofMinutes(2);

    private JetBrainsRuntimeDownloader() {
    }

    /**
     * Download the latest JetBrains Runtime for the current platform and extract it below the given directory.
     *
     * When the runtime has already been downloaded before, the existing directory is returned without downloading
     * anything again.
     *
     * @param jdkDirectory
     *            The directory the runtimes are extracted into
     * @param monitor
     *            Progress monitor, may be null
     * @return The directory the runtime was extracted into
     * @throws IOException
     *             if the release cannot be resolved, downloaded or extracted
     * @throws InterruptedException
     *             if the download is interrupted
     */
    public static Path downloadLatestJBR(Path jdkDirectory, IProgressMonitor monitor)
            throws IOException, InterruptedException {
        SubMonitor progress = SubMonitor.convert(monitor, "Downloading JetBrains Runtime", 100);

        progress.subTask("Looking up the latest JetBrains Runtime release");
        String downloadUrl = resolveDownloadUrl();
        progress.worked(5);

        String fileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
        Path extractPath = jdkDirectory.resolve(fileName.substring(0, fileName.length() - TAR_GZ.length()));

        if (Files.isDirectory(extractPath)) {
            VaadinPluginLog.info("JetBrains Runtime already downloaded to " + extractPath);
            progress.done();
            return extractPath;
        }

        Files.createDirectories(jdkDirectory);
        Path archive = jdkDirectory.resolve(fileName + ".part");

        try {
            download(downloadUrl, archive, progress.split(85));

            progress.setWorkRemaining(10);
            progress.subTask("Extracting JetBrains Runtime");
            TarGzExtractor.extract(archive, extractPath, progress.split(10));
        } catch (IOException | RuntimeException e) {
            deleteQuietly(archive);
            deleteRecursivelyQuietly(extractPath);
            throw e;
        }

        deleteQuietly(archive);
        VaadinPluginLog.info("JetBrains Runtime downloaded to " + extractPath);
        return extractPath;
    }

    /**
     * Resolve the download URL of the latest JetBrains Runtime release for the current platform.
     *
     * @return The download URL
     * @throws IOException
     *             if no matching release can be found
     * @throws InterruptedException
     *             if the lookup is interrupted
     */
    public static String resolveDownloadUrl() throws IOException, InterruptedException {
        String architecture = getArchitecture();
        if (architecture == null) {
            throw new IOException("JetBrains Runtime is not available for this platform: "
                    + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        }

        String releaseId = findLatestStableReleaseId(get(JETBRAINS_GITHUB_RELEASES_API));
        if (releaseId == null) {
            throw new IOException("No JetBrains Runtime release found");
        }

        String body = readReleaseBody(get(JETBRAINS_GITHUB_RELEASES_API + "/" + releaseId));
        String downloadUrl = findDownloadUrl(body, architecture);
        if (downloadUrl == null) {
            throw new IOException("No JetBrains Runtime download found for " + architecture);
        }
        return downloadUrl;
    }

    /**
     * Find the id of the latest stable release in a GitHub releases response.
     *
     * @param releasesJson
     *            The JSON returned by the GitHub releases API
     * @return The release id, or null if there is no stable release
     */
    public static String findLatestStableReleaseId(String releasesJson) {
        JsonElement parsed = JsonParser.parseString(releasesJson);
        if (!parsed.isJsonArray()) {
            return null;
        }

        String latestTag = null;
        String latestId = null;
        for (JsonElement element : parsed.getAsJsonArray()) {
            JsonObject release = element.getAsJsonObject();
            if (release.has("prerelease") && release.get("prerelease").getAsBoolean()) {
                continue;
            }
            if (!release.has("tag_name") || !release.has("id")) {
                continue;
            }
            String tag = release.get("tag_name").getAsString();
            if (latestTag == null || tag.compareTo(latestTag) > 0) {
                latestTag = tag;
                latestId = release.get("id").getAsString();
            }
        }
        return latestId;
    }

    private static String readReleaseBody(String releaseJson) {
        JsonObject release = JsonParser.parseString(releaseJson).getAsJsonObject();
        return release.has("body") && !release.get("body").isJsonNull() ? release.get("body").getAsString() : "";
    }

    /**
     * Find the JBR SDK download URL for the given architecture in a release description.
     *
     * The description contains a Markdown table with one row per artifact, for example:
     *
     * <pre>
     * | osx-aarch64 | JBRSDK | [jbrsdk-21.0.7-osx-aarch64-b1038.58.tar.gz](https://cache-redirector.jetbrains.com/...) |
     * </pre>
     *
     * @param releaseBody
     *            The release description
     * @param architecture
     *            The architecture to look for, for example "osx-aarch64"
     * @return The download URL, or null if the release has no matching artifact
     */
    public static String findDownloadUrl(String releaseBody, String architecture) {
        if (releaseBody == null || architecture == null) {
            return null;
        }

        for (String line : releaseBody.split("\n")) {
            String[] columns = line.split("\\|");
            if (columns.length < 4) {
                continue;
            }

            String arch = columns[1].trim();
            String sdkType = columns[2].replace("*", "").trim();
            String url = columns[3].replaceFirst("\\[.*\\]", "").replace("(", "").replace(")", "").trim();

            if (SDK_TYPE.equals(sdkType) && url.endsWith(TAR_GZ) && architecture.equals(arch)) {
                return url;
            }
        }
        return null;
    }

    /**
     * Get the architecture string JetBrains uses in the release descriptions.
     *
     * @return The architecture, for example "linux-x64", or null for unsupported platforms
     */
    public static String getArchitecture() {
        return getArchitecture(System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
    }

    public static String getArchitecture(String osName, String osArch) {
        String os = osName.toLowerCase(Locale.ROOT);
        String arch = osArch.toLowerCase(Locale.ROOT);

        String platform;
        if (os.contains("mac") || os.contains("darwin")) {
            platform = "osx";
        } else if (os.contains("win")) {
            platform = "windows";
        } else if (os.contains("linux")) {
            platform = "linux";
        } else {
            return null;
        }

        String suffix;
        if (arch.equals("aarch64") || arch.equals("arm64")) {
            suffix = "aarch64";
        } else if (arch.contains("64")) {
            suffix = "x64";
        } else {
            suffix = "x86";
        }

        return platform + "-" + suffix;
    }

    private static String get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(TIMEOUT)
                .header("Accept", "application/vnd.github+json").GET().build();

        HttpResponse<String> response = newClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to query " + url + ": HTTP " + response.statusCode());
        }
        return response.body();
    }

    /**
     * Download a file, reporting progress and honouring cancellation.
     */
    private static void download(String url, Path destination, IProgressMonitor monitor)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(TIMEOUT).GET().build();

        HttpResponse<InputStream> response = newClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to download " + url + ": HTTP " + response.statusCode());
        }

        long total = response.headers().firstValueAsLong("content-length").orElse(-1);
        SubMonitor progress = SubMonitor.convert(monitor, 100);

        try (InputStream in = response.body(); OutputStream out = Files.newOutputStream(destination)) {
            byte[] buffer = new byte[64 * 1024];
            long downloaded = 0;
            int reportedPercentage = 0;
            int read;

            while ((read = in.read(buffer)) >= 0) {
                if (progress.isCanceled()) {
                    throw new OperationCanceledException();
                }
                out.write(buffer, 0, read);
                downloaded += read;

                if (total > 0) {
                    int percentage = (int) (downloaded * 100 / total);
                    if (percentage > reportedPercentage) {
                        progress.worked(percentage - reportedPercentage);
                        reportedPercentage = percentage;
                    }
                    progress.subTask("Downloading JetBrains Runtime: " + percentage + "%");
                } else {
                    progress.subTask("Downloading JetBrains Runtime: " + (downloaded / (1024 * 1024)) + " MB");
                }
            }
        }
    }

    private static HttpClient newClient() {
        return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(TIMEOUT).build();
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            VaadinPluginLog.debug("Could not delete " + path + ": " + e.getMessage());
        }
    }

    private static void deleteRecursivelyQuietly(Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder()).forEach(JetBrainsRuntimeDownloader::deleteQuietly);
        } catch (IOException e) {
            VaadinPluginLog.debug("Could not delete " + path + ": " + e.getMessage());
        }
    }
}
