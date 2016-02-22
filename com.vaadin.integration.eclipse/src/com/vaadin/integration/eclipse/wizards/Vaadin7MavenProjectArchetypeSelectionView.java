package com.vaadin.integration.eclipse.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.vaadin.integration.eclipse.wizards.Vaadin7MavenProjectWizard.VaadinArchetype;

public class Vaadin7MavenProjectArchetypeSelectionView extends Composite {

    private final Group archetypesComposite;
    private final List<Button> vaadinArchetypeButtons;

    public Vaadin7MavenProjectArchetypeSelectionView(
            List<VaadinArchetype> vaadinArchetypes, Composite parent, int style) {
        super(parent, SWT.NONE);

        this.vaadinArchetypeButtons = new ArrayList<Button>(vaadinArchetypes.size());

        archetypesComposite = createContents(parent, vaadinArchetypes);

        // this default selection should be done by the wizard
        selectVaadinArchetype(vaadinArchetypes.get(0));
    }

    private Group createContents(Composite parent, List<VaadinArchetype> vaadinArchetypes) {
        setLayout(new GridLayout(1, false));

        // TODO there should be more standard ways to do selection in Eclipse

        Group main = new Group(this, SWT.NONE);
        main.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        main.setLayout(new GridLayout(1,false));

        for (VaadinArchetype vaadinArch : vaadinArchetypes) {
            Button btnArchetype = new Button(main, SWT.RADIO | SWT.WRAP | SWT.TOP);
            
            btnArchetype.setText(vaadinArch.getTitle());
            btnArchetype.setData(vaadinArch);
            btnArchetype.setLayoutData(new GridData(SWT.DEFAULT,SWT.DEFAULT,true,false,1,1));
            vaadinArchetypeButtons.add(btnArchetype);
            Label descriptionText = new Label(main,SWT.WRAP);
            GridData labelGridData = new GridData(SWT.DEFAULT,SWT.DEFAULT,true,false,1,1);
            labelGridData.horizontalIndent = 70;
            descriptionText.setLayoutData(labelGridData);
            descriptionText.setText(vaadinArch.getDescription());
        }

        return main;
    }

    public VaadinArchetype getVaadinArchetype() {
        for(Button btn : vaadinArchetypeButtons) {
        	if(btn.getSelection()) return (VaadinArchetype)btn.getData();
        }
        return (VaadinArchetype) vaadinArchetypeButtons.get(0).getData();
    }

    public void selectVaadinArchetype(VaadinArchetype archetype) {
        for(Button btn : vaadinArchetypeButtons) {
        	btn.setSelection(archetype == btn.getData());
        }
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

}
