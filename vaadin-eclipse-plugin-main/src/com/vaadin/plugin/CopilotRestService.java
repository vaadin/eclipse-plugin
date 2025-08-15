package com.vaadin.plugin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.BadLocationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Starts a small HTTP server for Copilot integration.
 */
public class CopilotRestService {
    private HttpServer server;
    private String endpoint;

    /** Start the embedded HTTP server on a random port. */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLocalHost(), 0), 0);
        String contextPath = "/vaadin/" + CopilotUtil.getServiceName();
        server.createContext(contextPath, new Handler());
        server.start();
        endpoint = "http://localhost:" + server.getAddress().getPort() + contextPath;
        System.out.println("Copilot REST service started at " + endpoint);
        
        // Create dotfiles for all open projects
        createDotFilesForOpenProjects();
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
            System.err.println("Failed to create dotfiles: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class Handler implements HttpHandler {
        private final Gson gson = new Gson();
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            
            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            
            System.out.println("Received Copilot request: " + body);
            
            try {
                JsonObject requestJson = JsonParser.parseString(body).getAsJsonObject();
                String command = requestJson.get("command").getAsString();
                String projectBasePath = requestJson.get("projectBasePath").getAsString();
                JsonObject data = requestJson.has("data") ? requestJson.get("data").getAsJsonObject() : new JsonObject();
                
                String response = handleCommand(command, projectBasePath, data);
                
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } catch (Exception e) {
                e.printStackTrace();
                String errorResponse = "{\"error\": \"" + e.getMessage() + "\"}";
                byte[] errorBytes = errorResponse.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, errorBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorBytes);
                }
            }
        }
        
        private String handleCommand(String command, String projectBasePath, JsonObject data) {
            System.out.println("Handling command: " + command + " for project: " + projectBasePath);
            
            // Find the Eclipse project
            IProject project = findProject(projectBasePath);
            if (project == null) {
                return "{\"error\": \"Project not found: " + projectBasePath + "\"}";
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
                case "getModulePaths":
                    return handleGetModulePaths(project);
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
                    return "{\"error\": \"Unknown command: " + command + "\"}";
            }
        }
        
        private IProject findProject(String projectBasePath) {
            IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
            for (IProject project : projects) {
                if (project.getLocation() != null && 
                    project.getLocation().toPortableString().equals(projectBasePath)) {
                    return project;
                }
            }
            return null;
        }
        
        private String handleWrite(IProject project, JsonObject data) {
            try {
                String fileName = data.get("file").getAsString();
                String content = data.get("content").getAsString();
                String undoLabel = data.has("undoLabel") ? data.get("undoLabel").getAsString() : null;
                
                System.out.println("Write command for project: " + project.getName() + ", file: " + fileName);
                
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
                    return "{\"error\": \"File not found in project: " + fileName + "\"}";
                }
                
                final IFile finalFile = file;
                final String finalContent = content;
                
                // Execute file write operation in UI thread
                PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
                    try {
                        java.io.ByteArrayInputStream stream = new java.io.ByteArrayInputStream(finalContent.getBytes("UTF-8"));
                        
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
                        
                    } catch (Exception e) {
                        System.err.println("Error writing file: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "ok");
                return gson.toJson(response);
                
            } catch (Exception e) {
                System.err.println("Error in write handler: " + e.getMessage());
                e.printStackTrace();
                return "{\"error\": \"" + e.getMessage() + "\"}";
            }
        }
        
        private String handleWriteBase64(IProject project, JsonObject data) {
            try {
                String fileName = data.get("file").getAsString();
                String base64Content = data.get("content").getAsString();
                String undoLabel = data.has("undoLabel") ? data.get("undoLabel").getAsString() : null;
                
                System.out.println("WriteBase64 command for project: " + project.getName() + ", file: " + fileName);
                
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
                    return "{\"error\": \"File not found in project: " + fileName + "\"}";
                }
                
                final IFile finalFile = file;
                final String finalBase64Content = base64Content;
                
                // Execute file write operation in UI thread
                PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
                    try {
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
                        
                    } catch (Exception e) {
                        System.err.println("Error writing base64 file: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "ok");
                return gson.toJson(response);
                
            } catch (Exception e) {
                System.err.println("Error in writeBase64 handler: " + e.getMessage());
                e.printStackTrace();
                return "{\"error\": \"" + e.getMessage() + "\"}";
            }
        }
        
        private String handleDelete(IProject project, JsonObject data) {
            try {
                String fileName = data.get("file").getAsString();
                
                System.out.println("Delete command for project: " + project.getName() + ", file: " + fileName);
                
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
                    return "{\"error\": \"File not found in project: " + fileName + "\"}";
                }
                
                if (!file.exists()) {
                    return "{\"error\": \"File does not exist: " + fileName + "\"}";
                }
                
                final IFile finalFile = file;
                
                // Execute file delete operation in UI thread
                PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
                    try {
                        finalFile.delete(true, null);
                        System.out.println("File deleted: " + fileName);
                        
                    } catch (Exception e) {
                        System.err.println("Error deleting file: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "ok");
                return gson.toJson(response);
                
            } catch (Exception e) {
                System.err.println("Error in delete handler: " + e.getMessage());
                e.printStackTrace();
                return "{\"error\": \"" + e.getMessage() + "\"}";
            }
        }
        
        private String handleUndo(IProject project, JsonObject data) {
            // TODO: Implement undo functionality
            System.out.println("Undo command for project: " + project.getName());
            return "{\"status\": \"ok\"}";
        }
        
        private String handleRedo(IProject project, JsonObject data) {
            // TODO: Implement redo functionality
            System.out.println("Redo command for project: " + project.getName());
            return "{\"status\": \"ok\"}";
        }
        
        private String handleRefresh(IProject project) {
            try {
                System.out.println("Refresh command for project: " + project.getName());
                
                // Execute refresh operation in UI thread
                PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
                    try {
                        // Refresh the entire project
                        project.refreshLocal(IResource.DEPTH_INFINITE, null);
                        System.out.println("Project refreshed: " + project.getName());
                        
                    } catch (Exception e) {
                        System.err.println("Error refreshing project: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "ok");
                return gson.toJson(response);
                
            } catch (Exception e) {
                System.err.println("Error in refresh handler: " + e.getMessage());
                e.printStackTrace();
                return "{\"error\": \"" + e.getMessage() + "\"}";
            }
        }
        
        private String handleShowInIde(IProject project, JsonObject data) {
            try {
                String fileName = data.get("file").getAsString();
                int line = data.has("line") ? data.get("line").getAsInt() : 0;
                int column = data.has("column") ? data.get("column").getAsInt() : 0;
                
                System.out.println("ShowInIde command for project: " + project.getName() + 
                                 ", file: " + fileName + ", line: " + line + ", column: " + column);
                
                if (line < 0 || column < 0) {
                    return "{\"error\": \"Invalid line or column number (" + line + ":" + column + ")\"}";
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
                    return "{\"error\": \"File not found: " + fileName + "\"}";
                }
                
                final IFile finalFile = file;
                final int finalLine = line;
                final int finalColumn = column;
                
                // Execute show in IDE operation in UI thread
                PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
                    try {
                        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                        if (window != null) {
                            IWorkbenchPage page = window.getActivePage();
                            if (page != null) {
                                // Open the file in editor
                                ITextEditor editor = (ITextEditor) IDE.openEditor(page, finalFile, true);
                                
                                if (editor != null && finalLine > 0) {
                                    // Navigate to specific line and column
                                    IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
                                    if (document != null) {
                                        try {
                                            // Convert line number to offset (Eclipse uses 0-based line numbers)
                                            int offset = document.getLineOffset(finalLine - 1) + finalColumn;
                                            editor.selectAndReveal(offset, 0);
                                        } catch (BadLocationException e) {
                                            // If line/column is invalid, just open the file
                                            System.err.println("Invalid line/column, opening file without navigation: " + e.getMessage());
                                        }
                                    }
                                }
                                
                                // Bring window to front
                                window.getShell().forceActive();
                            }
                        }
                        
                        System.out.println("File opened in IDE: " + fileName + " at " + finalLine + ":" + finalColumn);
                        
                    } catch (PartInitException e) {
                        System.err.println("Error opening file in IDE: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "ok");
                return gson.toJson(response);
                
            } catch (Exception e) {
                System.err.println("Error in showInIde handler: " + e.getMessage());
                e.printStackTrace();
                return "{\"error\": \"" + e.getMessage() + "\"}";
            }
        }
        
        private String handleGetModulePaths(IProject project) {
            // TODO: Implement get module paths
            System.out.println("GetModulePaths command for project: " + project.getName());
            Map<String, Object> response = new HashMap<>();
            response.put("modules", new Object[]{});
            return gson.toJson(response);
        }
        
        private String handleCompileFiles(IProject project, JsonObject data) {
            // TODO: Implement compile files
            System.out.println("CompileFiles command for project: " + project.getName());
            return "{\"status\": \"ok\"}";
        }
        
        private String handleRestartApplication(IProject project, JsonObject data) {
            // TODO: Implement restart application
            System.out.println("RestartApplication command for project: " + project.getName());
            return "{\"status\": \"ok\"}";
        }
        
        private String handleGetVaadinRoutes(IProject project) {
            // TODO: Implement get Vaadin routes
            System.out.println("GetVaadinRoutes command for project: " + project.getName());
            Map<String, Object> response = new HashMap<>();
            response.put("routes", new Object[]{});
            return gson.toJson(response);
        }
        
        private String handleGetVaadinVersion(IProject project) {
            // TODO: Implement get Vaadin version
            System.out.println("GetVaadinVersion command for project: " + project.getName());
            Map<String, Object> response = new HashMap<>();
            response.put("version", "24.0.0");
            return gson.toJson(response);
        }
        
        private String handleGetVaadinComponents(IProject project, JsonObject data) {
            // TODO: Implement get Vaadin components
            System.out.println("GetVaadinComponents command for project: " + project.getName());
            Map<String, Object> response = new HashMap<>();
            response.put("components", new Object[]{});
            return gson.toJson(response);
        }
        
        private String handleGetVaadinEntities(IProject project, JsonObject data) {
            // TODO: Implement get Vaadin entities
            System.out.println("GetVaadinEntities command for project: " + project.getName());
            Map<String, Object> response = new HashMap<>();
            response.put("entities", new Object[]{});
            return gson.toJson(response);
        }
        
        private String handleGetVaadinSecurity(IProject project) {
            // TODO: Implement get Vaadin security
            System.out.println("GetVaadinSecurity command for project: " + project.getName());
            Map<String, Object> response = new HashMap<>();
            response.put("security", new Object[]{});
            return gson.toJson(response);
        }
        
        private String handleReloadMavenModule(IProject project, JsonObject data) {
            // TODO: Implement reload Maven module
            System.out.println("ReloadMavenModule command for project: " + project.getName());
            return "{\"status\": \"ok\"}";
        }
        
        private String handleHeartbeat(IProject project) {
            System.out.println("Heartbeat command for project: " + project.getName());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "alive");
            response.put("version", "1.0.0");
            response.put("ide", "eclipse");
            return gson.toJson(response);
        }

        private void createParentFolders(IFile file) throws Exception {
            IFolder parent = (IFolder) file.getParent();
            if (parent != null && !parent.exists()) {
                createParentFolders(parent);
                parent.create(true, true, null);
            }
        }
        
        private void createParentFolders(IFolder folder) throws Exception {
            IFolder parent = (IFolder) folder.getParent();
            if (parent != null && !parent.exists()) {
                createParentFolders(parent);
                parent.create(true, true, null);
            }
        }
    }
}
