package com.vaadin.integration.eclipse.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * Property page grouping Vaadin related project properties.
 *
 * Vaadin version selection is here, future subpages may contain more settings.
 */
public class VaadinProjectPropertyPage extends PropertyPage {

    private VaadinIvyProjectPropertyPage ivyPropertyPage = new VaadinIvyProjectPropertyPage();

    public VaadinProjectPropertyPage() {
    }

    @Override
    protected void performDefaults() {
        ivyPropertyPage.performDefaults();
    }

    @Override
    public boolean performOk() {
        return ivyPropertyPage.performOk();
    }

    /**
     * @see PreferencePage#createContents(Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
        return ivyPropertyPage.createContents(parent);
    }

    @Override
    public void setElement(IAdaptable element) {
        super.setElement(element);

        IProject project = null;
        if (getElement() instanceof IJavaProject) {
            project = ((IJavaProject) getElement()).getProject();
        } else if (getElement() instanceof IProject) {
            project = (IProject) getElement();
        }

        ivyPropertyPage.setProject(project);
    }

    @Override
    public void dispose() {
        super.dispose();
        ivyPropertyPage.dispose();
    }
}