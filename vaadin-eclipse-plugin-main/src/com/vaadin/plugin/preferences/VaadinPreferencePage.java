package com.vaadin.plugin.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.vaadin.plugin.Activator;

public class VaadinPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String PREF_ENABLE_TELEMETRY = "com.vaadin.plugin.telemetry.enabled";

    public VaadinPreferencePage() {
        super(GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription("Vaadin Eclipse Plugin Preferences");
    }

    @Override
    public void createFieldEditors() {
        addField(new BooleanFieldEditor(PREF_ENABLE_TELEMETRY,
                "Enable usage statistics collection\n\n"
                        + "Help us improve the Vaadin Eclipse Plugin by sending anonymous usage statistics.\n"
                        + "We collect information about plugin features you use to understand how to make\n"
                        + "the plugin better. No personal information or project code is collected.\n\n"
                        + "You can change this setting at any time.",
                getFieldEditorParent()));
    }

    @Override
    public void init(IWorkbench workbench) {
    }
}
