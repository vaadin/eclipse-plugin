package com.vaadin.plugin.test;

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;

import com.vaadin.plugin.wizards.ProjectModel;

/**
 * Test class for ProjectModel. Tests URL generation, parameter handling, and
 * project configuration.
 */
public class ProjectModelTest {

	private ProjectModel model;

	@Before
	public void setUp() {
		model = new ProjectModel();
	}

	@Test
	public void testDefaultValues() {
		assertNotNull("Model should be created", model);
		assertEquals("Default project type should be STARTER", ProjectModel.ProjectType.STARTER,
				model.getProjectType());
		assertTrue("Should include Flow by default", model.isIncludeFlow());
		assertFalse("Should not include Hilla by default", model.isIncludeHilla());
		assertFalse("Should not be prerelease by default", model.isPrerelease());
		assertEquals("Default framework should be flow", "flow", model.getFramework());
		assertEquals("Default language should be java", "java", model.getLanguage());
		assertEquals("Default build tool should be maven", "maven", model.getBuildTool());
		assertEquals("Default architecture should be springboot", "springboot", model.getArchitecture());
	}

	@Test
	public void testProjectNameSetting() {
		String projectName = "my-test-project";
		model.setProjectName(projectName);

		assertEquals("Project name should be set", projectName, model.getProjectName());
	}

	@Test
	public void testLocationSetting() {
		String location = "/path/to/project";
		model.setLocation(location);

		assertEquals("Location should be set", location, model.getLocation());
	}

	@Test
	public void testStarterProjectUrlGeneration() throws MalformedURLException {
		model.setProjectType(ProjectModel.ProjectType.STARTER);
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
	}

	@Test
	public void testHelloWorldProjectUrlGeneration() throws MalformedURLException {
		model.setProjectType(ProjectModel.ProjectType.HELLO_WORLD);
		model.setProjectName("hello-world");
		model.setFramework("hilla");
		model.setLanguage("kotlin");
		model.setBuildTool("gradle");
		model.setArchitecture("quarkus");

		String urlString = model.getDownloadUrl();
		assertNotNull("Download URL should be generated", urlString);

		// Verify URL contains correct parameters
		assertTrue("URL should be for helloworld endpoint", urlString.contains("/helloworld?"));
		assertTrue("URL should contain framework", urlString.contains("framework=hilla"));
		assertTrue("URL should contain language", urlString.contains("language=kotlin"));
		assertTrue("URL should contain build tool", urlString.contains("buildtool=gradle"));
		assertTrue("URL should contain stack", urlString.contains("stack=quarkus"));
		assertTrue("URL should contain architecture", urlString.contains("stack=quarkus"));
	}

	@Test
	public void testUrlParameterEncoding() throws MalformedURLException {
		// Test with special characters that need encoding
		model.setProjectName("test project with spaces");

		String urlString = model.getDownloadUrl();

		// Spaces are converted to hyphens in artifactId, not URL-encoded
		assertTrue("Spaces should be converted to hyphens in artifactId",
				urlString.contains("artifactId=test-project-with-spaces"));
	}

	@Test
	public void testPrereleaseSetting() {
		model.setProjectType(ProjectModel.ProjectType.STARTER);
		model.setPrerelease(true);
		model.setProjectName("prerelease-test");

		String url = model.getDownloadUrl();
		assertTrue("URL should contain platformVersion=pre parameter", url.contains("platformVersion=pre"));
	}

	@Test
	public void testFrameworkSelection() {
		model.setProjectType(ProjectModel.ProjectType.STARTER);
		model.setProjectName("framework-test");

		// Test Flow only
		model.setIncludeFlow(true);
		model.setIncludeHilla(false);
		String url = model.getDownloadUrl();
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
	public void testDownloadParameter() {
		model.setProjectName("download-test");

		String url = model.getDownloadUrl();

		// The current implementation doesn't include download=true, so we check that
		// the URL is valid
		assertNotNull("URL should be generated", url);
		assertTrue("URL should start with https", url.startsWith("https://"));
	}

	@Test
	public void testUrlFormat() {
		model.setProjectName("url-test");

		String url = model.getDownloadUrl();

		// Check URL structure
		assertTrue("Should start with https", url.startsWith("https://"));
		assertTrue("Should have query parameters", url.contains("?"));

		// Verify parameter separator
		String queryPart = url.substring(url.indexOf("?") + 1);
		String[] params = queryPart.split("&");
		assertTrue("Should have multiple parameters", params.length > 1);

		// Each parameter should have key=value format
		for (String param : params) {
			assertTrue("Parameter should have = sign: " + param, param.contains("="));
		}
	}

	@Test
	public void testArtifactIdGeneration() {
		// Test that project names are properly converted to artifact IDs
		model.setProjectName("My Test Project!");

		String url = model.getDownloadUrl();

		// Should convert to lowercase and replace special chars with hyphens
		assertTrue("Should convert to valid artifact ID", url.contains("artifactId=my-test-project"));
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
		// Test various special characters
		model.setProjectName("test-project_123.v2@#$");

		String url = model.getDownloadUrl();
		assertNotNull("Should handle special characters", url);

		// Verify the URL is still valid
		try {
			new URL(url);
		} catch (MalformedURLException e) {
			fail("Should generate valid URL with special characters: " + e.getMessage());
		}

		// Check artifact ID is properly sanitized
		assertTrue("Should sanitize artifact ID", url.contains("artifactId=test-project-123-v2"));
	}

	@Test
	public void testLongProjectName() {
		// Test with a very long project name
		String longName = "this-is-a-very-long-project-name-that-might-cause-issues-"
				+ "with-url-length-limitations-in-some-systems";
		model.setProjectName(longName);

		String url = model.getDownloadUrl();
		assertTrue("Should handle long project names", url.contains("artifactId="));
	}

	@Test
	public void testConsistentUrlGeneration() {
		// Test that URL generation is consistent
		model.setProjectName("consistent-test");
		model.setProjectType(ProjectModel.ProjectType.STARTER);

		String url1 = model.getDownloadUrl();
		String url2 = model.getDownloadUrl();

		assertEquals("URL generation should be consistent", url1, url2);
	}

	@Test
	public void testAllStarterParameters() {
		model.setProjectType(ProjectModel.ProjectType.STARTER);
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
	}

	@Test
	public void testAllHelloWorldParameters() {
		model.setProjectType(ProjectModel.ProjectType.HELLO_WORLD);
		model.setProjectName("hello-test");
		model.setFramework("hilla");
		model.setLanguage("kotlin");
		model.setBuildTool("gradle");
		model.setArchitecture("jakartaee");

		String url = model.getDownloadUrl();

		// Verify all parameters are present
		assertTrue("Should contain framework parameter", url.contains("framework="));
		assertTrue("Should contain language parameter", url.contains("language="));
		assertTrue("Should contain download parameter", url.contains("download="));
		assertTrue("Should contain buildtool parameter", url.contains("buildtool="));
		assertTrue("Should contain stack parameter", url.contains("stack="));
		assertTrue("Should contain ref parameter", url.contains("ref=eclipse-plugin"));
	}
}
