package com.vaadin.plugin.launch;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;

import com.vaadin.plugin.util.VaadinPluginLog;

/**
 * Listener that hooks into server launch events to trigger a build for Vaadin projects. The Vaadin builder will
 * automatically generate necessary files if Vaadin dependencies are detected.
 */
public class ServerLaunchListener implements ILaunchListener {

    @Override
    public void launchAdded(ILaunch launch) {
        try {
            ILaunchConfiguration config = launch.getLaunchConfiguration();
            if (config == null) {
                return;
            }

            // Check if this is a server launch
            IServer server = ServerUtil.getServer(config);
            if (server == null) {
                return;
            }

            // Get the modules being deployed
            IModule[] modules = server.getModules();
            if (modules == null || modules.length == 0) {
                return;
            }

            for (IModule module : modules) {
                IProject project = module.getProject();
                if (project != null) {
                    // Trigger a build to ensure hello.txt is generated if this is a Vaadin project
                    // The builder will check for Vaadin dependencies internally
                    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
                }
            }

        } catch (Exception e) {
            // Log but don't fail the launch
            VaadinPluginLog.error("Failed to trigger build: " + e.getMessage());
        }
    }

    @Override
    public void launchRemoved(ILaunch launch) {
        // Nothing to clean up
    }

    @Override
    public void launchChanged(ILaunch launch) {
        // Not needed for this implementation
    }
}
