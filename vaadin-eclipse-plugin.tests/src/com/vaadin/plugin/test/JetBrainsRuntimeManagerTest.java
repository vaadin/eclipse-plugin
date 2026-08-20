package com.vaadin.plugin.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.plugin.hotswap.JetBrainsRuntimeManager;

/**
 * Tests for locating the Java home of a downloaded JetBrains Runtime.
 */
public class JetBrainsRuntimeManagerTest {

	private Path directory;

	@Before
	public void createDirectory() throws IOException {
		directory = Files.createTempDirectory("jbr-manager-test");
	}

	@After
	public void deleteDirectory() throws IOException {
		if (directory == null || !Files.exists(directory)) {
			return;
		}
		try (Stream<Path> paths = Files.walk(directory)) {
			paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
		}
	}

	@Test
	public void findsJavaHomeInInstallationDirectory() throws IOException {
		Path javaHome = createJavaHome(directory);
		assertEquals(javaHome.toFile(), findJavaHome(directory));
	}

	@Test
	public void findsJavaHomeInsideArchiveRootDirectory() throws IOException {
		// The archives unpack into a directory named after the release, for
		// example jbrsdk-25.0.4-linux-aarch64-b508.27
		Path javaHome = createJavaHome(directory.resolve("jbrsdk-25.0.4-linux-aarch64-b508.27"));
		assertEquals(javaHome.toFile(), findJavaHome(directory));
	}

	@Test
	public void findsJavaHomeInMacOsBundle() throws IOException {
		Path javaHome = createJavaHome(
				directory.resolve("jbrsdk-25.0.4-osx-aarch64-b508.27").resolve("Contents").resolve("Home"));
		assertEquals(javaHome.toFile(), findJavaHome(directory));
	}

	@Test
	public void returnsNullWithoutJavaExecutable() throws IOException {
		Files.createDirectories(directory.resolve("jbrsdk-25.0.4-linux-aarch64-b508.27").resolve("lib"));
		assertNull(findJavaHome(directory));
	}

	@Test
	public void identifiesJetBrainsRuntimeFromReleaseFile() throws IOException {
		Path javaHome = createJavaHome(directory);
		Files.writeString(javaHome.resolve("release"),
				"IMPLEMENTOR=\"JetBrains s.r.o.\"\nJAVA_VERSION=\"25.0.4\"\n");

		assertTrue(manager().isJetBrainsRuntimeHome(javaHome.toFile()));
	}

	@Test
	public void doesNotTreatPlainJdkAsJetBrainsRuntime() throws IOException {
		// ~/.vaadin/jdk is shared with the other Vaadin IDE plugins and may hold a
		// stock JDK, which must not end up receiving the JBR-only -XX flags
		Path javaHome = createJavaHome(directory);
		Files.writeString(javaHome.resolve("release"), "IMPLEMENTOR=\"Eclipse Adoptium\"\nJAVA_VERSION=\"25.0.4\"\n");

		assertFalse(manager().isJetBrainsRuntimeHome(javaHome.toFile()));
	}

	@Test
	public void doesNotGuessVendorWithoutReleaseFile() throws IOException {
		Path javaHome = createJavaHome(directory);

		assertFalse(manager().isJetBrainsRuntimeHome(javaHome.toFile()));
	}

	private static JetBrainsRuntimeManager manager() {
		return JetBrainsRuntimeManager.getInstance();
	}

	private static File findJavaHome(Path directory) {
		return JetBrainsRuntimeManager.getInstance().findJavaHome(directory.toFile());
	}

	private static Path createJavaHome(Path javaHome) throws IOException {
		Path bin = javaHome.resolve("bin");
		Files.createDirectories(bin);
		Files.createFile(bin.resolve(isWindows() ? "java.exe" : "java"));
		return javaHome;
	}

	private static boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase().contains("win");
	}
}
