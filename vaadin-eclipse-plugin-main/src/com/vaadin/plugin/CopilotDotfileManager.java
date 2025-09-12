package com.vaadin.plugin;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import com.vaadin.plugin.util.VaadinPluginLog;

/**
 * Manages the .copilot-plugin dotfile for Vaadin projects. Creates the dotfile in .eclipse folder when a Vaadin project
 * is opened. Removes it when the project is closed.
 */
public class CopilotDotfileManager implements IResourceChangeListener {

    private static final String DOTFILE_NAME = ".copilot-plugin";
    private static final String ECLIPSE_FOLDER = ".eclipse";
    private static final String PLUGIN_VERSION = "1.0.0"; // TODO: Get from bundle version

    private static CopilotDotfileManager instance;

    public static CopilotDotfileManager getInstance() {
        if (instance == null) {
            instance = new CopilotDotfileManager();
        }
        return instance;
    }

    private CopilotDotfileManager() {
        // Register as resource change listener
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
                IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE);
    }

    /**
     * Initialize dotfiles for all open Vaadin projects
     */
    public void initialize() {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject[] projects = workspace.getRoot().getProjects();

        for (IProject project : projects) {
            if (project.isOpen() && isVaadinProject(project)) {
                createDotfile(project);
            }
        }
    }

    /**
     * Cleanup dotfiles when shutting down
     */
    public void shutdown() {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject[] projects = workspace.getRoot().getProjects();

        for (IProject project : projects) {
            if (project.isOpen()) {
                removeDotfile(project);
            }
        }

        workspace.removeResourceChangeListener(this);
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
            // Handle project open
            try {
                event.getDelta().accept(new IResourceDeltaVisitor() {
                    @Override
                    public boolean visit(IResourceDelta delta) throws CoreException {
                        IResource resource = delta.getResource();
                        if (resource instanceof IProject) {
                            IProject project = (IProject) resource;

                            // Handle newly added projects (imported or created)
                            if (delta.getKind() == IResourceDelta.ADDED) {
                                if (project.isOpen() && isVaadinProject(project)) {
                                    createDotfile(project);
                                }
                            }
                            // Handle project open/close state changes
                            else if (delta.getKind() == IResourceDelta.CHANGED
                                    && (delta.getFlags() & IResourceDelta.OPEN) != 0) {
                                if (project.isOpen() && isVaadinProject(project)) {
                                    createDotfile(project);
                                } else if (!project.isOpen()) {
                                    removeDotfile(project);
                                }
                            }
                        }
                        return true;
                    }
                });
            } catch (CoreException e) {
                VaadinPluginLog.error("Error in resource change listener", e);
            }
        } else if (event.getType() == IResourceChangeEvent.PRE_CLOSE
                || event.getType() == IResourceChangeEvent.PRE_DELETE) {
            // Handle project close/delete
            IProject project = (IProject) event.getResource();
            removeDotfile(project);
        }
    }

    /**
     * Check if a project is a Vaadin project
     */
    private boolean isVaadinProject(IProject project) {
        if (!project.isOpen()) {
            return false;
        }

        try {
            // Check for pom.xml with Vaadin dependency
            IResource pomFile = project.findMember("pom.xml");
            if (pomFile != null && pomFile.exists()) {
                Path pomPath = Paths.get(pomFile.getLocationURI());
                String content = Files.readString(pomPath);
                if (content.contains("com.vaadin") || content.contains("vaadin-")) {
                    return true;
                }
            }

            // Check for build.gradle with Vaadin dependency
            IResource gradleFile = project.findMember("build.gradle");
            if (gradleFile != null && gradleFile.exists()) {
                Path gradlePath = Paths.get(gradleFile.getLocationURI());
                String content = Files.readString(gradlePath);
                if (content.contains("com.vaadin") || content.contains("vaadin-")) {
                    return true;
                }
            }

            // Check for build.gradle.kts with Vaadin dependency
            IResource gradleKtsFile = project.findMember("build.gradle.kts");
            if (gradleKtsFile != null && gradleKtsFile.exists()) {
                Path gradleKtsPath = Paths.get(gradleKtsFile.getLocationURI());
                String content = Files.readString(gradleKtsPath);
                if (content.contains("com.vaadin") || content.contains("vaadin-")) {
                    return true;
                }
            }

            // Check if it's a Java project with Vaadin classes
            if (project.hasNature(JavaCore.NATURE_ID)) {
                IJavaProject javaProject = JavaCore.create(project);
                VaadinProjectAnalyzer analyzer = new VaadinProjectAnalyzer(javaProject);
                // Check if there are any Vaadin routes (indicates a Vaadin project)
                try {
                    return !analyzer.findVaadinRoutes().isEmpty();
                } catch (CoreException e) {
                    // Ignore and continue checking
                }
            }
        } catch (Exception e) {
            // Ignore errors and assume not a Vaadin project
        }

        return false;
    }

    /**
     * Create the .copilot-plugin dotfile for a project
     */
    private void createDotfile(IProject project) {
        try {
            IPath projectLocation = project.getLocation();
            if (projectLocation == null) {
                return;
            }

            // Create .eclipse folder if it doesn't exist
            Path eclipseFolder = Paths.get(projectLocation.toString(), ECLIPSE_FOLDER);
            Files.createDirectories(eclipseFolder);

            // Create the dotfile
            Path dotfilePath = eclipseFolder.resolve(DOTFILE_NAME);

            // Get the REST service endpoint
            String endpoint = System.getProperty("vaadin.copilot.endpoint", "http://localhost:0/copilot");

            // Get supported actions from CopilotRestService
            String[] supportedActions = { "write", "writeBase64", "delete", "refresh", "showInIde", "undo", "redo",
                    "getVaadinRoutes", "getVaadinComponents", "getVaadinEntities", "getVaadinSecurity",
                    "getVaadinVersion", "getModulePaths", "compileFiles", "restartApplication", "reloadMavenModule",
                    "heartbeat" };

            // Create properties content
            Properties props = new Properties();
            props.setProperty("endpoint", endpoint);
            props.setProperty("ide", "eclipse");
            props.setProperty("version", PLUGIN_VERSION);
            props.setProperty("supportedActions", String.join(",", supportedActions));

            // Write properties to string
            StringWriter stringWriter = new StringWriter();
            props.store(stringWriter, "Vaadin Copilot Integration Runtime Properties");

            // Write to file
            Files.writeString(dotfilePath, stringWriter.toString());

            // Refresh the project to show the new file
            // Schedule refresh as a workspace job to avoid resource tree lock issues
            Job refreshJob = Job.create("Refresh project " + project.getName(), monitor -> {
                try {
                    if (project.exists() && project.isOpen()) {
                        project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
                    }
                } catch (CoreException e) {
                    // Log but don't fail - refresh is not critical
                    VaadinPluginLog.error("Failed to refresh project " + project.getName() + ": " + e.getMessage());
                }
            });
            refreshJob.setRule(project);
            refreshJob.schedule(100); // Small delay to ensure resource tree is unlocked

            VaadinPluginLog.info("Created .copilot-plugin dotfile for project: " + project.getName());

        } catch (Exception e) {
            VaadinPluginLog.error("Failed to create dotfile for project " + project.getName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Remove the .copilot-plugin dotfile for a project
     */
    private void removeDotfile(IProject project) {
        try {
            IPath projectLocation = project.getLocation();
            if (projectLocation == null) {
                return;
            }

            Path dotfilePath = Paths.get(projectLocation.toString(), ECLIPSE_FOLDER, DOTFILE_NAME);
            if (Files.exists(dotfilePath)) {
                Files.delete(dotfilePath);
                VaadinPluginLog.info("Removed .copilot-plugin dotfile for project: " + project.getName());
            }

        } catch (Exception e) {
            VaadinPluginLog.error("Failed to remove dotfile for project " + project.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Update the dotfile for a project (e.g., when endpoint changes)
     */
    public void updateDotfile(IProject project) {
        if (project.isOpen() && isVaadinProject(project)) {
            createDotfile(project);
        }
    }

    /**
     * Update all dotfiles (e.g., when endpoint changes)
     */
    public void updateAllDotfiles() {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject[] projects = workspace.getRoot().getProjects();

        for (IProject project : projects) {
            updateDotfile(project);
        }
    }
}
