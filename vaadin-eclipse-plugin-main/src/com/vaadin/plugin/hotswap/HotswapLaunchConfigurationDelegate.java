package com.vaadin.plugin.hotswap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.launching.JavaRuntime;

/**
 * Launch configuration delegate for debugging with Hotswap Agent. Extends the standard Java launch delegate to add
 * Hotswap Agent configuration.
 */
public class HotswapLaunchConfigurationDelegate extends JavaLaunchDelegate {

    public static final String HOTSWAP_LAUNCH_MODE = "hotswap";

    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
            throws CoreException {

        // Ensure we're in debug mode
        if (!"debug".equals(mode)) {
            throw new CoreException(
                    new Status(IStatus.ERROR, "vaadin-eclipse-plugin", "Hotswap Agent can only be used in debug mode"));
        }

        // Check for JBR
        IVMInstall vm = verifyVMInstall(configuration);
        JetBrainsRuntimeManager jbrManager = JetBrainsRuntimeManager.getInstance();

        if (!jbrManager.isJetBrainsRuntime(vm)) {
            // Try to find a compatible JBR
            String javaVersion = getRequiredJavaVersion(configuration);
            IVMInstall jbr = jbrManager.getCompatibleJBR(javaVersion);

            if (jbr == null) {
                throw new CoreException(new Status(IStatus.ERROR, "vaadin-eclipse-plugin",
                        "JetBrains Runtime is required for Hotswap Agent. " + "Please install JBR " + javaVersion
                                + " and configure it in the launch configuration."));
            }

            // Update the configuration to use JBR
            ILaunchConfigurationWorkingCopy wc = configuration.getWorkingCopy();
            wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH,
                    JavaRuntime.newJREContainerPath(jbr).toString());
            configuration = wc.doSave();
        }

        // Check if the JBR is broken
        if (jbrManager.isBrokenJBR(vm)) {
            throw new CoreException(new Status(IStatus.ERROR, "vaadin-eclipse-plugin",
                    "The installed JBR version is known to have issues with Hotswap Agent. "
                            + "Please install a different version of JBR."));
        }

        // Add Hotswap Agent JVM arguments
        configuration = addHotswapArguments(configuration);

        // Launch with the modified configuration
        super.launch(configuration, mode, launch, monitor);
    }

    /**
     * Add Hotswap Agent JVM arguments to the launch configuration.
     *
     * @param configuration
     *            The launch configuration
     * @return The modified configuration
     * @throws CoreException
     *             if modification fails
     */
    private ILaunchConfiguration addHotswapArguments(ILaunchConfiguration configuration) throws CoreException {
        ILaunchConfigurationWorkingCopy wc = configuration.getWorkingCopy();

        // Get existing VM arguments
        String existingArgs = wc.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "");
        List<String> argsList = new ArrayList<>();
        if (!existingArgs.isEmpty()) {
            argsList.addAll(Arrays.asList(existingArgs.split("\\s+")));
        }

        // Get Hotswap Agent JVM arguments
        HotswapAgentManager agentManager = HotswapAgentManager.getInstance();
        try {
            String[] hotswapArgs = agentManager.getHotswapJvmArgs();

            // Add Hotswap arguments if not already present
            for (String arg : hotswapArgs) {
                if (!containsArgument(argsList, arg)) {
                    argsList.add(arg);
                }
            }

        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, "vaadin-eclipse-plugin",
                    "Failed to configure Hotswap Agent: " + e.getMessage(), e));
        }

        // Join arguments back into a string
        String newArgs = String.join(" ", argsList);
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, newArgs);

        return wc.doSave();
    }

    /**
     * Check if an argument list contains a specific argument. Handles arguments with values (e.g., -javaagent:path).
     *
     * @param argsList
     *            The list of arguments
     * @param arg
     *            The argument to check for
     * @return true if the argument is present
     */
    private boolean containsArgument(List<String> argsList, String arg) {
        if (arg.contains("=") || arg.contains(":")) {
            String prefix = arg.substring(0, arg.indexOf(arg.contains("=") ? "=" : ":") + 1);
            return argsList.stream().anyMatch(a -> a.startsWith(prefix));
        }
        return argsList.contains(arg);
    }

    /**
     * Get the required Java version for the launch configuration.
     *
     * @param configuration
     *            The launch configuration
     * @return The Java version string (e.g., "17", "21")
     * @throws CoreException
     *             if the version cannot be determined
     */
    private String getRequiredJavaVersion(ILaunchConfiguration configuration) throws CoreException {
        // Try to determine from project settings
        // For now, default to Java 17 which is common for Vaadin projects
        return "17";
    }

    @Override
    public IVMRunner getVMRunner(ILaunchConfiguration configuration, String mode) throws CoreException {
        // Always use debug runner for Hotswap
        return super.getVMRunner(configuration, "debug");
    }

    @Override
    public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
        // Get the standard classpath
        String[] classpath = super.getClasspath(configuration);

        // We might need to add Hotswap Agent to the classpath in some cases
        // For now, return the standard classpath
        return classpath;
    }

    @Override
    protected void setDefaultSourceLocator(ILaunch launch, ILaunchConfiguration configuration) throws CoreException {
        // Use the standard source locator
        super.setDefaultSourceLocator(launch, configuration);
    }

    /**
     * Create a Hotswap-enabled launch configuration from an existing one.
     *
     * @param originalConfig
     *            The original launch configuration
     * @return The new Hotswap-enabled configuration
     * @throws CoreException
     *             if creation fails
     */
    public static ILaunchConfiguration createHotswapConfiguration(ILaunchConfiguration originalConfig)
            throws CoreException {

        ILaunchConfigurationWorkingCopy wc = originalConfig.copy(originalConfig.getName() + " [Hotswap]");

        // Set the delegate class
        wc.setAttribute("org.eclipse.jdt.launching.LAUNCH_DELEGATE_ID",
                "com.vaadin.plugin.hotswap.HotswapLaunchConfigurationDelegate");

        // Ensure debug mode
        wc.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID,
                "org.eclipse.jdt.launching.sourceLocator.JavaSourceLookupDirector");

        return wc.doSave();
    }
}
