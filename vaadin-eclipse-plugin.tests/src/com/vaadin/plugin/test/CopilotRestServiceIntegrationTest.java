package com.vaadin.plugin.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.vaadin.plugin.CopilotRestService;
import com.vaadin.plugin.Message;

/**
 * Integration tests for CopilotRestService that test the full REST API chain
 * with real file operations in an Eclipse workspace.
 */
public class CopilotRestServiceIntegrationTest extends BaseIntegrationTest {

	private CopilotRestService restService;
	private String baseEndpoint;
	private HttpClient httpClient;
	private Gson gson;

	@Override
	protected void doSetUp() throws CoreException {
		restService = new CopilotRestService();
		httpClient = HttpClient.newHttpClient();
		gson = new Gson();

		try {
			restService.start();
			baseEndpoint = restService.getEndpoint();
			assertNotNull("REST service endpoint should not be null", baseEndpoint);

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
	public void testHeartbeat() throws Exception {
		// Test the heartbeat endpoint to ensure service is running
		String response = sendRestRequest("heartbeat", new Message.HeartbeatMessage());

		assertNotNull("Response should not be null", response);
		JsonObject responseObj = gson.fromJson(response, JsonObject.class);
		assertEquals("alive", responseObj.get("status").getAsString());
		assertEquals("eclipse", responseObj.get("ide").getAsString());
	}

	@Test
	public void testWriteFileEndpoint() throws Exception {
		// Test writing a new file
		String fileName = testProject.getLocation().append("test-file.txt").toString();
		String content = "Hello, World!\nThis is a test file.";

		Message.WriteFileMessage writeMsg = new Message.WriteFileMessage(fileName, "Test Write", content);
		String response = sendRestRequest("write", writeMsg);

		assertNotNull("Response should not be null", response);
		JsonObject responseObj = gson.fromJson(response, JsonObject.class);

		if (responseObj.has("error")) {
			// In headless mode, workbench is not available
			String error = responseObj.get("error").getAsString();
			assertTrue("Expected workbench error in headless mode",
					error.contains("Workbench") || error.contains("not been created"));
			return; // Skip file verification in headless mode
		}

		assertEquals("ok", responseObj.get("status").getAsString());

		// Verify the file was actually created
		IFile file = testProject.getFile("test-file.txt");
		assertTrue("File should exist after write", file.exists());

		// Verify file contents
		try (java.io.InputStream is = file.getContents()) {
			String actualContent = new String(is.readAllBytes(), "UTF-8");
			assertEquals("File content should match", content, actualContent);
		}
	}

	@Test
	public void testWriteFileInSubdirectory() throws Exception {
		// Test writing a file in a subdirectory (should create parent folders)
		String fileName = testProject.getLocation().append("src/main/java/Test.java").toString();
		String content = "public class Test {\n    // Generated file\n}";

		Message.WriteFileMessage writeMsg = new Message.WriteFileMessage(fileName, "Test Write", content);
		String response = sendRestRequest("write", writeMsg);

		assertNotNull("Response should not be null", response);
		JsonObject responseObj = gson.fromJson(response, JsonObject.class);

		if (responseObj.has("error")) {
			// In headless mode, workbench is not available
			String error = responseObj.get("error").getAsString();
			assertTrue("Expected workbench error in headless mode",
					error.contains("Workbench") || error.contains("not been created"));
			return; // Skip file verification in headless mode
		}

		assertEquals("ok", responseObj.get("status").getAsString());

		// Verify the file and parent directories were created
		IFolder srcFolder = testProject.getFolder("src");
		assertTrue("src folder should exist", srcFolder.exists());

		IFolder mainFolder = srcFolder.getFolder("main");
		assertTrue("main folder should exist", mainFolder.exists());

		IFolder javaFolder = mainFolder.getFolder("java");
		assertTrue("java folder should exist", javaFolder.exists());

		IFile file = javaFolder.getFile("Test.java");
		assertTrue("File should exist after write", file.exists());

		// Verify file contents
		try (java.io.InputStream is = file.getContents()) {
			String actualContent = new String(is.readAllBytes(), "UTF-8");
			assertEquals("File content should match", content, actualContent);
		}
	}

	@Test
	public void testWriteBase64Endpoint() throws Exception {
		// Test writing a binary file using base64 encoding
		byte[] binaryData = "This is binary data\u0000\u0001\u0002".getBytes("UTF-8");
		String base64Content = Base64.getEncoder().encodeToString(binaryData);
		String fileName = testProject.getLocation().append("binary-file.dat").toString();

		Message.WriteFileMessage writeMsg = new Message.WriteFileMessage(fileName, "Test Base64 Write", base64Content);
		String response = sendRestRequest("writeBase64", writeMsg);

		assertNotNull("Response should not be null", response);
		JsonObject responseObj = gson.fromJson(response, JsonObject.class);

		if (responseObj.has("error")) {
			// In headless mode, workbench is not available
			String error = responseObj.get("error").getAsString();
			assertTrue("Expected workbench error in headless mode",
					error.contains("Workbench") || error.contains("not been created"));
			return; // Skip file verification in headless mode
		}

		assertEquals("ok", responseObj.get("status").getAsString());

		// Verify the file was created with correct binary content
		IFile file = testProject.getFile("binary-file.dat");
		assertTrue("Binary file should exist after write", file.exists());

		// Verify file contents
		try (java.io.InputStream is = file.getContents()) {
			byte[] actualContent = is.readAllBytes();
			assertArrayEquals("Binary file content should match", binaryData, actualContent);
		}
	}

	@Test
	public void testDeleteEndpoint() throws Exception {
		// First create a file
		IFile file = testProject.getFile("to-delete.txt");
		file.create(new java.io.ByteArrayInputStream("Delete me".getBytes()), true, null);
		assertTrue("File should exist before delete", file.exists());

		// Test deleting the file
		String fileName = file.getLocation().toString();
		Message.DeleteMessage deleteMsg = new Message.DeleteMessage(fileName);
		String response = sendRestRequest("delete", deleteMsg);

		assertNotNull("Response should not be null", response);
		JsonObject responseObj = gson.fromJson(response, JsonObject.class);

		if (responseObj.has("error")) {
			// In headless mode, workbench is not available
			String error = responseObj.get("error").getAsString();
			assertTrue("Expected workbench error in headless mode",
					error.contains("Workbench") || error.contains("not been created"));
			return; // Skip file verification in headless mode
		}

		assertEquals("ok", responseObj.get("status").getAsString());

		// Verify the file was deleted
		assertFalse("File should not exist after delete", file.exists());
	}

	@Test
	public void testRefreshEndpoint() throws Exception {
		// Create a file outside of Eclipse's knowledge
		java.io.File externalFile = new java.io.File(testProject.getLocation().toFile(), "external-file.txt");
		try (java.io.FileWriter writer = new java.io.FileWriter(externalFile)) {
			writer.write("Created externally");
		}

		// The file should not be visible to Eclipse initially
		IFile eclipseFile = testProject.getFile("external-file.txt");
		assertFalse("File should not be visible before refresh", eclipseFile.exists());

		// Test refresh endpoint
		String response = sendRestRequest("refresh", new Message.RefreshMessage());

		assertNotNull("Response should not be null", response);
		JsonObject responseObj = gson.fromJson(response, JsonObject.class);

		if (responseObj.has("error")) {
			// In headless mode, workbench is not available
			String error = responseObj.get("error").getAsString();
			assertTrue("Expected workbench error in headless mode",
					error.contains("Workbench") || error.contains("not been created"));
			// Clean up
			externalFile.delete();
			return; // Skip file verification in headless mode
		}

		assertEquals("ok", responseObj.get("status").getAsString());

		// After refresh, the file should be visible to Eclipse
		assertTrue("File should be visible after refresh", eclipseFile.exists());

		// Clean up
		externalFile.delete();
	}

	@Test
	public void testShowInIdeEndpoint() throws Exception {
		// Create a test file with multiple lines
		String content = "Line 1\nLine 2\nLine 3\nTarget line\nLine 5";
		IFile file = testProject.getFile("show-in-ide.txt");
		file.create(new java.io.ByteArrayInputStream(content.getBytes()), true, null);

		// Test opening the file at a specific line
		String fileName = file.getLocation().toString();
		Message.ShowInIdeMessage showMsg = new Message.ShowInIdeMessage(fileName, 4, 0); // Target line

		String response = sendRestRequest("showInIde", showMsg);

		assertNotNull("Response should not be null", response);
		JsonObject responseObj = gson.fromJson(response, JsonObject.class);

		if (responseObj.has("error")) {
			// In headless mode, workbench is not available
			String error = responseObj.get("error").getAsString();
			assertTrue("Expected workbench error in headless mode",
					error.contains("Workbench") || error.contains("not been created"));
			return; // Skip verification in headless mode
		}

		assertEquals("ok", responseObj.get("status").getAsString());

		// Note: We can't easily verify that the editor actually opened to the correct
		// line
		// in a headless test environment, but we can verify the endpoint responds
		// correctly
	}

	@Test
	public void testWriteUpdateExistingFile() throws Exception {
		// Create an initial file
		String fileName = testProject.getLocation().append("update-test.txt").toString();
		String initialContent = "Initial content";

		Message.WriteFileMessage writeMsg1 = new Message.WriteFileMessage(fileName, "Initial Write", initialContent);
		sendRestRequest("write", writeMsg1);

		// Verify initial file
		IFile file = testProject.getFile("update-test.txt");
		assertTrue("File should exist after initial write", file.exists());

		// Update the file with new content
		String updatedContent = "Updated content\nWith new line";
		Message.WriteFileMessage writeMsg2 = new Message.WriteFileMessage(fileName, "Update Write", updatedContent);
		String response = sendRestRequest("write", writeMsg2);

		assertNotNull("Response should not be null", response);
		JsonObject responseObj = gson.fromJson(response, JsonObject.class);

		if (responseObj.has("error")) {
			// In headless mode, workbench is not available
			String error = responseObj.get("error").getAsString();
			assertTrue("Expected workbench error in headless mode",
					error.contains("Workbench") || error.contains("not been created"));
			return; // Skip file verification in headless mode
		}

		assertEquals("ok", responseObj.get("status").getAsString());

		// Verify the file was updated
		try (java.io.InputStream is = file.getContents()) {
			String actualContent = new String(is.readAllBytes(), "UTF-8");
			assertEquals("File content should be updated", updatedContent, actualContent);
		}
	}

	@Test
	public void testErrorHandling() throws Exception {
		// Test writing to an invalid path (outside project)
		String invalidFileName = "/invalid/path/outside/project.txt";
		String content = "This should fail";

		Message.WriteFileMessage writeMsg = new Message.WriteFileMessage(invalidFileName, "Invalid Write", content);
		String response = sendRestRequest("write", writeMsg);

		assertNotNull("Response should not be null", response);
		JsonObject responseObj = gson.fromJson(response, JsonObject.class);
		assertTrue("Response should contain error", responseObj.has("error"));
		assertNotNull("Error message should not be null", responseObj.get("error").getAsString());
	}

	/**
	 * Helper method to send REST requests to the service.
	 */
	private String sendRestRequest(String command, Object data) throws IOException, InterruptedException {
		Message.CopilotRestRequest request = new Message.CopilotRestRequest(command,
				testProject.getLocation().toString(), data);

		String requestBody = gson.toJson(request);

		HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(baseEndpoint))
				.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();

		HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

		assertEquals("HTTP status should be 200", 200, httpResponse.statusCode());
		return httpResponse.body();
	}
}
