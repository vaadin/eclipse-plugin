# Vaadin Eclipse Plugin - Copilot Integration

## Project Overview

Eclipse plugin providing Copilot integration for Vaadin projects through a REST API interface. The plugin starts an embedded HTTP server that communicates with Vaadin Copilot for AI-assisted development.

## REST API Endpoints

All endpoints receive POST requests with JSON containing:
- `command`: The operation to perform
- `projectBasePath`: The project's file system path
- `data`: Command-specific parameters

### File Operations
- **write** - Write text content (params: `file`, `content`, `undoLabel`)
- **writeBase64** - Write binary content as base64 (params: `file`, `content`, `undoLabel`)
- **delete** - Delete files with undo tracking (params: `file`)
- **refresh** - Refresh project in workspace

### IDE Integration
- **showInIde** - Open file in editor (params: `file`, `line`, `column`)
- **undo/redo** - Perform undo/redo (params: `files` array, returns: `performed`)

### Project Analysis
- **getVaadinRoutes** - Find @Route annotated classes
- **getVaadinComponents** - Find Vaadin Component subclasses (params: `includeMethods`)
- **getVaadinEntities** - Find JPA @Entity classes (params: `includeMethods`)
- **getVaadinSecurity** - Find Spring Security configurations
- **getVaadinVersion** - Detect Vaadin version from classpath
- **getModulePaths** - Get project structure information

### Build & Execution
- **compileFiles** - Trigger incremental build (params: `files` array)
- **restartApplication** - Restart launch configurations (params: `mainClass` optional)
- **reloadMavenModule** - Refresh Maven modules (params: `moduleName` optional)
- **heartbeat** - Service health check

## Build Commands

```bash
# Format code (REQUIRED before committing)
mvn spotless:apply

# Compile only
mvn clean compile

# Run tests
mvn clean install

# Skip tests
mvn clean install -DskipTests
```

## Implementation Status

### ✅ Completed
- All core REST endpoints
- File operations with undo/redo support
- Binary file handling with base64 encoding
- Project analysis for Vaadin elements
- Eclipse IDE integration
- Comprehensive integration tests
- New Vaadin Project wizard
- Hotswap Agent support with JetBrains Runtime
- Build-time flow-build-info.json generation

### ⚠️ Limitations vs IntelliJ Plugin
1. **Architecture**: Monolithic switch-based handler vs individual handler classes
2. **Undo/Redo**: Simple per-file tracking vs IntelliJ's UndoManager integration
3. **Compilation**: No error tracking service (IntelliJ has CompilationStatusManagerService)
4. **Endpoint Discovery**: Basic annotation scanning vs microservices framework integration
5. **Project Events**: Basic open/close listeners vs comprehensive ProjectManagerListener

## Known Issues

1. **Headless limitations**: Some UI operations don't work in test environment
2. **No operation batching**: Multiple rapid operations aren't grouped for undo
3. **Undo/redo in tests**: Operation history context not properly initialized in test environment
4. **Java project classpath**: VaadinProjectAnalyzer tests fail due to classpath nesting issues

## Testing

### Test Coverage
- **CopilotRestServiceIntegrationTest** - REST endpoints with real workspace operations
- **CopilotClientIntegrationTest** - Client-server communication and endpoint contracts
- **VaadinProjectAnalyzerTest** - Project analysis functionality
- **NewVaadinProjectWizardTest** - Project creation wizard

Tests run in headless Eclipse environment with temporary workspace creation and automatic cleanup.

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

## Technical Notes

- **Port**: Dynamically assigned, communicated via dotfiles in `.vaadin/copilot/`
- **Java Version**: JavaSE-17
- **Eclipse Target**: 2024-03 release
- **Key Dependencies**: Eclipse JDT, Debug Core, Gson
- **Builder**: VaadinBuildParticipant automatically generates flow-build-info.json for projects with Vaadin dependencies
- **Hotswap**: Requires JetBrains Runtime for hot code replacement support