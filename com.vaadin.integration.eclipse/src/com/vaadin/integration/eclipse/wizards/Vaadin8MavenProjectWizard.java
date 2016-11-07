package com.vaadin.integration.eclipse.wizards;

import java.util.ArrayList;
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
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.wizards.AbstractMavenProjectWizard;
import org.eclipse.m2e.core.ui.internal.wizards.MavenProjectWizardArchetypeParametersPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.PlatformUI;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.preferences.PreferenceConstants;
import com.vaadin.integration.eclipse.util.network.MavenVersionManager;

@SuppressWarnings("restriction")
public class Vaadin8MavenProjectWizard extends VaadinMavenProjectWizard {

    public static final String WIZARD_PAGE_TITLE = "Vaadin 8 Project with Maven";

    @Override
    public void addPages() {
        super.addPages();
        for (IWizardPage page : getPages()) {
            page.setTitle(WIZARD_PAGE_TITLE);
        }
    }

    @Override
    protected List<VaadinArchetype> getAvailableArchetypes() {
        boolean includePrereleases = VaadinPlugin.getInstance()
                .getPreferenceStore()
                .getBoolean(PreferenceConstants.PRERELEASE_ARCHETYPES_ENABLED);
        List<VaadinArchetype> vaadinArchetypes = MavenVersionManager
                .getAvailableArchetypes(includePrereleases, "8.*");
        // TODO remove this hack once Vaadin 8 has been released
        if (vaadinArchetypes.isEmpty() && includePrereleases == false) {
            vaadinArchetypes = MavenVersionManager.getAvailableArchetypes(true,
                    "8.*");
        }
        return vaadinArchetypes;
    }
}
