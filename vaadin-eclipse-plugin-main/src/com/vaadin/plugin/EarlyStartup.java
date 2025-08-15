package com.vaadin.plugin;

import org.eclipse.ui.IStartup;

/**
 * Ensures the plug-in is activated when the workbench starts so the {@link Activator} can launch the embedded REST
 * service.
 */
public class EarlyStartup implements IStartup {

    @Override
    public void earlyStartup() {
        // Trigger plug-in activation so the BundleActivator runs
    }
}
