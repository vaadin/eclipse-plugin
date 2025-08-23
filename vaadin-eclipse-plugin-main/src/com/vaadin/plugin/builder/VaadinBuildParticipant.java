package com.vaadin.plugin.builder;

import java.io.ByteArrayInputStream;
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

/**
 * Build participant that generates files in the output folder during compilation. These files will be automatically
 * included by WTP in the deployment. Only activates for projects with Vaadin dependencies.
 */
public class VaadinBuildParticipant extends IncrementalProjectBuilder {

    public static final String BUILDER_ID = "vaadin-eclipse-plugin.vaadinBuilder";
    private static final String HELLO_FILE_NAME = "hello.txt";

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

        // Generate the hello.txt file in the output folder
        generateHelloFile(project, monitor);

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
                    String path = entry.getPath().toString().toLowerCase();
                    // Check for Vaadin in the path (covers JARs, Maven dependencies, etc.)
                    if (path.contains("vaadin")) {
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
     * Generates hello.txt in the project's output folder (target/classes or bin). This file will be automatically
     * included in WTP deployment.
     */
    private void generateHelloFile(IProject project, IProgressMonitor monitor) {
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

            // Create the hello.txt file in the output folder
            IFile helloFile = outputFolder.getFile(HELLO_FILE_NAME);

            // Content: the absolute path of the project
            String content = project.getLocation().toOSString();
            ByteArrayInputStream contentStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

            if (helloFile.exists()) {
                // Update existing file
                helloFile.setContents(contentStream, IResource.FORCE, monitor);
            } else {
                // Create new file
                helloFile.create(contentStream, IResource.FORCE, monitor);
            }

            // Mark as derived so it won't be committed to version control
            helloFile.setDerived(true, monitor);

            System.out.println("Generated " + HELLO_FILE_NAME + " in output folder: " + outputFolder.getFullPath());

        } catch (CoreException e) {
            System.err.println("Failed to generate hello.txt: " + e.getMessage());
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
                    IFile helloFile = outputFolder.getFile(HELLO_FILE_NAME);
                    if (helloFile.exists()) {
                        helloFile.delete(true, monitor);
                    }
                }
            }
        } catch (CoreException e) {
            // Ignore cleanup errors
        }
    }
}
