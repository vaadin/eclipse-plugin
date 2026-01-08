package com.vaadin.plugin.navigation;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.vaadin.plugin.util.VaadinPluginLog;

public class StyleSheetHyperlink implements IHyperlink {

    private final IRegion region;
    private final IFile targetFile;
    private final String filePath;

    public StyleSheetHyperlink(IRegion region, IFile targetFile, String filePath) {
        this.region = region;
        this.targetFile = targetFile;
        this.filePath = filePath;
    }

    @Override
    public IRegion getHyperlinkRegion() {
        return region;
    }

    @Override
    public String getHyperlinkText() {
        return "Open '" + targetFile.getName() + "'";
    }

    @Override
    public String getTypeLabel() {
        return null;
    }

    @Override
    public void open() {
        try {
            VaadinPluginLog.debug("Opening file: " + targetFile.getFullPath());
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            IDE.openEditor(page, targetFile, true);
        } catch (Exception e) {
            VaadinPluginLog.error("Failed to open file: " + targetFile.getFullPath(), e);
        }
    }
}
