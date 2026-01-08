package com.vaadin.plugin.navigation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.texteditor.ITextEditor;

import com.vaadin.plugin.util.VaadinPluginLog;

public class StyleSheetHyperlinkDetector extends AbstractHyperlinkDetector {

    private static final Pattern STYLESHEET_PATTERN = Pattern.compile("@StyleSheet\\s*\\(\\s*\"([^\"]+)\"\\s*\\)");

    @Override
    public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
        if (textViewer == null || region == null) {
            return null;
        }

        IDocument document = textViewer.getDocument();
        if (document == null) {
            return null;
        }

        try {
            int offset = region.getOffset();
            int lineNumber = document.getLineOfOffset(offset);
            int lineOffset = document.getLineOffset(lineNumber);
            int lineLength = document.getLineLength(lineNumber);
            String lineText = document.get(lineOffset, lineLength);
            int offsetInLine = offset - lineOffset;

            VaadinPluginLog.debug("Detecting hyperlink at offset: " + offset);

            Matcher matcher = STYLESHEET_PATTERN.matcher(lineText);

            while (matcher.find()) {
                String filePath = matcher.group(1);
                int quoteStart = matcher.start(1) - 1;
                int filePathStart = quoteStart + 1;
                int filePathEnd = filePathStart + filePath.length();

                if (offsetInLine >= filePathStart && offsetInLine <= filePathEnd) {
                    VaadinPluginLog.debug("Found @StyleSheet annotation with path: " + filePath);

                    IFile resolvedFile = resolveFile(textViewer, filePath);
                    if (resolvedFile != null) {
                        IRegion hyperlinkRegion = new Region(lineOffset + filePathStart, filePath.length());
                        return new IHyperlink[] { new StyleSheetHyperlink(hyperlinkRegion, resolvedFile, filePath) };
                    }
                }
            }

        } catch (BadLocationException e) {
            VaadinPluginLog.error("Error detecting StyleSheet hyperlink", e);
        }

        return null;
    }

    private IFile resolveFile(ITextViewer textViewer, String filePath) {
        ITextEditor editor = getAdapter(ITextEditor.class);
        if (editor == null) {
            return null;
        }

        IFile editorFile = editor.getEditorInput().getAdapter(IFile.class);
        if (editorFile == null) {
            return null;
        }

        IJavaProject javaProject = JavaCore.create(editorFile.getProject());
        if (javaProject == null || !javaProject.exists()) {
            return null;
        }

        return StyleSheetPathResolver.resolveStyleSheetPath(javaProject, filePath);
    }
}
