package com.vaadin.plugin.debug;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.debug.core.IJavaExceptionBreakpoint;
import org.eclipse.jdt.debug.core.IJavaThread;
import com.vaadin.plugin.util.VaadinPluginLog;

/**
 * Debug event listener that filters out SilentException breakpoints to prevent the debugger from stopping unnecessarily
 * when starting Vaadin applications. This mimics the behavior of Spring Tool Suite (STS) which automatically ignores
 * these exceptions.
 *
 * @see <a href= "https://github.com/spring-projects/spring-boot/issues/3100">Spring Boot Issue #3100</a>
 */
public class SilentExceptionFilter implements IDebugEventSetListener {

    private static final String SILENT_EXCEPTION_NAME = "org.springframework.boot.devtools.restart.SilentExitExceptionHandler$SilentExitException";
    private static final String SILENT_EXCEPTION_SHORT_NAME = "SilentExitException";

    @Override
    public void handleDebugEvents(DebugEvent[] events) {
        for (DebugEvent event : events) {
            if (event.getKind() == DebugEvent.SUSPEND && event.getDetail() == DebugEvent.BREAKPOINT) {
                Object source = event.getSource();

                if (source instanceof IJavaThread) {
                    IJavaThread thread = (IJavaThread) source;

                    try {
                        IBreakpoint[] breakpoints = thread.getBreakpoints();

                        for (IBreakpoint breakpoint : breakpoints) {
                            if (breakpoint instanceof IJavaExceptionBreakpoint) {
                                IJavaExceptionBreakpoint exceptionBreakpoint = (IJavaExceptionBreakpoint) breakpoint;
                                String typeName = exceptionBreakpoint.getTypeName();

                                if (isSilentException(typeName)) {
                                    thread.resume();
                                    return;
                                }
                            }
                        }
                    } catch (Exception e) {
                        VaadinPluginLog.error("Error handling debug event: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Check if the exception type is a SilentException that should be ignored.
     *
     * @param typeName
     *            The fully qualified name of the exception type
     * @return true if this is a SilentException that should be ignored
     */
    private boolean isSilentException(String typeName) {
        return typeName != null
                && (typeName.equals(SILENT_EXCEPTION_NAME) || typeName.endsWith(SILENT_EXCEPTION_SHORT_NAME));
    }

    /**
     * Register this filter with the debug plugin.
     */
    public void register() {
        DebugPlugin.getDefault().addDebugEventListener(this);
    }

    /**
     * Unregister this filter from the debug plugin.
     */
    public void unregister() {
        DebugPlugin.getDefault().removeDebugEventListener(this);
    }
}
