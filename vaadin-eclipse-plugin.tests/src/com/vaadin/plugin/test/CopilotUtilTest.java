package com.vaadin.plugin.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

import com.vaadin.plugin.CopilotUtil;

/**
 * Tests for CopilotUtil functionality including dotfile creation.
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
    
    @Test
    public void testSaveDotFile() throws IOException {
        String projectPath = testProject.getLocation().toString();
        int testPort = 9090;
        
        // Save dotfile
        CopilotUtil.saveDotFile(projectPath, testPort);
        
        // Verify dotfile was created
        File dotFile = new File(projectPath, ".vaadin/copilot/vaadin-copilot.properties");
        assertTrue("Dotfile should exist", dotFile.exists());
        assertTrue("Dotfile should be a file", dotFile.isFile());
        
        // Verify dotfile contents
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(dotFile)) {
            props.load(fis);
        }
        
        // Check required properties
        String endpoint = props.getProperty("endpoint");
        assertNotNull("Endpoint property should exist", endpoint);
        assertTrue("Endpoint should contain correct port", endpoint.contains(":" + testPort));
        assertTrue("Endpoint should contain service name", endpoint.contains(CopilotUtil.getServiceName()));
        
        String ide = props.getProperty("ide");
        assertEquals("IDE should be eclipse", "eclipse", ide);
        
        String version = props.getProperty("version");
        assertNotNull("Version should exist", version);
        
        String supportedActions = props.getProperty("supportedActions");
        assertNotNull("Supported actions should exist", supportedActions);
        assertTrue("Supported actions should contain write", supportedActions.contains("write"));
        
        // Verify parent directories were created
        File vaadinDir = new File(projectPath, ".vaadin");
        assertTrue("Vaadin directory should exist", vaadinDir.exists());
        assertTrue("Vaadin directory should be a directory", vaadinDir.isDirectory());
        
        File copilotDir = new File(vaadinDir, "copilot");
        assertTrue("Copilot directory should exist", copilotDir.exists());
        assertTrue("Copilot directory should be a directory", copilotDir.isDirectory());
    }
    
    @Test
    public void testSaveDotFileOverwrite() throws IOException {
        String projectPath = testProject.getLocation().toString();
        int testPort1 = 7070;
        int testPort2 = 8080;
        
        // Save dotfile first time
        CopilotUtil.saveDotFile(projectPath, testPort1);
        
        File dotFile = new File(projectPath, ".vaadin/copilot/vaadin-copilot.properties");
        assertTrue("Dotfile should exist after first save", dotFile.exists());
        
        // Verify first port
        Properties props1 = new Properties();
        try (FileInputStream fis = new FileInputStream(dotFile)) {
            props1.load(fis);
        }
        String endpoint1 = props1.getProperty("endpoint");
        assertTrue("First endpoint should contain first port", endpoint1.contains(":" + testPort1));
        
        // Save dotfile second time with different port
        CopilotUtil.saveDotFile(projectPath, testPort2);
        
        // Verify file was overwritten
        Properties props2 = new Properties();
        try (FileInputStream fis = new FileInputStream(dotFile)) {
            props2.load(fis);
        }
        String endpoint2 = props2.getProperty("endpoint");
        assertTrue("Second endpoint should contain second port", endpoint2.contains(":" + testPort2));
        assertFalse("Second endpoint should not contain first port", endpoint2.contains(":" + testPort1));
    }
    
    @Test
    public void testSaveDotFileWithInvalidPath() {
        // Test with path that doesn't exist - should not throw exception
        String invalidPath = "/this/path/does/not/exist";
        
        try {
            CopilotUtil.saveDotFile(invalidPath, 8080);
            // Should not throw exception, just log error
        } catch (Exception e) {
            fail("Should not throw exception for invalid path: " + e.getMessage());
        }
    }
}