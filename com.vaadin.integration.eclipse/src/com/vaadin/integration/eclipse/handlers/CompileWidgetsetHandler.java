package com.vaadin.integration.eclipse.handlers;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;

import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.builder.WidgetsetBuildManager;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.WidgetsetUtil;

/**
 * Handler for Compile Widgetset action.
 *
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class CompileWidgetsetHandler extends AbstractVaadinCompileHandler {

    public CompileWidgetsetHandler() {
    }

    @Override
    public void startCompileJob(ISelection currentSelection,
            IEditorPart activeEditor) {
        startCompileWidgetsetJob(currentSelection, activeEditor, null);
    }

    public static void startCompileWidgetsetJob(
            final ISelection currentSelection, final IEditorPart activeEditor,
            ISchedulingRule schedulingRule) {
        Job job = new Job("Compiling widgetset...") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    monitor.beginTask("Compiling widgetset", 1);

                    // 1. Selected gwt.xml file
                    IFile file = getSelectedFile(currentSelection);
                    boolean compiled = false;
                    if (file != null) {
                        IProject project = file.getProject();
                        VaadinFacetUtils.fixFacetVersion(project);
                        if (WidgetsetUtil.isWidgetsetManagedByPlugin(project)) {
                            WidgetsetUtil.ensureWidgetsetNature(project);
                            compiled = compileFile(monitor, file);
                        }
                    }

                    // 2. The only gwt.xml found in the selected project
                    if (!compiled) {
                        IProject project = ProjectUtil
                                .getProject(currentSelection);
                        if (VaadinFacetUtils.isVaadinProject(project)) {
                            VaadinFacetUtils.fixFacetVersion(project);
                            if (WidgetsetUtil
                                    .isWidgetsetManagedByPlugin(project)) {
                                WidgetsetUtil.ensureWidgetsetNature(project);
                                IJavaProject jproject = JavaCore
                                        .create(project);
                                WidgetsetBuildManager.compileWidgetsets(
                                        jproject, monitor);
                                compiled = true;
                            }
                        }
                    }

                    // 3. The only gwt.xml found in the project defined by the
                    // open editor
                    if (!compiled) {
                        file = getFileForEditor(activeEditor);
                        if (file != null
                                && WidgetsetUtil
                                .isWidgetsetManagedByPlugin(file
                                        .getProject())) {
                            VaadinFacetUtils.fixFacetVersion(file.getProject());
                            compiled = compileFile(monitor, file);
                        }
                    }

                    // No widget set found
                    if (!compiled) {
                        ErrorUtil
                        .displayErrorFromBackgroundThread(
                                "Select widgetset",
                                "Select a widgetset file (..widgetset.gwt.xml) or a Vaadin project to compile.");
                    }
                } catch (OperationCanceledException e) {
                    // Do nothing if user cancels compilation
                } catch (Exception e) {
                    showException(e);
                    // Also log the exception
                    ErrorUtil.handleBackgroundException(IStatus.ERROR,
                            "Widgetset compilation failed", e);
                } finally {
                    monitor.done();
                }
                return Status.OK_STATUS;
            }

        };

        if (schedulingRule != null) {
            job.setRule(schedulingRule);
        }
        job.setUser(false);
        job.schedule();
    }

    private static IFile getSelectedFile(ISelection currentSelection) {
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

    // try to compile a file as a GWT widgetset, or if not one, try to
    // compile widgetsets in the containing project
    protected static boolean compileFile(IProgressMonitor monitor, IFile file)
            throws CoreException, IOException, InterruptedException {
        if (!WidgetsetUtil.isWidgetsetManagedByPlugin(file.getProject())) {
            return false;
        }
        // only one branch is executed so progress is tracked correctly
        boolean compiled = false;
        if (file != null && file.getName().endsWith(".gwt.xml")
                && file.getName().toLowerCase().contains("widgetset")) {
            WidgetsetBuildManager.compileWidgetset(file, monitor);
            compiled = true;
        }
        if (!compiled) {
            IProject project = ProjectUtil.getProject(file);
            if (VaadinFacetUtils.isVaadinProject(project)) {
                IJavaProject jproject = JavaCore.create(project);
                WidgetsetBuildManager.compileWidgetsets(jproject, monitor);
                compiled = true;
            }
        }

        return compiled;
    }

    protected static void showException(final Exception e) {
        ErrorUtil.displayErrorFromBackgroundThread("Error compiling widgetset",
                "Error compiling widgetset:\n" + e.getClass().getName() + " - "
                        + e.getMessage() + "\n\nSee error log for details.");
    }
}
