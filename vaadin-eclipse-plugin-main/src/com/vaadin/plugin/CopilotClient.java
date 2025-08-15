package com.vaadin.plugin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

/**
 * Client for communicating with the Copilot REST service.
 */
public class CopilotClient {

    private final String endpoint;
    private final String projectBasePath;
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public CopilotClient(String endpoint, String projectBasePath) {
        this.endpoint = endpoint;
        this.projectBasePath = projectBasePath;
    }

    public HttpResponse<String> undo(Path path) throws IOException, InterruptedException {
        return send("undo", new Message.UndoRedoMessage(Collections.singletonList(path.toString())));
    }

    public HttpResponse<String> redo(Path path) throws IOException, InterruptedException {
        return send("redo", new Message.UndoRedoMessage(Collections.singletonList(path.toString())));
    }

    public HttpResponse<String> write(Path path, String content) throws IOException, InterruptedException {
        return write(path, content, "File modification");
    }
    
    public HttpResponse<String> write(Path path, String content, String undoLabel) throws IOException, InterruptedException {
        return send("write", new Message.WriteFileMessage(path.toString(), undoLabel, content));
    }

    public HttpResponse<String> restartApplication() throws IOException, InterruptedException {
        return send("restartApplication", new Message.RestartApplicationMessage());
    }

    public HttpResponse<String> writeBinary(Path path, String content) throws IOException, InterruptedException {
        return writeBinary(path, content, "Binary file modification");
    }
    
    public HttpResponse<String> writeBinary(Path path, String content, String undoLabel) throws IOException, InterruptedException {
        return send("writeBase64", new Message.WriteFileMessage(path.toString(), undoLabel, content));
    }

    public HttpResponse<String> showInIde(Path path, int line, int column) throws IOException, InterruptedException {
        return send("showInIde", new Message.ShowInIdeMessage(path.toString(), line, column));
    }

    public HttpResponse<String> refresh() throws IOException, InterruptedException {
        return send("refresh", new Message.RefreshMessage());
    }

    public HttpResponse<String> delete(Path path) throws IOException, InterruptedException {
        return send("delete", new Message.DeleteMessage(path.toString()));
    }

    public HttpResponse<String> compileFiles(java.util.List<String> files) throws IOException, InterruptedException {
        return send("compileFiles", new Message.CompileMessage(files));
    }

    public HttpResponse<String> getModulePaths() throws IOException, InterruptedException {
        return send("getModulePaths", new Message.GetModulePathsMessage());
    }

    public HttpResponse<String> reloadMavenModule(String moduleName) throws IOException, InterruptedException {
        return send("reloadMavenModule", new Message.ReloadMavenModuleMessage(moduleName));
    }

    public HttpResponse<String> heartbeat() throws IOException, InterruptedException {
        return send("heartbeat", new Message.HeartbeatMessage());
    }

    public Optional<JsonObject> getVaadinRoutes() throws IOException, InterruptedException {
        return sendForJson("getVaadinRoutes", new Message.GetVaadinRoutesMessage());
    }

    public Optional<JsonObject> getVaadinVersion() throws IOException, InterruptedException {
        return sendForJson("getVaadinVersion", new Message.GetVaadinVersionMessage());
    }

    public Optional<JsonObject> getVaadinComponents(boolean includeMethods) throws IOException, InterruptedException {
        return sendForJson("getVaadinComponents", new Message.GetVaadinComponentsMessage(includeMethods));
    }

    public Optional<JsonObject> getVaadinEntities(boolean includeMethods) throws IOException, InterruptedException {
        return sendForJson("getVaadinEntities", new Message.GetVaadinPersistenceMessage(includeMethods));
    }

    public Optional<JsonObject> getVaadinSecurity() throws IOException, InterruptedException {
        return sendForJson("getVaadinSecurity", new Message.GetVaadinSecurityMessage());
    }
    
    /**
     * Generic send command method for tests.
     */
    public HttpResponse<String> sendCommand(String command, JsonObject data) throws IOException, InterruptedException {
        return send(command, data);
    }

    private HttpResponse<String> send(String command, Object data) throws IOException, InterruptedException {
        Message.CopilotRestRequest message = new Message.CopilotRestRequest(command, projectBasePath, data);
        String body = gson.toJson(message);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private Optional<JsonObject> sendForJson(String command, Object dataCommand) throws IOException, InterruptedException {
        HttpResponse<String> response = send(command, dataCommand);
        
        if (response.statusCode() != 200) {
            System.err.println("Unexpected response (" + response.statusCode() + 
                             ") communicating with the IDE plugin: " + response.body());
            return Optional.empty();
        }
        
        if (response.body() != null && !response.body().isEmpty()) {
            JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
            return Optional.of(responseJson);
        }
        
        return Optional.empty();
    }
}