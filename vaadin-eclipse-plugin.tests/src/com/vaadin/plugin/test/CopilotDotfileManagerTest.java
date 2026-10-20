package com.vaadin.plugin.test;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

import com.vaadin.plugin.CopilotDotfileManager;

/**
 * Tests that the .copilot-plugin dotfile is only created for Vaadin projects.
 */
public class CopilotDotfileManagerTest extends BaseIntegrationTest {

	@Test
	public void testDotfileNotCreatedForProjectWithoutBuildFile() {
		CopilotDotfileManager.getInstance().updateDotfile(testProject);

		assertFalse("Dotfile should not be created for a non-Vaadin project", Files.exists(getDotfilePath()));
	}

	@Test
	public void testDotfileNotCreatedForNonVaadinMavenProject() throws CoreException {
		createPom("<groupId>org.example</groupId><artifactId>app</artifactId>"
				+ "<description>Notes about vaadin-grid</description>");

		CopilotDotfileManager.getInstance().updateDotfile(testProject);

		assertFalse("Dotfile should not be created for a non-Vaadin project", Files.exists(getDotfilePath()));
	}

	@Test
	public void testDotfileCreatedForVaadinMavenProject() throws CoreException {
		createPom("<groupId>org.example</groupId><artifactId>app</artifactId><dependencies><dependency>"
				+ "<groupId>com.vaadin</groupId><artifactId>vaadin</artifactId></dependency></dependencies>");

		CopilotDotfileManager.getInstance().updateDotfile(testProject);

		assertTrue("Dotfile should be created for a Vaadin project", Files.exists(getDotfilePath()));
	}

	private void createPom(String content) throws CoreException {
		String pom = "<project><modelVersion>4.0.0</modelVersion>" + content + "</project>";
		testProject.getFile("pom.xml").create(new ByteArrayInputStream(pom.getBytes(StandardCharsets.UTF_8)), true,
				null);
	}

	private Path getDotfilePath() {
		return Paths.get(testProject.getLocation().toString(), ".eclipse", ".copilot-plugin");
	}
}
