package com.vaadin.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.vaadin.plugin.util.VaadinPluginLog;

/**
 * Manages undo/redo operations for Copilot file modifications.
 */
public class CopilotUndoManager {

    private static CopilotUndoManager instance;
    private static final IUndoContext WORKSPACE_CONTEXT = new WorkspaceUndoContext();
    private IOperationHistory operationHistory;
    private Map<String, List<IUndoableOperation>> fileOperations;

    private CopilotUndoManager() {
        operationHistory = OperationHistoryFactory.getOperationHistory();
        fileOperations = new HashMap<>();
    }

    public static synchronized CopilotUndoManager getInstance() {
        if (instance == null) {
            instance = new CopilotUndoManager();
        }
        return instance;
    }

    /**
     * Record a file modification operation for undo/redo.
     */
    public void recordOperation(IFile file, String oldContent, String newContent, String label) {
        recordOperation(file, oldContent, newContent, label, false);
    }

    /**
     * Record a file modification operation for undo/redo with binary flag.
     */
    public void recordOperation(IFile file, String oldContent, String newContent, String label, boolean isBase64) {
        // Create operation but don't execute it since content was already changed
        CopilotFileEditOperation operation = new CopilotFileEditOperation(file, oldContent, newContent, label,
                isBase64);

        try {
            // The file content has already been changed externally
            // We add the operation to the history in executed state
            IUndoContext context = getOrCreateWorkspaceContext();

            if (context != null) {
                operation.addContext(context);
            }
            operationHistory.add(operation);

            // Track operation for this file
            String filePath = file.getFullPath().toString();
            fileOperations.computeIfAbsent(filePath, k -> new ArrayList<>()).add(operation);

        } catch (Exception e) {
            VaadinPluginLog.error("Failed to record operation: " + e.getMessage(), e);
        }
    }

    /**
     * Get or create a workspace undo context. This handles cases where the workspace adapter might not be available.
     */
    private IUndoContext getOrCreateWorkspaceContext() {
        // First try to get the workspace context
        IUndoContext context = ResourcesPlugin.getWorkspace().getAdapter(IUndoContext.class);

        if (context == null) {
            // If no workspace context, use our singleton custom context
            // This ensures tests can run even without full workspace setup
            context = WORKSPACE_CONTEXT;
        }

        return context;
    }

    /**
     * Perform undo for specified files.
     */
    public boolean performUndo(List<String> filePaths) {
        boolean performed = false;

        try {
            for (String filePath : filePaths) {
                IFile file = findFile(filePath);
                if (file != null) {
                    // Get the workspace context which our operations use
                    IUndoContext context = getOrCreateWorkspaceContext();
                    if (context != null && operationHistory.canUndo(context)) {
                        IStatus status = operationHistory.undo(context, null, null);
                        if (status.isOK()) {
                            performed = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            VaadinPluginLog.error("Error performing undo: " + e.getMessage(), e);
        }

        return performed;
    }

    /**
     * Perform redo for specified files.
     */
    public boolean performRedo(List<String> filePaths) {
        boolean performed = false;

        try {
            for (String filePath : filePaths) {
                IFile file = findFile(filePath);
                if (file != null) {
                    // Get the workspace context which our operations use
                    IUndoContext context = getOrCreateWorkspaceContext();
                    if (context != null && operationHistory.canRedo(context)) {
                        IStatus status = operationHistory.redo(context, null, null);
                        if (status.isOK()) {
                            performed = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            VaadinPluginLog.error("Error performing redo: " + e.getMessage(), e);
        }

        return performed;
    }

    /**
     * Find IFile from absolute path.
     */
    private IFile findFile(String absolutePath) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IResource[] resources = workspace.getRoot().findFilesForLocationURI(new java.io.File(absolutePath).toURI());

        if (resources.length > 0 && resources[0] instanceof IFile) {
            return (IFile) resources[0];
        }

        return null;
    }

    /**
     * Get undo context for a file.
     */
    private IUndoContext getUndoContext(IFile file) {
        // Try to get context from open editor
        if (PlatformUI.isWorkbenchRunning()) {
            try {
                IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                // Find editor by checking all open editors
                IEditorPart[] editors = page.getEditors();
                for (IEditorPart editor : editors) {
                    if (editor.getEditorInput() != null && editor.getEditorInput().getAdapter(IFile.class) == file) {
                        if (editor instanceof ITextEditor) {
                            ITextEditor textEditor = (ITextEditor) editor;
                            Object adapter = textEditor.getAdapter(IUndoContext.class);
                            if (adapter instanceof IUndoContext) {
                                return (IUndoContext) adapter;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Workbench not available or error
            }
        }

        // Return workspace context as fallback
        return ResourcesPlugin.getWorkspace().getAdapter(IUndoContext.class);
    }

    /**
     * Custom workspace undo context for when the default one isn't available. This is primarily for test environments
     * where the workspace might not be fully initialized.
     */
    private static class WorkspaceUndoContext implements IUndoContext {
        private static final String LABEL = "Copilot Workspace Context";

        @Override
        public String getLabel() {
            return LABEL;
        }

        @Override
        public boolean matches(IUndoContext context) {
            // Match with itself or other workspace contexts
            return context == this || context instanceof WorkspaceUndoContext
                    || (context != null && LABEL.equals(context.getLabel()));
        }
    }

    /**
     * Custom undoable operation for Copilot file edits.
     */
    private static class CopilotFileEditOperation implements IUndoableOperation {

        private final IFile file;
        private final String oldContent;
        private final String newContent;
        private final String label;
        private final boolean isBase64;
        private IUndoContext[] contexts;

        public CopilotFileEditOperation(IFile file, String oldContent, String newContent, String label) {
            this(file, oldContent, newContent, label, false);
        }

        public CopilotFileEditOperation(IFile file, String oldContent, String newContent, String label,
                boolean isBase64) {
            this.file = file;
            this.oldContent = oldContent;
            this.newContent = newContent;
            this.label = label != null ? label : "Copilot Edit";
            this.isBase64 = isBase64;
            // Initialize with empty contexts, will be added during recordOperation
            this.contexts = new IUndoContext[0];
        }

        @Override
        public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
            // Should not be called since content is already applied
            // But if it is called, apply the new content
            return setFileContent(newContent);
        }

        @Override
        public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
            return setFileContent(oldContent);
        }

        @Override
        public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
            return setFileContent(newContent);
        }

        private IStatus setFileContent(String content) {
            try {
                byte[] bytes;
                if (isBase64) {
                    // For binary files, content is base64 encoded
                    if (content.isEmpty()) {
                        // Empty content for file deletion
                        bytes = new byte[0];
                    } else {
                        bytes = java.util.Base64.getDecoder().decode(content);
                    }
                } else {
                    // For text files, content is plain text
                    bytes = content.getBytes("UTF-8");
                }

                java.io.ByteArrayInputStream stream = new java.io.ByteArrayInputStream(bytes);
                file.setContents(stream, true, true, null);
                return Status.OK_STATUS;
            } catch (Exception e) {
                return new Status(IStatus.ERROR, "vaadin-eclipse-plugin",
                        "Failed to set file content: " + e.getMessage(), e);
            }
        }

        @Override
        public boolean canExecute() {
            return file.exists();
        }

        @Override
        public boolean canUndo() {
            return file.exists();
        }

        @Override
        public boolean canRedo() {
            return file.exists();
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public IUndoContext[] getContexts() {
            return contexts;
        }

        @Override
        public boolean hasContext(IUndoContext context) {
            for (IUndoContext c : contexts) {
                if (c.matches(context)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void addContext(IUndoContext context) {
            // Add context if not already present
            for (IUndoContext c : contexts) {
                if (c.matches(context)) {
                    return; // Already has this context
                }
            }
            // Create new array with added context
            IUndoContext[] newContexts = new IUndoContext[contexts.length + 1];
            System.arraycopy(contexts, 0, newContexts, 0, contexts.length);
            newContexts[contexts.length] = context;
            contexts = newContexts;
        }

        @Override
        public void removeContext(IUndoContext context) {
            // Not needed for our use case but implemented for completeness
            List<IUndoContext> remaining = new ArrayList<>();
            for (IUndoContext c : contexts) {
                if (!c.matches(context)) {
                    remaining.add(c);
                }
            }
            contexts = remaining.toArray(new IUndoContext[0]);
        }

        @Override
        public void dispose() {
            // Nothing to dispose
        }
    }
}
