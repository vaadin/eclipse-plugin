# Vaadin Eclipse Plugin - Copilot Integration

## Project Overview

This Eclipse plugin provides Copilot integration for Vaadin projects, enabling AI-assisted development through a REST API interface. The plugin starts an embedded HTTP server that communicates with Vaadin Copilot, allowing developers to leverage AI capabilities directly within their Eclipse IDE.

## Architecture

### Core Components

1. **CopilotRestService** (`com.vaadin.plugin.CopilotRestService`)
   - Embedded HTTP server using `com.sun.net.httpserver`
   - Handles REST API requests from Vaadin Copilot
   - Manages file operations, project analysis, and IDE integration
   - Port: Dynamically assigned, communicated via dotfiles

2. **VaadinProjectAnalyzer** (`com.vaadin.plugin.VaadinProjectAnalyzer`)
   - Analyzes Java projects for Vaadin-specific elements
   - Finds routes, components, entities, and security configurations
   - Uses Eclipse JDT APIs for type hierarchy and annotation scanning

3. **CopilotUndoManager** (`com.vaadin.plugin.CopilotUndoManager`)
   - Tracks file modifications for undo/redo support
   - Integrates with Eclipse's operation history framework
   - Singleton pattern for global operation tracking

4. **CopilotClient** (`com.vaadin.plugin.CopilotClient`)
   - HTTP client for testing REST endpoints
   - Used in integration tests to verify service functionality

5. **CopilotUtil** (`com.vaadin.plugin.CopilotUtil`)
   - Utility functions for dotfile management
   - Service name generation and endpoint formatting

## REST API Endpoints

All endpoints receive POST requests with JSON payloads containing:
- `command`: The operation to perform
- `projectBasePath`: The project's file system path
- `data`: Command-specific parameters

### Implemented Endpoints

#### File Operations
- **write** - Write text content to files
  - Creates parent directories as needed
  - Supports undo/redo tracking
  - Parameters: `file`, `content`, `undoLabel`

- **writeBase64** - Write binary content to files
  - Base64 decoded before writing
  - Parameters: `file`, `content`, `undoLabel`

- **delete** - Delete files
  - Tracks content for undo capability
  - Parameters: `file`

- **refresh** - Refresh project in workspace
  - Triggers Eclipse resource refresh
  - No additional parameters

#### IDE Integration
- **showInIde** - Open file in editor at specific location
  - Parameters: `file`, `line`, `column`
  - Handles headless mode gracefully

- **undo/redo** - Perform undo/redo operations
  - Parameters: `files` (array of file paths)
  - Returns: `performed` (boolean)

#### Project Analysis
- **getVaadinRoutes** - Find @Route annotated classes
  - Returns: Array of `{route, classname}`

- **getVaadinComponents** - Find Vaadin Component subclasses
  - Parameters: `includeMethods` (boolean)
  - Returns: Array of component information

- **getVaadinEntities** - Find JPA @Entity classes
  - Parameters: `includeMethods` (boolean)
  - Returns: Array of entity information

- **getVaadinSecurity** - Find Spring Security configurations
  - Returns: `security` configs and `userDetails` services

- **getVaadinVersion** - Detect Vaadin version from classpath
  - Scans JAR files for version information
  - Returns: `version` string

- **getModulePaths** - Get project structure information
  - Returns comprehensive module path data
  - Includes source, test, resource, and output paths

#### Build & Execution
- **compileFiles** - Trigger incremental build
  - Parameters: `files` (array)
  - Uses Eclipse's build system

- **restartApplication** - Restart launch configurations
  - Parameters: `mainClass` (optional)
  - Integrates with Eclipse Debug framework

- **reloadMavenModule** - Refresh Maven modules
  - Parameters: `moduleName` (optional)
  - Triggers workspace refresh

#### Utility
- **heartbeat** - Service health check
  - Returns: `status`, `version`, `ide`

## Project Structure

```
eclipse-plugin/
├── pom.xml                                 # Parent POM (Tycho-based)
├── vaadin-eclipse-plugin-main/            # Main plugin module
│   ├── META-INF/
│   │   └── MANIFEST.MF                    # OSGi bundle manifest
│   ├── plugin.xml                         # Eclipse plugin descriptor
│   ├── pom.xml                           # Plugin module POM
│   └── src/com/vaadin/plugin/
│       ├── Activator.java                # Plugin lifecycle
│       ├── CopilotClient.java            # REST client for testing
│       ├── CopilotRestService.java       # Main REST service
│       ├── CopilotUndoManager.java       # Undo/redo management
│       ├── CopilotUtil.java              # Utility functions
│       ├── Message.java                  # Request/response DTOs
│       └── VaadinProjectAnalyzer.java    # Project analysis
└── vaadin-eclipse-plugin.tests/          # Test fragment
    ├── META-INF/
    │   └── MANIFEST.MF                   # Test fragment manifest
    ├── pom.xml                           # Test module POM
    └── src/com/vaadin/plugin/test/
        ├── AllTests.java                 # Test suite
        ├── BaseIntegrationTest.java     # Test base class
        ├── CopilotClientIntegrationTest.java
        ├── CopilotRestServiceIntegrationTest.java
        └── CopilotUtilTest.java

```

## Build System

### Technology Stack
- **Build Tool**: Maven with Tycho for OSGi/Eclipse plugin development
- **Java Version**: JavaSE-17
- **Eclipse Target**: 2024-03 release
- **Testing**: JUnit 4 with Tycho Surefire for OSGi testing

### Key Dependencies
- `org.eclipse.core.resources` - Workspace and resource management
- `org.eclipse.jdt.core` - Java development tools
- `org.eclipse.debug.core` - Launch configuration support
- `org.eclipse.ui.*` - User interface integration
- `com.google.gson` - JSON serialization

### Build Commands
```bash
# Format code (ALWAYS run before committing)
mvn spotless:apply

# Check code formatting without applying changes
mvn spotless:check

# Compile only
mvn clean compile

# Run tests
mvn clean install

# Skip tests
mvn clean install -DskipTests
```

## Testing

### Test Coverage
- **CopilotRestServiceIntegrationTest** (9 tests)
  - Tests REST endpoints with real workspace operations
  - Creates temporary Eclipse projects
  - Verifies file operations and API responses

- **CopilotClientIntegrationTest** (14 tests)
  - Tests client-server communication
  - Validates all endpoint contracts
  - Error handling scenarios

- **CopilotUtilTest** (6 tests)
  - Dotfile creation and management
  - Service name generation
  - Path handling

### Test Execution
Tests run in a headless Eclipse environment (OSGi runtime) with:
- Temporary workspace creation
- Isolated project setup
- Automatic cleanup

## Implementation Status

### ✅ Completed Features
- All core REST endpoints implemented
- File operations with undo/redo support
- Binary file handling with proper base64 encoding/decoding for undo/redo
- Project analysis for Vaadin elements
- Eclipse IDE integration
- Launch configuration management
- Maven module refresh
- Comprehensive integration tests
- Multi-module Tycho build setup
- Test coverage for VaadinProjectAnalyzer, CopilotUndoManager, and advanced endpoints
- Binary file undo/redo tests

### ⚠️ Limitations vs IntelliJ Plugin
1. **Architecture**: Monolithic service vs handler-based architecture
2. **Undo/Redo**: Basic implementation vs advanced batching system
3. **Compilation**: Simple build trigger vs status tracking service
4. **Analysis**: Basic annotation scanning vs advanced endpoint discovery
5. **Events**: No project lifecycle listeners
6. **Hilla**: No Hilla endpoint support

### 🔴 Missing Test Coverage
- VaadinProjectAnalyzer functionality
- CopilotUndoManager operations
- Advanced endpoint implementations
- Launch configuration restart with real configs

## Development Workflow

### Adding New Endpoints

1. Add handler method in `CopilotRestService`:
```java
private String handleNewCommand(IProject project, JsonObject data) {
    // Implementation
    Map<String, Object> response = new HashMap<>();
    response.put("status", "ok");
    return gson.toJson(response);
}
```

2. Register in switch statement:
```java
case "newCommand":
    return handleNewCommand(project, data);
```

3. Add request DTO in `Message.java` if needed

4. Add integration test in `CopilotRestServiceIntegrationTest`

### Running in Eclipse IDE

1. Import as existing Maven project
2. Right-click plugin project → Run As → Eclipse Application
3. In runtime workbench, create/open a Vaadin project
4. Check console for "Copilot REST service started at..."
5. Verify dotfile creation in project's `.vaadin/copilot/`

### Debugging

- Service endpoint URL is logged to console on startup
- All requests/responses are logged
- Dotfiles created in `<project>/.vaadin/copilot/vaadin-copilot.properties`
- Test failures include full stack traces in surefire reports

## Known Issues

1. **Binary file undo/redo**: Fixed - Now properly handles base64 encoded content for binary files
2. **Eclipse IDE import**: Fixed - Main plugin exports packages via MANIFEST.MF for test fragment visibility
3. **Headless limitations**: Some UI operations don't work in test environment
4. **No operation batching**: Multiple rapid operations aren't grouped for undo
5. **Limited error recovery**: Some error conditions could be handled more gracefully
6. **Undo/redo in tests**: Operation history context not properly initialized in test environment, causing undo/redo tests to fail
7. **Java project classpath**: VaadinProjectAnalyzer tests fail due to classpath nesting issues in test environment

## Future Enhancements

### High Priority
1. Refactor to handler-based architecture
2. Add compilation status tracking service
3. Implement operation batching for undo/redo
4. Add comprehensive unit tests for analyzer and undo manager

### Medium Priority
1. Add project event listeners for automatic dotfile management
2. Implement Hilla endpoint discovery
3. Create centralized error handling service
4. Add VCS awareness for file operations

### Low Priority
1. Add user notification system
2. Improve UI integration (window focus, etc.)
3. Create preferences/configuration UI
4. Add performance monitoring

## References

- [Eclipse Plugin Development](https://www.eclipse.org/pde/)
- [Tycho Documentation](https://tycho.eclipseprojects.io/)
- [Eclipse JDT Core](https://www.eclipse.org/jdt/core/)
- [Vaadin Copilot](https://vaadin.com/docs/latest/tools/copilot)
- [IntelliJ Plugin Source](https://github.com/vaadin/intellij-plugin)

## License

This project follows the same licensing as the Vaadin framework. See LICENSE file for details.

## Contributors

- Initial implementation by Claude (AI assistant)
- Project structure and requirements provided by human developer
- Based on IntelliJ plugin architecture and functionality

## Development Practices

### Code Formatting
**IMPORTANT**: Always run `mvn spotless:apply` from the project root before committing code.
- This ensures consistent code formatting across the entire codebase
- The Spotless plugin is configured in the root pom.xml with Eclipse formatter settings
- Formatting includes import organization, trailing whitespace removal, and consistent indentation

### Automatic Commits
When using Claude as an AI assistant, commits are automatically created when:
- A task is completed and the project is in a working state
- Significant milestones are reached
- User explicitly requests a commit
- **Note**: `mvn spotless:apply` should be run before each commit

### Documentation Updates
This AGENTS.md file is automatically updated when:
- Significant changes are made to the project structure
- New features are implemented
- Important issues are identified or resolved
- Development practices change

### Code Verification
**CRITICAL**: Always verify changes by compiling the project after making modifications:
1. Run `mvn clean compile` to ensure the code compiles
2. Fix any compilation errors before committing
3. Run tests with `mvn clean install` when appropriate
4. Never commit code that doesn't compile