package com.vaadin.integration.eclipse.wizards;

import java.util.List;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.vaadin.integration.eclipse.wizards.Vaadin7MavenProjectWizard.VaadinArchetype;

public class Vaadin7MavenProjectArchetypeSelectionPage extends WizardPage {

    private List<VaadinArchetype> vaadinArchetypes;
    private VaadinArchetype selectedArchetype;

    protected Vaadin7MavenProjectArchetypeSelectionPage(
            List<VaadinArchetype> vaadinArchetypes) {
        super("Select Project Type");

        this.vaadinArchetypes = vaadinArchetypes;
        this.selectedArchetype = vaadinArchetypes.get(0);
    }

    public void setVaadinArchetype(VaadinArchetype vaadinArchetype) {
        selectedArchetype = vaadinArchetype;

        getContainer().showPage(getNextPage());
    }

    public void createControl(Composite parent) {
        setTitle("Create New Vaadin 7 Maven Project");
        setDescription("Select a project template from the list below");
        setControl(new Vaadin7MavenProjectArchetypeSelectionView(this,
                vaadinArchetypes, parent, SWT.NONE));
    }

    @Override
    public boolean isPageComplete() {
        return selectedArchetype != null && super.isPageComplete();
    }

    public VaadinArchetype getVaadinArchetype() {
        return selectedArchetype;
    }

}
