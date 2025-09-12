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
			// Try to close the project first to release any locks
			if (testProject.isOpen()) {
				testProject.close(null);
			}
			
			// Add a small delay to allow file handles to be released on Windows
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// Ignore
			}
			
			// Try to delete with retry logic for Windows file locking issues
			int retries = 3;
			CoreException lastException = null;
			for (int i = 0; i < retries; i++) {
				try {
					testProject.delete(true, true, null);
					return; // Success
				} catch (CoreException e) {
					lastException = e;
					if (i < retries - 1) {
						try {
							Thread.sleep(200);
						} catch (InterruptedException ie) {
							// Ignore
						}
					}
				}
			}
			// If we still couldn't delete after retries, throw the last exception
			if (lastException != null) {
				throw lastException;
			}
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
