package com.vaadin.integration.eclipse.util;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.preferences.PreferenceConstants;

public class PreferenceUtil {

    private ScopedPreferenceStore prefStore;

    private PreferenceUtil(IProject project) {
        prefStore = new ScopedPreferenceStore(new ProjectScope(project),
                VaadinPlugin.PLUGIN_ID);
    }

    // preference store keys

    public static PreferenceUtil get(IProject project) {
        return new PreferenceUtil(project);
    }

    /*
     * "Global" pref keys, referenced from elsewhere. (Project scoped keys
     * follow later.)
     */

    // true when Ivy resolving info popup is disabled
    public static final String PREFERENCES_IVYINFO_DISABLED = VaadinPlugin.PLUGIN_ID
            + "." + "IvyInformationDisabled";

    // true when Eclipse bug info popup is disabled
    public static final String PREFERENCES_ECLIPSE_LUNA_SR1_BUG_INFO = VaadinPlugin.PLUGIN_ID
            + "." + "Eclipse44BugInformationDisabled";

    /*
     * Project scoped keys
     */

    // "true"/"false"/missing - if missing, check if >1 widgetset exists
    private static final String PREFERENCES_WIDGETSET_DIRTY = VaadinPlugin.PLUGIN_ID
            + "." + "widgetsetDirty";
    // true to suspend automatic widgetset build requests for the project
    private static final String PREFERENCES_WIDGETSET_SUSPENDED = VaadinPlugin.PLUGIN_ID
            + "." + "widgetsetBuildsSuspended";
    // "OBF"/"PRETTY"/"DETAILED" or missing (default to "OBF")
    private static final String PREFERENCES_WIDGETSET_STYLE = VaadinPlugin.PLUGIN_ID
            + "." + "widgetsetStyle";
    // a number of threads to use (-localWorkers) or missing
    private static final String PREFERENCES_WIDGETSET_PARALLELISM = VaadinPlugin.PLUGIN_ID
            + "." + "widgetsetParallelism";

    // the time last compilation lasted, used for estimation in progress monitor
    private static final String PREFERENCES_WIDGETSET_COMPILATION_ETA = VaadinPlugin.PLUGIN_ID
            + "." + "widgetsetCompilationEta";

    // to output compilation messages to console or not
    private static final String PREFERENCES_WIDGETSET_VERBOSE = VaadinPlugin.PLUGIN_ID
            + "." + "widgetsetVerbose";

    // extra parameters for widgetset compilation
    private static final String PREFERENCES_WIDGETSET_EXTRA_PARAMETERS = VaadinPlugin.PLUGIN_ID
            + "." + "widgetsetExtraParameters";

    // extra JVM parameters for widgetset compilation
    private static final String PREFERENCES_WIDGETSET_EXTRA_JVM_PARAMETERS = VaadinPlugin.PLUGIN_ID
            + "." + "widgetsetExtraJvmParameters";

    // project type flags - note that in the future, there could be multiple
    // flags set at the same time
    private static final String PREFERENCES_PROJECT_TYPE_GAE = VaadinPlugin.PLUGIN_ID
            + "." + "projectTypeGae";

    // "true"/"false"/missing - missing means false
    private static final String PREFERENCES_USE_LATEST_NIGHTLY = VaadinPlugin.PLUGIN_ID
            + "." + "useLatestNightly";

    // "true"/"false"/missing - if missing, default value is taken from
    // Eclipse preferences
    private static final String PREFERENCES_UPDATE_NOTIFICATION_ENABLED = VaadinPlugin.PLUGIN_ID
            + ".notifyOfVaadinUpdates";

    // true to suspend scanning for addon themes and creating the addons.scss
    // file
    private static final String PREFERENCES_ADDON_THEME_SCANNING_SUSPENDED = VaadinPlugin.PLUGIN_ID
            + "." + "addonThemesSuspended";

    // true to suspend automatic compilation of themes
    private static final String PREFERENCES_THEME_COMPILATION_SUSPENDED = VaadinPlugin.PLUGIN_ID
            + "." + "themeCompilationSuspended";

    // VaadinPlugin.COMPILE_ACTION_* or missing - if missing, defaults to
    // widgetset
    private static final String PREFERENCES_PREVIOUS_COMPILE_ACTION = VaadinPlugin.PLUGIN_ID
            + "." + "previousCompileAction";

    /**
     * Checks whether scanning for addon themes has explicitly been suspended by
     * the user
     */
    public boolean isAddonThemeScanningSuspended() {
        if (!prefStore.contains(PREFERENCES_ADDON_THEME_SCANNING_SUSPENDED)) {
            return false;
        } else {
            return prefStore
                    .getBoolean(PREFERENCES_ADDON_THEME_SCANNING_SUSPENDED);
        }
    }

    /**
     * Suspends the addon theme scanning and so stops updating the addons.scss
     * file with further changes
     * 
     * @param suspended
     *            true if no updates to the addons.scss should be made
     *            automatically
     */
    public void setAddonThemeScanningSuspended(boolean suspended) {
        prefStore.setValue(PREFERENCES_ADDON_THEME_SCANNING_SUSPENDED,
                suspended);
    }

    /**
     * Checks whether automatic compilation of themes has explicitly been
     * suspended by the user
     */
    public boolean isThemeCompilationSuspended() {
        if (!prefStore.contains(PREFERENCES_THEME_COMPILATION_SUSPENDED)) {
            return false;
        } else {
            return prefStore
                    .getBoolean(PREFERENCES_THEME_COMPILATION_SUSPENDED);
        }
    }

    /**
     * Suspends automatic theme compilation
     * 
     * @param suspended
     *            true if themes should not be compiled automatically
     */
    public void setThemeCompilationSuspended(boolean suspended) {
        prefStore.setValue(PREFERENCES_THEME_COMPILATION_SUSPENDED, suspended);
    }

    /**
     * Checks whether widgetset building for a project has been suspended
     * explicitly by the user.
     * 
     * @param project
     * @return
     */
    public boolean isWidgetsetCompilationSuspended() {
        if (!prefStore.contains(PREFERENCES_WIDGETSET_SUSPENDED)) {
            return true;
        } else {
            return prefStore.getBoolean(PREFERENCES_WIDGETSET_SUSPENDED);
        }
    }

    /**
     * Sets the suspended flag for widgetset compilation. If suspended the
     * widgetset will not be compiled automatically by the plugin. Returns true
     * if the value was changed, false if it remained the same.
     * 
     * @param parallelism
     * @return
     */
    public boolean setWidgetsetCompilationSuspended(boolean suspended) {
        boolean oldValue = isWidgetsetCompilationSuspended();
        // need to store as String because default value is "true"
        prefStore.setValue(PREFERENCES_WIDGETSET_SUSPENDED,
                Boolean.toString(suspended));
        return oldValue != suspended;
    }

    public boolean isWidgetsetCompilationVerboseMode() {
        if (!prefStore.contains(PREFERENCES_WIDGETSET_VERBOSE)) {
            return true;
        } else {
            return prefStore.getBoolean(PREFERENCES_WIDGETSET_VERBOSE);
        }
    }

    /**
     * Sets the verbosity mode used in widgetset compilation. Returns true if
     * the value was changed, false if it remained the same.
     * 
     * @param parallelism
     * @return
     */
    public boolean setWidgetsetCompilationVerboseMode(boolean verbose) {
        boolean oldValue = isWidgetsetCompilationVerboseMode();
        // need to store as String because default value is "true"
        prefStore.setValue(PREFERENCES_WIDGETSET_VERBOSE,
                Boolean.toString(verbose));
        return oldValue != verbose;

    }

    public String getWidgetsetCompilationStyle() {
        if (!prefStore.contains(PREFERENCES_WIDGETSET_STYLE)) {
            return "OBF";
        } else {
            return prefStore.getString(PREFERENCES_WIDGETSET_STYLE);
        }
    }

    /**
     * Sets the style parameter used in widgetset compilation. Returns true if
     * the value was changed, false if it remained the same.
     * 
     * @param style
     * @return
     */
    public boolean setWidgetsetCompilationStyle(String style) {
        String oldValue = getWidgetsetCompilationStyle();
        if (style == null || "".equals(style)) {
            // Convert "" -> "OBF" to be more explicit
            style = "OBF";
        }
        if (oldValue == null || "".equals(oldValue)) {
            // Convert ""-> "OBF" for backwards compatibility.
            oldValue = "OBF";
        }
        prefStore.setValue(PREFERENCES_WIDGETSET_STYLE, style);
        return !equals(oldValue, style);

    }

    public String getWidgetsetCompilationExtraParameters() {
        return prefStore.getString(PREFERENCES_WIDGETSET_EXTRA_PARAMETERS);
    }

    public String getWidgetsetCompilationExtraJvmParameters() {
        return prefStore.getString(PREFERENCES_WIDGETSET_EXTRA_JVM_PARAMETERS);
    }

    /**
     * Sets the extra command line parameters used in widgetset compilation.
     * Returns true if the value was changed, false if it remained the same.
     * 
     * @param params
     * @return
     */
    public boolean setWidgetsetCompilationExtraParameters(String params) {
        String oldValue = prefStore
                .getString(PREFERENCES_WIDGETSET_EXTRA_PARAMETERS);
        if (params == null) {
            params = "";
        }
        prefStore.setValue(PREFERENCES_WIDGETSET_EXTRA_PARAMETERS, params);
        return !equals(oldValue, params);

    }

    /**
     * Sets the extra JVM parameters used in widgetset compilation. Returns true
     * if the value was changed, false if it remained the same.
     * 
     * @param params
     * @return
     */
    public boolean setWidgetsetCompilationExtraJvmParameters(String params) {
        String oldValue = prefStore
                .getString(PREFERENCES_WIDGETSET_EXTRA_JVM_PARAMETERS);
        if (params == null) {
            params = "";
        }
        prefStore.setValue(PREFERENCES_WIDGETSET_EXTRA_JVM_PARAMETERS, params);
        return !equals(oldValue, params);

    }

    public String getWidgetsetCompilationParallelism() {
        if (!prefStore.contains(PREFERENCES_WIDGETSET_PARALLELISM)) {
            return "";
        } else {
            return prefStore.getString(PREFERENCES_WIDGETSET_PARALLELISM);
        }
    }

    /**
     * Sets the parallelism parameter used in widgetset compilation. Returns
     * true if the value was changed, false if it remained the same.
     * 
     * @param parallelism
     * @return
     */
    public boolean setWidgetsetCompilationParallelism(String parallelism) {
        String oldValue = getWidgetsetCompilationParallelism();
        prefStore.setValue(PREFERENCES_WIDGETSET_PARALLELISM, parallelism);
        return !equals(oldValue, parallelism);
    }

    /**
     * Compares the two strings. Returns true if both are null or both contain
     * the same characters.
     * 
     * @param oldValue
     * @param newValue
     * @return
     */
    private boolean equals(String oldValue, String newValue) {
        if (oldValue == null) {
            return newValue == null;
        }
        if (newValue == null) {
            return false;
        }
        return oldValue.equals(newValue);
    }

    public void persist() throws IOException {
        prefStore.save();
    }

    public long getEstimatedCompilationTime() {
        if (prefStore.contains(PREFERENCES_WIDGETSET_COMPILATION_ETA)) {
            return prefStore.getLong(PREFERENCES_WIDGETSET_COMPILATION_ETA);
        } else {
            /**
             * Make an initial wild guess that compilation takes two minutes.
             */
            return 120 * 1000l;
        }

    }

    public void setWidgetsetCompilationTimeEstimate(long estimate) {
        prefStore.setValue(PREFERENCES_WIDGETSET_COMPILATION_ETA, estimate);

    }

    /**
     * Checks if the widgetset is marked as dirty. Returns true for dirty, false
     * for not dirty and null if no marking was found.
     * 
     * @return
     */
    public Boolean isWidgetsetDirty() {
        if (prefStore.contains(PREFERENCES_WIDGETSET_DIRTY)) {
            return prefStore.getBoolean(PREFERENCES_WIDGETSET_DIRTY);
        }

        return null;

    }

    public void setWidgetsetDirty(boolean dirty) {
        prefStore
                .setValue(PREFERENCES_WIDGETSET_DIRTY, Boolean.toString(dirty));
    }

    /**
     * Checks if the project is configured to use the latest nightly build.
     * 
     * @return
     */
    public boolean isUsingLatestNightly() {
        if (prefStore.contains(PREFERENCES_USE_LATEST_NIGHTLY)) {
            return prefStore.getBoolean(PREFERENCES_USE_LATEST_NIGHTLY);
        }
        return false;
    }

    /**
     * Sets whether the project should always use the latest nightly build of
     * the branch. Returns true if the value was changed, false if it remained
     * the same.
     * 
     * @param style
     * @return
     */
    public boolean setUsingLatestNightly(boolean useLatestNightly) {
        boolean oldValue = isUsingLatestNightly();
        prefStore.setValue(PREFERENCES_USE_LATEST_NIGHTLY,
                Boolean.toString(useLatestNightly));
        return oldValue != useLatestNightly;
    }

    /**
     * Checks whether the project is configured to show a notification when it
     * is possible to update to a newer Vaadin version.
     *
     * @return whether Vaadin version update notifications are enabled
     */
    public boolean isUpdateNotificationEnabled() {
        if (prefStore.contains(PREFERENCES_UPDATE_NOTIFICATION_ENABLED)) {
            return prefStore
                    .getBoolean(PREFERENCES_UPDATE_NOTIFICATION_ENABLED);
        }
        // If the project does not have this preference set, use the default
        // from Eclipse preferences.
        return VaadinPlugin
                .getInstance()
                .getPreferenceStore()
                .getBoolean(
                        PreferenceConstants.UPDATE_NOTIFICATIONS_IN_NEW_PROJECTS);
    }

    /**
     * Sets whether a notification should be shown about possible Vaadin version
     * updates for the project.
     *
     * @param enableNotifications
     *            whether Vaadin update notifications are enabled
     */
    public void setUpdateNotificationEnabled(boolean enableNotifications) {
        prefStore.setValue(PREFERENCES_UPDATE_NOTIFICATION_ENABLED,
                Boolean.toString(enableNotifications));
    }

    /**
     * Sets a flag that marks if the project is a Google App Engine project.
     * 
     * @param gaeProject
     */
    public void setProjectTypeGae(boolean gaeProject) {
        prefStore.setValue(PREFERENCES_PROJECT_TYPE_GAE, gaeProject);
    }

    public boolean isGaeProject() {
        if (prefStore.contains(PREFERENCES_PROJECT_TYPE_GAE)) {
            return prefStore.getBoolean(PREFERENCES_PROJECT_TYPE_GAE);
        }

        return false;
    }

    /**
     * Returns the previously used compile action ("widgetset" or "theme" or
     * "both") - use the constants {@link VaadinPlugin}.COMPILE_ACTION_*.
     * 
     * @return previously used compile action, by default "widgetset" if none
     *         saved
     */
    public String getPreviousCompileAction() {
        if (!prefStore.contains(PREFERENCES_PREVIOUS_COMPILE_ACTION)) {
            return VaadinPlugin.COMPILE_ACTION_WIDGETSET;
        } else {
            return prefStore.getString(PREFERENCES_PREVIOUS_COMPILE_ACTION);
        }
    }

    /**
     * Sets the previously used compile action ("widgetset", "theme" or "both").
     * Returns true if the value was changed, false if it remained the same.
     * 
     * @param action
     *            either "widgetset", "theme" or "both" - use the constants
     *            {@link VaadinPlugin}.COMPILE_ACTION_*
     * @return true if the value was changed
     */
    public boolean setPreviousCompileAction(String action) {
        String oldValue = getPreviousCompileAction();
        prefStore.setValue(PREFERENCES_PREVIOUS_COMPILE_ACTION, action);
        return !equals(oldValue, action);
    }
}
