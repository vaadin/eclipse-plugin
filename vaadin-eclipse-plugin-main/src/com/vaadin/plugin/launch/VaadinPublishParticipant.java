package com.vaadin.plugin.launch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.PublishOperation;
import org.eclipse.wst.server.core.model.PublishTaskDelegate;

/**
 * Participates in the server publish process to inject files into the WAR.
 */
public class VaadinPublishParticipant extends PublishTaskDelegate {

    private static final String HELLO_FILE_NAME = "hello.txt";

    @Override
    public PublishOperation[] getTasks(IServer server, int kind, List modules, List kindList) {
        if (modules == null || modules.isEmpty()) {
            return new PublishOperation[0];
        }

        List<PublishOperation> operations = new ArrayList<>();

        for (int i = 0; i < modules.size(); i++) {
            IModule[] module = (IModule[]) modules.get(i);
            if (module != null && module.length > 0) {
                operations.add(new VaadinWarInjectionOperation(server, module[0]));
            }
        }

        return operations.toArray(new PublishOperation[0]);
    }

    /**
     * Custom publish operation that injects files into the WAR.
     */
    private static class VaadinWarInjectionOperation extends PublishOperation {
        private final IServer server;
        private final IModule module;

        public VaadinWarInjectionOperation(IServer server, IModule module) {
            super("Inject Vaadin files", "Injecting files into WAR deployment");
            this.server = server;
            this.module = module;
        }

        @Override
        public void execute(IProgressMonitor monitor, IAdaptable info) throws CoreException {
            try {
                IProject project = module.getProject();
                if (project == null) {
                    return;
                }

                // Get the deployment path for this module
                IPath deploymentPath = getDeploymentPath();
                if (deploymentPath == null) {
                    return;
                }

                // Create and inject the hello.txt file
                injectHelloFile(project, deploymentPath);

            } catch (Exception e) {
                throw new CoreException(
                        new Status(IStatus.ERROR, "vaadin-eclipse-plugin", "Failed to inject file into WAR", e));
            }
        }

        private IPath getDeploymentPath() {
            try {
                // Get the server's runtime location
                IPath serverPath = server.getRuntime().getLocation();
                if (serverPath != null) {
                    // Construct path to deployment directory
                    // This is server-specific but commonly webapps for Tomcat
                    IPath deployPath = serverPath.append("webapps").append(module.getName());
                    return deployPath;
                }
            } catch (Exception e) {
                System.err.println("Failed to get deployment path: " + e.getMessage());
            }
            return null;
        }

        private void injectHelloFile(IProject project, IPath deploymentPath) throws IOException {
            // Get the project's absolute path
            String projectPath = project.getLocation().toOSString();

            // Create the hello.txt file in WEB-INF/classes so it's available as a classpath
            // resource
            File deployDir = deploymentPath.toFile();
            File webInfDir = new File(deployDir, "WEB-INF");
            File classesDir = new File(webInfDir, "classes");

            // Ensure the WEB-INF/classes directory exists
            if (!classesDir.exists()) {
                classesDir.mkdirs();
            }

            // Write hello.txt to WEB-INF/classes so it's available via
            // getResourceAsStream()
            File helloFile = new File(classesDir, HELLO_FILE_NAME);
            try (FileWriter writer = new FileWriter(helloFile)) {
                writer.write(projectPath);
            }

            System.out.println(
                    "Injected " + HELLO_FILE_NAME + " as classpath resource at: " + helloFile.getAbsolutePath());
        }

        @Override
        public int getOrder() {
            // Run after normal publishing
            return 100;
        }

        @Override
        public int getKind() {
            return REQUIRED;
        }
    }
}
