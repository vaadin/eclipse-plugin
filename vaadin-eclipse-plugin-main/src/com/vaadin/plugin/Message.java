package com.vaadin.plugin;

import java.util.List;

/**
 * Message classes for Copilot REST API communication.
 */
public class Message {

    public static class Command {
        public String command;
        public Object data;
        
        public Command() {}
        
        public Command(String command, Object data) {
            this.command = command;
            this.data = data;
        }
    }

    public static class CopilotRestRequest {
        public String command;
        public String projectBasePath;
        public Object data;
        
        public CopilotRestRequest() {}
        
        public CopilotRestRequest(String command, String projectBasePath, Object data) {
            this.command = command;
            this.projectBasePath = projectBasePath;
            this.data = data;
        }
    }

    public static class WriteFileMessage {
        public String file;
        public String undoLabel;
        public String content;
        
        public WriteFileMessage() {}
        
        public WriteFileMessage(String file, String undoLabel, String content) {
            this.file = file;
            this.undoLabel = undoLabel;
            this.content = content;
        }
    }

    public static class UndoRedoMessage {
        public List<String> files;
        
        public UndoRedoMessage() {}
        
        public UndoRedoMessage(List<String> files) {
            this.files = files;
        }
    }

    public static class ShowInIdeMessage {
        public String file;
        public Integer line;
        public Integer column;
        
        public ShowInIdeMessage() {}
        
        public ShowInIdeMessage(String file, Integer line, Integer column) {
            this.file = file;
            this.line = line;
            this.column = column;
        }
    }

    public static class RefreshMessage {
        public RefreshMessage() {}
    }

    public static class RestartApplicationMessage {
        public RestartApplicationMessage() {}
    }

    public static class CompileMessage {
        public List<String> files;
        
        public CompileMessage() {}
        
        public CompileMessage(List<String> files) {
            this.files = files;
        }
    }

    public static class DeleteMessage {
        public String file;
        
        public DeleteMessage() {}
        
        public DeleteMessage(String file) {
            this.file = file;
        }
    }

    public static class GetVaadinRoutesMessage {
        public GetVaadinRoutesMessage() {}
    }

    public static class GetVaadinVersionMessage {
        public GetVaadinVersionMessage() {}
    }

    public static class GetVaadinComponentsMessage {
        public boolean includeMethods;
        
        public GetVaadinComponentsMessage() {}
        
        public GetVaadinComponentsMessage(boolean includeMethods) {
            this.includeMethods = includeMethods;
        }
    }

    public static class GetVaadinPersistenceMessage {
        public boolean includeMethods;
        
        public GetVaadinPersistenceMessage() {}
        
        public GetVaadinPersistenceMessage(boolean includeMethods) {
            this.includeMethods = includeMethods;
        }
    }

    public static class GetVaadinSecurityMessage {
        public GetVaadinSecurityMessage() {}
    }

    public static class GetModulePathsMessage {
        public GetModulePathsMessage() {}
    }

    public static class ReloadMavenModuleMessage {
        public String moduleName;
        
        public ReloadMavenModuleMessage() {}
        
        public ReloadMavenModuleMessage(String moduleName) {
            this.moduleName = moduleName;
        }
    }

    public static class HeartbeatMessage {
        public HeartbeatMessage() {}
    }
}