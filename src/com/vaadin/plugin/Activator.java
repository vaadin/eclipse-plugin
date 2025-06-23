package com.vaadin.plugin;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {
    public static final String PLUGIN_ID = "vaadin-eclipse-plugin";
    private static Activator plugin;
    private RestServer server;

    public Activator() {}

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        server = new RestServer();
        server.start();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (server != null) {
            server.stop();
            server = null;
        }
        plugin = null;
        super.stop(context);
    }

    public static Activator getDefault() {
        return plugin;
    }

    public int getPort() {
        return server != null ? server.getPort() : -1;
    }
}
