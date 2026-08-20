package com.vaadin.plugin.hotswap;

<<<<<<< HEAD
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
=======
>>>>>>> origin/main
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import com.vaadin.plugin.TelemetryService;
<<<<<<< HEAD
import com.vaadin.plugin.util.VaadinPluginLog;
=======
import com.vaadin.plugin.hotswap.JbrSelector.JbrCandidate;
>>>>>>> origin/main

/**
 * Launch shortcut for debugging Java applications with Hotswap Agent. This adds "Java Application using Hotswap Agent"
 * to the Debug As menu.
 */
@SuppressWarnings("restriction")
public class HotswapLaunchShortcut implements ILaunchShortcut2 {

    private static final String DOWNLOAD_JBR_LABEL = "Download JetBrains Runtime";

    private static final String CONTINUE_WITHOUT_JBR_LABEL = "Continue Without JBR";

    @Override
    public void launch(ISelection selection, String mode) {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            Object element = structuredSelection.getFirstElement();

            if (element instanceof ICompilationUnit) {
                ICompilationUnit cu = (ICompilationUnit) element;
                launchJavaElement(cu, mode);
            } else if (element instanceof IType) {
                IType type = (IType) element;
                launchJavaElement(type, mode);
            } else if (element instanceof IJavaProject) {
                IJavaProject javaProject = (IJavaProject) element;
                launchJavaProject(javaProject, mode);
            } else if (element instanceof IProject) {
                IProject project = (IProject) element;
                IJavaProject javaProject = JavaCore.create(project);
                if (javaProject != null && javaProject.exists()) {
                    launchJavaProject(javaProject, mode);
                }
            } else if (element instanceof IAdaptable) {
                IAdaptable adaptable = (IAdaptable) element;
                IJavaElement javaElement = adaptable.getAdapter(IJavaElement.class);
                if (javaElement != null) {
                    if (javaElement instanceof IJavaProject) {
                        launchJavaProject((IJavaProject) javaElement, mode);
                    } else {
                        launchJavaElement(javaElement, mode);
                    }
                }
            }
        }
    }

    @Override
    public void launch(IEditorPart editor, String mode) {
        IJavaElement element = editor.getEditorInput().getAdapter(IJavaElement.class);
        if (element != null) {
            launchJavaElement(element, mode);
        }
    }

    /**
     * Launch a Java element with Hotswap Agent.
     *
     * @param element
     *            The Java element to launch
     * @param mode
     *            The launch mode (should be "debug")
     */
    private void launchJavaElement(IJavaElement element, String mode) {
        if (!"debug".equals(mode)) {
            MessageDialog.openError(getShell(), "Hotswap Agent", "Hotswap Agent can only be used in debug mode.");
            return;
        }

        try {
            IType mainType = findMainType(element);
            if (mainType == null) {
                if (element instanceof IType) {
                    IType selectedType = (IType) element;
                    MessageDialog.openError(getShell(), "No Main Method",
                            "The selected class '" + selectedType.getElementName()
                                    + "' does not have a main method and no main class was found in the project.\n\n"
                                    + "Please select a class with a main method or the project itself.");
                } else {
                    MessageDialog.openError(getShell(), "No Main Method",
                            "No main method found in the selected element or project.");
                }
                return;
            }
<<<<<<< HEAD

            // Check for Hotswap Agent
            HotswapAgentManager agentManager = HotswapAgentManager.getInstance();
            if (!agentManager.isInstalled()) {
                String version = agentManager.installHotswapAgent();
                if (version == null) {
                    MessageDialog.openError(getShell(), "Hotswap Agent Error", "Failed to install Hotswap Agent.");
                    return;
                }
            }

            // Check for JBR, offering to download one when it is missing
            IVMInstall jbr;
            try {
                jbr = resolveJetBrainsRuntime();
            } catch (OperationCanceledException e) {
                return;
            }

            // Create or find launch configuration
            ILaunchConfiguration config = findOrCreateLaunchConfiguration(mainType, jbr);
            if (config != null) {
                trackDebugLaunch(mainType, jbr != null);
                DebugUITools.launch(config, mode);
            }

=======
            launchMainType(mainType, mode);
>>>>>>> origin/main
        } catch (Exception e) {
            MessageDialog.openError(getShell(), "Launch Error",
                    "Failed to launch with Hotswap Agent: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Launch a Java project (find main class automatically).
     *
     * @param javaProject
     *            The Java project to launch
     * @param mode
     *            The launch mode (should be "debug")
     */
    private void launchJavaProject(IJavaProject javaProject, String mode) {
        if (!"debug".equals(mode)) {
            MessageDialog.openError(getShell(), "Hotswap Agent", "Hotswap Agent can only be used in debug mode.");
            return;
        }

        try {
            IType mainType = findMainTypeInProject(javaProject);
            if (mainType == null) {
                MessageDialog.openError(getShell(), "No Main Class",
                        "Could not find a main class in the project. Please select a specific class with a main method.");
                return;
            }
<<<<<<< HEAD

            // Check for Hotswap Agent
            HotswapAgentManager agentManager = HotswapAgentManager.getInstance();
            if (!agentManager.isInstalled()) {
                String version = agentManager.installHotswapAgent();
                if (version == null) {
                    MessageDialog.openError(getShell(), "Hotswap Agent Error", "Failed to install Hotswap Agent.");
                    return;
                }
            }

            // Check for JBR, offering to download one when it is missing
            IVMInstall jbr;
            try {
                jbr = resolveJetBrainsRuntime();
            } catch (OperationCanceledException e) {
                return;
            }

            // Create or find launch configuration
            ILaunchConfiguration config = findOrCreateLaunchConfiguration(mainType, jbr);
            if (config != null) {
                trackDebugLaunch(mainType, jbr != null);
                DebugUITools.launch(config, mode);
            }

=======
            launchMainType(mainType, mode);
>>>>>>> origin/main
        } catch (Exception e) {
            MessageDialog.openError(getShell(), "Launch Error",
                    "Failed to launch with Hotswap Agent: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
<<<<<<< HEAD
     * Find a JetBrains Runtime to launch with, offering to download the latest one when none is installed.
     *
     * @return The JBR to launch with, or null when the user decided to launch without one
     * @throws OperationCanceledException
     *             if the user cancelled the launch
     */
    private IVMInstall resolveJetBrainsRuntime() {
        JetBrainsRuntimeManager jbrManager = JetBrainsRuntimeManager.getInstance();
        IVMInstall jbr = jbrManager.findInstalledJBR();
        if (jbr != null) {
            return jbr;
        }

        MessageDialog dialog = new MessageDialog(getShell(), "JetBrains Runtime Required", null,
                "Hotswap Agent requires JetBrains Runtime (JBR) for enhanced class redefinition.\n\n"
                        + "JBR is not currently installed. It can be downloaded from "
                        + "https://github.com/JetBrains/JetBrainsRuntime and installed into "
                        + jbrManager.getJdkInstallPath() + ".\n\n"
                        + "Note: Hotswap Agent may not work properly without JBR.",
                MessageDialog.QUESTION,
                new String[] { DOWNLOAD_JBR_LABEL, CONTINUE_WITHOUT_JBR_LABEL, IDialogConstants.CANCEL_LABEL }, 0);

        switch (dialog.open()) {
        case 0:
            return downloadJetBrainsRuntime(jbrManager);
        case 1:
            return null;
        default:
            throw new OperationCanceledException();
=======
     * Shared post-mainType launch flow: install agent, find compatible JBR for the project's required Java version,
     * prompt if missing, create the launch config, and launch it.
     */
    private void launchMainType(IType mainType, String mode) throws Exception {
        HotswapAgentManager agentManager = HotswapAgentManager.getInstance();
        if (!agentManager.isInstalled()) {
            String version = agentManager.installHotswapAgent();
            if (version == null) {
                MessageDialog.openError(getShell(), "Hotswap Agent Error", "Failed to install Hotswap Agent.");
                return;
            }
        }

        IJavaProject javaProject = mainType.getJavaProject();
        int requiredMajor = getRequiredJavaMajorVersion(javaProject);

        JetBrainsRuntimeManager jbrManager = JetBrainsRuntimeManager.getInstance();
        // Not necessarily a JBR: JbrSelector falls back to plain JDKs, so check
        // isJbr() before emitting JBR-only flags.
        JbrCandidate selected = jbrManager.findCompatibleCandidate(requiredMajor).orElse(null);

        if (selected == null || !selected.isJbr()) {
            boolean install = MessageDialog.openQuestion(getShell(), "JetBrains Runtime Required",
                    "Hotswap Agent requires JetBrains Runtime (JBR) for enhanced class redefinition.\n\n"
                            + "JBR is not currently installed. Would you like to continue anyway?\n\n"
                            + "Note: Hotswap Agent may not work properly without JBR.");

            if (!install) {
                return;
            }
        }

        ILaunchConfiguration config = findOrCreateLaunchConfiguration(mainType, selected);
        if (config != null) {
            trackDebugLaunch(mainType, selected, requiredMajor);
            DebugUITools.launch(config, mode);
>>>>>>> origin/main
        }
    }

    /**
<<<<<<< HEAD
     * Download and register the latest JetBrains Runtime while showing a progress dialog.
     *
     * @param jbrManager
     *            The runtime manager
     * @return The downloaded JBR, or null when the user decided to launch without one
     * @throws OperationCanceledException
     *             if the user cancelled the download or the launch
     */
    private IVMInstall downloadJetBrainsRuntime(JetBrainsRuntimeManager jbrManager) {
        IVMInstall[] downloaded = new IVMInstall[1];

        try {
            new ProgressMonitorDialog(getShell()).run(true, true, monitor -> {
                try {
                    downloaded[0] = jbrManager.downloadAndInstallJBR(monitor);
                } catch (IOException | InterruptedException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InterruptedException | OperationCanceledException e) {
            throw new OperationCanceledException();
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof OperationCanceledException || cause instanceof InterruptedException) {
                throw new OperationCanceledException();
            }
            VaadinPluginLog.error("Failed to download JetBrains Runtime: " + cause.getMessage(), cause);

            boolean launchAnyway = MessageDialog.openQuestion(getShell(), "JetBrains Runtime Download Failed",
                    "Could not download JetBrains Runtime: " + cause.getMessage() + "\n\n"
                            + "Would you like to launch without JBR?\n\n"
                            + "Note: Hotswap Agent may not work properly without JBR.");
            if (!launchAnyway) {
                throw new OperationCanceledException();
            }
            return null;
        }

        return downloaded[0];
    }

    private void trackDebugLaunch(IType mainType, boolean hasJBR) {
=======
     * Read the project's required Java major version from JDT's compiler compliance setting. JDT's compliance is the
     * source of truth — m2e and Buildship propagate Maven/Gradle compiler settings into it, so this single read covers
     * Maven, Gradle, and manual JDT projects. {@code true} enables inheritance from workspace defaults so projects
     * without an explicit setting still resolve.
     */
    private int getRequiredJavaMajorVersion(IJavaProject javaProject) {
        if (javaProject == null) {
            return 0;
        }
        String compliance = javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true);
        return JbrSelector.parseMajor(compliance);
    }

    private void trackDebugLaunch(IType mainType, JbrCandidate selected, int requiredMajor) {
>>>>>>> origin/main
        try {
            java.util.Map<String, Object> properties = new java.util.HashMap<>();
            properties.put("main_class", mainType.getFullyQualifiedName());
            properties.put("project_name", mainType.getJavaProject().getElementName());
            properties.put("has_jbr", selected != null && selected.isJbr());
            properties.put("project_required_version", requiredMajor);
            if (selected != null) {
                properties.put("jbr_major_version", selected.majorVersion());
                properties.put("jbr_is_jetbrains", selected.isJbr());
            }
            TelemetryService.getInstance().trackEvent("DebugWithHotswap", properties);
        } catch (Exception e) {
            // Ignore telemetry errors
        }
    }

    /**
     * Find the main type from a Java element.
     *
     * @param element
     *            The Java element
     * @return The main type, or null if not found
     */
    private IType findMainType(IJavaElement element) throws JavaModelException {
        IType type = null;

        if (element instanceof ICompilationUnit) {
            ICompilationUnit cu = (ICompilationUnit) element;
            IType[] types = cu.getTypes();

            // Look for a type with main method
            for (IType t : types) {
                if (hasMainMethod(t)) {
                    type = t;
                    break;
                }
            }

            // If multiple types with main, let user choose
            if (type == null && types.length > 0) {
                List<IType> mainTypes = new ArrayList<>();
                for (IType t : types) {
                    if (hasMainMethod(t)) {
                        mainTypes.add(t);
                    }
                }

                if (mainTypes.size() == 1) {
                    type = mainTypes.get(0);
                } else if (mainTypes.size() > 1) {
                    type = chooseMainType(mainTypes);
                } else {
                    // No main method found in this file, try to find it in the project
                    IJavaProject javaProject = cu.getJavaProject();
                    type = findMainTypeInProject(javaProject);
                }
            }
        } else if (element instanceof IType) {
            type = (IType) element;
            // If the selected type doesn't have a main method, find one in the project
            if (!hasMainMethod(type)) {
                IJavaProject javaProject = type.getJavaProject();
                IType projectMainType = findMainTypeInProject(javaProject);
                if (projectMainType != null) {
                    type = projectMainType;
                }
            }
        } else if (element instanceof IJavaProject) {
            type = findMainTypeInProject((IJavaProject) element);
        }

        return type;
    }

    /**
     * Check if a type has a main method.
     *
     * @param type
     *            The type to check
     * @return true if it has a main method
     */
    private boolean hasMainMethod(IType type) {
        try {
            return type.getMethod("main", new String[] { "[QString;" }).exists();
        } catch (Exception e) {
            // Catch any exception since JavaModelException may not be available
            return false;
        }
    }

    /**
     * Let the user choose from multiple main types.
     *
     * @param mainTypes
     *            The list of types with main methods
     * @return The selected type, or null if cancelled
     */
    private IType chooseMainType(List<IType> mainTypes) {
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(),
                DebugUITools.newDebugModelPresentation());

        dialog.setTitle("Select Main Type");
        dialog.setMessage("Select the main type to launch:");
        dialog.setElements(mainTypes.toArray());

        if (dialog.open() == Window.OK) {
            return (IType) dialog.getFirstResult();
        }

        return null;
    }

    /**
     * Find or create a launch configuration for the given type with Hotswap.
     *
     * @param type
     *            The main type
     * @param selected
     *            The selected runtime (optional). JBR-only JVM flags are emitted only when it is an actual JBR, so the
     *            launch can run on stock OpenJDK without "Unrecognized VM option" errors.
     * @return The launch configuration
     */
    private ILaunchConfiguration findOrCreateLaunchConfiguration(IType type, JbrCandidate selected) throws Exception {

        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType javaAppType = launchManager
                .getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);

        String projectName = type.getJavaProject().getElementName();
        String typeName = type.getFullyQualifiedName();
        String configName = type.getElementName() + " [Hotswap]";

        // Reuse an existing configuration if one matches, otherwise create it
        ILaunchConfigurationWorkingCopy wc = null;
        ILaunchConfiguration[] configs = launchManager.getLaunchConfigurations(javaAppType);
        for (ILaunchConfiguration config : configs) {
            if (configName.equals(config.getName())) {
                String configProject = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
                String configType = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "");

                if (projectName.equals(configProject) && typeName.equals(configType)) {
                    wc = config.getWorkingCopy();
                    break;
                }
            }
        }

        if (wc == null) {
            wc = javaAppType.newInstance(null, configName);

            // Set basic attributes
            wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
            wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, typeName);

            // Set source locator
            wc.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID,
                    "org.eclipse.jdt.launching.sourceLocator.JavaSourceLookupDirector");
        }

        // Always refresh runtime and JVM arguments, including on configurations saved
        // by earlier plugin versions — otherwise a config predating this fix keeps its
        // stale JRE and malformed --add-opens arguments forever. A null path clears the
        // pinned JRE so the launch falls back to the project default.
        boolean isJbr = selected != null && selected.isJbr();
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH,
                selected == null ? null : JavaRuntime.newJREContainerPath(selected.vm()).toString());

        // JBR-only -XX:* flags go out only on JBR — stock OpenJDK rejects them at
        // startup with "Unrecognized VM option".
        HotswapAgentManager agentManager = HotswapAgentManager.getInstance();
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS,
                agentManager.getHotswapJvmArgsString(isJbr));

        return wc.doSave();
    }

    /**
     * Find a main type in the project (looking for Application classes or classes with main method).
     *
     * @param javaProject
     *            The Java project to search
     * @return The main type, or null if not found
     */
    private IType findMainTypeInProject(IJavaProject javaProject) throws JavaModelException {
        // First look for Spring Boot Application classes
        try {
            IType springBootApp = findSpringBootApplication(javaProject);
            if (springBootApp != null) {
                return springBootApp;
            }
        } catch (Exception e) {
            // Ignore and continue
        }

        // Look for any class with main method
        List<IType> mainTypes = new ArrayList<>();
        IPackageFragment[] packages = javaProject.getPackageFragments();
        for (IPackageFragment pkg : packages) {
            if (pkg.getKind() == IPackageFragmentRoot.K_SOURCE) {
                ICompilationUnit[] units = pkg.getCompilationUnits();
                for (ICompilationUnit unit : units) {
                    IType[] types = unit.getTypes();
                    for (IType type : types) {
                        if (hasMainMethod(type)) {
                            // If it's named Application or contains Application, prefer it
                            if (type.getElementName().contains("Application")) {
                                return type;
                            }
                            mainTypes.add(type);
                        }
                    }
                }
            }
        }

        if (mainTypes.size() == 1) {
            return mainTypes.get(0);
        } else if (mainTypes.size() > 1) {
            // Let user choose
            return chooseMainType(mainTypes);
        }

        return null;
    }

    /**
     * Find a Spring Boot application class in the project.
     *
     * @param javaProject
     *            The Java project to search
     * @return The Spring Boot application type, or null if not found
     */
    private IType findSpringBootApplication(IJavaProject javaProject) throws JavaModelException {
        IPackageFragment[] packages = javaProject.getPackageFragments();
        for (IPackageFragment pkg : packages) {
            if (pkg.getKind() == IPackageFragmentRoot.K_SOURCE) {
                ICompilationUnit[] units = pkg.getCompilationUnits();
                for (ICompilationUnit unit : units) {
                    IType[] types = unit.getTypes();
                    for (IType type : types) {
                        // Check for @SpringBootApplication annotation
                        IAnnotation[] annotations = type.getAnnotations();
                        for (IAnnotation annotation : annotations) {
                            String annotationName = annotation.getElementName();
                            if ("SpringBootApplication".equals(annotationName)
                                    || "org.springframework.boot.autoconfigure.SpringBootApplication"
                                            .equals(annotationName)) {
                                if (hasMainMethod(type)) {
                                    return type;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get the active shell.
     *
     * @return The active shell
     */
    private Shell getShell() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    }

    @Override
    public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
        // Not used, but required by interface
        return null;
    }

    @Override
    public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editorpart) {
        // Not used, but required by interface
        return null;
    }

    @Override
    public IResource getLaunchableResource(ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection ss = (IStructuredSelection) selection;
            Object element = ss.getFirstElement();
            if (element instanceof IAdaptable) {
                return ((IAdaptable) element).getAdapter(IResource.class);
            }
        }
        return null;
    }

    @Override
    public IResource getLaunchableResource(IEditorPart editorpart) {
        return editorpart.getEditorInput().getAdapter(IResource.class);
    }
}
