package com.vaadin.plugin;

import org.eclipse.ui.IStartup;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkUtil;

/**
 * Starts the REST service when the workbench starts. The service is registered
 * using the OSGi {@code HttpService} and stopped when the bundle shuts down.
 */
public class EarlyStartup implements IStartup {
    private CopilotRestService restService;

    @Override
    public void earlyStartup() {
        BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
        restService = new CopilotRestService();
        try {
            restService.start(context);
            System.setProperty("vaadin.copilot.endpoint", restService.getEndpoint());
        } catch (Exception e) {
            System.err.println("Failed to register Copilot servlet");
            e.printStackTrace();
        }

        context.addBundleListener(new BundleListener() {
            @Override
            public void bundleChanged(BundleEvent event) {
                if (event.getBundle().equals(FrameworkUtil.getBundle(getClass())) &&
                        event.getType() == BundleEvent.STOPPING) {
                    restService.stop(context);
                }
            }
        });
    }
}
