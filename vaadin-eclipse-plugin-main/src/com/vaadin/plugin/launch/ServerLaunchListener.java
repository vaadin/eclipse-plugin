package com.vaadin.plugin.launch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;

/**
 * Listener that hooks into server launch events to inject files into the WAR.
 */
public class ServerLaunchListener implements ILaunchListener {

    private static final String HELLO_FILE_NAME = "hello.txt";
    private static final String TEMP_DIR_PREFIX = "vaadin-war-hook-";

    @Override
    public void launchAdded(ILaunch launch) {
        try {
            ILaunchConfiguration config = launch.getLaunchConfiguration();
            if (config == null) {
                return;
            }

            // Check if this is a server launch
            IServer server = ServerUtil.getServer(config);
            if (server == null) {
                return;
            }

            // Get the modules being deployed
            IModule[] modules = server.getModules();
            if (modules == null || modules.length == 0) {
                return;
            }

            for (IModule module : modules) {
                IProject project = module.getProject();
                if (project != null) {
                    injectFileIntoWar(project, server);
                }
            }

        } catch (Exception e) {
            // Log but don't fail the launch
            System.err.println("Failed to inject file into WAR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void launchRemoved(ILaunch launch) {
        // Clean up temp files if needed
        cleanupTempFiles();
    }

    @Override
    public void launchChanged(ILaunch launch) {
        // Not needed for this implementation
    }

    /**
     * Injects the hello.txt file into the WAR deployment.
     */
    private void injectFileIntoWar(IProject project, IServer server) throws IOException, CoreException {
        // Get the project's absolute path
        IPath projectLocation = project.getLocation();
        String projectPath = projectLocation.toOSString();

        // Create a temporary directory for our injected files
        Path tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX);
        File helloFile = new File(tempDir.toFile(), HELLO_FILE_NAME);

        // Write the project path to hello.txt
        try (FileWriter writer = new FileWriter(helloFile)) {
            writer.write(projectPath);
        }

        // Get the server's deployment directory
        IPath deployPath = server.getRuntime().getLocation();

        // For servers like Tomcat, we need to find the webapps directory
        // and ensure our file gets included in the deployment
        addFileToDeployment(project, helloFile, server);
    }

    /**
     * Adds the file to the deployment by placing it in the appropriate location.
     */
    private void addFileToDeployment(IProject project, File helloFile, IServer server) {
        try {
            // Get the server's runtime location for deployment
            IPath serverPath = server.getRuntime().getLocation();
            if (serverPath != null) {
                // For Tomcat, the webapps directory is typically at runtime/webapps
                Path serverDir = Paths.get(serverPath.toOSString());
                Path webappsDir = serverDir.resolve("webapps");

                if (Files.exists(webappsDir)) {
                    // Find the deployed application directory
                    String contextRoot = project.getName(); // Simple assumption
                    Path appDir = webappsDir.resolve(contextRoot);

                    // Create WEB-INF/classes directory structure
                    Path webInfDir = appDir.resolve("WEB-INF");
                    Path classesDir = webInfDir.resolve("classes");

                    if (!Files.exists(classesDir)) {
                        Files.createDirectories(classesDir);
                    }

                    // Place hello.txt in WEB-INF/classes so it's available as a classpath resource
                    Path targetFile = classesDir.resolve(HELLO_FILE_NAME);
                    Files.copy(helloFile.toPath(), targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                    System.out.println("Added " + HELLO_FILE_NAME + " as classpath resource at: " + targetFile);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to add file to deployment: " + e.getMessage());
        }
    }

    /**
     * Cleans up temporary files created during deployment.
     */
    private void cleanupTempFiles() {
        try {
            // Clean up any temp directories we created
            File tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX).toFile().getParentFile();
            if (tempDir != null && tempDir.exists()) {
                File[] tempDirs = tempDir.listFiles((dir, name) -> name.startsWith(TEMP_DIR_PREFIX));
                if (tempDirs != null) {
                    for (File dir : tempDirs) {
                        deleteDirectory(dir);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }
}
