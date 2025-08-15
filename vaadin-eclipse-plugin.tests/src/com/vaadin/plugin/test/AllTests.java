package com.vaadin.plugin.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test suite for all Vaadin Eclipse Plugin tests.
 */
@RunWith(Suite.class)
@SuiteClasses({CopilotRestServiceIntegrationTest.class, CopilotClientIntegrationTest.class, CopilotUtilTest.class,
		VaadinProjectAnalyzerTest.class, CopilotUndoManagerTest.class, AdvancedEndpointsTest.class,
		BinaryFileUndoRedoTest.class})
public class AllTests {
}
