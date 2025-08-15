package com.vaadin.plugin.test;

import org.junit.Test;
import static org.junit.Assert.fail;

/**
 * Simple test to verify test execution works.
 */
public class SimpleTest {
    
    @Test
    public void testFail() {
        fail("This test should intentionally fail to verify test execution works");
    }
    
    @Test 
    public void testPass() {
        // This should pass
        assert true;
    }
}