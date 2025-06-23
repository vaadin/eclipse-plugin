package com.vaadin.plugin;

import static org.junit.Assert.assertTrue;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IStatusLineManager;
import org.junit.Test;

@SuppressWarnings("restriction")
public class IconPresenceTest {

    @Test
    public void testStatusBarIconRendered() throws Exception {
        final boolean[] found = new boolean[1];
        Display.getDefault().syncExec(() -> {
            WorkbenchWindow win = (WorkbenchWindow) PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow();
            IStatusLineManager mgr = win.getStatusLineManager();
            for (IContributionItem item : mgr.getItems()) {
                if ("vaadin-eclipse-plugin.toolbars.statusLogo".equals(item.getId())) {
                    found[0] = true;
                    break;
                }
            }
        });
        assertTrue("Expected Vaadin status-bar icon to be rendered", found[0]);
    }
}