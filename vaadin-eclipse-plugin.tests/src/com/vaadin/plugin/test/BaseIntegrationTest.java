package com.vaadin.plugin.test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.junit.After;
import org.junit.Before;

/**
 * Base class for integration tests that need a real Eclipse project.
 */
public abstract class BaseIntegrationTest {

	protected static final String TEST_PROJECT_NAME = "vaadin-test-project";
	protected IProject testProject;
	protected IWorkspace workspace;

	@Before
	public void setUp() throws CoreException {
		workspace = ResourcesPlugin.getWorkspace();

		// Create a test project
		testProject = workspace.getRoot().getProject(TEST_PROJECT_NAME);
		if (!testProject.exists()) {
			IProjectDescription description = workspace.newProjectDescription(TEST_PROJECT_NAME);
			testProject.create(description, null);
		}

		if (!testProject.isOpen()) {
			testProject.open(null);
		}

		// Additional setup in subclasses
		doSetUp();
	}

	@After
	public void tearDown() throws CoreException {
		// Additional cleanup in subclasses
		doTearDown();

		// Clean up test project
		if (testProject != null && testProject.exists()) {
			testProject.delete(true, true, null);
		}
	}

	/**
	 * Override in subclasses for additional setup.
	 */
	protected void doSetUp() throws CoreException {
		// Default implementation does nothing
	}

	/**
	 * Override in subclasses for additional cleanup.
	 */
	protected void doTearDown() throws CoreException {
		// Default implementation does nothing
	}
}
