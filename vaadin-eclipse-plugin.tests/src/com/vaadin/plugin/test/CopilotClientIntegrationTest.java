package com.vaadin.plugin.test;

import static org.junit.Assert.*;

import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.vaadin.plugin.CopilotClient;
import com.vaadin.plugin.CopilotRestService;

/**
 * Integration tests for CopilotClient that test the client-side REST API calls
 * against a real CopilotRestService instance.
 */
public class CopilotClientIntegrationTest extends BaseIntegrationTest {

	private CopilotRestService restService;
	private CopilotClient client;

	@Override
	protected void doSetUp() throws CoreException {
		restService = new CopilotRestService();

		try {
			restService.start();
			String endpoint = restService.getEndpoint();
			String projectPath = testProject.getLocation().toString();

			client = new CopilotClient(endpoint, projectPath);

			// Give the server a moment to fully start
			Thread.sleep(100);
		} catch (Exception e) {
			fail("Failed to start REST service: " + e.getMessage());
		}
	}

	@Override
	protected void doTearDown() throws CoreException {
		if (restService != null) {
			restService.stop();
			restService = null;
		}
		if (client != null) {
			client = null;
		}
		// Force garbage collection to release file handles
		System.gc();
		// Small delay to allow cleanup
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// Ignore
		}
	}

	@Test
	public void testClientHeartbeat() throws Exception {
		HttpResponse<String> response = client.heartbeat();

		assertEquals("HTTP status should be 200", 200, response.statusCode());
		assertNotNull("Response body should not be null", response.body());
		assertTrue("Response should contain status", response.body().contains("\"status\""));
		assertTrue("Response should contain 'alive'", response.body().contains("alive"));
	}

	@Test
	public void testClientWriteFile() throws Exception {
		Path filePath = Paths.get(testProject.getLocation().toString(), "client-test.txt");
		String content = "Content written by CopilotClient";

		HttpResponse<String> response = client.write(filePath, content);

		assertEquals("HTTP status should be 200", 200, response.statusCode());
		assertNotNull("Response body should not be null", response.body());
		assertTrue("Response should indicate success", response.body().contains("\"status\":\"ok\""));

		// Verify file was created
		IFile file = testProject.getFile("client-test.txt");
		assertTrue("File should exist after client write", file.exists());

		// Verify content
		try (java.io.InputStream is = file.getContents()) {
			String actualContent = new String(is.readAllBytes(), "UTF-8");
			assertEquals("File content should match", content, actualContent);
		}
	}

	@Test
	public void testClientWriteBinaryFile() throws Exception {
		byte[] binaryData = "Binary data from client\u0000\u0001\u0002".getBytes("UTF-8");
		String base64Content = Base64.getEncoder().encodeToString(binaryData);
		Path filePath = Paths.get(testProject.getLocation().toString(), "client-binary.dat");

		HttpResponse<String> response = client.writeBinary(filePath, base64Content);

		assertEquals("HTTP status should be 200", 200, response.statusCode());
		assertNotNull("Response body should not be null", response.body());
		assertTrue("Response should indicate success", response.body().contains("\"status\":\"ok\""));

		// Verify file was created
		IFile file = testProject.getFile("client-binary.dat");
		assertTrue("Binary file should exist after client write", file.exists());

		// Verify binary content
		try (java.io.InputStream is = file.getContents()) {
			byte[] actualContent = is.readAllBytes();
			assertArrayEquals("Binary content should match", binaryData, actualContent);
		}
	}

	@Test
	public void testClientDeleteFile() throws Exception {
		// First create a file to delete
		IFile file = testProject.getFile("client-delete.txt");
		file.create(new java.io.ByteArrayInputStream("Delete me via client".getBytes()), true, null);
		assertTrue("File should exist before delete", file.exists());

		Path filePath = Paths.get(file.getLocation().toString());
		HttpResponse<String> response = client.delete(filePath);

		assertEquals("HTTP status should be 200", 200, response.statusCode());
		assertNotNull("Response body should not be null", response.body());
		assertTrue("Response should indicate success", response.body().contains("\"status\":\"ok\""));

		// Verify file was deleted
		assertFalse("File should not exist after client delete", file.exists());
	}

	@Test
	public void testClientRefresh() throws Exception {
		HttpResponse<String> response = client.refresh();

		assertEquals("HTTP status should be 200", 200, response.statusCode());
		assertNotNull("Response body should not be null", response.body());
		assertTrue("Response should indicate success", response.body().contains("\"status\":\"ok\""));
	}

	@Test
	public void testClientShowInIde() throws Exception {
		// Create a test file
		IFile file = testProject.getFile("client-show.txt");
		String content = "Line 1\nLine 2\nTarget line for client test\nLine 4";
		file.create(new java.io.ByteArrayInputStream(content.getBytes()), true, null);

		Path filePath = Paths.get(file.getLocation().toString());
		HttpResponse<String> response = client.showInIde(filePath, 3, 0);

		assertEquals("HTTP status should be 200", 200, response.statusCode());
		assertNotNull("Response body should not be null", response.body());
		assertTrue("Response should indicate success", response.body().contains("\"status\":\"ok\""));
	}

	@Test
	public void testClientUndoRedo() throws Exception {
		Path filePath = Paths.get(testProject.getLocation().toString(), "undo-test.txt");

		// Test undo (currently stubbed, but should not fail)
		HttpResponse<String> undoResponse = client.undo(filePath);
		assertEquals("HTTP status should be 200", 200, undoResponse.statusCode());

		// Test redo (currently stubbed, but should not fail)
		HttpResponse<String> redoResponse = client.redo(filePath);
		assertEquals("HTTP status should be 200", 200, redoResponse.statusCode());
	}

	@Test
	public void testClientRestartApplication() throws Exception {
		HttpResponse<String> response = client.restartApplication();

		assertEquals("HTTP status should be 200", 200, response.statusCode());
		assertNotNull("Response body should not be null", response.body());
		// Currently stubbed, but should not fail
		assertTrue("Response should indicate success", response.body().contains("\"status\":\"ok\""));
	}

	@Test
	public void testClientGetVaadinRoutes() throws Exception {
		Optional<JsonObject> response = client.getVaadinRoutes();

		assertTrue("Response should be present", response.isPresent());
		JsonObject responseObj = response.get();
		assertTrue("Response should contain routes", responseObj.has("routes"));
		// Currently returns empty array, but structure should be correct
		assertTrue("Routes should be an array", responseObj.get("routes").isJsonArray());
	}

	@Test
	public void testClientGetVaadinVersion() throws Exception {
		Optional<JsonObject> response = client.getVaadinVersion();

		assertTrue("Response should be present", response.isPresent());
		JsonObject responseObj = response.get();
		assertTrue("Response should contain version", responseObj.has("version"));
		assertNotNull("Version should not be null", responseObj.get("version").getAsString());
	}

	@Test
	public void testClientGetVaadinComponents() throws Exception {
		Optional<JsonObject> response = client.getVaadinComponents(true);

		assertTrue("Response should be present", response.isPresent());
		JsonObject responseObj = response.get();
		assertTrue("Response should contain components", responseObj.has("components"));
		assertTrue("Components should be an array", responseObj.get("components").isJsonArray());
	}

	@Test
	public void testClientGetVaadinEntities() throws Exception {
		Optional<JsonObject> response = client.getVaadinEntities(false);

		assertTrue("Response should be present", response.isPresent());
		JsonObject responseObj = response.get();
		assertTrue("Response should contain entities", responseObj.has("entities"));
		assertTrue("Entities should be an array", responseObj.get("entities").isJsonArray());
	}

	@Test
	public void testClientGetVaadinSecurity() throws Exception {
		Optional<JsonObject> response = client.getVaadinSecurity();

		assertTrue("Response should be present", response.isPresent());
		JsonObject responseObj = response.get();
		assertTrue("Response should contain security", responseObj.has("security"));
		assertTrue("Security should be an array", responseObj.get("security").isJsonArray());
	}

	@Test
	public void testGetModulePathsWithNativePath() throws Exception {
		// Test that getModulePaths works with native OS paths
		// On Windows: paths with backslashes like "C:\\dev\\project"
		// On Linux/Mac: paths with forward slashes like "/home/user/project"
		// The Copilot JavaSourcePathDetector expects a response with a "project" field

		// Get the actual project path as the OS would return it
		String actualProjectPath = testProject.getLocation().toOSString();

		// Use the native OS path format
		String testPath = actualProjectPath;
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			// On Windows, verify path contains backslashes
			assertTrue("Windows path should contain backslashes", testPath.contains("\\"));
		} else {
			// On Unix, verify path contains forward slashes
			assertTrue("Unix path should contain forward slashes", testPath.contains("/"));
		}

		// Send a raw REST request simulating what Copilot sends
		String jsonRequest = String.format("{\"command\":\"getModulePaths\",\"projectBasePath\":\"%s\",\"data\":{}}",
				testPath.replace("\\", "\\\\") // Escape backslashes for JSON
		);

		java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
		java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
				.uri(java.net.URI.create(restService.getEndpoint())).header("Content-Type", "application/json")
				.POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonRequest)).build();

		java.net.http.HttpResponse<String> response = httpClient.send(request,
				java.net.http.HttpResponse.BodyHandlers.ofString());

		assertEquals("HTTP status should be 200", 200, response.statusCode());

		// Parse the response
		com.google.gson.JsonObject responseObj = new com.google.gson.Gson().fromJson(response.body(),
				com.google.gson.JsonObject.class);

		// CRITICAL: The response MUST have a "project" field, not an "error" field
		// This is what fails without the fix - Copilot gets null for project field
		assertFalse("Response should NOT contain error field", responseObj.has("error"));
		assertTrue("Response MUST contain 'project' field for Copilot compatibility", responseObj.has("project"));

		JsonObject project = responseObj.getAsJsonObject("project");
		assertNotNull("Project field must not be null (causes NullPointerException in Copilot)", project);

		assertTrue("Project must have basePath", project.has("basePath"));
		assertTrue("Project must have modules array", project.has("modules"));

		// The basePath in response should match the actual project location
		String responseBasePath = project.get("basePath").getAsString();
		// Compare normalized paths
		String normalizedActual = actualProjectPath.replace('\\', '/');
		String normalizedResponse = responseBasePath.replace('\\', '/');
		assertEquals("Response basePath should match project location", normalizedActual, normalizedResponse);

		// CRITICAL: Verify that the modules array contains actual data
		// Without the fix, the project won't be found and this data won't be populated
		com.google.gson.JsonArray modules = project.getAsJsonArray("modules");
		assertNotNull("Modules array must not be null", modules);
		assertTrue("At least one module should be present", modules.size() > 0);

		// Check the first module has the expected fields
		JsonObject firstModule = modules.get(0).getAsJsonObject();
		assertTrue("Module must have name", firstModule.has("name"));
		assertTrue("Module must have contentRoots", firstModule.has("contentRoots"));

		// Since this is a test project, it should have the test project name
		String moduleName = firstModule.get("name").getAsString();
		assertEquals("Module name should match test project", "vaadin-test-project", moduleName);

		// Verify contentRoots contains actual paths
		com.google.gson.JsonArray contentRoots = firstModule.getAsJsonArray("contentRoots");
		assertTrue("Content roots should not be empty", contentRoots.size() > 0);
		String contentRoot = contentRoots.get(0).getAsString();
		// The content root should be a valid path containing the project name
		assertTrue("Content root should contain project name",
				contentRoot.replace('\\', '/').contains("vaadin-test-project"));
	}

	@Test
	public void testGetModulePathsWithWindowsStylePath() throws Exception {
		// This test verifies the fix for the Windows path comparison issue
		// The bug: On Windows, Copilot sends paths with backslashes (C:\dev\project)
		// but Eclipse's toPortableString() returns forward slashes (C:/dev/project)
		// causing project lookup to fail and resulting in NullPointerException

		String actualProjectPath = testProject.getLocation().toOSString();

		// Simulate Windows-style path with backslashes
		String windowsStylePath;
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			// On Windows, use actual path with backslashes
			windowsStylePath = actualProjectPath;
		} else {
			// On Unix, simulate Windows path by converting slashes to backslashes
			// This tests that the service can handle Windows paths even on Unix
			windowsStylePath = actualProjectPath.replace('/', '\\');
		}

		// Send request with Windows-style path
		String jsonRequest = String.format("{\"command\":\"getModulePaths\",\"projectBasePath\":\"%s\",\"data\":{}}",
				windowsStylePath.replace("\\", "\\\\") // Escape for JSON
		);

		java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
		java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
				.uri(java.net.URI.create(restService.getEndpoint())).header("Content-Type", "application/json")
				.POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonRequest)).build();

		java.net.http.HttpResponse<String> response = httpClient.send(request,
				java.net.http.HttpResponse.BodyHandlers.ofString());

		assertEquals("HTTP status should be 200", 200, response.statusCode());

		com.google.gson.JsonObject responseObj = new com.google.gson.Gson().fromJson(response.body(),
				com.google.gson.JsonObject.class);

		// The fix ensures that Windows paths are properly matched
		assertFalse("Should not have error when using Windows-style path", responseObj.has("error"));
		assertTrue("Must have project field", responseObj.has("project"));

		JsonObject project = responseObj.getAsJsonObject("project");
		assertNotNull("Project must not be null (was causing NPE before fix)", project);

		// Verify the project was found and populated correctly
		com.google.gson.JsonArray modules = project.getAsJsonArray("modules");
		assertTrue("Should find modules when project is matched", modules.size() > 0);

		// Verify the module name to ensure we found the right project
		String moduleName = modules.get(0).getAsJsonObject().get("name").getAsString();
		assertEquals("Should find the correct project", "vaadin-test-project", moduleName);
	}

	@Test
	public void testGetModulePathsWithNonExistentProject() throws Exception {
		// This test verifies that getModulePaths returns a valid empty structure
		// instead of an error when the project is not found
		// This is important for Copilot compatibility

		String nonExistentPath = "/non/existent/project/path";

		String jsonRequest = String.format("{\"command\":\"getModulePaths\",\"projectBasePath\":\"%s\",\"data\":{}}",
				nonExistentPath);

		java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
		java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
				.uri(java.net.URI.create(restService.getEndpoint())).header("Content-Type", "application/json")
				.POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonRequest)).build();

		java.net.http.HttpResponse<String> response = httpClient.send(request,
				java.net.http.HttpResponse.BodyHandlers.ofString());

		assertEquals("HTTP status should be 200", 200, response.statusCode());

		com.google.gson.JsonObject responseObj = new com.google.gson.Gson().fromJson(response.body(),
				com.google.gson.JsonObject.class);

		// Critical: Must return empty project structure, not an error
		assertFalse("Should NOT return error for non-existent project", responseObj.has("error"));
		assertTrue("Must have project field even when project not found", responseObj.has("project"));

		JsonObject project = responseObj.getAsJsonObject("project");
		assertNotNull("Project field must not be null", project);

		// Should have the requested path and empty modules
		assertTrue("Should have basePath field", project.has("basePath"));
		assertEquals("basePath should match requested path", nonExistentPath, project.get("basePath").getAsString());

		assertTrue("Should have modules field", project.has("modules"));
		com.google.gson.JsonArray modules = project.getAsJsonArray("modules");
		assertEquals("Modules should be empty for non-existent project", 0, modules.size());
	}

	@Test
	public void testClientErrorHandling() throws Exception {
		// Test with invalid project path
		CopilotClient invalidClient = new CopilotClient(restService.getEndpoint(), "/invalid/project/path");
		Path filePath = Paths.get("/invalid/path/file.txt");

		try {
			HttpResponse<String> response = invalidClient.write(filePath, "content");
			// Should get a response but with error status
			assertEquals("HTTP status should be 200 (error in response body)", 200, response.statusCode());
			assertTrue("Response should contain error", response.body().contains("error"));
		} catch (Exception e) {
			// Network errors are also acceptable for invalid requests
			assertNotNull("Exception should have a message", e.getMessage());
		}
	}
}
