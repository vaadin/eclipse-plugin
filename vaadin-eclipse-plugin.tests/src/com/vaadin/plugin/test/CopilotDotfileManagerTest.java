package com.vaadin.plugin.test;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Test;

import com.vaadin.plugin.CopilotDotfileManager;

/**
 * Tests that the .copilot-plugin dotfile is only created for Vaadin projects.
 */
public class CopilotDotfileManagerTest extends BaseIntegrationTest {

	@Test
	public void testDotfileNotCreatedForNonVaadinJavaProject() throws CoreException {
		makeJavaProject();
		createFile("src/com/example/Service.java", "package com.example;\npublic class Service {}\n");

		CopilotDotfileManager.getInstance().updateDotfile(testProject);

		assertFalse("Dotfile should not be created for a non-Vaadin project", Files.exists(getDotfilePath()));
	}

	@Test
	public void testDotfileNotCreatedForNonVaadinMavenProject() throws CoreException {
		createFile("pom.xml", "<project><groupId>org.example</groupId><artifactId>app</artifactId>"
				+ "<description>Notes about vaadin-grid</description></project>");

		CopilotDotfileManager.getInstance().updateDotfile(testProject);

		assertFalse("Dotfile should not be created for a non-Vaadin project", Files.exists(getDotfilePath()));
	}

	@Test
	public void testDotfileCreatedForVaadinDependencyInBuildFile() throws CoreException, IOException {
		for (String buildFile : new String[]{"pom.xml", "build.gradle", "build.gradle.kts"}) {
			createFile(buildFile, "dependency on com.vaadin:vaadin");

			CopilotDotfileManager.getInstance().updateDotfile(testProject);

			assertTrue("Dotfile should be created for a Vaadin project with " + buildFile,
					Files.exists(getDotfilePath()));
			testProject.getFile(buildFile).delete(true, null);
			Files.delete(getDotfilePath());
		}
	}

	@Test
	public void testDotfileCreatedForJavaProjectWithVaadinServiceOnClasspath() throws CoreException {
		makeJavaProject();
		createFile("src/com/vaadin/flow/server/VaadinService.java",
				"package com.vaadin.flow.server;\npublic class VaadinService {}\n");

		CopilotDotfileManager.getInstance().updateDotfile(testProject);

		assertTrue("Dotfile should be created for a project with flow-server", Files.exists(getDotfilePath()));
	}

	@Test
	public void testDotfileCreatedForJavaProjectWithRoute() throws CoreException {
		makeJavaProject();
		createFile("src/com/example/MainView.java", "package com.example;\n"
				+ "import com.vaadin.flow.router.Route;\n@Route(\"\")\npublic class MainView {}\n");

		CopilotDotfileManager.getInstance().updateDotfile(testProject);

		assertTrue("Dotfile should be created for a project with Vaadin routes", Files.exists(getDotfilePath()));
	}

	private void makeJavaProject() throws CoreException {
		IProjectDescription description = testProject.getDescription();
		description.setNatureIds(new String[]{JavaCore.NATURE_ID});
		testProject.setDescription(description, null);

		IFolder srcFolder = testProject.getFolder("src");
		srcFolder.create(true, true, null);
		IClasspathEntry containerEntry = JavaCore.newContainerEntry(
				org.eclipse.core.runtime.Path.fromPortableString("org.eclipse.jdt.launching.JRE_CONTAINER"));
		JavaCore.create(testProject).setRawClasspath(
				new IClasspathEntry[]{JavaCore.newSourceEntry(srcFolder.getFullPath()), containerEntry}, null);
	}

	private void createFile(String path, String content) throws CoreException {
		IFile file = testProject.getFile(path);
		for (int i = 1; i < file.getFullPath().segmentCount() - 1; i++) {
			IFolder folder = testProject.getFolder(file.getProjectRelativePath().uptoSegment(i));
			if (!folder.exists()) {
				folder.create(true, true, null);
			}
		}
		file.create(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), true, null);
	}

	private Path getDotfilePath() {
		return Paths.get(testProject.getLocation().toString(), ".eclipse", ".copilot-plugin");
	}
}
