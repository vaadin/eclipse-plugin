package com.vaadin.plugin.test;

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;

import com.vaadin.plugin.wizards.StarterProjectModel;

/**
 * Test class for StarterProjectModel. Tests URL generation for starter
 * projects.
 */
public class StarterProjectModelTest {

	private StarterProjectModel model;

	@Before
	public void setUp() {
		model = new StarterProjectModel();
	}

	@Test
	public void testDefaultValues() {
		assertNotNull("Model should be created", model);
		assertTrue("Should include Flow by default", model.isIncludeFlow());
		assertFalse("Should not include Hilla by default", model.isIncludeHilla());
		assertFalse("Should not be prerelease by default", model.isPrerelease());
	}

	@Test
	public void testStarterProjectUrlGeneration() throws MalformedURLException {
		model.setProjectName("test-project");

		String urlString = model.getDownloadUrl();
		assertNotNull("Download URL should be generated", urlString);

		// Verify it's a valid URL
		URL url = new URL(urlString);
		assertNotNull("Should create valid URL object", url);

		// Check URL contains expected parameters
		assertTrue("URL should contain artifactId", urlString.contains("artifactId=test-project"));
		assertTrue("URL should contain base URL", urlString.contains("start.vaadin.com"));
		assertTrue("URL should be for skeleton endpoint", urlString.contains("/skeleton?"));
		assertTrue("URL should contain frameworks parameter", urlString.contains("frameworks="));
		assertTrue("URL should contain platformVersion parameter", urlString.contains("platformVersion="));
	}

	@Test
	public void testPrereleaseSetting() {
		model.setPrerelease(true);
		model.setProjectName("prerelease-test");

		String url = model.getDownloadUrl();
		assertTrue("URL should contain platformVersion=pre parameter", url.contains("platformVersion=pre"));
	}

	@Test
	public void testFrameworkSelection() {
		model.setProjectName("framework-test");

		// Test Empty selection (neither Flow nor Hilla)
		model.setIncludeFlow(false);
		model.setIncludeHilla(false);
		String url = model.getDownloadUrl();
		assertFalse("Shouldn't contain frameworks parameter", url.matches(".*frameworks=.*"));

		// Test Flow only
		model.setIncludeFlow(true);
		model.setIncludeHilla(false);
		url = model.getDownloadUrl();
		assertTrue("Should have frameworks=flow", url.contains("frameworks=flow"));

		// Test Hilla only
		model.setIncludeFlow(false);
		model.setIncludeHilla(true);
		url = model.getDownloadUrl();
		assertTrue("Should have frameworks=hilla", url.contains("frameworks=hilla"));

		// Test both (Flow and Hilla)
		model.setIncludeFlow(true);
		model.setIncludeHilla(true);
		url = model.getDownloadUrl();
		assertTrue("Should have frameworks=flow,hilla", url.contains("frameworks=flow,hilla"));
	}

	@Test
	public void testNullProjectName() {
		// Test that null project name doesn't break URL generation
		model.setProjectName(null);

		String url = model.getDownloadUrl();
		assertNotNull("Should handle null project name", url);
		assertTrue("Should still be valid URL format", url.startsWith("https://"));
		assertTrue("Should use default artifactId", url.contains("artifactId=my-app"));
	}

	@Test
	public void testSpecialCharactersInProjectName() {
		model.setProjectName("test-project_123.v2@#$");

		String url = model.getDownloadUrl();
		assertNotNull("Should handle special characters", url);
		assertTrue("Special characters should be replaced", url.contains("artifactId=test-project-123-v2"));
	}

	@Test
	public void testAllStarterParameters() {
		model.setProjectName("full-test");
		model.setPrerelease(true);
		model.setIncludeFlow(true);
		model.setIncludeHilla(true);

		String url = model.getDownloadUrl();

		// Verify all parameters are present
		assertTrue("Should contain artifactId parameter", url.contains("artifactId="));
		assertTrue("Should contain frameworks parameter", url.contains("frameworks="));
		assertTrue("Should contain platformVersion parameter", url.contains("platformVersion="));
		assertTrue("Should contain ref parameter", url.contains("ref=eclipse-plugin"));
		assertTrue("Should contain download parameter", url.contains("download=true"));
	}

	@Test
	public void testLatestVersionByDefault() {
		model.setProjectName("latest-test");
		model.setPrerelease(false);

		String url = model.getDownloadUrl();
		assertTrue("Should contain platformVersion=latest", url.contains("platformVersion=latest"));
	}
}
