# eclipse-plugin

This Eclipse plugin:
- Displays a Vaadin logo in the bottom right of the Eclipse status bar when the plugin is active.
- Auto-detects Vaadin projects (projects containing Vaadin on their classpath) and only enables menu, toolbar, and status-bar contributions when such a project is selected.

A JUnit plug-in test (IconPresenceTest) in `src-test/` launches the IDE, interrogates the status bar, and asserts that the Vaadin logo contribution (ID `vaadin-eclipse-plugin.toolbars.statusLogo`) is actually rendered.
