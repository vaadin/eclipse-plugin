package com.vaadin.integration.eclipse.maven;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.m2e.actions.ExecutePomAction;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class MavenUtil {

    public static boolean isMavenProject(IProject project) {
        if (project == null) {
            return false;
        }

        try {
            return project.hasNature(IMavenConstants.NATURE_ID);
        } catch (CoreException e) {
            return false;
        }
    }

    public static void runMavenGoal(IProject project,
            final String goal) {
        // Convert selection if necessary as runMavenGoal only
        // understands certain kinds of selection.
        // We only need to know the project, as it is only used to find
        // the base directory.
        final IStructuredSelection selection = new StructuredSelection(project);

        Display display = PlatformUI.getWorkbench().getDisplay();
        if (!display.isDisposed()) {
            // this needs to be done in the UI thread and will trigger a
            // background job
            display.asyncExec(new Runnable() {
                public void run() {
                    // TODO using internal package of m2e - can this be avoided
                    // without excessive duplication of code?
                    ExecutePomAction exec = new ExecutePomAction();
                    exec.setInitializationData(null, "", goal);
                    exec.launch(selection, ILaunchManager.RUN_MODE);
                }
            });
        }
    }

    public static boolean compileWidgetSet(IProject project) {
        runMavenGoal(project, "vaadin:update-widgetset vaadin:compile");
        return true;
    }

    public static boolean compileTheme(IProject project) {
        runMavenGoal(project,
                "vaadin:update-theme vaadin:compile-theme");
        return true;
    }

    public static boolean compileThemeAndWidgetset(IProject project) {
        // TODO see above
        runMavenGoal(
                project,
                "vaadin:update-theme vaadin:update-widgetset vaadin:compile-theme vaadin:compile");
        return true;
    }
}
