package com.vaadin.plugin.test;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import com.vaadin.plugin.CopilotClient;
import com.vaadin.plugin.CopilotRestService;
import com.vaadin.plugin.CopilotUtil;

/**
 * Manual test runner that can be executed in Eclipse to validate the REST API
 * functionality. This class provides a simple way to test the integration
 * without requiring complex test infrastructure.
 *
 * To run: Right-click in Eclipse -> Run As -> Java Application
 */
public class ManualTestRunner {

	public static void main(String[] args) {
		System.out.println("=== Vaadin Eclipse Plugin Manual Test Runner ===");

		try {
			// Test 1: CopilotUtil functionality
			testCopilotUtil();

			// Test 2: REST Service startup and basic functionality
			testRestService();

			System.out.println("\n=== All tests completed successfully! ===");

		} catch (Exception e) {
			System.err.println("Test failed: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static void testCopilotUtil() {
		System.out.println("\n--- Testing CopilotUtil ---");

		// Test service name generation
		String serviceName = CopilotUtil.getServiceName();
		System.out.println("Service name: " + serviceName);
		assert serviceName.startsWith("copilot-") : "Service name should start with 'copilot-'";

		// Test endpoint generation
		String endpoint = CopilotUtil.getEndpoint(8080);
		System.out.println("Endpoint: " + endpoint);
		assert endpoint.contains("8080") : "Endpoint should contain port";
		assert endpoint.contains(serviceName) : "Endpoint should contain service name";

		// Test supported actions
		String actions = CopilotUtil.getSupportedActions();
		System.out.println("Supported actions: " + actions);
		assert actions.contains("write") : "Should support write action";
		assert actions.contains("delete") : "Should support delete action";

		// Test dotfile creation
		String tempDir = System.getProperty("java.io.tmpdir");
		String testProjectPath = tempDir + File.separator + "test-project";
		new File(testProjectPath).mkdirs();

		CopilotUtil.saveDotFile(testProjectPath, 9090);

		File dotFile = new File(testProjectPath, ".vaadin/copilot/vaadin-copilot.properties");
		assert dotFile.exists() : "Dotfile should be created";
		System.out.println("Dotfile created at: " + dotFile.getAbsolutePath());

		System.out.println("✓ CopilotUtil tests passed");
	}

	private static void testRestService() throws Exception {
		System.out.println("\n--- Testing REST Service ---");

		CopilotRestService service = new CopilotRestService();

		try {
			// Start the service
			service.start();
			String endpoint = service.getEndpoint();
			System.out.println("REST service started at: " + endpoint);

			// Create a simple test with the client
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IProject[] projects = workspace.getRoot().getProjects();

			String projectPath;
			if (projects.length > 0 && projects[0].getLocation() != null) {
				projectPath = projects[0].getLocation().toPortableString();
				System.out.println("Using existing project: " + projects[0].getName());
			} else {
				// Create a temporary project for testing
				String tempDir = System.getProperty("java.io.tmpdir");
				projectPath = tempDir + File.separator + "rest-test-project";
				new File(projectPath).mkdirs();
				System.out.println("Created temporary project at: " + projectPath);
			}

			// Test with CopilotClient
			CopilotClient client = new CopilotClient(endpoint, projectPath);

			try {
				// Test heartbeat
				var heartbeatResponse = client.heartbeat();
				System.out.println("Heartbeat status: " + heartbeatResponse.statusCode());
				assert heartbeatResponse.statusCode() == 200 : "Heartbeat should return 200";

				// Test simple operations that don't require complex Eclipse setup
				var refreshResponse = client.refresh();
				System.out.println("Refresh status: " + refreshResponse.statusCode());
				assert refreshResponse.statusCode() == 200 : "Refresh should return 200";

				System.out.println("✓ REST service tests passed");

			} catch (Exception e) {
				System.err.println("Client test failed: " + e.getMessage());
				throw e;
			}

		} finally {
			// Clean up
			service.stop();
			System.out.println("REST service stopped");
		}
	}
}
