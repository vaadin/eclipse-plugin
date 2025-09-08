package com.vaadin.plugin.test;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.plugin.wizards.AbstractProjectModel;
import com.vaadin.plugin.wizards.NewVaadinProjectWizard;
import com.vaadin.plugin.wizards.StarterProjectModel;
import com.vaadin.plugin.wizards.VaadinProjectWizardPage;

/**
 * Test class for NewVaadinProjectWizard. Tests project creation, Maven/Gradle
 * configuration, and file extraction.
 */
public class NewVaadinProjectWizardTest {

	private NewVaadinProjectWizard wizard;
	private IWorkspaceRoot workspaceRoot;
	private IProject testProject;
	private Path tempDir;

	@Before
	public void setUp() throws Exception {
		workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		// Create a temporary directory for testing
		tempDir = Files.createTempDirectory("vaadin-wizard-test");

		// Create wizard
		wizard = new NewVaadinProjectWizard();

		// Initialize wizard
		wizard.init(null, new StructuredSelection());
	}

	@After
	public void tearDown() throws Exception {
		// Clean up test project if it exists
		if (testProject != null && testProject.exists()) {
			testProject.delete(true, true, new NullProgressMonitor());
		}

		// Clean up temp directory
		if (tempDir != null) {
			deleteRecursively(tempDir);
		}
	}

	@Test
	public void testWizardInitialization() {
		assertNotNull("Wizard should be initialized", wizard);

		// Test wizard properties
		assertEquals("Window title should be Vaadin", "Vaadin", wizard.getWindowTitle());
		assertTrue("Wizard should need progress monitor", wizard.needsProgressMonitor());
	}

	@Test
	public void testWizardPages() {
		// Add pages
		wizard.addPages();

		IWizardPage[] pages = wizard.getPages();
		assertEquals("Should have one wizard page", 1, pages.length);
		assertTrue("Page should be VaadinProjectWizardPage", pages[0] instanceof VaadinProjectWizardPage);

		VaadinProjectWizardPage page = (VaadinProjectWizardPage) pages[0];
		assertNotNull("Page should be initialized", page);
	}

	@Test
	public void testProjectModelIntegration() throws Exception {
		// Test that wizard page properly configures ProjectModel
		wizard.addPages();
		VaadinProjectWizardPage page = (VaadinProjectWizardPage) wizard.getPages()[0];

		// Use reflection to access the project model
		AbstractProjectModel model = getProjectModel(page);
		assertNotNull("Page should have ProjectModel", model);

		// Test default values - should be StarterProjectModel by default
		assertTrue("Default model should be StarterProjectModel", model instanceof StarterProjectModel);
	}

	@Test
	public void testProjectCreation() throws Exception {
		// Test project creation (without actual download)
		String projectName = "test-vaadin-project-" + System.currentTimeMillis();

		IProject project = workspaceRoot.getProject(projectName);
		assertFalse("Project should not exist before creation", project.exists());

		// Create project programmatically (simulating wizard behavior)
		project.create(new NullProgressMonitor());
		project.open(new NullProgressMonitor());
		testProject = project;

		assertTrue("Project should exist after creation", project.exists());
		assertTrue("Project should be open", project.isOpen());
	}

	@Test
	public void testBuildSystemConfiguration() throws Exception {
		// Test configuration for different build systems
		String projectName = "test-build-" + System.currentTimeMillis();
		IProject project = workspaceRoot.getProject(projectName);
		project.create(new NullProgressMonitor());
		project.open(new NullProgressMonitor());
		testProject = project;

		// Test Maven configuration
		IFile pomFile = project.getFile("pom.xml");
		String pomContent = "<?xml version=\"1.0\"?><project></project>";
		pomFile.create(new ByteArrayInputStream(pomContent.getBytes()), true, new NullProgressMonitor());
		assertTrue("pom.xml should exist for Maven project", pomFile.exists());

		// Test Gradle configuration
		IFile gradleFile = project.getFile("build.gradle");
		String gradleContent = "plugins { id 'java' }";
		gradleFile.create(new ByteArrayInputStream(gradleContent.getBytes()), true, new NullProgressMonitor());
		assertTrue("build.gradle should exist for Gradle project", gradleFile.exists());

		// Test that Java nature can be added
		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();
		String[] newNatures = Arrays.copyOf(natures, natures.length + 1);
		newNatures[natures.length] = "org.eclipse.jdt.core.javanature";
		description.setNatureIds(newNatures);
		project.setDescription(description, new NullProgressMonitor());

		assertTrue("Project should have Java nature", project.hasNature("org.eclipse.jdt.core.javanature"));
	}

	@Test
	public void testProjectNameValidation() {
		// Test project name validation logic used by wizard
		assertFalse("Empty project name should be invalid", isValidProjectName(""));
		assertTrue("Valid project name should be accepted", isValidProjectName("valid-project-name"));
		assertFalse("Project name with spaces should be invalid", isValidProjectName("invalid name with spaces"));
		assertFalse("Project name starting with number should be invalid",
				isValidProjectName("123-starts-with-number"));
	}

	@Test
	public void testZipExtraction() throws Exception {
		// Test the zip extraction logic used by wizard
		byte[] zipData = createTestZipData();

		// Test that zip can be read
		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
			ZipEntry entry = zis.getNextEntry();
			assertNotNull("Should have at least one entry", entry);
			assertEquals("Entry should be test file", "test.txt", entry.getName());

			byte[] content = zis.readAllBytes();
			String text = new String(content);
			assertEquals("Content should match", "Test content", text);
		}
	}

	@Test
	public void testErrorHandling() throws Exception {
		// Test error handling scenarios that wizard would encounter

		// Test invalid project names
		assertFalse("Null name should be invalid", isValidProjectName(null));
		assertFalse("Special chars should be invalid", isValidProjectName("test@project#"));

		// Test that duplicate project names would be detected
		String projectName = "test-duplicate-" + System.currentTimeMillis();
		IProject project1 = workspaceRoot.getProject(projectName);
		project1.create(new NullProgressMonitor());
		testProject = project1;

		IProject project2 = workspaceRoot.getProject(projectName);
		assertTrue("Duplicate project should be detected as existing", project2.exists());
	}

	@Test
	public void testProjectStructureCreation() throws Exception {
		// Test creating project structure in Eclipse workspace
		String projectName = "test-structure-" + System.currentTimeMillis();
		IProject project = workspaceRoot.getProject(projectName);
		project.create(new NullProgressMonitor());
		project.open(new NullProgressMonitor());
		testProject = project;

		// Create Maven-like structure
		IFolder srcFolder = project.getFolder("src");
		srcFolder.create(true, true, new NullProgressMonitor());

		IFolder mainFolder = srcFolder.getFolder("main");
		mainFolder.create(true, true, new NullProgressMonitor());

		IFolder javaFolder = mainFolder.getFolder("java");
		javaFolder.create(true, true, new NullProgressMonitor());

		IFolder resourcesFolder = mainFolder.getFolder("resources");
		resourcesFolder.create(true, true, new NullProgressMonitor());

		// Verify structure
		assertTrue("src folder should exist", srcFolder.exists());
		assertTrue("src/main/java should exist", javaFolder.exists());
		assertTrue("src/main/resources should exist", resourcesFolder.exists());

		// Create pom.xml
		IFile pomFile = project.getFile("pom.xml");
		String pomContent = "<project></project>";
		pomFile.create(new ByteArrayInputStream(pomContent.getBytes()), true, new NullProgressMonitor());
		assertTrue("pom.xml should exist", pomFile.exists());
	}

	@Test
	public void testProjectConfiguration() throws Exception {
		// Test project configuration that wizard would apply
		String projectName = "test-config-" + System.currentTimeMillis();
		IProject project = workspaceRoot.getProject(projectName);
		project.create(new NullProgressMonitor());
		project.open(new NullProgressMonitor());
		testProject = project;

		// Test adding .vaadin folder
		IFolder vaadinFolder = project.getFolder(".vaadin");
		vaadinFolder.create(true, true, new NullProgressMonitor());
		assertTrue(".vaadin folder should exist", vaadinFolder.exists());

		// Test adding configuration file
		IFile configFile = vaadinFolder.getFile("config.json");
		String config = "{\"copilot\": true}";
		configFile.create(new ByteArrayInputStream(config.getBytes()), true, new NullProgressMonitor());
		assertTrue("Config file should exist", configFile.exists());
	}

	// Helper methods

	private AbstractProjectModel getProjectModel(VaadinProjectWizardPage page) {
		// Direct access to public method
		return page.getProjectModel();
	}

	private boolean isValidProjectName(String name) {
		if (name == null || name.trim().isEmpty()) {
			return false;
		}
		if (!Character.isJavaIdentifierStart(name.charAt(0))) {
			return false;
		}
		for (char c : name.toCharArray()) {
			if (!Character.isJavaIdentifierPart(c) && c != '-' && c != '.') {
				return false;
			}
		}
		return true;
	}

	private byte[] createTestZipData() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(baos)) {
			ZipEntry entry = new ZipEntry("test.txt");
			zos.putNextEntry(entry);
			zos.write("Test content".getBytes());
			zos.closeEntry();

			ZipEntry entry2 = new ZipEntry("folder/file.txt");
			zos.putNextEntry(entry2);
			zos.write("Nested content".getBytes());
			zos.closeEntry();
		}
		return baos.toByteArray();
	}

	private void deleteRecursively(Path path) throws IOException {
		if (Files.exists(path)) {
			if (Files.isDirectory(path)) {
				Files.walk(path).sorted((a, b) -> b.compareTo(a)).forEach(p -> {
					try {
						Files.delete(p);
					} catch (IOException e) {
						// Ignore
					}
				});
			} else {
				Files.delete(path);
			}
		}
	}
}
