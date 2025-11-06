package com.vaadin.plugin;

import org.eclipse.debug.core.DebugPlugin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.vaadin.plugin.debug.SilentExceptionFilter;
import com.vaadin.plugin.launch.ServerLaunchListener;
import com.vaadin.plugin.util.VaadinPluginLog;

/**
 * Bundle activator that starts the REST service when the plug-in is activated and stops it on shutdown.
 */
public class Activator implements BundleActivator {
    private CopilotRestService restService;
    private ServerLaunchListener serverLaunchListener;
    private CopilotDotfileManager dotfileManager;
    private SilentExceptionFilter silentExceptionFilter;
    private TelemetryService telemetryService;

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

            // Initialize telemetry
            telemetryService = TelemetryService.getInstance();
            java.util.Map<String, Object> properties = new java.util.HashMap<>();
            String proKey = System.getProperty("vaadin.prokey", "");
            if (!proKey.isEmpty()) {
                properties.put("ProKey", proKey);
            }
            telemetryService.trackEvent("PluginInitialized", properties);
        } catch (Exception e) {
            VaadinPluginLog.error("Failed to start Vaadin Eclipse Plugin: " + e.getMessage(), e);
            // Clean up any partially initialized resources
            stop(context);
            throw e;
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // Shutdown telemetry
        if (telemetryService != null) {
            telemetryService.shutdown();
            telemetryService = null;
        }

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
