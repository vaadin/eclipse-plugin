package com.vaadin.integration.eclipse.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.vaadin.integration.eclipse.wizards.Vaadin7MavenProjectWizard.VaadinArchetype;

public class Vaadin7MavenProjectArchetypeSelectionPage extends WizardPage {

    private Vaadin7MavenProjectWizard vaadin7MavenProjectWizard;
    private VaadinArchetype[] vaadinArchetypes;

    protected Vaadin7MavenProjectArchetypeSelectionPage(
            Vaadin7MavenProjectWizard vaadin7MavenProjectWizard,
            VaadinArchetype[] vaadinArchetypes) {
        super("Select Project Type");

        this.vaadin7MavenProjectWizard = vaadin7MavenProjectWizard;
        this.vaadinArchetypes = vaadinArchetypes;
    }

    public void setVaadinArchetype(VaadinArchetype vaadinArchetype) {
        vaadin7MavenProjectWizard.setVaadinArchetype(vaadinArchetype);
    }

    public void createControl(Composite parent) {
        setTitle("Create New Vaadin 7 Maven Project");
        setDescription("Select a project template from the list below");
        setControl(new Vaadin7MavenProjectArchetypeSelectionView(this,
                vaadinArchetypes, parent, SWT.NONE));
    }
}
