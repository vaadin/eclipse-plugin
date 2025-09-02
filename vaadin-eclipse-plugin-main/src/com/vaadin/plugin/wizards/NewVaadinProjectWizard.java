package com.vaadin.plugin.wizards;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

/**
 * New Vaadin Project creation wizard.
 */
public class NewVaadinProjectWizard extends Wizard implements INewWizard {

    private VaadinProjectWizardPage mainPage;
    private IWorkbench workbench;

    public NewVaadinProjectWizard() {
        super();
        setNeedsProgressMonitor(true);
        setWindowTitle("Vaadin");
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
    }

    @Override
    public void addPages() {
        mainPage = new VaadinProjectWizardPage();
        addPage(mainPage);
    }

    @Override
    public boolean performFinish() {
        final ProjectModel model = mainPage.getProjectModel();

        IRunnableWithProgress op = new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException {
                try {
                    doFinish(model, monitor);
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }
        };

        try {
            getContainer().run(true, false, op);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            Throwable realException = e.getTargetException();
            MessageDialog.openError(getShell(), "Error", "Project creation failed: " + realException.getMessage());
            return false;
        }

        return true;
    }

    private void doFinish(ProjectModel model, IProgressMonitor monitor)
            throws IOException, CoreException, InterruptedException {
        SubMonitor subMonitor = SubMonitor.convert(monitor, "Creating Vaadin project...", 100);

        // Step 1: Download project ZIP
        subMonitor.subTask("Downloading project template...");
        Path tempZip = downloadProject(model, subMonitor.split(40));

        // Step 2: Extract to workspace
        subMonitor.subTask("Extracting project...");
        Path projectPath = extractProject(tempZip, model.getProjectName(), subMonitor.split(30));

        // Step 3: Import project based on type
        subMonitor.subTask("Importing project...");
        IProject project = null;

        if (Files.exists(projectPath.resolve("pom.xml"))) {
            // Import as Maven project directly
            project = importMavenProject(projectPath, model.getProjectName(), subMonitor.split(25));
        } else if (Files.exists(projectPath.resolve("build.gradle"))
                || Files.exists(projectPath.resolve("build.gradle.kts"))) {
            // Import as Gradle project
            project = importGradleProject(projectPath, model.getProjectName(), subMonitor.split(25));
        } else {
            // Import as generic Eclipse project
            project = importProject(projectPath, model.getProjectName(), subMonitor.split(25));
        }

        // Step 4: Open README
        subMonitor.subTask("Opening README...");
        openReadme(project, subMonitor.split(5));

        // Clean up
        Files.deleteIfExists(tempZip);
    }

    private Path downloadProject(ProjectModel model, IProgressMonitor monitor)
            throws IOException, InterruptedException {
        String downloadUrl = model.getDownloadUrl();
        Path tempFile = Files.createTempFile("vaadin-project", ".zip");

        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).GET().build();

        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tempFile));

        if (response.statusCode() != 200) {
            throw new IOException("Failed to download project: HTTP " + response.statusCode());
        }

        return tempFile;
    }

    private Path extractProject(Path zipFile, String projectName, IProgressMonitor monitor) throws IOException {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        Path workspacePath = Paths.get(root.getLocation().toString());
        Path finalProjectPath = workspacePath.resolve(projectName);

        // If project directory already exists, delete it
        if (Files.exists(finalProjectPath)) {
            deleteDirectory(finalProjectPath);
        }

        // Create the project directory
        Files.createDirectories(finalProjectPath);

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile.toFile())))) {
            ZipEntry entry;
            byte[] buffer = new byte[4096];
            String rootFolder = null;

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                // Identify the root folder in the ZIP (if any)
                if (rootFolder == null && entryName.contains("/")) {
                    int firstSlash = entryName.indexOf("/");
                    rootFolder = entryName.substring(0, firstSlash + 1);
                }

                // Skip the root folder itself and strip it from the path
                String targetName = entryName;
                if (rootFolder != null && entryName.startsWith(rootFolder)) {
                    targetName = entryName.substring(rootFolder.length());
                    // Skip if it's just the root folder entry itself
                    if (targetName.isEmpty()) {
                        zis.closeEntry();
                        continue;
                    }
                }

                Path targetPath = finalProjectPath.resolve(targetName);

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    try (FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }

            return finalProjectPath;
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path).sorted(java.util.Comparator.reverseOrder()).map(Path::toFile)
                    .forEach(java.io.File::delete);
        }
    }

    private IProject importMavenProject(Path projectPath, String projectName, IProgressMonitor monitor)
            throws CoreException {
        // Use the regular import and then configure as Maven
        System.out.println("=== Creating project and configuring Maven ===");

        // First create the project normally
        IProject project = importProject(projectPath, projectName, monitor);

        try {
            // Then configure it as a Maven project
            org.eclipse.m2e.core.project.IProjectConfigurationManager configManager = org.eclipse.m2e.core.MavenPlugin
                    .getProjectConfigurationManager();

            // Create resolver configuration
            org.eclipse.m2e.core.project.ResolverConfiguration resolverConfig = new org.eclipse.m2e.core.project.ResolverConfiguration();
            resolverConfig.setResolveWorkspaceProjects(true);

            // Enable Maven nature on the project
            configManager.enableMavenNature(project, resolverConfig, monitor);

            // Force update project configuration - this is important for Kotlin projects
            // and ensures all dependencies are downloaded and configured
            org.eclipse.m2e.core.project.MavenUpdateRequest updateRequest = new org.eclipse.m2e.core.project.MavenUpdateRequest(
                    java.util.Collections.singletonList(project), // projects to update
                    false, // offline
                    true   // force update snapshots
            );
            
            configManager.updateProjectConfiguration(updateRequest, monitor);

            // Additional refresh to ensure all resources are visible
            project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

            System.out.println("Maven nature enabled and project configured with forced update");
            System.out.println("Has Maven nature: " + project.hasNature("org.eclipse.m2e.core.maven2Nature"));

        } catch (Exception e) {
            System.err.println("Failed to configure Maven nature: " + e.getMessage());
            e.printStackTrace();
        }

        return project;
    }

    private IProject importGradleProject(Path projectPath, String projectName, IProgressMonitor monitor)
            throws CoreException {
        System.out.println("=== Importing Gradle project ===");
        System.out.println("Project path: " + projectPath);
        System.out.println("Project name: " + projectName);

        IProject project = null;
        
        // Try to use Buildship's import mechanism if available
        try {
            // This will throw NoClassDefFoundError if Buildship is not available
            project = importGradleProjectWithBuildship(projectPath, projectName, monitor);
            if (project != null) {
                System.out.println("Gradle project imported with Buildship successfully");
                return project;
            }
        } catch (NoClassDefFoundError | ClassNotFoundException e) {
            System.out.println("Buildship not available, using basic Gradle configuration");
        } catch (Exception e) {
            System.err.println("Failed to import Gradle project with Buildship: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Fall back to basic import
        project = importProject(projectPath, projectName, monitor);
        configureBasicGradleProject(project, monitor);
        
        return project;
    }
    
    /**
     * Import a Gradle project using Buildship API directly.
     * This method will fail with NoClassDefFoundError if Buildship is not available,
     * which is caught by the caller.
     */
    private IProject importGradleProjectWithBuildship(Path projectPath, String projectName, IProgressMonitor monitor) 
            throws Exception {
        // Direct API calls - will fail if Buildship is not available
        org.eclipse.buildship.core.GradleWorkspace workspace = org.eclipse.buildship.core.GradleCore.getWorkspace();
        
        // Create build configuration
        org.eclipse.buildship.core.BuildConfiguration buildConfig = org.eclipse.buildship.core.BuildConfiguration
                .forRootProjectDirectory(projectPath.toFile())
                .overrideWorkspaceConfiguration(true)
                .build();
        
        // Create a new Gradle build for this configuration
        org.eclipse.buildship.core.GradleBuild gradleBuild = workspace.createBuild(buildConfig);
        
        // Synchronize the project - this will import it and set up everything
        gradleBuild.synchronize(monitor);
        
        // The project should now exist in the workspace
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = root.getProject(projectName);
        
        // Ensure the project is open and refreshed
        if (project != null && project.exists()) {
            if (!project.isOpen()) {
                project.open(monitor);
            }
            project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
            
            // Give Buildship a moment to finish background tasks
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
            
            // One more refresh to be sure
            project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }
        
        return project;
    }
    
    private void configureBasicGradleProject(IProject project, IProgressMonitor monitor) throws CoreException {
        // Add Gradle nature and Java nature if not already present
        IProjectDescription description = project.getDescription();
        String[] natures = description.getNatureIds();
        
        boolean hasJavaNature = false;
        boolean hasGradleNature = false;
        
        for (String nature : natures) {
            if ("org.eclipse.jdt.core.javanature".equals(nature)) {
                hasJavaNature = true;
            }
            if ("org.eclipse.buildship.core.gradleprojectnature".equals(nature)) {
                hasGradleNature = true;
            }
        }
        
        java.util.List<String> newNatures = new java.util.ArrayList<>(java.util.Arrays.asList(natures));
        if (!hasJavaNature) {
            newNatures.add("org.eclipse.jdt.core.javanature");
        }
        if (!hasGradleNature) {
            newNatures.add("org.eclipse.buildship.core.gradleprojectnature");
        }
        
        if (!hasJavaNature || !hasGradleNature) {
            description.setNatureIds(newNatures.toArray(new String[0]));
            
            // Add builders
            org.eclipse.core.resources.ICommand javaBuilder = description.newCommand();
            javaBuilder.setBuilderName("org.eclipse.jdt.core.javabuilder");
            
            org.eclipse.core.resources.ICommand gradleBuilder = description.newCommand();
            gradleBuilder.setBuilderName("org.eclipse.buildship.core.gradleprojectbuilder");
            
            description.setBuildSpec(new org.eclipse.core.resources.ICommand[] { javaBuilder, gradleBuilder });
            
            project.setDescription(description, monitor);
        }
        
        project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
    }

    private IProject importProject(Path projectPath, String projectName, IProgressMonitor monitor)
            throws CoreException {
        System.out.println("=== Using regular Eclipse project import ===");
        System.out.println("Project path: " + projectPath);
        System.out.println("Project name: " + projectName);

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = root.getProject(projectName);

        if (!project.exists()) {
            // Create project description
            IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
            description.setLocation(null); // Use default location

            // Create and open project
            project.create(description, monitor);
            project.open(monitor);

            // Refresh to pick up extracted files
            project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }

        return project;
    }

    private void openReadme(IProject project, IProgressMonitor monitor) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
            try {
                IResource readme = project.findMember("README.md");
                if (readme == null) {
                    readme = project.findMember("readme.md");
                }

                if (readme != null && readme.exists()) {
                    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                    IDE.openEditor(page, (org.eclipse.core.resources.IFile) readme);
                }
            } catch (PartInitException e) {
                // Ignore - README opening is not critical
            }
        });
    }
}
