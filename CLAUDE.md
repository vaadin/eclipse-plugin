# Eclipse Plugin Build and Test Instructions

## Prerequisites
- Java 17 or higher
- Maven 3.8+
- Node.js (for certain build tasks)

## Project Structure
This is a Tycho-based Eclipse plugin project with the following modules:
- `vaadin-eclipse-plugin-main/` - Main plugin code
- `vaadin-eclipse-plugin.tests/` - Test plugin
- `vaadin-eclipse-plugin-site/` - Update site

## Building the Project

### Clean Build
```bash
mvn clean compile
```

### Package (without tests)
```bash
mvn clean package -DskipTests
```

### Full Build with Tests
```bash
mvn clean verify
```

### Install to Local Repository
```bash
mvn clean install
```

## Running Tests

### Run All Tests
```bash
mvn verify
```
Note: `mvn test` alone does not execute tests in Tycho projects. Use `mvn verify` instead.

### Run Specific Test Class
```bash
mvn verify -Dtest=ProjectModelTest
```

### Check Test Results
Test reports are generated in:
```
vaadin-eclipse-plugin.tests/target/surefire-reports/
```

## Common Issues

### Tycho/OSGi Build Notes
- This project uses Tycho for building Eclipse plugins
- Dependencies are managed through MANIFEST.MF files, not standard Maven dependencies
- Test execution requires `mvn verify` (not `mvn test`) due to OSGi runtime requirements

## Debugging Test Failures
1. Check the surefire reports for detailed error messages
2. Look for line numbers in stack traces to identify the exact failure point
3. Ensure all modules are built before running tests

## Code Style
- Follow existing code conventions in the project
- No comments unless specifically required
- Use existing patterns and libraries
- Run `mvn spotless:apply` to format the code

## JSON Communication Convention
- **IMPORTANT**: All JSON communication uses native OS file paths, not portable paths
- Use `toOSString()` when writing paths to JSON (not `toPortableString()`)
- On Windows: paths will have backslashes (e.g., `C:\Users\project`)
- On Unix/Mac: paths will have forward slashes (e.g., `/home/user/project`)
- When parsing JSON, backslashes are escaped in the raw JSON but parsed correctly
- This applies to all JSON files including `flow-build-info.json` and REST API responses

## Key Test Classes
- `ProjectModelTest` - Tests URL generation for project creation
- `CopilotUndoManagerTest` - Tests undo/redo functionality
- `VaadinProjectAnalyzerTest` - Tests project analysis features
