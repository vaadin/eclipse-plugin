package com.vaadin.plugin.hotswap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * Launch shortcut for debugging Java applications with Hotswap Agent. This adds "Java Application using Hotswap Agent"
 * to the Debug As menu.
 */
@SuppressWarnings("restriction")
public class HotswapLaunchShortcut implements ILaunchShortcut2 {

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
            } else if (element instanceof IAdaptable) {
                IAdaptable adaptable = (IAdaptable) element;
                IJavaElement javaElement = adaptable.getAdapter(IJavaElement.class);
                if (javaElement != null) {
                    launchJavaElement(javaElement, mode);
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
        // Only support debug mode
        if (!"debug".equals(mode)) {
            MessageDialog.openError(getShell(), "Hotswap Agent", "Hotswap Agent can only be used in debug mode.");
            return;
        }

        try {
            // Find the main type
            IType mainType = findMainType(element);
            if (mainType == null) {
                MessageDialog.openError(getShell(), "No Main Method", "No main method found in the selected class.");
                return;
            }

            // Check for Hotswap Agent
            HotswapAgentManager agentManager = HotswapAgentManager.getInstance();
            if (!agentManager.isInstalled()) {
                String version = agentManager.installHotswapAgent();
                if (version == null) {
                    MessageDialog.openError(getShell(), "Hotswap Agent Error", "Failed to install Hotswap Agent.");
                    return;
                }
            }

            // Check for JBR
            JetBrainsRuntimeManager jbrManager = JetBrainsRuntimeManager.getInstance();
            IVMInstall jbr = jbrManager.findInstalledJBR();

            if (jbr == null) {
                boolean install = MessageDialog.openQuestion(getShell(), "JetBrains Runtime Required",
                        "Hotswap Agent requires JetBrains Runtime (JBR) for enhanced class redefinition.\n\n"
                                + "JBR is not currently installed. Would you like to continue anyway?\n\n"
                                + "Note: Hotswap Agent may not work properly without JBR.");

                if (!install) {
                    return;
                }
            }

            // Create or find launch configuration
            ILaunchConfiguration config = findOrCreateLaunchConfiguration(mainType, jbr);
            if (config != null) {
                DebugUITools.launch(config, mode);
            }

        } catch (Exception e) {
            MessageDialog.openError(getShell(), "Launch Error",
                    "Failed to launch with Hotswap Agent: " + e.getMessage());
            e.printStackTrace();
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
                } else if (types.length == 1) {
                    // No main method found, but only one type - check if it's a test or has other
                    // entry point
                    type = types[0];
                }
            }
        } else if (element instanceof IType) {
            type = (IType) element;
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
     * @param jbr
     *            The JBR installation (optional)
     * @return The launch configuration
     */
    private ILaunchConfiguration findOrCreateLaunchConfiguration(IType type, IVMInstall jbr)
            throws CoreException, IOException {

        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType javaAppType = launchManager
                .getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);

        String projectName = type.getJavaProject().getElementName();
        String typeName = type.getFullyQualifiedName();
        String configName = type.getElementName() + " [Hotswap]";

        // Look for existing configuration
        ILaunchConfiguration[] configs = launchManager.getLaunchConfigurations(javaAppType);
        for (ILaunchConfiguration config : configs) {
            if (configName.equals(config.getName())) {
                String configProject = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
                String configType = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "");

                if (projectName.equals(configProject) && typeName.equals(configType)) {
                    return config;
                }
            }
        }

        // Create new configuration
        ILaunchConfigurationWorkingCopy wc = javaAppType.newInstance(null, configName);

        // Set basic attributes
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, typeName);

        // Set JBR if available
        if (jbr != null) {
            wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH,
                    JavaRuntime.newJREContainerPath(jbr).toString());
        }

        // Add Hotswap Agent JVM arguments
        HotswapAgentManager agentManager = HotswapAgentManager.getInstance();
        String[] hotswapArgs = agentManager.getHotswapJvmArgs();
        String vmArgs = String.join(" ", hotswapArgs);
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);

        // Set source locator
        wc.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID,
                "org.eclipse.jdt.launching.sourceLocator.JavaSourceLookupDirector");

        return wc.doSave();
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
