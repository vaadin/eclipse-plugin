# Vaadin Eclipse Plugin - Main Module

This is the main Eclipse plugin module for Vaadin support.

## Architecture Decisions

### JSON Path Convention

**All JSON communication in this plugin uses native OS file paths, not portable paths.**

This is a critical architectural decision that affects:
- REST API responses (CopilotRestService)
- Generated JSON files (flow-build-info.json)
- Inter-process communication with Vaadin Copilot

#### Implementation Guidelines

1. **Writing paths to JSON**: Always use `toOSString()` method
   ```java
   String path = project.getLocation().toOSString();  // ✓ Correct
   String path = project.getLocation().toPortableString();  // ✗ Wrong
   ```

2. **Expected path formats**:
   - Windows: `C:\Users\username\project` (backslashes)
   - Unix/Mac: `/home/username/project` (forward slashes)

3. **JSON escaping**: When paths are written to JSON, backslashes are automatically escaped:
   - Actual path: `C:\Users\project`
   - In JSON: `"path": "C:\\Users\\project"`
   - When parsed: `C:\Users\project`

4. **Testing**: When testing JSON content, parse the JSON to compare actual values rather than string matching:
   ```java
   JsonObject json = JsonParser.parseString(jsonContent).getAsJsonObject();
   String path = json.get("npmFolder").getAsString();
   assertEquals(expected, path);  // Handles escaping correctly
   ```

## Key Components

- **CopilotRestService**: REST API server for Copilot integration
- **VaadinBuildParticipant**: Eclipse build participant that generates flow-build-info.json
- **VaadinBuilderConfigurator**: Manages Eclipse builder configuration
- **VaadinProjectWizard**: New project creation wizard

## Testing

Tests are located in the `vaadin-eclipse-plugin.tests` module. Run with:
```bash
mvn verify
```