package com.vaadin.plugin.test;

import static org.junit.Assert.*;

import java.net.MalformedURLException;

import org.junit.Before;
import org.junit.Test;

import com.vaadin.plugin.wizards.HelloWorldProjectModel;

/**
 * Test class for HelloWorldProjectModel. Tests URL generation for hello world
 * projects.
 */
public class HelloWorldProjectModelTest {

	private HelloWorldProjectModel model;

	@Before
	public void setUp() {
		model = new HelloWorldProjectModel();
	}

	@Test
	public void testDefaultValues() {
		assertNotNull("Model should be created", model);
		assertEquals("Default framework should be flow", "flow", model.getFramework());
		assertEquals("Default language should be java", "java", model.getLanguage());
		assertEquals("Default build tool should be maven", "maven", model.getBuildTool());
		assertEquals("Default architecture should be springboot", "springboot", model.getArchitecture());
	}

	@Test
	public void testHelloWorldProjectUrlGeneration() throws MalformedURLException {
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
	}

	@Test
	public void testAllHelloWorldParameters() {
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
	public void testFlowFramework() {
		model.setProjectName("flow-test");
		model.setFramework("flow");

		String url = model.getDownloadUrl();
		assertTrue("Should contain framework=flow", url.contains("framework=flow"));
	}

	@Test
	public void testKotlinLanguage() {
		model.setProjectName("kotlin-test");
		model.setLanguage("kotlin");

		String url = model.getDownloadUrl();
		assertTrue("Should contain language=kotlin", url.contains("language=kotlin"));
	}

	@Test
	public void testGradleBuildTool() {
		model.setProjectName("gradle-test");
		model.setBuildTool("gradle");

		String url = model.getDownloadUrl();
		assertTrue("Should contain buildtool=gradle", url.contains("buildtool=gradle"));
	}

	@Test
	public void testAllArchitectures() {
		String[] architectures = {"springboot", "quarkus", "jakartaee", "servlet"};

		for (String arch : architectures) {
			model.setProjectName("arch-test-" + arch);
			model.setArchitecture(arch);

			String url = model.getDownloadUrl();
			assertTrue("Should contain stack=" + arch, url.contains("stack=" + arch));
		}
	}

	@Test
	public void testCustomGroupIdInUrl() {
		model.setProjectName("custom-groupid-test");
		String customGroupId = "org.example.custom";
		model.setGroupId(customGroupId);

		String url = model.getDownloadUrl();
		assertNotNull("URL should be generated", url);
		// The groupId should be URL-encoded
		String encodedGroupId = java.net.URLEncoder.encode(customGroupId, java.nio.charset.StandardCharsets.UTF_8);
		assertTrue("URL should contain the custom groupId", url.contains("groupId=" + encodedGroupId));
	}
}
