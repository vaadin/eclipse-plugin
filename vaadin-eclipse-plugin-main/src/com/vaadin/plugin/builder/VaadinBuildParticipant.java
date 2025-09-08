package com.vaadin.plugin.builder;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Build participant that generates files in the output folder during compilation. These files will be automatically
 * included by WTP in the deployment. Only activates for projects with Vaadin dependencies.
 */
public class VaadinBuildParticipant extends IncrementalProjectBuilder {

    public static final String BUILDER_ID = "vaadin-eclipse-plugin.vaadinBuilder";
    private static final String FLOW_BUILD_INFO_PATH = "META-INF/VAADIN/config/flow-build-info.json";

    @Override
    protected IProject[] build(int kind, java.util.Map<String, String> args, IProgressMonitor monitor)
            throws CoreException {

        IProject project = getProject();
        System.out.println(
                "VaadinBuildParticipant.build() called for project: " + (project != null ? project.getName() : "null"));

        if (project == null || !project.isAccessible()) {
            System.out.println("  - Project is null or not accessible");
            return null;
        }

        // Check if this is a Java project
        if (!project.hasNature(JavaCore.NATURE_ID)) {
            System.out.println("  - Not a Java project");
            return null;
        }

        // Check if project has Vaadin dependencies
        boolean hasVaadin = hasVaadinDependency(project);
        System.out.println("  - Has Vaadin dependencies: " + hasVaadin);

        if (!hasVaadin) {
            return null;
        }

        // Update or create the flow-build-info.json file in the output folder
        updateFlowBuildInfo(project, monitor);

        return null;
    }

    /**
     * Checks if the project has Vaadin dependencies by examining the resolved classpath.
     */
    private boolean hasVaadinDependency(IProject project) {
        try {
            IJavaProject javaProject = JavaCore.create(project);
            if (javaProject != null) {
                // Check the resolved classpath entries (includes Maven/Gradle dependencies)
                IClasspathEntry[] classpath = javaProject.getResolvedClasspath(true);
                for (IClasspathEntry entry : classpath) {
                    String fullPath = entry.getPath().toString();
                    // Extract just the filename from the path
                    String filename = fullPath.substring(fullPath.lastIndexOf('/') + 1).toLowerCase();

                    // Check for Vaadin in the filename only (not the full path)
                    // This avoids false positives from temp directories containing "vaadin"
                    if (filename.contains("vaadin")) {
                        System.out.println("    Found Vaadin dependency: " + entry.getPath());
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // If we can't determine, assume no Vaadin dependency
            System.err.println("Error checking for Vaadin dependencies: " + e.getMessage());
        }

        return false;
    }

    /**
     * Updates or creates flow-build-info.json in the project's output folder. This file will be automatically included
     * in WTP deployment.
     */
    private void updateFlowBuildInfo(IProject project, IProgressMonitor monitor) {
        try {
            IJavaProject javaProject = JavaCore.create(project);
            if (javaProject == null) {
                return;
            }

            // Get the output location (e.g., target/classes or bin)
            IPath outputLocation = javaProject.getOutputLocation();
            IFolder outputFolder = project.getWorkspace().getRoot().getFolder(outputLocation);

            // Ensure the output folder exists
            if (!outputFolder.exists()) {
                return; // Output folder doesn't exist yet, will be created by Java builder
            }

            // Create the META-INF/VAADIN/config directory structure
            IFolder metaInfFolder = outputFolder.getFolder("META-INF");
            IFolder vaadinFolder = metaInfFolder.getFolder("VAADIN");
            IFolder configFolder = vaadinFolder.getFolder("config");

            // Create directories if they don't exist
            if (!metaInfFolder.exists()) {
                metaInfFolder.create(IResource.FORCE | IResource.DERIVED, true, monitor);
            }
            if (!vaadinFolder.exists()) {
                vaadinFolder.create(IResource.FORCE | IResource.DERIVED, true, monitor);
            }
            if (!configFolder.exists()) {
                configFolder.create(IResource.FORCE | IResource.DERIVED, true, monitor);
            }

            // Get or create the flow-build-info.json file
            IFile flowBuildInfoFile = configFolder.getFile("flow-build-info.json");

            // Read existing JSON or create new one
            JsonObject json;
            if (flowBuildInfoFile.exists()) {
                try (InputStreamReader reader = new InputStreamReader(flowBuildInfoFile.getContents(),
                        StandardCharsets.UTF_8)) {
                    json = JsonParser.parseReader(reader).getAsJsonObject();
                }
            } else {
                json = new JsonObject();
            }

            // Add or update npmFolder
            String projectPath = project.getLocation().toOSString();
            json.addProperty("npmFolder", projectPath);

            // Convert to formatted JSON string
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String updatedContent = gson.toJson(json);
            ByteArrayInputStream contentStream = new ByteArrayInputStream(
                    updatedContent.getBytes(StandardCharsets.UTF_8));

            if (flowBuildInfoFile.exists()) {
                // Update existing file
                flowBuildInfoFile.setContents(contentStream, IResource.FORCE, monitor);
            } else {
                // Create new file
                flowBuildInfoFile.create(contentStream, IResource.FORCE, monitor);
            }

            // Mark as derived so it won't be committed to version control
            flowBuildInfoFile.setDerived(true, monitor);
            metaInfFolder.setDerived(true, monitor);
            vaadinFolder.setDerived(true, monitor);
            configFolder.setDerived(true, monitor);

            System.out.println("Updated flow-build-info.json in output folder: " + configFolder.getFullPath());

        } catch (Exception e) {
            System.err.println("Failed to update flow-build-info.json: " + e.getMessage());
        }
    }

    @Override
    protected void clean(IProgressMonitor monitor) throws CoreException {
        // Clean up the generated file when project is cleaned
        IProject project = getProject();
        if (project == null || !project.isAccessible()) {
            return;
        }

        try {
            IJavaProject javaProject = JavaCore.create(project);
            if (javaProject != null) {
                IPath outputLocation = javaProject.getOutputLocation();
                IFolder outputFolder = project.getWorkspace().getRoot().getFolder(outputLocation);

                if (outputFolder.exists()) {
                    // Navigate to META-INF/VAADIN/config
                    IFolder metaInfFolder = outputFolder.getFolder("META-INF");
                    if (metaInfFolder.exists()) {
                        IFolder vaadinFolder = metaInfFolder.getFolder("VAADIN");
                        if (vaadinFolder.exists()) {
                            IFolder configFolder = vaadinFolder.getFolder("config");
                            if (configFolder.exists()) {
                                IFile flowBuildInfoFile = configFolder.getFile("flow-build-info.json");
                                if (flowBuildInfoFile.exists()) {
                                    flowBuildInfoFile.delete(true, monitor);
                                }
                                // Clean up empty directories
                                if (configFolder.members().length == 0) {
                                    configFolder.delete(true, monitor);
                                }
                            }
                            if (vaadinFolder.exists() && vaadinFolder.members().length == 0) {
                                vaadinFolder.delete(true, monitor);
                            }
                        }
                        if (metaInfFolder.exists() && metaInfFolder.members().length == 0) {
                            metaInfFolder.delete(true, monitor);
                        }
                    }
                }
            }
        } catch (CoreException e) {
            // Ignore cleanup errors
        }
    }
}
