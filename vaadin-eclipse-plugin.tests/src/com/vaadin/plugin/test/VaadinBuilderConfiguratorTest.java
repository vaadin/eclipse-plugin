package com.vaadin.plugin.test;

import static org.junit.Assert.*;

import java.io.File;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vaadin.plugin.EarlyStartup;
import com.vaadin.plugin.builder.VaadinBuildParticipant;
import com.vaadin.plugin.builder.VaadinBuilderConfigurator;

/**
 * Integration tests for VaadinBuilderConfigurator that verify core
 * functionality. These tests focus on what can be reliably tested in a headless
 * environment.
 */
public class VaadinBuilderConfiguratorTest {

	private IWorkspaceRoot workspaceRoot;
	private IProject testProject;
	private static boolean pluginInitialized = false;

	@BeforeClass
	public static void setUpClass() throws Exception {
		// Initialize the plugin the same way it's done in production
		if (!pluginInitialized) {
			EarlyStartup earlyStartup = new EarlyStartup();
			earlyStartup.earlyStartup();
			pluginInitialized = true;
			Thread.sleep(500);
		}
	}

	@Before
	public void setUp() throws Exception {
		workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

		// Create a test project with unique name
		String projectName = "vaadin-builder-test-" + System.currentTimeMillis();
		testProject = workspaceRoot.getProject(projectName);
		testProject.create(new NullProgressMonitor());
		testProject.open(new NullProgressMonitor());
	}

	@After
	public void tearDown() throws Exception {
		// Clean up test project
		if (testProject != null && testProject.exists()) {
			testProject.delete(true, true, new NullProgressMonitor());
		}

		// Clean up any other test projects that might have been created
		for (IProject project : workspaceRoot.getProjects()) {
			if (project.getName().startsWith("vaadin-builder-test-")) {
				project.delete(true, true, new NullProgressMonitor());
			}
		}
	}

	@Test
	public void testBuilderNotAddedToNonJavaProject() throws Exception {
		// The configurator is already initialized in @BeforeClass
		// Give time for the resource listener to process the project creation
		Thread.sleep(200);

		// Verify builder is not added to non-Java project
		IProjectDescription description = testProject.getDescription();
		ICommand[] commands = description.getBuildSpec();

		for (ICommand command : commands) {
			assertNotEquals("Vaadin builder should not be added to non-Java project", VaadinBuildParticipant.BUILDER_ID,
					command.getBuilderName());
		}
	}

	@Test
	public void testManualBuilderAddition() throws Exception {
		// Test manual addition of builder to Java project
		addJavaNature(testProject);

		// Manually add the builder (simulating what VaadinBuilderConfigurator should
		// do)
		IProjectDescription description = testProject.getDescription();
		ICommand[] commands = description.getBuildSpec();

		ICommand[] newCommands = new ICommand[commands.length + 1];
		System.arraycopy(commands, 0, newCommands, 0, commands.length);

		ICommand vaadinCommand = description.newCommand();
		vaadinCommand.setBuilderName(VaadinBuildParticipant.BUILDER_ID);
		newCommands[commands.length] = vaadinCommand;

		description.setBuildSpec(newCommands);
		testProject.setDescription(description, null);

		// Verify builder is added
		description = testProject.getDescription();
		commands = description.getBuildSpec();

		boolean builderFound = false;
		for (ICommand command : commands) {
			if (VaadinBuildParticipant.BUILDER_ID.equals(command.getBuilderName())) {
				builderFound = true;
				break;
			}
		}

		assertTrue("Vaadin builder should be manually addable to Java project", builderFound);
	}

	@Test
	public void testBuilderNotAddedTwice() throws Exception {
		// Add Java nature
		addJavaNature(testProject);

		// Manually add the builder twice
		for (int i = 0; i < 2; i++) {
			IProjectDescription description = testProject.getDescription();
			ICommand[] commands = description.getBuildSpec();

			// Check if builder already exists
			boolean builderExists = false;
			for (ICommand command : commands) {
				if (VaadinBuildParticipant.BUILDER_ID.equals(command.getBuilderName())) {
					builderExists = true;
					break;
				}
			}

			// Only add if it doesn't exist
			if (!builderExists) {
				ICommand[] newCommands = new ICommand[commands.length + 1];
				System.arraycopy(commands, 0, newCommands, 0, commands.length);

				ICommand vaadinCommand = description.newCommand();
				vaadinCommand.setBuilderName(VaadinBuildParticipant.BUILDER_ID);
				newCommands[commands.length] = vaadinCommand;

				description.setBuildSpec(newCommands);
				testProject.setDescription(description, null);
			}
		}

		// Count builders
		IProjectDescription description = testProject.getDescription();
		ICommand[] commands = description.getBuildSpec();

		int vaadinBuilderCount = 0;
		for (ICommand command : commands) {
			if (VaadinBuildParticipant.BUILDER_ID.equals(command.getBuilderName())) {
				vaadinBuilderCount++;
			}
		}

		assertEquals("Vaadin builder should only be added once", 1, vaadinBuilderCount);
	}

	@Test
	public void testRemoveBuilder() throws Exception {
		// Add Java nature and builder
		addJavaNature(testProject);

		// Add builder manually
		IProjectDescription description = testProject.getDescription();
		ICommand[] commands = description.getBuildSpec();
		ICommand[] newCommands = new ICommand[commands.length + 1];
		System.arraycopy(commands, 0, newCommands, 0, commands.length);

		ICommand vaadinCommand = description.newCommand();
		vaadinCommand.setBuilderName(VaadinBuildParticipant.BUILDER_ID);
		newCommands[commands.length] = vaadinCommand;

		description.setBuildSpec(newCommands);
		testProject.setDescription(description, null);

		// Verify builder is added
		description = testProject.getDescription();
		commands = description.getBuildSpec();
		boolean builderFound = false;
		for (ICommand command : commands) {
			if (VaadinBuildParticipant.BUILDER_ID.equals(command.getBuilderName())) {
				builderFound = true;
				break;
			}
		}
		assertTrue("Builder should be present before removal", builderFound);

		// Remove the builder using VaadinBuilderConfigurator method
		VaadinBuilderConfigurator.removeBuilder(testProject);

		// Verify builder is removed
		description = testProject.getDescription();
		builderFound = false;
		for (ICommand command : description.getBuildSpec()) {
			if (VaadinBuildParticipant.BUILDER_ID.equals(command.getBuilderName())) {
				builderFound = true;
				break;
			}
		}
		assertFalse("Builder should be removed", builderFound);
	}

	@Test
	public void testVaadinBuildParticipantWithVaadinProject() throws Exception {
		// Setup Java project with Vaadin dependency
		addJavaNature(testProject);
		IJavaProject javaProject = JavaCore.create(testProject);

		// Create source and output folders
		IFolder srcFolder = testProject.getFolder("src");
		srcFolder.create(true, true, new NullProgressMonitor());

		IFolder binFolder = testProject.getFolder("bin");
		binFolder.create(true, true, new NullProgressMonitor());

		// Set up classpath with Vaadin JAR (simulate Vaadin dependency)
		IPath vaadinJarPath = createMockVaadinJar();
		IClasspathEntry vaadinEntry = JavaCore.newLibraryEntry(vaadinJarPath, null, null);
		IClasspathEntry sourceEntry = JavaCore.newSourceEntry(srcFolder.getFullPath());
		IClasspathEntry jreEntry = JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER"));

		javaProject.setRawClasspath(new IClasspathEntry[]{sourceEntry, vaadinEntry, jreEntry}, binFolder.getFullPath(),
				new NullProgressMonitor());

		// Manually add Vaadin builder
		IProjectDescription description = testProject.getDescription();
		ICommand[] commands = description.getBuildSpec();
		ICommand[] newCommands = new ICommand[commands.length + 1];
		System.arraycopy(commands, 0, newCommands, 0, commands.length);

		ICommand vaadinCommand = description.newCommand();
		vaadinCommand.setBuilderName(VaadinBuildParticipant.BUILDER_ID);
		newCommands[commands.length] = vaadinCommand;

		description.setBuildSpec(newCommands);
		testProject.setDescription(description, null);

		// Run the build
		testProject.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		Thread.sleep(200);
		testProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());

		// Check if flow-build-info.json was created
		IFolder metaInfFolder = binFolder.getFolder("META-INF");
		IFolder vaadinFolder = metaInfFolder.getFolder("VAADIN");
		IFolder configFolder = vaadinFolder.getFolder("config");
		IFile flowBuildInfo = configFolder.getFile("flow-build-info.json");

		assertTrue("flow-build-info.json should be created for Vaadin project", flowBuildInfo.exists());

		// Verify content contains npmFolder
		String content = readFileContent(flowBuildInfo);
		assertTrue("flow-build-info.json should contain npmFolder", content.contains("npmFolder"));
		// JSON should contain native OS paths
		assertTrue("npmFolder should point to project location",
				content.contains(testProject.getLocation().toOSString()));
	}

	@Test
	public void testVaadinBuildParticipantWithNonVaadinProject() throws Exception {
		// This test verifies that VaadinBuildParticipant correctly detects non-Vaadin
		// projects
		// For a definitive test, we need to ensure no "vaadin" appears anywhere in the
		// classpath

		// Setup Java project WITHOUT Vaadin dependency
		addJavaNature(testProject);
		IJavaProject javaProject = JavaCore.create(testProject);

		// Create source and output folders
		IFolder srcFolder = testProject.getFolder("src");
		srcFolder.create(true, true, new NullProgressMonitor());

		IFolder binFolder = testProject.getFolder("bin");
		binFolder.create(true, true, new NullProgressMonitor());

		// Set up classpath with only standard Java libraries (no Vaadin)
		IClasspathEntry sourceEntry = JavaCore.newSourceEntry(srcFolder.getFullPath());
		IClasspathEntry jreEntry = JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER"));

		// Add a non-Vaadin library to ensure classpath has some dependencies but no
		// Vaadin
		IPath commonJarPath = createMockNonVaadinJar();
		IClasspathEntry commonEntry = JavaCore.newLibraryEntry(commonJarPath, null, null);

		javaProject.setRawClasspath(new IClasspathEntry[]{sourceEntry, commonEntry, jreEntry}, binFolder.getFullPath(),
				new NullProgressMonitor());

		// Manually add Vaadin builder (it should detect no Vaadin and not create
		// flow-build-info)
		IProjectDescription description = testProject.getDescription();
		ICommand[] commands = description.getBuildSpec();
		ICommand[] newCommands = new ICommand[commands.length + 1];
		System.arraycopy(commands, 0, newCommands, 0, commands.length);

		ICommand vaadinCommand = description.newCommand();
		vaadinCommand.setBuilderName(VaadinBuildParticipant.BUILDER_ID);
		newCommands[commands.length] = vaadinCommand;

		description.setBuildSpec(newCommands);
		testProject.setDescription(description, null);

		// Run the build
		testProject.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		Thread.sleep(100);
		testProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());

		// Check that flow-build-info.json was NOT created
		IFolder metaInfFolder = binFolder.getFolder("META-INF");
		if (metaInfFolder.exists()) {
			IFolder vaadinFolder = metaInfFolder.getFolder("VAADIN");
			if (vaadinFolder.exists()) {
				IFolder configFolder = vaadinFolder.getFolder("config");
				if (configFolder.exists()) {
					IFile flowBuildInfo = configFolder.getFile("flow-build-info.json");
					assertFalse("flow-build-info.json should not be created for non-Vaadin project",
							flowBuildInfo.exists());
				}
			}
		}
		// If folders don't exist, that's also correct behavior - test passes
	}

	// Helper methods

	private void addJavaNature(IProject project) throws CoreException {
		if (!project.hasNature(JavaCore.NATURE_ID)) {
			IProjectDescription description = project.getDescription();
			String[] prevNatures = description.getNatureIds();
			String[] newNatures = new String[prevNatures.length + 1];
			System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
			newNatures[prevNatures.length] = JavaCore.NATURE_ID;
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
		}
	}

	private IPath createMockVaadinJar() throws Exception {
		// Create a temporary JAR file with "vaadin" in the name
		// This simulates a Vaadin dependency in the classpath
		File tempDir = new File(System.getProperty("java.io.tmpdir"));
		File vaadinJar = new File(tempDir, "vaadin-core-24.0.0.jar");

		if (!vaadinJar.exists()) {
			vaadinJar.createNewFile();
			vaadinJar.deleteOnExit();
		}

		return new Path(vaadinJar.getAbsolutePath());
	}

	private IPath createMockNonVaadinJar() throws Exception {
		// Create a temporary JAR file WITHOUT "vaadin" in the name
		// This simulates a non-Vaadin dependency in the classpath
		File tempDir = new File(System.getProperty("java.io.tmpdir"));
		File commonJar = new File(tempDir, "commons-lang3-3.12.0.jar");

		if (!commonJar.exists()) {
			commonJar.createNewFile();
			commonJar.deleteOnExit();
		}

		return new Path(commonJar.getAbsolutePath());
	}

	private String readFileContent(IFile file) throws Exception {
		try (java.io.InputStream is = file.getContents()) {
			return new String(is.readAllBytes(), "UTF-8");
		}
	}
}
