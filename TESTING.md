# Vaadin Eclipse Plugin - Testing Guide

## Test Architecture

The plugin uses a comprehensive testing approach with integration tests that validate the complete REST API functionality against real Eclipse workspace operations.

## Test Organization

Tests are organized as Eclipse test fragments that run within the OSGi runtime environment. This ensures accurate testing of Eclipse-specific functionality including:

- Workspace and resource management
- Project lifecycle operations
- Editor integration
- Build system interactions

## Running Tests

### Maven Build
```bash
# Run all tests
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Run specific test class
mvn test -Dtest=TestClassName
```

### Eclipse IDE
1. Import project as existing Maven project
2. Right-click test class → Run As → JUnit Plug-in Test
3. Tests run in a separate Eclipse runtime workbench

### Manual Testing
For manual validation of REST endpoints:
1. Launch Eclipse with the plugin installed
2. Check console for REST service port
3. Use REST client tools to test endpoints
4. Verify operations in Eclipse workspace

### Running CI Workflow Locally with act

You can run the GitHub Actions CI workflow locally using the [act](https://github.com/nektos/act) tool. This simulates the CI build and test steps on your machine.

#### Install act
```bash
brew install act # macOS
# or see https://github.com/nektos/act for other platforms
```

#### Run the workflow
```bash
act pull_request -W .github/workflows/build.yml --artifact-server-path /tmp/artifacts --container-architecture linux/amd64
```

This command:
- Uses the workflow in `.github/workflows/build.yml`
- Stores workflow artifacts in `/tmp/artifacts`
- Uses the `linux/amd64` container architecture (recommended for Tycho/Eclipse builds)

**Note:**
- The act tool is for local simulation only; actual CI runs on GitHub Actions.

## Test Coverage Areas

### Core Functionality
- REST API endpoint validation
- File operations (create, read, update, delete)
- Binary file handling with Base64 encoding
- Directory structure creation
- Workspace synchronization

### Integration Points
- Eclipse resource API integration
- Java project analysis
- Build system triggers
- Editor operations
- Undo/redo framework

### Error Handling
- Invalid file paths
- Project boundary validation
- Malformed requests
- Concurrent operation safety
- Resource cleanup

## Test Best Practices

### Test Isolation
- Each test creates temporary projects
- Automatic cleanup after test completion
- No shared state between tests
- Random port allocation for REST service

### Validation Approach
- HTTP status code verification
- Response content validation
- File system state verification
- Eclipse workspace consistency checks
- Thread safety validation

### Performance Considerations
- Tests use in-memory workspace when possible
- Minimal disk I/O for faster execution
- Parallel test execution where applicable
- Resource pooling for repeated operations

## Continuous Integration

Tests are designed to run in headless environments:
- No UI dependencies for core tests
- Configurable workspace location
- Exit codes for build system integration
- XML test reports for CI tools

## Troubleshooting

### Common Issues
- **OSGi resolution failures**: Check MANIFEST.MF dependencies
- **Port conflicts**: Service uses dynamic port allocation
- **Workspace locks**: Ensure proper cleanup in tearDown
- **Permission errors**: Verify file system permissions

### Debug Options
```bash
# Enable verbose logging
mvn test -Dorg.eclipse.equinox.http.jetty.log.stderr.threshold=DEBUG

# Run with Eclipse console
-consoleLog -console
```

## Test Maintenance

### Adding New Tests
1. Extend appropriate base test class
2. Use existing test utilities
3. Follow naming conventions
4. Document test purpose
5. Ensure cleanup in tearDown

### Updating Tests
- Keep tests focused on behavior, not implementation
- Update tests when API contracts change
- Maintain backwards compatibility tests
- Document breaking changes

## Quality Metrics

Target metrics for test suite:
- Code coverage: >80% for core functionality
- Test execution time: <5 minutes for full suite
- Flakiness rate: <1% failure rate
- Clear failure messages for debugging

## Future Improvements

Planned enhancements to testing infrastructure:
- Mock framework for external dependencies
- Performance benchmarking suite
- Stress testing for concurrent operations
- Integration with mutation testing tools
- Automated UI testing with SWTBot