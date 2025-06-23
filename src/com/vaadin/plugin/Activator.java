package com.vaadin.plugin;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Bundle activator that starts the REST service when the plug-in is
 * activated and stops it on shutdown.
 */
public class Activator implements BundleActivator {
    private CopilotRestService restService;

    @Override
    public void start(BundleContext context) throws Exception {
        restService = new CopilotRestService();
        restService.start(context);
        System.setProperty("vaadin.copilot.endpoint", restService.getEndpoint());
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (restService != null) {
            restService.stop(context);
            restService = null;
        }
    }
}
