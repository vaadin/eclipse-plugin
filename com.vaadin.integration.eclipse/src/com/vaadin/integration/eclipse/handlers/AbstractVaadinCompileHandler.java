package com.vaadin.integration.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;

import com.vaadin.integration.eclipse.util.ProjectUtil;

/**
 * Abstract base class for theme and widgetset compilation handlers
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public abstract class AbstractVaadinCompileHandler extends AbstractHandler {

    protected AbstractVaadinCompileHandler() {
    }

    /**
     * the command has been executed, so extract extract the needed information
     * from the application context.
     */
    public Object execute(final ExecutionEvent event) throws ExecutionException {

        final ISelection currentSelection = HandlerUtil
                .getCurrentSelection(event);
        final IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);

        startCompileJob(currentSelection, activeEditor);

        return null;
    }

    protected abstract void startCompileJob(ISelection currentSelection,
            IEditorPart activeEditor);

    public static IFile getFileForEditor(IEditorPart editor) {
        IFile file = null;
        if (editor != null
                && editor.getEditorInput() instanceof IFileEditorInput) {
            IFileEditorInput input = (IFileEditorInput) editor.getEditorInput();
            file = input.getFile();
        }
        return file;
    }

    /**
     * Find a project based on current selection and active editor.
     * 
     * @param currentSelection
     * @param activeEditor
     * @return project or null if no suitable project found based on selection
     *         and active editor
     */
    public static IProject getProject(ISelection currentSelection,
            IEditorPart activeEditor) {
        if (currentSelection instanceof IStructuredSelection
                && ((IStructuredSelection) currentSelection).size() == 1) {
            IStructuredSelection ssel = (IStructuredSelection) currentSelection;
            Object obj = ssel.getFirstElement();
            if (obj instanceof IFile) {
                IFile file = (IFile) obj;
                return file.getProject();
            }
            IProject project = ProjectUtil.getProject(currentSelection);
            if (project == null) {
                IFile file = getFileForEditor(activeEditor);
                if (file != null && file.exists()) {
                    return file.getProject();
                }
            } else {
                return project;
            }
        } else {
            IFile file = getFileForEditor(activeEditor);
            if (file != null) {
                return file.getProject();
            }
        }
        return null;
    }

    protected static IFile getSelectedFile(ISelection currentSelection) {

        if (currentSelection instanceof IStructuredSelection
                && ((IStructuredSelection) currentSelection).size() == 1) {
            IStructuredSelection ssel = (IStructuredSelection) currentSelection;
            Object obj = ssel.getFirstElement();
            if (obj instanceof IFile) {
                return (IFile) obj;
            }

        }

        return null;
    }

    protected static IProject getSelectedProject(ISelection currentSelection,
            IEditorPart activeEditor) {
        IProject project = ProjectUtil.getAnyProject(currentSelection);
        if (project != null) {
            return project;
        }

        IFile file = getFileForEditor(activeEditor);
        if (file != null) {
            return file.getProject();
        }

        return null;
    }

}
