# Test Coverage Improvement Plan

## Recent Progress
- ✅ **VaadinBuilderConfiguratorTest**: Created comprehensive integration tests (6/6 passing, 100% success rate)
  - Tests builder addition/removal, flow-build-info.json generation, duplicate prevention
  - Properly initializes plugin through EarlyStartup for realistic testing
  - Runs successfully in headless Eclipse environment
  - Fixed VaadinBuildParticipant.hasVaadinDependency() to check only filename, not full path

# Test Coverage Improvement Plan

## Current Test Coverage Analysis

### Test Quality Summary
- **Total Test Files**: 11
- **Total Test Methods**: ~95
- **High Quality Tests**: 7 files (73%)
- **Medium Quality Tests**: 2 files (21%)
- **Missing Critical Coverage**: 2 major components (0% coverage)

### Coverage by Component

| Component | Current Coverage | Status |
|-----------|-----------------|---------|
| CopilotRestService | Good | ✅ 12 integration tests |
| CopilotClient | Good | ✅ 14 integration tests |
| CopilotUndoManager | Good | ✅ 5 comprehensive tests |
| VaadinProjectAnalyzer | Excellent | ✅ 20 comprehensive tests |
| ProjectModel | Good | ✅ 11 tests |
| VaadinBuilderConfigurator | Excellent | ✅ 6 integration tests (100% pass) |
| CopilotDotfileManager | **None** | ❌ 0 tests |
| NewVaadinProjectWizard | Poor | ⚠️ Missing core functionality tests |
| CopilotUtil | Minimal | ⚠️ Only 4 basic tests |

## Critical Issues

### 1. Components with Zero Test Coverage
- **CopilotDotfileManager**: Manages `.vaadin/copilot/vaadin-copilot.properties`

### 2. Major Functionality Not Tested
- Project download from start.vaadin.com
- ZIP extraction and import
- Maven/Gradle project configuration
- Copilot auto-activation on Vaadin projects
- Multi-module project support

## Test Implementation Priority

### 🔴 HIGH PRIORITY (Must Fix)

#### 1. VaadinBuilderConfigurator Tests ✅ COMPLETED (100% pass rate)
**File**: `VaadinBuilderConfiguratorTest.java`
- [x] Test builder addition to Java projects
- [x] Test builder removal functionality
- [x] Test non-Vaadin project handling (builder added but no flow-build-info)
- [x] Test Vaadin project handling (flow-build-info.json created)
- [x] Test duplicate builder prevention
- [x] Test manual builder configuration
- [x] Test that builder is not added to non-Java projects
- [x] Fixed hasVaadinDependency() to check only JAR filename, not full path

#### 2. CopilotDotfileManager Tests
**File**: `CopilotDotfileManagerTest.java`
- [ ] Test dotfile creation with correct properties
- [ ] Test dotfile update when port changes
- [ ] Test dotfile deletion on deactivation
- [ ] Test file watching for external changes
- [ ] Test concurrent access handling
- [ ] Test invalid file handling

#### 3. NewVaadinProjectWizard Real Integration Tests
**File**: `NewVaadinProjectWizardIntegrationTest.java`
- [ ] Test actual project download from start.vaadin.com
- [ ] Test ZIP extraction to workspace
- [ ] Test Maven project import and configuration
- [ ] Test Gradle project import and configuration
- [ ] Test project type selection (STARTER, EMPTY, etc.)
- [ ] Test tech stack selection
- [ ] Test error handling for network failures
- [ ] Test cancellation during download

### 🟡 MEDIUM PRIORITY (Should Fix)

#### 4. Enhanced CopilotUtil Tests
**File**: Update `CopilotUtilTest.java`
- [ ] Test port allocation and management
- [ ] Test concurrent service name generation
- [ ] Test dotfile save with various project paths
- [ ] Test error handling in dotfile creation
- [ ] Test special characters in paths
- [ ] Test supported actions list completeness

#### 5. Maven/Gradle Import Tests
**File**: `ProjectImportTest.java`
- [ ] Test Maven project import with dependencies
- [ ] Test Gradle project import with dependencies
- [ ] Test multi-module Maven project
- [ ] Test multi-module Gradle project
- [ ] Test dependency resolution
- [ ] Test build file modifications

#### 6. Copilot Lifecycle Tests
**File**: `CopilotLifecycleTest.java`
- [ ] Test service start/stop sequence
- [ ] Test port conflict resolution
- [ ] Test multiple concurrent instances
- [ ] Test Eclipse restart with active Copilot
- [ ] Test project deletion with active Copilot
- [ ] Test workspace switch

### 🟢 LOW PRIORITY (Nice to Have)

#### 7. Performance Tests
**File**: `PerformanceTest.java`
- [ ] Test REST service response times
- [ ] Test large file handling
- [ ] Test many concurrent requests
- [ ] Test memory usage under load
- [ ] Test project analysis performance

#### 8. UI Automation Tests
**File**: `UIAutomationTest.java`
- [ ] Test wizard UI flow end-to-end
- [ ] Test project selection dialog
- [ ] Test error dialog display
- [ ] Test progress monitoring
- [ ] Test cancellation UI

#### 9. Edge Cases and Error Handling
**File**: `EdgeCaseTest.java`
- [ ] Test corrupted project files
- [ ] Test missing dependencies
- [ ] Test invalid project structures
- [ ] Test permission issues
- [ ] Test disk space issues

## Implementation Guidelines

### Test Structure
```java
@Test
public void testFeatureName_WhenCondition_ShouldExpectedBehavior() {
    // Given - Setup test data and environment
    
    // When - Execute the feature
    
    // Then - Assert expected outcomes
}
```

### Mock vs Integration Tests
- **Use Mocks**: For external dependencies (network, file system)
- **Use Integration Tests**: For core functionality validation
- **Use Both**: Start with integration, add unit tests for edge cases

### Test Data Management
- Use temporary directories for file operations
- Clean up resources in `@After` methods
- Use unique names to avoid conflicts
- Provide realistic test data

## Metrics Goals

### Target Coverage
- **Line Coverage**: > 80%
- **Branch Coverage**: > 75%
- **Critical Path Coverage**: 100%

### Test Execution
- **Unit Tests**: < 100ms per test
- **Integration Tests**: < 5s per test
- **Full Suite**: < 2 minutes

## Test Categories

### Unit Tests (Fast, Isolated)
- Model classes
- Utility functions
- Validators
- Parsers

### Integration Tests (Real Components)
- REST endpoints
- File operations
- Eclipse API integration
- Project operations

### End-to-End Tests (Full Flow)
- Complete wizard flow
- Project creation to Copilot activation
- Full undo/redo cycle

## Missing Test Infrastructure

### Required Test Utilities
- [ ] Mock start.vaadin.com server
- [ ] Test project templates
- [ ] Eclipse test workspace manager
- [ ] Async operation test helpers

### CI/CD Requirements
- [ ] Headless Eclipse test runner
- [ ] Test report generation
- [ ] Coverage report integration
- [ ] Failure notifications

## Completion Tracking

### Phase 1 (Week 1)
- [ ] VaadinBuilderConfigurator tests
- [ ] CopilotDotfileManager tests
- [ ] Basic project download test

### Phase 2 (Week 2)
- [ ] Complete wizard integration tests
- [ ] Enhanced CopilotUtil tests
- [ ] Maven/Gradle import tests

### Phase 3 (Week 3)
- [ ] Lifecycle tests
- [ ] Performance tests
- [ ] Edge case tests

## Notes

### Current Issues
1. `NewVaadinProjectWizardTest` only tests UI, not actual functionality
2. No tests verify the actual Vaadin project creation flow
3. Builder integration is completely untested
4. Dotfile management has no test coverage

### Technical Debt
- Mock-heavy tests in wizard package need real integration tests
- Missing test utilities for async operations
- No performance benchmarks established
- No stress testing for concurrent operations

### Dependencies
- Need test server for start.vaadin.com mock
- Need sample Vaadin projects for testing
- Need Eclipse test framework setup improvements

## Success Criteria

A test is considered complete when:
1. It tests actual functionality, not mocks
2. It has clear assertions
3. It handles both success and failure cases
4. It cleans up after itself
5. It runs reliably in CI/CD

## Related Documentation
- [Eclipse Testing Best Practices](https://wiki.eclipse.org/Eclipse_Plug-in_Development_FAQ#Testing)
- [Tycho Test Plugin Documentation](https://tycho.eclipseprojects.io/doc/latest/tycho-surefire-plugin/test-mojo.html)
- [JUnit Best Practices](https://junit.org/junit5/docs/current/user-guide/#best-practices)