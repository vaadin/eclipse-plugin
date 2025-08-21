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

        // Step 3: Import as Eclipse project
        subMonitor.subTask("Importing project...");
        IProject project = importProject(projectPath, model.getProjectName(), subMonitor.split(20));

        // Step 4: Open README
        subMonitor.subTask("Opening README...");
        openReadme(project, subMonitor.split(10));

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

    private IProject importProject(Path projectPath, String projectName, IProgressMonitor monitor)
            throws CoreException {
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
