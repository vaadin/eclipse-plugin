package com.vaadin.plugin;

import org.eclipse.debug.core.DebugPlugin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.vaadin.plugin.launch.ServerLaunchListener;

/**
 * Bundle activator that starts the REST service when the plug-in is activated and stops it on shutdown.
 */
public class Activator implements BundleActivator {
    private CopilotRestService restService;
    private ServerLaunchListener serverLaunchListener;

    @Override
    public void start(BundleContext context) throws Exception {
        restService = new CopilotRestService();
        restService.start();
        System.setProperty("vaadin.copilot.endpoint", restService.getEndpoint());

        // Register the server launch listener
        serverLaunchListener = new ServerLaunchListener();
        DebugPlugin.getDefault().getLaunchManager().addLaunchListener(serverLaunchListener);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // Unregister the server launch listener
        if (serverLaunchListener != null) {
            DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(serverLaunchListener);
            serverLaunchListener = null;
        }

        if (restService != null) {
            restService.stop();
            restService = null;
        }
    }
}
