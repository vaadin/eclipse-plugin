package com.vaadin.plugin;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.NamespaceException;
import javax.servlet.ServletException;

/**
 * Bundle activator that starts the Copilot REST service on plugin startup.
 */
public class Activator implements BundleActivator {
    private CopilotRestService restService;

    @Override
    public void start(BundleContext context) throws Exception {
        restService = new CopilotRestService();
        try {
            restService.start(context);
            System.setProperty("vaadin.copilot.endpoint", restService.getEndpoint());
        } catch (ServletException | NamespaceException e) {
            System.err.println("Failed to register Copilot servlet");
            e.printStackTrace();
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (restService != null) {
            restService.stop(context);
            restService = null;
        }
    }
}
