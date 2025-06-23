package com.vaadin.plugin;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class VaadinProjectPropertyTester extends PropertyTester {

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof IProject)) {
            return false;
        }
        IProject project = (IProject) receiver;
        try {
            if (!project.isOpen() || !project.hasNature(JavaCore.NATURE_ID)) {
                return false;
            }
            IJavaProject javaProject = JavaCore.create(project);
            for (IClasspathEntry entry : javaProject.getRawClasspath()) {
                String segment = entry.getPath().lastSegment();
                if (segment != null && segment.toLowerCase().contains("vaadin")) {
                    return true;
                }
            }
        } catch (Exception e) {
        }
        return false;
    }
}