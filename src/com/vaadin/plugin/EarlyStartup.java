package com.vaadin.plugin;

import org.eclipse.ui.IStartup;

/**
 * Triggers plug-in activation when the Eclipse workbench starts so that the
 * {@link Activator} can register the REST service.
 */
public class EarlyStartup implements IStartup {
    @Override
    public void earlyStartup() {
        // Intentionally left empty - the presence of this class ensures that
        // the bundle is started during Eclipse startup.
    }
}
