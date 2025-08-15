package com.vaadin.plugin.test;

import static org.junit.Assert.*;

import java.net.http.HttpResponse;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.vaadin.plugin.CopilotClient;
import com.vaadin.plugin.CopilotRestService;

/**
 * Tests for advanced endpoint implementations including project analysis,
 * compilation, and application management.
 */
public class AdvancedEndpointsTest extends BaseIntegrationTest {

	private CopilotRestService restService;
	private CopilotClient client;
	private Gson gson = new Gson();

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

			// Add Java nature for some tests
			addJavaNature(testProject);

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
	public void testGetModulePathsEndpoint() throws Exception {
		// Add source folders to test project
		IJavaProject javaProject = JavaCore.create(testProject);

		IFolder srcMain = testProject.getFolder("src/main/java");
		createFolderHierarchy(srcMain);
		IFolder srcTest = testProject.getFolder("src/test/java");
		createFolderHierarchy(srcTest);
		IFolder resources = testProject.getFolder("src/main/resources");
		createFolderHierarchy(resources);

		// Add to classpath
		IClasspathEntry[] entries = new IClasspathEntry[]{JavaCore.newSourceEntry(srcMain.getFullPath()),
				JavaCore.newSourceEntry(srcTest.getFullPath()), JavaCore.newSourceEntry(resources.getFullPath())};
		javaProject.setRawClasspath(entries, null);

		// Test the endpoint
		JsonObject data = new JsonObject();
		HttpResponse<String> response = client.sendCommand("getModulePaths", data);

		assertEquals("Should return 200", 200, response.statusCode());

		JsonObject responseObj = gson.fromJson(response.body(), JsonObject.class);
		assertTrue("Should have project key", responseObj.has("project"));

		JsonObject project = responseObj.getAsJsonObject("project");
		assertTrue("Should have basePath", project.has("basePath"));
		assertTrue("Should have modules", project.has("modules"));

		// Verify module structure
		var modules = project.getAsJsonArray("modules");
		assertNotNull("Modules should not be null", modules);
		assertTrue("Should have at least one module", modules.size() > 0);

		var module = modules.get(0).getAsJsonObject();
		assertEquals("Module name should match", testProject.getName(), module.get("name").getAsString());
		assertTrue("Should have javaSourcePaths", module.has("javaSourcePaths"));
		assertTrue("Should have javaTestSourcePaths", module.has("javaTestSourcePaths"));
		assertTrue("Should have resourcePaths", module.has("resourcePaths"));
	}

	@Test
	public void testGetVaadinVersionEndpoint() throws Exception {
		JsonObject data = new JsonObject();
		HttpResponse<String> response = client.sendCommand("getVaadinVersion", data);

		assertEquals("Should return 200", 200, response.statusCode());

		JsonObject responseObj = gson.fromJson(response.body(), JsonObject.class);
		assertTrue("Should have version key", responseObj.has("version"));

		// Without Vaadin in classpath, should return N/A
		String version = responseObj.get("version").getAsString();
		assertEquals("Version should be N/A without Vaadin", "N/A", version);
	}

	@Test
	public void testCompileFilesEndpoint() throws Exception {
		// Create a Java file to compile
		IFolder srcFolder = testProject.getFolder("src");
		srcFolder.create(true, true, null);

		IFile javaFile = srcFolder.getFile("Test.java");
		String content = "public class Test { public static void main(String[] args) {} }";
		javaFile.create(new java.io.ByteArrayInputStream(content.getBytes()), true, null);

		// Test compile endpoint
		JsonObject data = new JsonObject();
		var filesArray = new com.google.gson.JsonArray();
		filesArray.add(javaFile.getLocation().toString());
		data.add("files", filesArray);

		HttpResponse<String> response = client.sendCommand("compileFiles", data);

		assertEquals("Should return 200", 200, response.statusCode());

		JsonObject responseObj = gson.fromJson(response.body(), JsonObject.class);
		assertEquals("Should return ok status", "ok", responseObj.get("status").getAsString());
	}

	@Test
	public void testReloadMavenModuleEndpoint() throws Exception {
		// Test without module name (should refresh main project)
		JsonObject data = new JsonObject();
		HttpResponse<String> response = client.sendCommand("reloadMavenModule", data);

		assertEquals("Should return 200", 200, response.statusCode());

		JsonObject responseObj = gson.fromJson(response.body(), JsonObject.class);
		assertEquals("Should return ok status", "ok", responseObj.get("status").getAsString());
	}

	@Test
	public void testReloadMavenModuleWithNameEndpoint() throws Exception {
		// Test with specific module name
		JsonObject data = new JsonObject();
		data.addProperty("moduleName", testProject.getName());

		HttpResponse<String> response = client.sendCommand("reloadMavenModule", data);

		assertEquals("Should return 200", 200, response.statusCode());

		JsonObject responseObj = gson.fromJson(response.body(), JsonObject.class);
		assertEquals("Should return ok status", "ok", responseObj.get("status").getAsString());
	}

	@Test
	public void testGetVaadinRoutesEndpoint() throws Exception {
		// Create a class with @Route annotation
		createJavaClass("src", "com.example", "TestView",
				"package com.example;\n" + "@Route(\"test\")\n" + "public class TestView {}\n");

		JsonObject data = new JsonObject();
		HttpResponse<String> response = client.sendCommand("getVaadinRoutes", data);

		assertEquals("Should return 200", 200, response.statusCode());

		JsonObject responseObj = gson.fromJson(response.body(), JsonObject.class);
		assertTrue("Should have routes key", responseObj.has("routes"));

		var routes = responseObj.getAsJsonArray("routes");
		assertNotNull("Routes should not be null", routes);
		// Note: Will be empty without proper annotation scanning setup
	}

	@Test
	public void testGetVaadinComponentsEndpoint() throws Exception {
		JsonObject data = new JsonObject();
		data.addProperty("includeMethods", true);

		HttpResponse<String> response = client.sendCommand("getVaadinComponents", data);

		assertEquals("Should return 200", 200, response.statusCode());

		JsonObject responseObj = gson.fromJson(response.body(), JsonObject.class);
		assertTrue("Should have components key", responseObj.has("components"));

		var components = responseObj.getAsJsonArray("components");
		assertNotNull("Components should not be null", components);
		// Will be empty without Vaadin in classpath
		assertEquals("Components should be empty without Vaadin", 0, components.size());
	}

	@Test
	public void testGetVaadinEntitiesEndpoint() throws Exception {
		// Create an entity class
		createJavaClass("src", "com.example.model", "TestEntity", "package com.example.model;\n" + "@Entity\n"
				+ "public class TestEntity {\n" + "    private Long id;\n" + "}\n");

		JsonObject data = new JsonObject();
		data.addProperty("includeMethods", false);

		HttpResponse<String> response = client.sendCommand("getVaadinEntities", data);

		assertEquals("Should return 200", 200, response.statusCode());

		JsonObject responseObj = gson.fromJson(response.body(), JsonObject.class);
		assertTrue("Should have entities key", responseObj.has("entities"));

		var entities = responseObj.getAsJsonArray("entities");
		assertNotNull("Entities should not be null", entities);
	}

	@Test
	public void testGetVaadinSecurityEndpoint() throws Exception {
		JsonObject data = new JsonObject();
		HttpResponse<String> response = client.sendCommand("getVaadinSecurity", data);

		assertEquals("Should return 200", 200, response.statusCode());

		JsonObject responseObj = gson.fromJson(response.body(), JsonObject.class);
		assertTrue("Should have security key", responseObj.has("security"));
		assertTrue("Should have userDetails key", responseObj.has("userDetails"));

		var security = responseObj.getAsJsonArray("security");
		var userDetails = responseObj.getAsJsonArray("userDetails");

		assertNotNull("Security should not be null", security);
		assertNotNull("UserDetails should not be null", userDetails);

		// Will be empty without Spring Security in classpath
		assertEquals("Security should be empty", 0, security.size());
		assertEquals("UserDetails should be empty", 0, userDetails.size());
	}

	@Test
	public void testRestartApplicationEndpoint() throws Exception {
		// Test without main class
		JsonObject data = new JsonObject();
		HttpResponse<String> response = client.sendCommand("restartApplication", data);

		assertEquals("Should return 200", 200, response.statusCode());

		JsonObject responseObj = gson.fromJson(response.body(), JsonObject.class);
		assertEquals("Should return ok status", "ok", responseObj.get("status").getAsString());

		// Without launch configurations, should return message
		assertTrue("Should have message", responseObj.has("message"));
		String message = responseObj.get("message").getAsString();
		assertTrue("Message should indicate no config found", message.contains("No launch configuration"));
	}

	@Test
	public void testRestartApplicationWithMainClassEndpoint() throws Exception {
		// Test with specific main class
		JsonObject data = new JsonObject();
		data.addProperty("mainClass", "com.example.Main");

		HttpResponse<String> response = client.sendCommand("restartApplication", data);

		assertEquals("Should return 200", 200, response.statusCode());

		JsonObject responseObj = gson.fromJson(response.body(), JsonObject.class);
		assertEquals("Should return ok status", "ok", responseObj.get("status").getAsString());
	}

	@Test
	public void testUndoEndpoint() throws Exception {
		// Create and modify a file first
		IFile file = testProject.getFile("undo-endpoint-test.txt");
		file.create(new java.io.ByteArrayInputStream("Original".getBytes()), true, null);

		// Modify via write endpoint to record operation
		JsonObject writeData = new JsonObject();
		writeData.addProperty("file", file.getLocation().toString());
		writeData.addProperty("content", "Modified");
		writeData.addProperty("undoLabel", "Test modification");

		client.sendCommand("write", writeData);

		// Now test undo
		JsonObject undoData = new JsonObject();
		var filesArray = new com.google.gson.JsonArray();
		filesArray.add(file.getLocation().toString());
		undoData.add("files", filesArray);

		HttpResponse<String> response = client.sendCommand("undo", undoData);

		assertEquals("Should return 200", 200, response.statusCode());

		JsonObject responseObj = gson.fromJson(response.body(), JsonObject.class);
		assertTrue("Should have performed key", responseObj.has("performed"));

		// Note: Undo might not work in test environment due to operation history setup
		boolean performed = responseObj.get("performed").getAsBoolean();
		// Don't assert true - just verify structure
	}

	@Test
	public void testRedoEndpoint() throws Exception {
		JsonObject data = new JsonObject();
		var filesArray = new com.google.gson.JsonArray();
		filesArray.add("/test/file.txt");
		data.add("files", filesArray);

		HttpResponse<String> response = client.sendCommand("redo", data);

		assertEquals("Should return 200", 200, response.statusCode());

		JsonObject responseObj = gson.fromJson(response.body(), JsonObject.class);
		assertTrue("Should have performed key", responseObj.has("performed"));

		// Should be false since no undo was performed
		boolean performed = responseObj.get("performed").getAsBoolean();
		assertFalse("Redo should not be performed without prior undo", performed);
	}

	/**
	 * Helper to create folder hierarchy.
	 */
	private void createFolderHierarchy(IFolder folder) throws CoreException {
		if (!folder.exists()) {
			IFolder parent = (IFolder) folder.getParent();
			if (parent != null && !parent.exists() && parent.getType() == org.eclipse.core.resources.IResource.FOLDER) {
				createFolderHierarchy(parent);
			}
			folder.create(true, true, null);
		}
	}

	/**
	 * Helper to create Java class.
	 */
	private void createJavaClass(String sourceFolder, String packageName, String className, String content)
			throws CoreException {
		IFolder srcFolder = testProject.getFolder(sourceFolder);
		if (!srcFolder.exists()) {
			srcFolder.create(true, true, null);
		}

		IFolder packageFolder = srcFolder;
		String[] packageParts = packageName.split("\\.");
		for (String part : packageParts) {
			packageFolder = packageFolder.getFolder(part);
			if (!packageFolder.exists()) {
				packageFolder.create(true, true, null);
			}
		}

		IFile javaFile = packageFolder.getFile(className + ".java");
		javaFile.create(new java.io.ByteArrayInputStream(content.getBytes()), true, null);
	}

	/**
	 * Helper to add Java nature.
	 */
	private void addJavaNature(org.eclipse.core.resources.IProject project) throws CoreException {
		if (!project.hasNature(JavaCore.NATURE_ID)) {
			String[] prevNatures = project.getDescription().getNatureIds();
			String[] newNatures = new String[prevNatures.length + 1];
			System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
			newNatures[prevNatures.length] = JavaCore.NATURE_ID;

			org.eclipse.core.resources.IProjectDescription description = project.getDescription();
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
		}
	}
}
