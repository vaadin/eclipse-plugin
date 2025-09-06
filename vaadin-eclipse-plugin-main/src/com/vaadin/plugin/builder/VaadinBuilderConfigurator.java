package com.vaadin.plugin.builder;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;

/**
 * Automatically adds the Vaadin builder to Java projects. The builder itself will check for Vaadin dependencies.
 */
public class VaadinBuilderConfigurator implements IResourceChangeListener {

    private static VaadinBuilderConfigurator instance;

    public static void initialize() {
        if (instance == null) {
            instance = new VaadinBuilderConfigurator();
            System.out.println("VaadinBuilderConfigurator: Initializing...");

            ResourcesPlugin.getWorkspace().addResourceChangeListener(instance, IResourceChangeEvent.POST_CHANGE);

            // Configure existing projects
            IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
            System.out.println("VaadinBuilderConfigurator: Found " + projects.length + " projects");

            for (IProject project : projects) {
                instance.configureProject(project);
            }
        }
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
            IResourceDelta delta = event.getDelta();
            if (delta != null) {
                processResourceDelta(delta);
            }
        }
    }

    private void processResourceDelta(IResourceDelta delta) {
        IResourceDelta[] children = delta.getAffectedChildren();
        for (IResourceDelta child : children) {
            if (child.getResource() instanceof IProject) {
                IProject project = (IProject) child.getResource();

                // Check if this is a new or opened project, or if description changed (nature
                // added)
                if ((child.getFlags() & IResourceDelta.OPEN) != 0 || child.getKind() == IResourceDelta.ADDED
                        || (child.getFlags() & IResourceDelta.DESCRIPTION) != 0) {
                    configureProject(project);
                }
            }
        }
    }

    private void configureProject(IProject project) {
        try {
            System.out.println("VaadinBuilderConfigurator: Checking project " + project.getName());

            if (!project.isOpen()) {
                System.out.println("  - Project is not open");
                return;
            }

            if (!project.hasNature(JavaCore.NATURE_ID)) {
                System.out.println("  - Not a Java project");
                return;
            }

            IProjectDescription desc = project.getDescription();
            ICommand[] commands = desc.getBuildSpec();

            // Check if builder is already present
            for (ICommand command : commands) {
                if (VaadinBuildParticipant.BUILDER_ID.equals(command.getBuilderName())) {
                    System.out.println("  - Builder already configured");
                    return; // Already configured
                }
            }

            // Add builder to project (after Java builder)
            ICommand[] newCommands = new ICommand[commands.length + 1];
            System.arraycopy(commands, 0, newCommands, 0, commands.length);

            ICommand vaadinCommand = desc.newCommand();
            vaadinCommand.setBuilderName(VaadinBuildParticipant.BUILDER_ID);
            newCommands[commands.length] = vaadinCommand;

            desc.setBuildSpec(newCommands);
            project.setDescription(desc, null);

            System.out.println("  - Added Vaadin builder to project: " + project.getName());

        } catch (CoreException e) {
            System.out.println("  - Error configuring project: " + e.getMessage());
        }
    }

    /**
     * Manually configure a specific project (useful for testing).
     */
    public static void configureProjectManually(IProject project) {
        if (instance != null) {
            instance.configureProject(project);
        }
    }

    /**
     * Removes the Vaadin builder from a project.
     */
    public static void removeBuilder(IProject project) {
        try {
            IProjectDescription description = project.getDescription();
            ICommand[] commands = description.getBuildSpec();

            for (int i = 0; i < commands.length; ++i) {
                if (VaadinBuildParticipant.BUILDER_ID.equals(commands[i].getBuilderName())) {
                    ICommand[] newCommands = new ICommand[commands.length - 1];
                    System.arraycopy(commands, 0, newCommands, 0, i);
                    System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
                    description.setBuildSpec(newCommands);
                    project.setDescription(description, null);
                    return;
                }
            }
        } catch (CoreException e) {
            // Ignore
        }
    }
}
