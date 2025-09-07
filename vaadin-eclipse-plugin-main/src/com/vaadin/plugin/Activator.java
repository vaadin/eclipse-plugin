package com.vaadin.plugin;

import org.eclipse.debug.core.DebugPlugin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.vaadin.plugin.debug.SilentExceptionFilter;
import com.vaadin.plugin.launch.ServerLaunchListener;

/**
 * Bundle activator that starts the REST service when the plug-in is activated and stops it on shutdown.
 */
public class Activator implements BundleActivator {
    private CopilotRestService restService;
    private ServerLaunchListener serverLaunchListener;
    private CopilotDotfileManager dotfileManager;
    private SilentExceptionFilter silentExceptionFilter;

    @Override
    public void start(BundleContext context) throws Exception {
        try {
            restService = new CopilotRestService();
            restService.start();
            System.setProperty("vaadin.copilot.endpoint", restService.getEndpoint());

            // Register the server launch listener
            serverLaunchListener = new ServerLaunchListener();
            DebugPlugin.getDefault().getLaunchManager().addLaunchListener(serverLaunchListener);

            // Register the silent exception filter
            silentExceptionFilter = new SilentExceptionFilter();
            silentExceptionFilter.register();

            // Initialize dotfile manager
            dotfileManager = CopilotDotfileManager.getInstance();
            dotfileManager.initialize();
            // Update all dotfiles with the current endpoint
            dotfileManager.updateAllDotfiles();
        } catch (Exception e) {
            System.err.println("Failed to start Vaadin Eclipse Plugin: " + e.getMessage());
            e.printStackTrace();
            // Clean up any partially initialized resources
            stop(context);
            throw e;
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // Cleanup dotfile manager
        if (dotfileManager != null) {
            dotfileManager.shutdown();
            dotfileManager = null;
        }

        // Unregister the silent exception filter
        if (silentExceptionFilter != null) {
            silentExceptionFilter.unregister();
            silentExceptionFilter = null;
        }

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
