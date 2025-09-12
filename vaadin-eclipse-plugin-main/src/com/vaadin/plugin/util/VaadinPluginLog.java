package com.vaadin.plugin.util;

import org.eclipse.core.runtime.ILog;

/**
 * Utility class for logging messages in the Vaadin Eclipse Plugin.
 * Uses the simplified ILog.get() approach available since Eclipse 2021-03.
 * Each call automatically uses the logger for the calling class's bundle.
 */
public class VaadinPluginLog {
    
    /**
     * Logs an informational message.
     * 
     * @param message the message to log
     */
    public static void info(String message) {
        ILog.get().info(message);
    }
    
    /**
     * Logs a warning message.
     * 
     * @param message the message to log
     */
    public static void warning(String message) {
        ILog.get().warn(message);
    }
    
    /**
     * Logs a warning message with an exception.
     * 
     * @param message the message to log
     * @param exception the exception to log
     */
    public static void warning(String message, Throwable exception) {
        ILog.get().warn(message, exception);
    }
    
    /**
     * Logs an error message.
     * 
     * @param message the message to log
     */
    public static void error(String message) {
        ILog.get().error(message);
    }
    
    /**
     * Logs an error message with an exception.
     * 
     * @param message the message to log
     * @param exception the exception to log
     */
    public static void error(String message, Throwable exception) {
        ILog.get().error(message, exception);
    }
    
    /**
     * Logs a debug message. Debug messages are always logged at INFO level
     * to help diagnose issues without requiring Eclipse to be in debug mode.
     * 
     * @param message the message to log
     */
    public static void debug(String message) {
        ILog.get().info("[DEBUG] " + message);
    }
}