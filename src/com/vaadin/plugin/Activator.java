package com.vaadin.plugin;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.NamespaceException;
import javax.servlet.ServletException;

public class Activator extends Plugin {
    private static Activator plugin;
    private CopilotRestService restService;

    public Activator() {
        plugin = this;
    }

    public static Activator getDefault() {
        return plugin;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        restService = new CopilotRestService();
        try {
            restService.start(context);
            System.setProperty("vaadin.copilot.endpoint", restService.getEndpoint());
        } catch (ServletException | NamespaceException e) {
            System.err.println("Failed to register Copilot servlet: " + e.getMessage());
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (restService != null) {
            restService.stop(context);
            restService = null;
        }
        plugin = null;
        super.stop(context);
    }
}
