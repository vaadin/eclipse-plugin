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

    public NewVaadinProjectWizard() {
        super();
        setNeedsProgressMonitor(true);
        setWindowTitle("Vaadin");
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
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

            // Update project configuration (non-deprecated method)
            configManager.updateProjectConfiguration(project, monitor);

            // Additional refresh to ensure all resources are visible
            project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

            System.out.println("Maven nature enabled and project configured");
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

        // First create the basic project
        IProject project = importProject(projectPath, projectName, monitor);

        // Try to use Buildship if available
        if (isBuildshipAvailable()) {
            try {
                configureBuildshipProject(project, projectPath, monitor);
                System.out.println("Gradle project configured with Buildship successfully");
            } catch (Exception e) {
                System.err.println("Failed to configure Gradle project with Buildship: " + e.getMessage());
                e.printStackTrace();
                // Fall back to basic Gradle configuration
                configureBasicGradleProject(project, monitor);
            }
        } else {
            System.out.println("Buildship not available, using basic Gradle configuration");
            configureBasicGradleProject(project, monitor);
        }

        return project;
    }

    private boolean isBuildshipAvailable() {
        try {
            Class.forName("org.eclipse.buildship.core.GradleCore");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void configureBuildshipProject(IProject project, Path projectPath, IProgressMonitor monitor) throws Exception {
        try {
            Class<?> gradleCoreClass = Class.forName("org.eclipse.buildship.core.GradleCore");
            Object gradleWorkspace = gradleCoreClass.getMethod("getWorkspace").invoke(null);

            // Build BuildConfiguration using reflection
            Class<?> buildConfigClass = Class.forName("org.eclipse.buildship.core.BuildConfiguration");
            Object buildConfigBuilder = buildConfigClass.getMethod("forRootProjectDirectory", java.io.File.class)
                    .invoke(null, projectPath.toFile());
            buildConfigBuilder = buildConfigBuilder.getClass().getMethod("overrideWorkspaceConfiguration", boolean.class)
                    .invoke(buildConfigBuilder, true);
            try {
                buildConfigBuilder = buildConfigBuilder.getClass().getMethod("offlineMode", boolean.class)
                        .invoke(buildConfigBuilder, false);
            } catch (NoSuchMethodException ex) {
                // ignore
            }
            Object buildConfig = buildConfigBuilder.getClass().getMethod("build").invoke(buildConfigBuilder);

            // Use createBuild(BuildConfiguration) to get GradleBuild
            Object gradleBuild = gradleWorkspace.getClass()
                .getMethod("createBuild", buildConfig.getClass())
                .invoke(gradleWorkspace, buildConfig);

            // Try to synchronize using NewProjectHandler if available
            try {
                Class<?> newProjectHandlerClass = null;
                Object newProjectHandler = null;
                try {
                    newProjectHandlerClass = Class.forName("org.eclipse.buildship.core.workspace.NewProjectHandler");
                    newProjectHandler = newProjectHandlerClass.getEnumConstants()[0]; // IMPORT
                } catch (ClassNotFoundException e) {
                    newProjectHandlerClass = Class.forName("org.eclipse.buildship.core.NewProjectHandler");
                    newProjectHandler = newProjectHandlerClass.getEnumConstants()[0]; // IMPORT
                }
                gradleBuild.getClass().getMethod("synchronize", newProjectHandlerClass, IProgressMonitor.class)
                        .invoke(gradleBuild, newProjectHandler, monitor);
            } catch (Exception ex) {
                gradleBuild.getClass().getMethod("synchronize", IProgressMonitor.class)
                        .invoke(gradleBuild, monitor);
            }

            // Refresh project with delay
            refreshProjectWithDelay(project, monitor);
        } catch (Exception e) {
            throw new Exception("Buildship Gradle import failed: " + e.getMessage(), e);
        }
    }

    /**
     * Refreshes the project after a short delay, both synchronously and asynchronously,
     * to ensure Gradle background tasks have completed and Eclipse picks up all changes.
     */
    private void refreshProjectWithDelay(IProject project, IProgressMonitor monitor) {
        try {
            Thread.sleep(1000);
            project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        } catch (Exception e) {
            // Ignore refresh errors
        }
        PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
            try {
                Thread.sleep(2000);
                project.refreshLocal(IResource.DEPTH_INFINITE, null);
            } catch (Exception e) {
                // Ignore refresh errors
            }
        });
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
