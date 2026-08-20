package com.vaadin.plugin.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.vaadin.plugin.hotswap.JetBrainsRuntimeDownloader;

/**
 * Tests for locating the JetBrains Runtime download for the current platform.
 */
public class JetBrainsRuntimeDownloaderTest {

	/**
	 * An excerpt of a JetBrains Runtime release description, the download links are listed in a Markdown table.
	 */
	private static final String RELEASE_BODY = String.join("\n", "Release notes", "",
			"| Arch | SDK Type | Download |", "| --- | --- | --- |",
			"| linux-aarch64 | JBR | [jbr-21.0.7-linux-aarch64-b1038.58.tar.gz](https://cache-redirector.jetbrains.com/intellij-jbr/jbr-21.0.7-linux-aarch64-b1038.58.tar.gz) |",
			"| linux-aarch64 | JBRSDK | [jbrsdk-21.0.7-linux-aarch64-b1038.58.tar.gz](https://cache-redirector.jetbrains.com/intellij-jbr/jbrsdk-21.0.7-linux-aarch64-b1038.58.tar.gz) |",
			"| linux-x64 | **JBRSDK** | [jbrsdk-21.0.7-linux-x64-b1038.58.tar.gz](https://cache-redirector.jetbrains.com/intellij-jbr/jbrsdk-21.0.7-linux-x64-b1038.58.tar.gz) |",
			"| osx-aarch64 | JBRSDK | [jbrsdk-21.0.7-osx-aarch64-b1038.58.pkg](https://cache-redirector.jetbrains.com/intellij-jbr/jbrsdk-21.0.7-osx-aarch64-b1038.58.pkg) |",
			"| osx-aarch64 | JBRSDK | [jbrsdk-21.0.7-osx-aarch64-b1038.58.tar.gz](https://cache-redirector.jetbrains.com/intellij-jbr/jbrsdk-21.0.7-osx-aarch64-b1038.58.tar.gz) |",
			"| windows-x64 | JBRSDK | [jbrsdk-21.0.7-windows-x64-b1038.58.tar.gz](https://cache-redirector.jetbrains.com/intellij-jbr/jbrsdk-21.0.7-windows-x64-b1038.58.tar.gz) |");

	@Test
	public void findsDownloadUrlForArchitecture() {
		assertEquals("https://cache-redirector.jetbrains.com/intellij-jbr/jbrsdk-21.0.7-osx-aarch64-b1038.58.tar.gz",
				JetBrainsRuntimeDownloader.findDownloadUrl(RELEASE_BODY, "osx-aarch64"));
		assertEquals("https://cache-redirector.jetbrains.com/intellij-jbr/jbrsdk-21.0.7-windows-x64-b1038.58.tar.gz",
				JetBrainsRuntimeDownloader.findDownloadUrl(RELEASE_BODY, "windows-x64"));
	}

	@Test
	public void picksSdkInsteadOfRuntime() {
		// The JBR row for linux-aarch64 comes first but only the JBRSDK ships
		// the tooling Hotswap Agent needs
		assertEquals("https://cache-redirector.jetbrains.com/intellij-jbr/jbrsdk-21.0.7-linux-aarch64-b1038.58.tar.gz",
				JetBrainsRuntimeDownloader.findDownloadUrl(RELEASE_BODY, "linux-aarch64"));
	}

	@Test
	public void ignoresMarkdownEmphasis() {
		assertEquals("https://cache-redirector.jetbrains.com/intellij-jbr/jbrsdk-21.0.7-linux-x64-b1038.58.tar.gz",
				JetBrainsRuntimeDownloader.findDownloadUrl(RELEASE_BODY, "linux-x64"));
	}

	@Test
	public void ignoresArchivesThatCannotBeExtracted() {
		String onlyInstallers = "| osx-x64 | JBRSDK | [jbrsdk.pkg](https://cache-redirector.jetbrains.com/jbrsdk.pkg) |";
		assertNull(JetBrainsRuntimeDownloader.findDownloadUrl(onlyInstallers, "osx-x64"));
	}

	@Test
	public void returnsNullForUnknownArchitecture() {
		assertNull(JetBrainsRuntimeDownloader.findDownloadUrl(RELEASE_BODY, "linux-riscv64"));
		assertNull(JetBrainsRuntimeDownloader.findDownloadUrl("Release without a table", "linux-x64"));
		assertNull(JetBrainsRuntimeDownloader.findDownloadUrl(RELEASE_BODY, null));
	}

	@Test
	public void mapsPlatformToJetBrainsArchitecture() {
		assertEquals("osx-aarch64", JetBrainsRuntimeDownloader.getArchitecture("Mac OS X", "aarch64"));
		assertEquals("osx-x64", JetBrainsRuntimeDownloader.getArchitecture("Mac OS X", "x86_64"));
		assertEquals("windows-x64", JetBrainsRuntimeDownloader.getArchitecture("Windows 11", "amd64"));
		assertEquals("windows-aarch64", JetBrainsRuntimeDownloader.getArchitecture("Windows 11", "aarch64"));
		assertEquals("linux-x64", JetBrainsRuntimeDownloader.getArchitecture("Linux", "amd64"));
		assertEquals("linux-x86", JetBrainsRuntimeDownloader.getArchitecture("Linux", "i386"));
	}

	@Test
	public void doesNotReportArmMachinesAsX64() {
		// "aarch64" contains "64" and used to be mapped to the x64 downloads
		assertEquals("linux-aarch64", JetBrainsRuntimeDownloader.getArchitecture("Linux", "aarch64"));
		assertEquals("osx-aarch64", JetBrainsRuntimeDownloader.getArchitecture("Mac OS X", "arm64"));
	}

	@Test
	public void returnsNullForUnsupportedOperatingSystem() {
		assertNull(JetBrainsRuntimeDownloader.getArchitecture("SunOS", "sparcv9"));
	}

	@Test
	public void findsLatestStableRelease() {
		String releases = "[" //
				+ "{\"id\": 100, \"tag_name\": \"jbr-release-21.0.5b631.7\", \"prerelease\": false}," //
				+ "{\"id\": 300, \"tag_name\": \"jbr-release-21.0.7b1038.58\", \"prerelease\": false}," //
				+ "{\"id\": 200, \"tag_name\": \"jbr-release-21.0.6b895.91\", \"prerelease\": false}" //
				+ "]";
		assertEquals("300", JetBrainsRuntimeDownloader.findLatestStableReleaseId(releases));
	}

	@Test
	public void ordersPatchLevelsNumerically() {
		// Compared as strings, "21.0.9" sorts above "21.0.10" and the newest
		// release would be skipped once the patch level reaches double digits
		String releases = "[" //
				+ "{\"id\": 100, \"tag_name\": \"jbr-release-21.0.9b1234.1\", \"prerelease\": false}," //
				+ "{\"id\": 200, \"tag_name\": \"jbr-release-21.0.10b1234.1\", \"prerelease\": false}" //
				+ "]";
		assertEquals("200", JetBrainsRuntimeDownloader.findLatestStableReleaseId(releases));
	}

	@Test
	public void skipsPreReleases() {
		String releases = "[" //
				+ "{\"id\": 100, \"tag_name\": \"jbr-release-21.0.5b631.7\", \"prerelease\": false}," //
				+ "{\"id\": 400, \"tag_name\": \"jbr-release-25.0.1b1200.1\", \"prerelease\": true}" //
				+ "]";
		assertEquals("100", JetBrainsRuntimeDownloader.findLatestStableReleaseId(releases));
	}

	@Test
	public void returnsNullWhenThereIsNoRelease() {
		assertNull(JetBrainsRuntimeDownloader.findLatestStableReleaseId("[]"));
		assertNull(JetBrainsRuntimeDownloader.findLatestStableReleaseId("{\"message\": \"rate limit exceeded\"}"));
	}
}
