package com.vaadin.plugin;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import java.io.IOException;

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
            restService.start();
        } catch (IOException e) {
            System.err.println("Failed to start Copilot REST service: " + e.getMessage());
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (restService != null) {
            restService.stop();
            restService = null;
        }
        plugin = null;
        super.stop(context);
    }
}
