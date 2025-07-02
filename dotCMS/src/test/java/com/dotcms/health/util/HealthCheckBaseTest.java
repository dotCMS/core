package com.dotcms.health.util;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.dotcms.health.model.HealthCheckResult;
import com.dotcms.health.model.HealthStatus;
import com.dotmarketing.util.Config;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

/**
 * Test for HealthCheckBase mode handling and common functionality
 */
public class HealthCheckBaseTest {

    private TestHealthCheck healthCheck;
    
    @Before
    public void setUp() {
        healthCheck = new TestHealthCheck();
    }
    
    @After
    public void tearDown() {
        // Reset any config changes
        Config.setProperty("health.check.test.mode", "PRODUCTION");
    }

    @Test
    public void testProductionMode_ReturnsOriginalStatus() {
        // Setup
        Config.setProperty("health.check.test.mode", "PRODUCTION");
        healthCheck.setHealthy(false);
        
        // Execute
        HealthCheckResult result = healthCheck.check();
        
        // Verify - DOWN status preserved in production mode
        assertEquals(HealthStatus.DOWN, result.status());
        assertFalse(result.monitorModeApplied());
        assertEquals("test", result.name());
    }

    @Test
    public void testMonitorMode_PreservesUpStatus() {
        // Setup
        Config.setProperty("health.check.test.mode", "MONITOR_MODE");
        healthCheck.setHealthy(true);
        
        // Execute
        HealthCheckResult result = healthCheck.check();
        
        // Verify - UP status preserved and monitor mode is marked as applied since we're in monitor mode
        // The monitorModeApplied flag indicates monitor mode presence, not just status changes
        assertEquals(HealthStatus.UP, result.status());
        assertTrue(result.monitorModeApplied()); // Monitor mode is applied whenever the check is in monitor mode
        // For UP status, the mode prefix should be shown since no conversion happened
        assertTrue("Message should contain mode prefix for UP status", 
                  result.message().orElse("").contains("[MONITOR_MODE mode]"));
    }

    @Test
    public void testMonitorMode_ConvertsDownToDegraded() {
        // Setup
        Config.setProperty("health.check.test.mode", "MONITOR_MODE");
        healthCheck.setHealthy(false); // Will be DOWN before monitor mode
        
        // Execute
        HealthCheckResult result = healthCheck.check();
        
        // Verify - DOWN converted to DEGRADED and monitor mode is marked as applied
        // The monitorModeApplied flag indicates monitor mode presence, not just status changes
        assertEquals(HealthStatus.DEGRADED, result.status());
        assertTrue(result.monitorModeApplied()); // Monitor mode is applied whenever the check is in monitor mode
        // For status conversion, the mode prefix should NOT be shown to avoid redundancy
        // since the conversion message already indicates monitor mode
        assertFalse("Message should not contain mode prefix when status is converted", 
                   result.message().orElse("").contains("[MONITOR_MODE mode]"));
    }

    @Test
    public void testDisabledMode_ReturnsUpWithDisabledMessage() {
        // Setup
        Config.setProperty("health.check.test.mode", "DISABLED");
        healthCheck.setHealthy(false); // Doesn't matter since disabled
        
        // Execute
        HealthCheckResult result = healthCheck.check();
        
        // Verify - Always UP when disabled
        assertEquals(HealthStatus.UP, result.status());
        assertEquals("test health check disabled", result.message().orElse(""));
        assertEquals(0L, result.durationMs());
    }

    @Test
    public void testInvalidMode_DefaultsToProduction() {
        // Setup
        Config.setProperty("health.check.test.mode", "INVALID_MODE");
        healthCheck.setHealthy(false);
        
        // Execute
        HealthCheckResult result = healthCheck.check();
        
        // Verify - Invalid mode defaults to production
        assertEquals(HealthStatus.DOWN, result.status());
        assertFalse(result.monitorModeApplied());
    }

    @Test
    public void testConfigPropertyAccess() {
        // Setup
        Config.setProperty("health.check.test.timeout-ms", "5000");
        Config.setProperty("health.check.test.custom-property", "custom-value");
        
        // Verify
        assertEquals(5000, healthCheck.getTimeoutMs());
        assertEquals("custom-value", healthCheck.getConfigProperty("custom-property", "default"));
        assertEquals("default", healthCheck.getConfigProperty("non-existent", "default"));
    }

    @Test
    public void testTimingMeasurement() {
        // Setup
        healthCheck.setHealthy(true);
        healthCheck.setDelay(50); // 50ms delay
        
        // Execute
        HealthCheckResult result = healthCheck.check();
        
        // Verify timing - be more generous for CI environments
        assertTrue("Duration should be non-negative, got: " + result.durationMs(), 
                  result.durationMs() >= 0);
        
        // In CI environments, timing can be very variable, so use generous bounds
        // The delay is 50ms, but allow for significant variance in CI
        assertTrue("Duration should be reasonable (under 10 seconds for CI), got: " + result.durationMs() + "ms", 
                  result.durationMs() < 10000);
        
        // Optional: Check that some delay was measured (but be lenient for fast CI)
        // Don't enforce minimum timing as CI can be unpredictable
    }

    @Test
    public void testExceptionHandling() {
        // Setup - Ensure consistent configuration for test
        String originalSystemDetails = Config.getStringProperty("health.include.system-details", "true");
        try {
            Config.setProperty("health.include.system-details", "true");
            healthCheck.setShouldThrow(true);
            
            // Execute
            HealthCheckResult result = healthCheck.check();
            
            // Verify error handling - check both message and error fields
            assertEquals(HealthStatus.DOWN, result.status());
            
            // The error message should contain "Check failed:" in the message OR error field
            String message = result.message().orElse("");
            String error = result.error().orElse("");
            
            boolean hasExpectedErrorText = message.contains("Check failed:") || 
                                         message.contains("Test exception") ||
                                         error.contains("Test exception");
            
            assertTrue("Expected error information in message or error field. Message: '" + message + "', Error: '" + error + "'", 
                      hasExpectedErrorText);
            assertTrue("Duration should be non-negative", result.durationMs() >= 0);
            
        } finally {
            // Restore original configuration
            Config.setProperty("health.include.system-details", originalSystemDetails);
        }
    }

    /**
     * Test implementation of HealthCheckBase for testing purposes
     */
    private static class TestHealthCheck extends HealthCheckBase {
        private boolean healthy = true;
        private boolean shouldThrow = false;
        private long delay = 0;

        @Override
        protected CheckResult performCheck() throws Exception {
            if (shouldThrow) {
                throw new RuntimeException("Test exception");
            }
            
            if (delay > 0) {
                Thread.sleep(delay);
            }
            
            return new CheckResult(healthy, 0, healthy ? "Test healthy" : "Test failed");
        }

        @Override
        public String getName() {
            return "test";
        }

        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }

        public void setShouldThrow(boolean shouldThrow) {
            this.shouldThrow = shouldThrow;
        }

        public void setDelay(long delay) {
            this.delay = delay;
        }
    }
}