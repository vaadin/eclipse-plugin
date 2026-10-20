package com.vaadin.plugin.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.vaadin.plugin.CopilotUtil;

/**
 * Tests for CopilotUtil functionality.
 */
public class CopilotUtilTest extends BaseIntegrationTest {

	@Test
	public void testServiceName() {
		String serviceName = CopilotUtil.getServiceName();
		assertNotNull("Service name should not be null", serviceName);
		assertTrue("Service name should start with 'copilot-'", serviceName.startsWith("copilot-"));
		assertTrue("Service name should have UUID suffix", serviceName.length() > 8);

		// Service name should be consistent across calls
		String serviceName2 = CopilotUtil.getServiceName();
		assertEquals("Service name should be consistent", serviceName, serviceName2);
	}

	@Test
	public void testGetEndpoint() {
		int testPort = 8080;
		String endpoint = CopilotUtil.getEndpoint(testPort);

		assertNotNull("Endpoint should not be null", endpoint);
		assertTrue("Endpoint should start with http://127.0.0.1", endpoint.startsWith("http://127.0.0.1"));
		assertTrue("Endpoint should contain port", endpoint.contains(":" + testPort));
		assertTrue("Endpoint should contain service name", endpoint.contains(CopilotUtil.getServiceName()));
		assertTrue("Endpoint should contain /vaadin/", endpoint.contains("/vaadin/"));
	}

	@Test
	public void testGetSupportedActions() {
		String actions = CopilotUtil.getSupportedActions();

		assertNotNull("Supported actions should not be null", actions);
		assertTrue("Should contain write action", actions.contains("write"));
		assertTrue("Should contain writeBase64 action", actions.contains("writeBase64"));
		assertTrue("Should contain delete action", actions.contains("delete"));
		assertTrue("Should contain refresh action", actions.contains("refresh"));
		assertTrue("Should contain showInIde action", actions.contains("showInIde"));
		assertTrue("Should contain heartbeat action", actions.contains("heartbeat"));
		assertTrue("Should contain getVaadinRoutes action", actions.contains("getVaadinRoutes"));
		assertTrue("Should contain getVaadinVersion action", actions.contains("getVaadinVersion"));

		// Verify comma separation
		String[] actionArray = actions.split(",");
		assertTrue("Should have multiple actions", actionArray.length > 5);
	}
}
