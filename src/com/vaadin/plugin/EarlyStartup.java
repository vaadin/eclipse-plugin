package com.vaadin.plugin;

import org.eclipse.ui.IStartup;


/**
 * Starts the REST service when the workbench starts. The service is registered
 * using the OSGi {@code HttpService} and stopped when the bundle shuts down.
 */
public class EarlyStartup implements IStartup {

    @Override
    public void earlyStartup() {
        // Trigger plug-in activation so the BundleActivator runs
    }
}
