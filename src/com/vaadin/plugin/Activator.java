package com.vaadin.plugin;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.NamespaceException;
import javax.servlet.ServletException;

/**
 * Bundle activator that starts the Copilot REST service on plugin startup.
 */
public class Activator extends AbstractUIPlugin {
    /** The plug-in ID as defined in MANIFEST.MF. */
    public static final String PLUGIN_ID = "vaadin-eclipse-plugin";

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
        plugin = null;
        super.stop(context);
    }
}
