package com.vaadin.plugin.launch;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleFile;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.ModuleDelegate;

/**
 * Adapter that adds additional resources to a web module during deployment.
 */
public class VaadinModuleArtifactAdapter extends ModuleDelegate {

    private static final String HELLO_FILE_NAME = "hello.txt";
    private IModule module;
    private IProject project;

    public VaadinModuleArtifactAdapter(IModule module) {
        this.module = module;
        this.project = module.getProject();
    }

    @Override
    public IStatus validate() {
        return Status.OK_STATUS;
    }

    @Override
    public IModuleResource[] members() throws CoreException {
        // Get the original module resources
        ModuleDelegate originalDelegate = (ModuleDelegate) module.loadAdapter(ModuleDelegate.class, null);
        IModuleResource[] originalResources = originalDelegate != null
                ? originalDelegate.members()
                : new IModuleResource[0];

        // Add our custom file
        IModuleResource[] newResources = new IModuleResource[originalResources.length + 1];
        System.arraycopy(originalResources, 0, newResources, 0, originalResources.length);

        // Create the hello.txt file resource
        newResources[originalResources.length] = createHelloFileResource();

        return newResources;
    }

    private IModuleResource createHelloFileResource() {
        String projectPath = project.getLocation().toOSString();
        byte[] content = projectPath.getBytes();

        // Place the file in WEB-INF/classes so it's available as a classpath resource
        IPath classesPath = new Path("WEB-INF").append("classes");
        return new VirtualModuleFile(HELLO_FILE_NAME, classesPath, content);
    }

    /**
     * A virtual file that exists only during deployment.
     */
    private static class VirtualModuleFile implements IModuleFile {
        private final String name;
        private final IPath path;
        private final byte[] content;
        private final long timestamp;

        public VirtualModuleFile(String name, IPath path, byte[] content) {
            this.name = name;
            this.path = path;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public IPath getModuleRelativePath() {
            return path;
        }

        @Override
        public Object getAdapter(Class adapter) {
            if (adapter == InputStream.class) {
                return new ByteArrayInputStream(content);
            }
            if (adapter == IFile.class) {
                // Return null as this is a virtual file
                return null;
            }
            return null;
        }

        public long getModificationStamp() {
            return timestamp;
        }

        public byte[] getContent() {
            return content;
        }
    }

    @Override
    public IModule[] getChildModules() {
        return new IModule[0];
    }
}
