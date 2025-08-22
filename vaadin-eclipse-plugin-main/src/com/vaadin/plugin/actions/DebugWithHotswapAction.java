package com.vaadin.plugin.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.vaadin.plugin.hotswap.HotswapAgentManager;
import com.vaadin.plugin.hotswap.HotswapLaunchConfigurationDelegate;
import com.vaadin.plugin.hotswap.JetBrainsRuntimeManager;

/**
 * Action to debug a project with Hotswap Agent. This action creates a special launch configuration with Hotswap Agent
 * enabled.
 */
public class DebugWithHotswapAction implements IWorkbenchWindowActionDelegate, IObjectActionDelegate {

    private IWorkbenchWindow window;
    private IProject selectedProject;

    @Override
    public void init(IWorkbenchWindow window) {
        this.window = window;
    }

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        // Not needed
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        selectedProject = null;

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            Object firstElement = structuredSelection.getFirstElement();

            if (firstElement instanceof IProject) {
                selectedProject = (IProject) firstElement;
            } else if (firstElement instanceof IJavaProject) {
                selectedProject = ((IJavaProject) firstElement).getProject();
            } else if (firstElement instanceof IResource) {
                selectedProject = ((IResource) firstElement).getProject();
            } else if (firstElement instanceof IAdaptable) {
                IAdaptable adaptable = (IAdaptable) firstElement;
                selectedProject = adaptable.getAdapter(IProject.class);
                if (selectedProject == null) {
                    IJavaProject javaProject = adaptable.getAdapter(IJavaProject.class);
                    if (javaProject != null) {
                        selectedProject = javaProject.getProject();
                    }
                }
            }
        }

        // Enable action only for Java projects
        action.setEnabled(selectedProject != null && isJavaProject(selectedProject));
    }

    @Override
    public void run(IAction action) {
        if (selectedProject == null) {
            showError("No project selected", "Please select a Java project to debug with Hotswap Agent.");
            return;
        }

        // Run in a background job
        Job job = new Job("Preparing Hotswap Debug") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    monitor.beginTask("Preparing Hotswap debug session", 100);

                    // Ensure Hotswap Agent is installed
                    monitor.subTask("Checking Hotswap Agent installation...");
                    HotswapAgentManager agentManager = HotswapAgentManager.getInstance();
                    if (!agentManager.isInstalled()) {
                        monitor.subTask("Installing Hotswap Agent...");
                        String version = agentManager.installHotswapAgent();
                        if (version == null) {
                            return new Status(IStatus.ERROR, "vaadin-eclipse-plugin",
                                    "Failed to install Hotswap Agent");
                        }
                    }
                    monitor.worked(30);

                    // Check for JBR
                    monitor.subTask("Checking JetBrains Runtime...");
                    JetBrainsRuntimeManager jbrManager = JetBrainsRuntimeManager.getInstance();
                    IVMInstall jbr = jbrManager.findInstalledJBR();

                    if (jbr == null) {
                        // Prompt user to install JBR
                        Display.getDefault().syncExec(() -> {
                            boolean install = MessageDialog.openQuestion(getShell(), "JetBrains Runtime Required",
                                    "Hotswap Agent requires JetBrains Runtime (JBR) for enhanced class redefinition.\n\n"
                                            + "JBR is not currently installed. Would you like to download and install it?\n\n"
                                            + "Note: This will download approximately 200MB.");

                            if (install) {
                                // TODO: Implement JBR download
                                MessageDialog.openInformation(getShell(), "Manual Installation Required",
                                        "Please download JetBrains Runtime from:\n"
                                                + "https://github.com/JetBrains/JetBrainsRuntime/releases\n\n"
                                                + "After downloading, extract it and add it as a JRE in:\n"
                                                + "Eclipse Preferences > Java > Installed JREs");
                            }
                        });

                        if (jbr == null) {
                            return new Status(IStatus.WARNING, "vaadin-eclipse-plugin",
                                    "JetBrains Runtime not installed. Hotswap Agent may not work properly.");
                        }
                    }
                    monitor.worked(30);

                    // Find or create launch configuration
                    monitor.subTask("Creating launch configuration...");
                    ILaunchConfiguration launchConfig = findOrCreateLaunchConfiguration(selectedProject);
                    if (launchConfig == null) {
                        return new Status(IStatus.ERROR, "vaadin-eclipse-plugin",
                                "Failed to create launch configuration");
                    }
                    monitor.worked(30);

                    // Launch in debug mode
                    monitor.subTask("Launching debug session...");
                    Display.getDefault().asyncExec(() -> {
                        DebugUITools.launch(launchConfig, ILaunchManager.DEBUG_MODE);
                    });
                    monitor.worked(10);

                    return Status.OK_STATUS;

                } catch (Exception e) {
                    return new Status(IStatus.ERROR, "vaadin-eclipse-plugin", "Failed to launch Hotswap debug session",
                            e);
                } finally {
                    monitor.done();
                }
            }
        };

        job.setUser(true);
        job.schedule();
    }

    /**
     * Find or create a launch configuration for the project.
     *
     * @param project
     *            The project to create a configuration for
     * @return The launch configuration, or null if creation failed
     * @throws CoreException
     *             if configuration creation fails
     */
    private ILaunchConfiguration findOrCreateLaunchConfiguration(IProject project) throws CoreException {
        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType javaAppType = launchManager
                .getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);

        // Look for existing configurations
        ILaunchConfiguration[] configs = launchManager.getLaunchConfigurations(javaAppType);
        String projectName = project.getName();

        for (ILaunchConfiguration config : configs) {
            String configProject = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
            if (projectName.equals(configProject)) {
                // Check if it's already a Hotswap configuration
                if (config.getName().contains("[Hotswap]")) {
                    return config;
                }
                // Create a Hotswap version of this configuration
                return HotswapLaunchConfigurationDelegate.createHotswapConfiguration(config);
            }
        }

        // No existing configuration found, create a new one
        // This would need to determine the main class, which is complex
        // For now, return null and let the user create the configuration manually
        Display.getDefault().syncExec(() -> {
            MessageDialog.openInformation(getShell(), "Launch Configuration Required",
                    "Please create a standard Java Application launch configuration first,\n"
                            + "then use Debug with Hotswap to launch it with Hotswap Agent enabled.");
        });

        return null;
    }

    /**
     * Check if a project is a Java project.
     *
     * @param project
     *            The project to check
     * @return true if it's a Java project
     */
    private boolean isJavaProject(IProject project) {
        try {
            return project.isOpen() && project.hasNature(JavaCore.NATURE_ID);
        } catch (CoreException e) {
            return false;
        }
    }

    /**
     * Show an error dialog.
     *
     * @param title
     *            The dialog title
     * @param message
     *            The error message
     */
    private void showError(String title, String message) {
        Display.getDefault().asyncExec(() -> {
            MessageDialog.openError(getShell(), title, message);
        });
    }

    /**
     * Get the active shell.
     *
     * @return The active shell
     */
    private Shell getShell() {
        if (window != null) {
            return window.getShell();
        }
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    }

    @Override
    public void dispose() {
        // Nothing to dispose
    }
}
