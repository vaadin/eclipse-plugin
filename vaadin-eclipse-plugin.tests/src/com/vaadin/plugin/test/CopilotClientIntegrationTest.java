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
