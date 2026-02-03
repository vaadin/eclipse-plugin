package com.vaadin.plugin.navigation;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;

import com.vaadin.plugin.util.VaadinPluginLog;

public class StyleSheetPathResolver {

    private static final String[] RESOURCE_LOCATIONS = { "", "src/main/webapp", "src/main/resources/META-INF/resources",
            "src/main/resources/static", "src/main/resources/public", "src/main/resources/resources" };

    public static IFile resolveStyleSheetPath(IJavaProject javaProject, String filePath) {
        if (javaProject == null || filePath == null || filePath.isEmpty()) {
            return null;
        }

        IProject project = javaProject.getProject();
        String normalizedPath = normalizeFilePath(filePath);

        VaadinPluginLog.debug("Resolving StyleSheet path: " + filePath);

        for (String location : RESOURCE_LOCATIONS) {
            String candidatePath = location.isEmpty() ? normalizedPath : location + "/" + normalizedPath;

            IFile candidate = project.getFile(new Path(candidatePath));
            if (candidate.exists()) {
                VaadinPluginLog.debug("StyleSheet resolved: " + filePath + " -> " + candidatePath);
                return candidate;
            }
        }

        VaadinPluginLog.debug("StyleSheet not found: " + filePath);
        return null;
    }

    private static String normalizeFilePath(String filePath) {
        if (filePath.startsWith("./")) {
            return filePath.substring(2);
        }
        return filePath;
    }
}
