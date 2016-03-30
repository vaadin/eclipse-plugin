package com.vaadin.integration.eclipse.properties;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.PreferenceUtil;

/**
 * Property page grouping Vaadin Maven project related project properties.
 * 
 * This page is not used directly as a property page but provides mostly the
 * same API so that {@link VaadinProjectPropertyPage} can forward requests to
 * the appropriate "subpage."
 */
public class VaadinMavenProjectPropertyPage implements IVaadinPropertyPage {

    private final Image ICON_INFORMATION_SMALL;

    private IProject project;

    private Composite composite;

    private StaticVaadinVersionComposite versionComposite;

    public VaadinMavenProjectPropertyPage() {
        super();
        ICON_INFORMATION_SMALL = new Image(Display.getDefault(), Display
                .getDefault().getSystemImage(SWT.ICON_INFORMATION)
                .getImageData().scaledTo(16, 16));
    }

    public void performDefaults() {
        // revert to the vaadin version currently in the project
        IProject project = getProject();

        // TODO fill this in
    }

    public boolean performOk() {
        final IProject project = getProject();
        if (project == null) {
            ErrorUtil.logInfo("Store preferences: not a Vaadin project");
            return true;
        }

        IJavaProject jproject = JavaCore.create(project);

        // TODO implement

        try {
            updatePreferences(project);
        } catch (IOException e) {
            ErrorUtil.handleBackgroundException(
                    "Could not save project preferences for " + project, e);
        }

        return true;
    }

    private boolean updatePreferences(IProject project) throws IOException {
        boolean modifiedValues = false;

        PreferenceUtil preferences = PreferenceUtil.get(project);

        // boolean oldUpdateNotificationsEnabled = preferences
        // .isUpdateNotificationEnabled();
        // boolean newUpdateNotificationsEnabled = versionComposite
        // .isUpdateNotificationsEnabled();
        // if (oldUpdateNotificationsEnabled != newUpdateNotificationsEnabled) {
        // preferences
        // .setUpdateNotificationEnabled(newUpdateNotificationsEnabled);
        // modifiedValues = true;
        // }

        // TODO implement

        if (modifiedValues) {
            preferences.persist();
        }


        return modifiedValues;
    }

    /**
     * @see PreferencePage#createContents(Composite)
     */
    public Control createContents(Composite parent) {
        composite = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout(1, false);
        composite.setLayout(layout);

        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        composite.setLayoutData(data);

        versionComposite = new StaticVaadinVersionComposite(composite, SWT.NONE);
        setProject(project);

        performDefaults();

        return composite;
    }

    public void setProject(IProject project) {
        this.project = project;

        if (versionComposite != null) {
            versionComposite.setProject(project);
        }
    }

    public IProject getProject() {
        return project;
    }

    private Shell getShell() {
        return composite.getShell();
    }

    public void dispose() {
        ICON_INFORMATION_SMALL.dispose();
    }

    public Control getControl() {
        return composite;
    }
}