package com.vaadin.plugin.util;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Utility class for logging messages in the Vaadin Eclipse Plugin.
 * Provides methods for logging at different levels (info, warning, error, debug).
 */
public class VaadinPluginLog {
    
    private static final String PLUGIN_ID = "vaadin-eclipse-plugin";
    private static ILog log;
    
    static {
        // Try to get the bundle using the correct plugin ID
        Bundle bundle = Platform.getBundle(PLUGIN_ID);
        if (bundle == null) {
            // Fallback: try to get the bundle from a class in this plugin
            bundle = FrameworkUtil.getBundle(VaadinPluginLog.class);
        }
        if (bundle != null) {
            log = Platform.getLog(bundle);
        }
    }
    
    /**
     * Logs an informational message.
     * 
     * @param message the message to log
     */
    public static void info(String message) {
        if (log != null) {
            log.log(new Status(IStatus.INFO, PLUGIN_ID, message));
        }
    }
    
    /**
     * Logs a warning message.
     * 
     * @param message the message to log
     */
    public static void warning(String message) {
        if (log != null) {
            log.log(new Status(IStatus.WARNING, PLUGIN_ID, message));
        }
    }
    
    /**
     * Logs a warning message with an exception.
     * 
     * @param message the message to log
     * @param exception the exception to log
     */
    public static void warning(String message, Throwable exception) {
        if (log != null) {
            log.log(new Status(IStatus.WARNING, PLUGIN_ID, message, exception));
        }
    }
    
    /**
     * Logs an error message.
     * 
     * @param message the message to log
     */
    public static void error(String message) {
        if (log != null) {
            log.log(new Status(IStatus.ERROR, PLUGIN_ID, message));
        }
    }
    
    /**
     * Logs an error message with an exception.
     * 
     * @param message the message to log
     * @param exception the exception to log
     */
    public static void error(String message, Throwable exception) {
        if (log != null) {
            log.log(new Status(IStatus.ERROR, PLUGIN_ID, message, exception));
        }
    }
    
    /**
     * Logs a debug message. Debug messages are always logged at INFO level
     * to help diagnose issues without requiring Eclipse to be in debug mode.
     * 
     * @param message the message to log
     */
    public static void debug(String message) {
        if (log != null) {
            log.log(new Status(IStatus.INFO, PLUGIN_ID, "[DEBUG] " + message));
        }
    }
}