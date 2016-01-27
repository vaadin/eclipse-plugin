package com.vaadin.integration.eclipse.wizards;

import java.util.List;
import java.util.Properties;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.model.Model;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.wizards.AbstactCreateMavenProjectJob;
import org.eclipse.m2e.core.ui.internal.wizards.AbstractMavenProjectWizard;
import org.eclipse.m2e.core.ui.internal.wizards.MavenProjectWizardArchetypeParametersPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;

@SuppressWarnings("restriction")
public class Vaadin7MavenProjectWizard extends AbstractMavenProjectWizard
implements INewWizard {

    /** The wizard page for gathering archetype project information. */
    protected MavenProjectWizardArchetypeParametersPage parametersPage;

    private VaadinArchetype vaadinArchetypes[] = new VaadinArchetype[7];
    private VaadinArchetype selectedArchetype = null;

    private Vaadin7MavenProjectArchetypeSelectionPage vaadinArchetypeSelectionPage;

    /**
     * Default constructor. Sets the title and image of the wizard.
     */
    public Vaadin7MavenProjectWizard() {
        super();
        setWindowTitle("New Vaadin 7 Maven Project");

        // TODO should have own icon
        setDefaultPageImageDescriptor(MavenImages.WIZ_NEW_PROJECT);
        setNeedsProgressMonitor(true);

        // TODO: should be populated automatically
        vaadinArchetypes[0] = new VaadinArchetype();
        vaadinArchetypes[0].setTitle("Clean Application");
        vaadinArchetypes[0]
                .setDescription("A clean, single module Vaadin application project. \n\nThis is suitable for small applications");
        vaadinArchetypes[0].setArchtype("vaadin-archetype-application",
                "com.vaadin", "7.4.2");

        // TODO: other archetypes need to be filled
        vaadinArchetypes[1] = new VaadinArchetype();
        vaadinArchetypes[1].setTitle("Clean Application, Multi Module");
        vaadinArchetypes[1]
                .setDescription("A clean, multi module Vaadin application project. \n\nThis is suitable for more complex applications");

        vaadinArchetypes[2] = new VaadinArchetype();
        vaadinArchetypes[2].setTitle("CRUD Example");
        vaadinArchetypes[2]
                .setDescription("A multi module example CRUD (create/read/update/delete) Vaadin application containing a login screen, basic access control examples and more\n\nProvides a good example on how you can structure a Vaadin application");

        vaadinArchetypes[3] = new VaadinArchetype();
        vaadinArchetypes[3].setTitle("JavaEE CRUD Example");
        vaadinArchetypes[3]
                .setDescription("A JavaEE (CDI, EJB, JPA) based multi module example CRUD (create/read/update/delete) Vaadin application containing a login screen, basic access control examples and more.\n\nProvides a good example on how you can structure a Vaadin JavaEE application");

        vaadinArchetypes[4] = new VaadinArchetype();
        vaadinArchetypes[4].setTitle("Spring CRUD Example");
        vaadinArchetypes[4]
                .setDescription("A Spring based multi module example CRUD (create/read/update/delete) Vaadin application containing a login screen, basic access control examples and more.\n\nProvides a good example on how you can structure a Vaadin Spring application");

        vaadinArchetypes[5] = new VaadinArchetype();
        vaadinArchetypes[5].setTitle("Liferay 6 Portlet");
        vaadinArchetypes[5]
                .setDescription("Creates a clean Liferay 6 portlet\n\nRequires separate installation of the Liferay Maven Plugin(?)");

        vaadinArchetypes[6] = new VaadinArchetype();
        vaadinArchetypes[6].setTitle("Add-on widget");
        vaadinArchetypes[6]
                .setDescription("A multi module widget add-on project which provides a good starting point for creating a re-usable Vaadin component including a demo application.\n\nPackages the add-on in a format ready to be deployed to Vaadin Directory");
    }

    @Override
    public void addPages() {
        /*
         * Vaadin Archetype selection page.
         */
        vaadinArchetypeSelectionPage = new Vaadin7MavenProjectArchetypeSelectionPage(
                this, vaadinArchetypes);

        /*
         * Archetype parameters page. The only needed page for Vaadin Archetype.
         */
        parametersPage = new MavenProjectWizardArchetypeParametersPage(
                importConfiguration);

        addPage(vaadinArchetypeSelectionPage);
        addPage(parametersPage);
    }

    // Replaced by setVaadinArchetype()
    // @Override
    // public void createPageControls(Composite pageContainer) {
    // super.createPageControls(pageContainer);
    // parametersPage.setUsed(true);
    // parametersPage.setArchetype(getArchetype());
    //
    // getContainer().updateButtons();
    // }

    /** Returns the model. */
    public Model getModel() {
        return parametersPage.getModel();
    }

    /**
     * To perform the actual project creation, an operation is created and run
     * using this wizard as execution context. That way, messages about the
     * progress of the project creation are displayed inside the wizard.
     */
    @Override
    public boolean performFinish() {
        // First of all, we extract all the information from the wizard pages.
        // Note that this should not be done inside the operation we will run
        // since many of the wizard pages' methods can only be invoked from
        // within the SWT event dispatcher thread. However, the operation spawns
        // a new separate thread to perform the actual work, i.e. accessing SWT
        // elements from within that thread would lead to an exception.

        final Model model = getModel();
        final String projectName = importConfiguration.getProjectName(model);
        IStatus nameStatus = importConfiguration.validateProjectName(model);
        if (!nameStatus.isOK()) {
            MessageDialog.openError(getShell(),
                    NLS.bind(Messages.wizardProjectJobFailed, projectName),
                    nameStatus.getMessage());
            return false;
        }

        IWorkspace workspace = ResourcesPlugin.getWorkspace();

        // When using the default workspace location for a project, we have to
        // pass null instead of the actual location.
        final IPath location = null;
        final IWorkspaceRoot root = workspace.getRoot();
        final IProject project = importConfiguration.getProject(root, model);

        boolean pomExists = root.getLocation().append(project.getName())
                .append(IMavenConstants.POM_FILE_NAME).toFile().exists();
        if (pomExists) {
            MessageDialog.openError(getShell(),
                    NLS.bind(Messages.wizardProjectJobFailed, projectName),
                    Messages.wizardProjectErrorPomAlreadyExists);
            return false;
        }

        final Job job;

        final Archetype archetype = selectedArchetype.getArchetype();

        final String groupId = model.getGroupId();
        final String artifactId = model.getArtifactId();
        final String version = model.getVersion();
        final String javaPackage = parametersPage.getJavaPackage();
        final Properties properties = parametersPage.getProperties();

        job = new AbstactCreateMavenProjectJob(NLS.bind(
                Messages.wizardProjectJobCreating, archetype.getArtifactId()),
                workingSets) {
            @Override
            protected List<IProject> doCreateMavenProjects(
                    IProgressMonitor monitor) throws CoreException {
                List<IProject> projects = MavenPlugin
                        .getProjectConfigurationManager()
                        .createArchetypeProjects(location,
                                archetype, //
                                groupId, artifactId, version, javaPackage,
                                properties, importConfiguration, monitor);
                return projects;
            }
        };

        job.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                final IStatus result = event.getResult();
                if (!result.isOK()) {
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageDialog.openError(getShell(), //
                                    NLS.bind(Messages.wizardProjectJobFailed,
                                            projectName), result.getMessage());
                        }
                    });
                }
            }
        });

        job.setRule(MavenPlugin.getProjectConfigurationManager().getRule());
        job.schedule();

        return true;
    }

    public void setVaadinArchetype(VaadinArchetype vaadinArchetype) {
        selectedArchetype = vaadinArchetype;
        parametersPage.setArchetype(selectedArchetype.getArchetype());
        parametersPage.setUsed(true);

        getContainer().updateButtons();
    }

    public class VaadinArchetype {

        private String title;
        private String description;
        private Archetype archtype;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Archetype getArchetype() {
            return archtype;
        }

        public void setArchtype(Archetype archtype) {
            this.archtype = archtype;
        }

        public void setArchtype(String artifactId, String groupId,
                String version) {
            archtype = new Archetype();
            archtype.setArtifactId(artifactId);
            archtype.setGroupId(groupId);
            archtype.setVersion(version);
        }
    }
}
