package com.vaadin.integration.eclipse.wizards;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.vaadin.integration.eclipse.wizards.Vaadin7MavenProjectWizard.VaadinArchetype;

public class Vaadin7MavenProjectArchetypeSelectionView extends Composite {

    private final List<VaadinArchetype> vaadinArchetypes;
    private final Composite archetypesComposite;

    private VaadinArchetype selectedArchetype;

    public Vaadin7MavenProjectArchetypeSelectionView(
            List<VaadinArchetype> vaadinArchetypes, Composite parent, int style) {
        super(parent, SWT.NONE);

        this.vaadinArchetypes = vaadinArchetypes;

        archetypesComposite = createContents(parent);

        // this default selection should be done by the wizard
        selectVaadinArchetype(vaadinArchetypes.get(0));
    }

    private Composite createContents(Composite parent) {
        setLayout(new GridLayout(1, false));

        // TODO there should be more standard ways to do selection in Eclipse

        ScrolledComposite scrolledComposite = new ScrolledComposite(this,
                SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        Composite main = new Composite(scrolledComposite, SWT.NONE);
        main.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        main.setLayout(new GridLayout(1, false));

        scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
                true, 1, 1));
        scrolledComposite.setSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);
        scrolledComposite.setContent(main);

        addMouseListener(new MouseClickHandler(this, null));

        for (VaadinArchetype vaadinArch : vaadinArchetypes) {
            MouseClickHandler mouseClickHandler = new MouseClickHandler(this,
                    vaadinArch);

            Composite composite = new Composite(main, SWT.NONE);
            composite.setData(vaadinArch);
            composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
                    false, 1, 1));
            GridLayout glComposite = new GridLayout(1, false);
            glComposite.marginLeft = 32;
            composite.setLayout(glComposite);
            composite.addMouseListener(mouseClickHandler);

            Label lblTitle = new Label(composite, SWT.NONE);
            lblTitle.setText(vaadinArch.getTitle());
            lblTitle.addMouseListener(mouseClickHandler);

            Label lblDescription = new Label(composite, SWT.WRAP);
            GridData lblDescriptionData = new GridData(SWT.FILL, SWT.CENTER,
                    true, false, 1, 1);
            lblDescriptionData.widthHint = 300;
            lblDescription.setLayoutData(lblDescriptionData);
            lblDescription.setText(vaadinArch.getDescription());
            lblDescription.addMouseListener(mouseClickHandler);
        }

        return main;
    }

    public VaadinArchetype getVaadinArchetype() {
        return selectedArchetype;
    }

    public void selectVaadinArchetype(VaadinArchetype archetype) {
        selectedArchetype = archetype;

        updateHighlight(archetype);
    }

    private void updateHighlight(VaadinArchetype archetype) {
        Color unselectedBackgroundColor = archetypesComposite.getDisplay()
                .getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        Color selectedBackgroundColor = archetypesComposite.getDisplay()
                .getSystemColor(SWT.COLOR_LIST_SELECTION);
        Color unselectedTextColor = archetypesComposite.getDisplay()
                .getSystemColor(SWT.COLOR_LIST_FOREGROUND);
        Color selectedTextColor = archetypesComposite.getDisplay()
                .getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);

        // update highlight
        for (Control archetypeComposite : archetypesComposite.getChildren()) {
            if (archetypeComposite instanceof Composite
                    && archetypeComposite.getData() == archetype) {
                updateColors((Composite) archetypeComposite,
                        selectedBackgroundColor,
                        selectedTextColor);
            } else {
                updateColors((Composite) archetypeComposite,
                        unselectedBackgroundColor,
                        unselectedTextColor);
            }
        }
    }

    private void updateColors(Composite archetypeComposite,
            Color backgroundColor, Color textColor) {
        archetypeComposite.setBackground(backgroundColor);
        for (Control child : archetypeComposite.getChildren()) {
            child.setForeground(textColor);
        }
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

    private static class MouseClickHandler extends MouseAdapter {

        private static VaadinArchetype downItem = null;

        private Vaadin7MavenProjectArchetypeSelectionView view;
        private VaadinArchetype currentItem;

        public MouseClickHandler(
                Vaadin7MavenProjectArchetypeSelectionView view,
                VaadinArchetype currentItem) {
            this.view = view;
            this.currentItem = currentItem;
        }

        @Override
        public void mouseDown(MouseEvent e) {
            downItem = currentItem;
        }

        @Override
        public void mouseUp(MouseEvent e) {
            if (downItem == currentItem && currentItem != null) {
                view.selectVaadinArchetype(currentItem);
            } else {
                downItem = null;
            }
        }
    }
}
