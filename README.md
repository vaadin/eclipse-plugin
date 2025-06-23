# eclipse-plugin

This Eclipse plugin displays a Vaadin logo in the bottom right of the Eclipse status bar when the plugin is active.

A JUnit plug-in test (IconPresenceTest) in `src-test/` launches the IDE, interrogates the status bar, and asserts that the Vaadin logo contribution (ID `vaadin-eclipse-plugin.toolbars.statusLogo`) is actually rendered after Eclipse starts.
