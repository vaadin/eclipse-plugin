package com.vaadin.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.vaadin.plugin.util.VaadinPluginLog;

/**
 * Starts a small HTTP server for Copilot integration.
 */
public class CopilotRestService {
    private HttpServer server;
    private String endpoint;

    /** Start the embedded HTTP server on a random port. */
    public void start() throws IOException {
        try {
            // Bind to localhost (127.0.0.1) explicitly
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            String contextPath = "/vaadin/" + CopilotUtil.getServiceName();
            server.createContext(contextPath, new Handler());

            // Add a simple health check endpoint for testing
            server.createContext("/health", exchange -> {
                String response = "OK";
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });

            server.setExecutor(null); // creates a default executor
            server.start();

            // Get the actual port after binding
            int actualPort = server.getAddress().getPort();
            endpoint = "http://localhost:" + actualPort + contextPath;
            VaadinPluginLog.info("Copilot REST service started successfully!");
            VaadinPluginLog.info("  Main endpoint: " + endpoint);
            VaadinPluginLog.info("  Health check: http://localhost:" + actualPort + "/health");
            VaadinPluginLog.info("  Server is listening on port " + actualPort);

            // Create dotfiles for all open projects
            createDotFilesForOpenProjects();
        } catch (IOException e) {
            VaadinPluginLog.error("Failed to start Copilot REST service: " + e.getMessage(), e);
            throw e;
        }
    }

    /** Stop the server if it is running. */
    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    /** Returns the full endpoint URL. */
    public String getEndpoint() {
        return endpoint;
    }

    /** Create dotfiles for all open Eclipse projects */
    private void createDotFilesForOpenProjects() {
        try {
            IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
            for (IProject project : projects) {
                if (project.isOpen() && project.getLocation() != null) {
                    String projectPath = project.getLocation().toPortableString();
                    CopilotUtil.saveDotFile(projectPath, server.getAddress().getPort());
                }
            }
        } catch (Exception e) {
            VaadinPluginLog.error("Failed to create dotfiles: " + e.getMessage(), e);
        }
    }

    private static class Handler implements HttpHandler {
        private final Gson gson = new Gson();

        /**
         * Creates a JSON error response with the given error message.
         *
         * @param errorMessage
         *            The error message to include in the response
         * @return JSON string representing the error response
         */
        private String createErrorResponse(String errorMessage) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", errorMessage);
            return gson.toJson(errorResponse);
        }

        /**
         * Creates a JSON success response with status "ok".
         *
         * @return JSON string representing the success response
         */
        private String createSuccessResponse() {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ok");
            return createResponse(response);
        }

        /**
         * Creates a JSON response with custom key-value pairs.
         *
         * @param responseData
         *            The data to include in the response
         * @return JSON string representing the response
         */
        private String createResponse(Map<String, Object> responseData) {
            return gson.toJson(responseData);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            VaadinPluginLog.debug("Received Copilot request: " + body);

            try {
                JsonObject requestJson = JsonParser.parseString(body).getAsJsonObject();
                String command = requestJson.get("command").getAsString();
                String projectBasePath = requestJson.get("projectBasePath").getAsString();
                JsonObject data = requestJson.has("data")
                        ? requestJson.get("data").getAsJsonObject()
                        : new JsonObject();

                String response = handleCommand(command, projectBasePath, data);

                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } catch (Exception e) {
                VaadinPluginLog.error("Error in HTTP request handler", e);
                byte[] errorBytes = createErrorResponse(e.getMessage()).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, errorBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorBytes);
                }
            }
        }

        private String handleCommand(String command, String projectBasePath, JsonObject data) {
            VaadinPluginLog.debug("Handling command: " + command + " for project: " + projectBasePath);

            // Special case for getModulePaths - it should return an empty project structure
            // if project not found
            if ("getModulePaths".equals(command)) {
                return handleGetModulePaths(projectBasePath);
            }

            // Find the Eclipse project
            IProject project = findProject(projectBasePath);
            if (project == null) {
                VaadinPluginLog.error("Project not found for path: " + projectBasePath);
                return createErrorResponse("Project not found: " + projectBasePath);
            }

            switch (command) {
            case "write":
                return handleWrite(project, data);
            case "writeBase64":
                return handleWriteBase64(project, data);
            case "delete":
                return handleDelete(project, data);
            case "undo":
                return handleUndo(project, data);
            case "redo":
                return handleRedo(project, data);
            case "refresh":
                return handleRefresh(project);
            case "showInIde":
                return handleShowInIde(project, data);
            case "compileFiles":
                return handleCompileFiles(project, data);
            case "restartApplication":
                return handleRestartApplication(project, data);
            case "getVaadinRoutes":
                return handleGetVaadinRoutes(project);
            case "getVaadinVersion":
                return handleGetVaadinVersion(project);
            case "getVaadinComponents":
                return handleGetVaadinComponents(project, data);
            case "getVaadinEntities":
                return handleGetVaadinEntities(project, data);
            case "getVaadinSecurity":
                return handleGetVaadinSecurity(project);
            case "reloadMavenModule":
                return handleReloadMavenModule(project, data);
            case "heartbeat":
                return handleHeartbeat(project);
            default:
                return createErrorResponse("Unknown command: " + command);
            }
        }

        private IProject findProject(String projectBasePath) {
            IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
            for (IProject project : projects) {
                if (project.getLocation() != null) {
                    // Compare both portable string (forward slashes) and OS string (native
                    // separators)
                    String projectLocationPortable = project.getLocation().toPortableString();
                    String projectLocationOS = project.getLocation().toOSString();

                    // Normalize the input path for comparison
                    String normalizedBasePath = projectBasePath.replace('\\', '/');

                    if (projectLocationPortable.equals(projectBasePath)
                            || projectLocationPortable.equals(normalizedBasePath)
                            || projectLocationOS.equals(projectBasePath)) {
                        return project;
                    }
                }
            }
            return null;
        }

        private String handleWrite(IProject project, JsonObject data) {
            try {
                String fileName = data.get("file").getAsString();
                String content = data.get("content").getAsString();
                String undoLabel = data.has("undoLabel") ? data.get("undoLabel").getAsString() : null;

                VaadinPluginLog.debug("Write command for project: " + project.getName() + ", file: " + fileName);

                // Convert absolute path to workspace-relative path
                IPath filePath = new org.eclipse.core.runtime.Path(fileName);
                IFile file = null;

                // Try to find the file within the project
                if (filePath.isAbsolute()) {
                    IPath projectPath = project.getLocation();
                    if (projectPath != null && projectPath.isPrefixOf(filePath)) {
                        IPath relativePath = filePath.removeFirstSegments(projectPath.segmentCount());
                        file = project.getFile(relativePath);
                    }
                }

                if (file == null) {
                    return createErrorResponse("File not found in project: " + fileName);
                }

                final IFile finalFile = file;
                final String finalContent = content;

                // Execute file write operation - no UI thread needed for file operations
                try {
                    // Get old content for undo if file exists
                    String oldContent = "";
                    if (finalFile.exists()) {
                        try (java.io.InputStream is = finalFile.getContents()) {
                            oldContent = new String(is.readAllBytes(), "UTF-8");
                        }
                    }

                    java.io.ByteArrayInputStream stream = new java.io.ByteArrayInputStream(
                            finalContent.getBytes("UTF-8"));

                    if (finalFile.exists()) {
                        // Update existing file
                        finalFile.setContents(stream, true, true, null);
                    } else {
                        // Create new file (and parent directories if needed)
                        createParentFolders(finalFile);
                        finalFile.create(stream, true, null);
                    }

                    // Refresh the file in workspace
                    finalFile.refreshLocal(IResource.DEPTH_ZERO, null);

                    // Record operation for undo/redo
                    CopilotUndoManager.getInstance().recordOperation(finalFile, oldContent, finalContent, undoLabel);

                } catch (Exception e) {
                    VaadinPluginLog.error("Error writing file: " + e.getMessage(), e);
                    return createErrorResponse(e.getMessage());
                }

                return createSuccessResponse();

            } catch (Exception e) {
                VaadinPluginLog.error("Error in write handler: " + e.getMessage(), e);
                return createErrorResponse(e.getMessage());
            }
        }

        private String handleWriteBase64(IProject project, JsonObject data) {
            try {
                String fileName = data.get("file").getAsString();
                String base64Content = data.get("content").getAsString();
                String undoLabel = data.has("undoLabel") ? data.get("undoLabel").getAsString() : null;

                VaadinPluginLog.debug("WriteBase64 command for project: " + project.getName() + ", file: " + fileName);

                // Convert absolute path to workspace-relative path
                IPath filePath = new org.eclipse.core.runtime.Path(fileName);
                IFile file = null;

                // Try to find the file within the project
                if (filePath.isAbsolute()) {
                    IPath projectPath = project.getLocation();
                    if (projectPath != null && projectPath.isPrefixOf(filePath)) {
                        IPath relativePath = filePath.removeFirstSegments(projectPath.segmentCount());
                        file = project.getFile(relativePath);
                    }
                }

                if (file == null) {
                    return createErrorResponse("File not found in project: " + fileName);
                }

                final IFile finalFile = file;
                final String finalBase64Content = base64Content;

                // Execute file write operation - no UI thread needed
                try {
                    // Get old content for undo if file exists
                    String oldContent = "";
                    if (finalFile.exists()) {
                        try (java.io.InputStream is = finalFile.getContents()) {
                            byte[] oldBytes = is.readAllBytes();
                            oldContent = java.util.Base64.getEncoder().encodeToString(oldBytes);
                        }
                    }

                    // Decode base64 content
                    byte[] decodedBytes = java.util.Base64.getDecoder().decode(finalBase64Content);
                    java.io.ByteArrayInputStream stream = new java.io.ByteArrayInputStream(decodedBytes);

                    if (finalFile.exists()) {
                        // Update existing file
                        finalFile.setContents(stream, true, true, null);
                    } else {
                        // Create new file (and parent directories if needed)
                        createParentFolders(finalFile);
                        finalFile.create(stream, true, null);
                    }

                    // Refresh the file in workspace
                    finalFile.refreshLocal(IResource.DEPTH_ZERO, null);

                    // For binary files, store the base64 content directly
                    CopilotUndoManager.getInstance().recordOperation(finalFile, oldContent, finalBase64Content,
                            undoLabel, true);

                } catch (Exception e) {
                    VaadinPluginLog.error("Error writing base64 file: " + e.getMessage(), e);
                    return createErrorResponse(e.getMessage());
                }

                return createSuccessResponse();

            } catch (Exception e) {
                VaadinPluginLog.error("Error in writeBase64 handler: " + e.getMessage(), e);
                return createErrorResponse(e.getMessage());
            }
        }

        private String handleDelete(IProject project, JsonObject data) {
            try {
                String fileName = data.get("file").getAsString();

                VaadinPluginLog.debug("Delete command for project: " + project.getName() + ", file: " + fileName);

                // Convert absolute path to workspace-relative path
                IPath filePath = new org.eclipse.core.runtime.Path(fileName);
                IFile file = null;

                // Try to find the file within the project
                if (filePath.isAbsolute()) {
                    IPath projectPath = project.getLocation();
                    if (projectPath != null && projectPath.isPrefixOf(filePath)) {
                        IPath relativePath = filePath.removeFirstSegments(projectPath.segmentCount());
                        file = project.getFile(relativePath);
                    }
                }

                if (file == null) {
                    return createErrorResponse("File not found in project: " + fileName);
                }

                if (!file.exists()) {
                    return createErrorResponse("File does not exist: " + fileName);
                }

                final IFile finalFile = file;

                // Execute file delete operation - no UI thread needed
                try {
                    // Get content for undo before deleting
                    String oldContent = "";
                    try (java.io.InputStream is = finalFile.getContents()) {
                        oldContent = new String(is.readAllBytes(), "UTF-8");
                    }

                    finalFile.delete(true, null);
                    VaadinPluginLog.info("File deleted: " + fileName);

                    // Record delete as setting content to empty (can be undone by recreating with
                    // old content)
                    CopilotUndoManager.getInstance().recordOperation(finalFile, oldContent, "", "Delete " + fileName);

                } catch (Exception e) {
                    VaadinPluginLog.error("Error deleting file: " + e.getMessage(), e);
                    return createErrorResponse(e.getMessage());
                }

                return createSuccessResponse();

            } catch (Exception e) {
                VaadinPluginLog.error("Error in delete handler: " + e.getMessage(), e);
                return createErrorResponse(e.getMessage());
            }
        }

        private String handleUndo(IProject project, JsonObject data) {
            VaadinPluginLog.debug("Undo command for project: " + project.getName());

            try {
                List<String> filePaths = new ArrayList<>();

                if (data.has("files") && data.get("files").isJsonArray()) {
                    for (var fileElement : data.get("files").getAsJsonArray()) {
                        filePaths.add(fileElement.getAsString());
                    }
                }

                boolean performed = CopilotUndoManager.getInstance().performUndo(filePaths);

                Map<String, Object> response = new HashMap<>();
                response.put("performed", performed);
                if (!performed) {
                    response.put("message", "No undo operations available for specified files");
                }
                return createResponse(response);

            } catch (Exception e) {
                VaadinPluginLog.error("Error performing undo: " + e.getMessage(), e);
                Map<String, Object> response = new HashMap<>();
                response.put("performed", false);
                response.put("error", e.getMessage());
                return createResponse(response);
            }
        }

        private String handleRedo(IProject project, JsonObject data) {
            VaadinPluginLog.debug("Redo command for project: " + project.getName());

            try {
                List<String> filePaths = new ArrayList<>();

                if (data.has("files") && data.get("files").isJsonArray()) {
                    for (var fileElement : data.get("files").getAsJsonArray()) {
                        filePaths.add(fileElement.getAsString());
                    }
                }

                boolean performed = CopilotUndoManager.getInstance().performRedo(filePaths);

                Map<String, Object> response = new HashMap<>();
                response.put("performed", performed);
                if (!performed) {
                    response.put("message", "No redo operations available for specified files");
                }
                return createResponse(response);

            } catch (Exception e) {
                VaadinPluginLog.error("Error performing redo: " + e.getMessage(), e);
                Map<String, Object> response = new HashMap<>();
                response.put("performed", false);
                response.put("error", e.getMessage());
                return createResponse(response);
            }
        }

        private String handleRefresh(IProject project) {
            try {
                VaadinPluginLog.debug("Refresh command for project: " + project.getName());

                // Execute refresh operation - no UI thread needed
                try {
                    // Refresh the entire project
                    project.refreshLocal(IResource.DEPTH_INFINITE, null);
                    VaadinPluginLog.info("Project refreshed: " + project.getName());

                } catch (Exception e) {
                    VaadinPluginLog.error("Error refreshing project: " + e.getMessage(), e);
                    return createErrorResponse(e.getMessage());
                }

                return createSuccessResponse();

            } catch (Exception e) {
                VaadinPluginLog.error("Error in refresh handler: " + e.getMessage(), e);
                return createErrorResponse(e.getMessage());
            }
        }

        private String handleShowInIde(IProject project, JsonObject data) {
            try {
                String fileName = data.get("file").getAsString();
                int line = data.has("line") ? data.get("line").getAsInt() : 0;
                int column = data.has("column") ? data.get("column").getAsInt() : 0;

                VaadinPluginLog.debug("ShowInIde command for project: " + project.getName() + ", file: " + fileName
                        + ", line: " + line + ", column: " + column);

                if (line < 0 || column < 0) {
                    return createErrorResponse("Invalid line or column number (" + line + ":" + column + ")");
                }

                // Convert absolute path to workspace-relative path
                IPath filePath = new org.eclipse.core.runtime.Path(fileName);
                IFile file = null;

                // Try to find the file within the project
                if (filePath.isAbsolute()) {
                    IPath projectPath = project.getLocation();
                    if (projectPath != null && projectPath.isPrefixOf(filePath)) {
                        IPath relativePath = filePath.removeFirstSegments(projectPath.segmentCount());
                        file = project.getFile(relativePath);
                    }
                }

                if (file == null || !file.exists()) {
                    return createErrorResponse("File not found: " + fileName);
                }

                final IFile finalFile = file;
                final int finalLine = line;
                final int finalColumn = column;

                // Execute show in IDE operation in UI thread (only if workbench is available)
                if (!PlatformUI.isWorkbenchRunning()) {
                    // In headless mode, we can't open editors
                    VaadinPluginLog.info("Workbench not available for showInIde operation");
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "ok"); // Still return success for testing
                    response.put("message", "Operation would open " + fileName + " at line " + finalLine);
                    return createResponse(response);
                }

                PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
                    try {
                        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                        if (window != null) {
                            IWorkbenchPage page = window.getActivePage();
                            if (page != null) {
                                // Open the file in editor
                                ITextEditor editor = (ITextEditor) IDE.openEditor(page, finalFile, true);

                                if (editor != null && finalLine > 0) {
                                    // Navigate to specific line and column
                                    IDocument document = editor.getDocumentProvider()
                                            .getDocument(editor.getEditorInput());
                                    if (document != null) {
                                        try {
                                            // Convert line number to offset (Eclipse uses 0-based line numbers)
                                            int offset = document.getLineOffset(finalLine - 1) + finalColumn;
                                            editor.selectAndReveal(offset, 0);
                                        } catch (BadLocationException e) {
                                            // If line/column is invalid, just open the file
                                            VaadinPluginLog
                                                    .warning("Invalid line/column, opening file without navigation: "
                                                            + e.getMessage());
                                        }
                                    }
                                }

                                // Bring window to front
                                window.getShell().forceActive();
                            }
                        }

                        VaadinPluginLog
                                .info("File opened in IDE: " + fileName + " at " + finalLine + ":" + finalColumn);

                    } catch (PartInitException e) {
                        VaadinPluginLog.error("Error opening file in IDE: " + e.getMessage(), e);
                    }
                });

                return createSuccessResponse();

            } catch (Exception e) {
                VaadinPluginLog.error("Error in showInIde handler: " + e.getMessage(), e);
                return createErrorResponse(e.getMessage());
            }
        }

        private String handleGetModulePaths(String projectBasePath) {
            VaadinPluginLog.debug("GetModulePaths command for project path: " + projectBasePath);

            Map<String, Object> response = new HashMap<>();
            Map<String, Object> projectInfo = new HashMap<>();
            List<Map<String, Object>> modules = new ArrayList<>();

            // Find the project
            IProject project = findProject(projectBasePath);

            if (project == null) {
                VaadinPluginLog.warning("Project not found for getModulePaths: " + projectBasePath);
                // Return empty but valid structure - Copilot expects a project object
                projectInfo.put("basePath", projectBasePath);
                projectInfo.put("modules", modules);
                response.put("project", projectInfo);
                return createResponse(response);
            }

            try {
                // Add the main project as a module
                Map<String, Object> module = new HashMap<>();
                module.put("name", project.getName());

                List<String> contentRoots = new ArrayList<>();
                // Use OS-specific path format for consistency with what Copilot sends
                contentRoots.add(project.getLocation().toOSString());
                module.put("contentRoots", contentRoots);

                // If it's a Java project, get source paths
                if (project.hasNature(JavaCore.NATURE_ID)) {
                    IJavaProject javaProject = JavaCore.create(project);

                    List<String> javaSourcePaths = new ArrayList<>();
                    List<String> javaTestSourcePaths = new ArrayList<>();
                    List<String> resourcePaths = new ArrayList<>();
                    List<String> testResourcePaths = new ArrayList<>();

                    IClasspathEntry[] entries = javaProject.getRawClasspath();
                    for (IClasspathEntry entry : entries) {
                        if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                            IPath path = entry.getPath();
                            String fullPath = project.getLocation().append(path.removeFirstSegments(1)).toOSString();

                            // Try to determine if it's test or main source
                            String pathStr = path.toString();
                            if (pathStr.contains("/test/") || pathStr.contains("/test-")) {
                                if (pathStr.contains("/resources")) {
                                    testResourcePaths.add(fullPath);
                                } else {
                                    javaTestSourcePaths.add(fullPath);
                                }
                            } else {
                                if (pathStr.contains("/resources")) {
                                    resourcePaths.add(fullPath);
                                } else {
                                    javaSourcePaths.add(fullPath);
                                }
                            }
                        }
                    }

                    module.put("javaSourcePaths", javaSourcePaths);
                    module.put("javaTestSourcePaths", javaTestSourcePaths);
                    module.put("resourcePaths", resourcePaths);
                    module.put("testResourcePaths", testResourcePaths);

                    // Get output path
                    IPath outputLocation = javaProject.getOutputLocation();
                    if (outputLocation != null) {
                        String outputPath = project.getLocation().append(outputLocation.removeFirstSegments(1))
                                .toOSString();
                        module.put("outputPath", outputPath);
                    }
                }

                modules.add(module);

                // Check for nested projects (modules)
                IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                for (IProject p : root.getProjects()) {
                    if (p.isOpen() && !p.equals(project)) {
                        IPath pLocation = p.getLocation();
                        IPath projectLocation = project.getLocation();
                        if (pLocation != null && projectLocation != null && projectLocation.isPrefixOf(pLocation)) {
                            // This is a nested module
                            Map<String, Object> nestedModule = new HashMap<>();
                            nestedModule.put("name", p.getName());

                            List<String> nestedContentRoots = new ArrayList<>();
                            nestedContentRoots.add(pLocation.toOSString());
                            nestedModule.put("contentRoots", nestedContentRoots);

                            modules.add(nestedModule);
                        }
                    }
                }

            } catch (Exception e) {
                VaadinPluginLog.error("Error getting module paths: " + e.getMessage(), e);
            }

            projectInfo.put("basePath", project.getLocation().toOSString());
            projectInfo.put("modules", modules);
            response.put("project", projectInfo);

            return createResponse(response);
        }

        private String handleCompileFiles(IProject project, JsonObject data) {
            VaadinPluginLog.debug("CompileFiles command for project: " + project.getName());

            try {
                // Get the list of files to compile
                if (data.has("files") && data.get("files").isJsonArray()) {
                    // In Eclipse, compilation happens automatically via builders
                    // We trigger a build for the project
                    project.build(org.eclipse.core.resources.IncrementalProjectBuilder.INCREMENTAL_BUILD, null);

                    VaadinPluginLog.info("Triggered incremental build for project: " + project.getName());
                }

                return createSuccessResponse();

            } catch (Exception e) {
                VaadinPluginLog.error("Error compiling files: " + e.getMessage(), e);
                return createErrorResponse(e.getMessage());
            }
        }

        private String handleRestartApplication(IProject project, JsonObject data) {
            VaadinPluginLog.debug("RestartApplication command for project: " + project.getName());

            try {
                String mainClass = data.has("mainClass") ? data.get("mainClass").getAsString() : null;

                ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();

                // First, try to find a running launch that matches
                ILaunch[] launches = launchManager.getLaunches();
                ILaunch targetLaunch = null;
                ILaunchConfiguration targetConfig = null;

                for (ILaunch launch : launches) {
                    if (!launch.isTerminated()) {
                        ILaunchConfiguration config = launch.getLaunchConfiguration();
                        if (config != null) {
                            try {
                                String projectName = config
                                        .getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
                                if (projectName.equals(project.getName())) {
                                    if (mainClass != null) {
                                        String configMainClass = config.getAttribute(
                                                IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "");
                                        if (configMainClass.equals(mainClass)) {
                                            targetLaunch = launch;
                                            targetConfig = config;
                                            break;
                                        }
                                    } else {
                                        // No specific main class requested, use first matching project
                                        targetLaunch = launch;
                                        targetConfig = config;
                                        break;
                                    }
                                }
                            } catch (CoreException e) {
                                // Skip this configuration
                            }
                        }
                    }
                }

                // If no running launch found, try to find a configuration
                if (targetConfig == null) {
                    ILaunchConfiguration[] configs = launchManager.getLaunchConfigurations();
                    for (ILaunchConfiguration config : configs) {
                        try {
                            String projectName = config
                                    .getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
                            if (projectName.equals(project.getName())) {
                                if (mainClass != null) {
                                    String configMainClass = config
                                            .getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "");
                                    if (configMainClass.equals(mainClass)) {
                                        targetConfig = config;
                                        break;
                                    }
                                } else {
                                    targetConfig = config;
                                    break;
                                }
                            }
                        } catch (CoreException e) {
                            // Skip this configuration
                        }
                    }
                }

                if (targetConfig != null) {
                    // Terminate existing launch if running
                    if (targetLaunch != null && !targetLaunch.isTerminated()) {
                        targetLaunch.terminate();
                        // Wait a bit for termination
                        Thread.sleep(500);
                    }

                    // Launch again
                    final ILaunchConfiguration finalConfig = targetConfig;

                    // Run in UI thread if workbench is available
                    if (PlatformUI.isWorkbenchRunning()) {
                        PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
                            DebugUITools.launch(finalConfig, ILaunchManager.RUN_MODE);
                        });
                    } else {
                        // Headless mode - launch directly
                        finalConfig.launch(ILaunchManager.RUN_MODE, null);
                    }

                    VaadinPluginLog.info("Restarted application for project: " + project.getName());

                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "ok");
                    response.put("message", "Application restarted");
                    return createResponse(response);
                } else {
                    // No configuration found - this is OK, just log it
                    VaadinPluginLog.info("No launch configuration found for project: " + project.getName());

                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "ok");
                    response.put("message", "No launch configuration found to restart");
                    return createResponse(response);
                }

            } catch (Exception e) {
                VaadinPluginLog.error("Error restarting application: " + e.getMessage(), e);
                return createErrorResponse(e.getMessage());
            }
        }

        private String handleGetVaadinRoutes(IProject project) {
            VaadinPluginLog.debug("GetVaadinRoutes command for project: " + project.getName());

            List<Map<String, Object>> routes = new ArrayList<>();

            try {
                if (project.hasNature(JavaCore.NATURE_ID)) {
                    IJavaProject javaProject = JavaCore.create(project);
                    VaadinProjectAnalyzer analyzer = new VaadinProjectAnalyzer(javaProject);
                    routes = analyzer.findVaadinRoutes();
                    VaadinPluginLog.info("Found " + routes.size() + " Vaadin routes");
                }
            } catch (Exception e) {
                VaadinPluginLog.error("Error getting Vaadin routes: " + e.getMessage(), e);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("routes", routes);
            return createResponse(response);
        }

        private String handleGetVaadinVersion(IProject project) {
            VaadinPluginLog.debug("GetVaadinVersion command for project: " + project.getName());

            try {
                // Check if it's a Java project
                if (!project.hasNature(JavaCore.NATURE_ID)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("version", "N/A");
                    return createResponse(response);
                }

                IJavaProject javaProject = JavaCore.create(project);
                IClasspathEntry[] classpathEntries = javaProject.getResolvedClasspath(true);

                // Look for Vaadin jars in classpath
                String vaadinVersion = null;
                for (IClasspathEntry entry : classpathEntries) {
                    if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                        String jarPath = entry.getPath().toString();

                        // Check for vaadin-core or flow-server jars
                        if (jarPath.contains("vaadin-core-") || jarPath.contains("flow-server-")) {
                            // Extract version from jar name (e.g., vaadin-core-24.1.0.jar)
                            int lastDash = jarPath.lastIndexOf('-');
                            int dotJar = jarPath.lastIndexOf(".jar");
                            if (lastDash > 0 && dotJar > lastDash) {
                                vaadinVersion = jarPath.substring(lastDash + 1, dotJar);
                                break;
                            }
                        }
                    }
                }

                // If not found by jar name, try to find VaadinService class and check its
                // package
                if (vaadinVersion == null) {
                    try {
                        IType vaadinServiceType = javaProject.findType("com.vaadin.flow.server.VaadinService");
                        if (vaadinServiceType != null && vaadinServiceType.exists()) {
                            // Found VaadinService, but couldn't determine version
                            vaadinVersion = "Unknown";
                        }
                    } catch (Exception e) {
                        // Type not found
                    }
                }

                Map<String, Object> response = new HashMap<>();
                response.put("version", vaadinVersion != null ? vaadinVersion : "N/A");
                return createResponse(response);

            } catch (Exception e) {
                VaadinPluginLog.error("Error getting Vaadin version: " + e.getMessage(), e);
                Map<String, Object> response = new HashMap<>();
                response.put("version", "N/A");
                return createResponse(response);
            }
        }

        private String handleGetVaadinComponents(IProject project, JsonObject data) {
            VaadinPluginLog.debug("GetVaadinComponents command for project: " + project.getName());

            boolean includeMethods = data.has("includeMethods") && data.get("includeMethods").getAsBoolean();
            List<Map<String, Object>> components = new ArrayList<>();

            try {
                if (project.hasNature(JavaCore.NATURE_ID)) {
                    IJavaProject javaProject = JavaCore.create(project);
                    VaadinProjectAnalyzer analyzer = new VaadinProjectAnalyzer(javaProject);
                    components = analyzer.findVaadinComponents(includeMethods);
                    VaadinPluginLog.info("Found " + components.size() + " Vaadin components");
                }
            } catch (Exception e) {
                VaadinPluginLog.error("Error getting Vaadin components: " + e.getMessage(), e);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("components", components);
            return createResponse(response);
        }

        private String handleGetVaadinEntities(IProject project, JsonObject data) {
            VaadinPluginLog.debug("GetVaadinEntities command for project: " + project.getName());

            boolean includeMethods = data.has("includeMethods") && data.get("includeMethods").getAsBoolean();
            List<Map<String, Object>> entities = new ArrayList<>();

            try {
                if (project.hasNature(JavaCore.NATURE_ID)) {
                    IJavaProject javaProject = JavaCore.create(project);
                    VaadinProjectAnalyzer analyzer = new VaadinProjectAnalyzer(javaProject);
                    entities = analyzer.findEntities(includeMethods);
                    VaadinPluginLog.info("Found " + entities.size() + " JPA entities");
                }
            } catch (Exception e) {
                VaadinPluginLog.error("Error getting Vaadin entities: " + e.getMessage(), e);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("entities", entities);
            return createResponse(response);
        }

        private String handleGetVaadinSecurity(IProject project) {
            VaadinPluginLog.debug("GetVaadinSecurity command for project: " + project.getName());

            List<Map<String, Object>> security = new ArrayList<>();
            List<Map<String, Object>> userDetails = new ArrayList<>();

            try {
                if (project.hasNature(JavaCore.NATURE_ID)) {
                    IJavaProject javaProject = JavaCore.create(project);
                    VaadinProjectAnalyzer analyzer = new VaadinProjectAnalyzer(javaProject);
                    security = analyzer.findSecurityConfigurations();
                    userDetails = analyzer.findUserDetailsServices();
                    VaadinPluginLog.info("Found " + security.size() + " security configs and " + userDetails.size()
                            + " user detail services");
                }
            } catch (Exception e) {
                VaadinPluginLog.error("Error getting Vaadin security: " + e.getMessage(), e);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("security", security);
            response.put("userDetails", userDetails);
            return createResponse(response);
        }

        private String handleReloadMavenModule(IProject project, JsonObject data) {
            VaadinPluginLog.debug("ReloadMavenModule command for project: " + project.getName());

            try {
                String moduleName = data.has("moduleName") ? data.get("moduleName").getAsString() : null;

                // In Eclipse, Maven projects are managed by M2E (Maven Integration for Eclipse)
                // This would require integration with m2e APIs
                // For now, we trigger a project refresh which will update Maven dependencies

                if (moduleName != null) {
                    // Find the specific module project
                    IProject moduleProject = ResourcesPlugin.getWorkspace().getRoot().getProject(moduleName);
                    if (moduleProject != null && moduleProject.exists()) {
                        moduleProject.refreshLocal(IResource.DEPTH_INFINITE, null);
                        VaadinPluginLog.info("Refreshed Maven module: " + moduleName);
                    }
                } else {
                    // Refresh the main project
                    project.refreshLocal(IResource.DEPTH_INFINITE, null);
                    VaadinPluginLog.info("Refreshed Maven project: " + project.getName());
                }

                return createSuccessResponse();

            } catch (Exception e) {
                VaadinPluginLog.error("Error reloading Maven module: " + e.getMessage(), e);
                return createErrorResponse(e.getMessage());
            }
        }

        private String handleHeartbeat(IProject project) {
            VaadinPluginLog.debug("Heartbeat command for project: " + project.getName());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "alive");
            response.put("version", "1.0.0");
            response.put("ide", "eclipse");
            return createResponse(response);
        }

        private void createParentFolders(IFile file) throws Exception {
            IResource parent = file.getParent();
            if (parent instanceof IFolder) {
                IFolder folder = (IFolder) parent;
                if (!folder.exists()) {
                    createParentFolders(folder);
                    folder.create(true, true, null);
                }
            }
        }

        private void createParentFolders(IFolder folder) throws Exception {
            IResource parent = folder.getParent();
            if (parent instanceof IFolder) {
                IFolder parentFolder = (IFolder) parent;
                if (!parentFolder.exists()) {
                    createParentFolders(parentFolder);
                    parentFolder.create(true, true, null);
                }
            }
        }
    }
}
