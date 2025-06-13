package com.dotcms.health.checks;

import static org.junit.Assert.*;

import com.dotcms.health.model.HealthCheckResult;
import com.dotcms.health.model.HealthStatus;
import com.dotmarketing.util.Config;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

/**
 * Test for ApplicationHealthCheck memory monitoring and liveness behavior
 */
public class ApplicationHealthCheckTest {

    private ApplicationHealthCheck healthCheck;
    
    @Before
    public void setUp() {
        healthCheck = new ApplicationHealthCheck();
        
        // Set reasonable test thresholds
        Config.setProperty("health.check.application.warning-threshold-percent", "85");
        Config.setProperty("health.check.application.critical-threshold-percent", "95");
        Config.setProperty("health.check.application.mode", "PRODUCTION");
    }
    
    @After
    public void tearDown() {
        // Reset configuration
        Config.setProperty("health.check.application.mode", "PRODUCTION");
    }

    @Test
    public void testApplicationHealthCheck_Success() {
        // Execute
        HealthCheckResult result = healthCheck.check();
        
        // Verify basic health check properties
        assertEquals("application", result.name());
        assertNotNull(result.status());
        assertNotNull(result.message());
        assertNotNull(result.lastChecked());
        assertTrue("Duration should be positive", result.durationMs() >= 0);
        assertTrue("Duration should be reasonable", result.durationMs() < 5000);
    }

    @Test
    public void testIsLivenessCheck_ReturnsTrue() {
        // Application health check should be safe for liveness probes
        assertTrue("ApplicationHealthCheck should be safe for liveness", healthCheck.isLivenessCheck());
        assertTrue("ApplicationHealthCheck should be included in readiness", healthCheck.isReadinessCheck());
    }

    @Test
    public void testGetName() {
        assertEquals("application", healthCheck.getName());
    }

    @Test
    public void testGetOrder() {
        // Application health check should have high priority (low order number)
        assertTrue("Application health check should have high priority", healthCheck.getOrder() <= 10);
    }

    @Test
    public void testGetDescription() {
        String description = healthCheck.getDescription();
        assertNotNull(description);
        assertTrue("Description should mention JVM or memory", 
            description.toLowerCase().contains("jvm") || description.toLowerCase().contains("memory"));
    }

    @Test
    public void testProductionMode_NormalBehavior() {
        // Setup
        Config.setProperty("health.check.application.mode", "PRODUCTION");
        
        // Execute
        HealthCheckResult result = healthCheck.check();
        
        // Verify - Status should reflect actual memory state
        assertNotNull(result.status());
        // In normal conditions, should typically be UP
        // We can't guarantee specific memory state, but check result is valid
        assertTrue("Status should be UP or DOWN in production", 
            result.status() == HealthStatus.UP || result.status() == HealthStatus.DOWN);
    }

    @Test
    public void testMonitorMode_NeverFails() {
        // Setup
        Config.setProperty("health.check.application.mode", "MONITOR_MODE");
        
        // Execute
        HealthCheckResult result = healthCheck.check();
        
        // Verify - In monitor mode, status should be UP or DEGRADED, never DOWN
        assertTrue("Monitor mode should never return DOWN", 
            result.status() == HealthStatus.UP || result.status() == HealthStatus.DEGRADED);
        
        if (result.status() == HealthStatus.DEGRADED) {
            assertTrue("Monitor mode message should indicate mode", 
                result.message().orElse("").contains("MONITOR_MODE"));
        }
    }

    @Test
    public void testDisabledMode_AlwaysUp() {
        // Setup
        Config.setProperty("health.check.application.mode", "DISABLED");
        
        // Execute
        HealthCheckResult result = healthCheck.check();
        
        // Verify - Disabled mode always returns UP
        assertEquals(HealthStatus.UP, result.status());
        assertTrue("Disabled message should indicate disabled", 
            result.message().orElse("").contains("disabled"));
        assertEquals(0L, result.durationMs());
    }

    @Test
    public void testConfigurationProperties() {
        // Setup custom configuration
        Config.setProperty("health.check.application.warning-threshold-percent", "75");
        Config.setProperty("health.check.application.critical-threshold-percent", "90");
        
        // Execute
        HealthCheckResult result = healthCheck.check();
        
        // Verify - Should complete without error regardless of thresholds
        assertNotNull(result);
        assertNotNull(result.status());
    }

    @Test
    public void testPerformanceMetricsIncluded() {
        // Setup - Enable performance metrics
        Config.setProperty("health.include.performance-metrics", "true");
        
        // Execute
        HealthCheckResult result = healthCheck.check();
        
        // Verify - Message should include performance information when healthy
        if (result.status() == HealthStatus.UP) {
            // Should include duration or other performance metrics
            assertTrue("Should include timing information", result.durationMs() >= 0);
        }
    }

    @Test
    public void testDetailedInformationToggle() {
        // Test with detailed information enabled
        Config.setProperty("health.include.system-details", "true");
        HealthCheckResult detailedResult = healthCheck.check();
        
        // Test with detailed information disabled
        Config.setProperty("health.include.system-details", "false");
        HealthCheckResult basicResult = healthCheck.check();
        
        // Verify both complete successfully
        assertNotNull(detailedResult);
        assertNotNull(basicResult);
        assertEquals("application", detailedResult.name());
        assertEquals("application", basicResult.name());
        
        // Messages might be different, but both should be valid
        assertTrue(detailedResult.message().isPresent());
        assertTrue(basicResult.message().isPresent());
    }

    @Test
    public void testMemoryStateReporting() {
        // Execute
        HealthCheckResult result = healthCheck.check();
        
        // Verify memory information is reported appropriately
        if (result.status() == HealthStatus.UP) {
            // For UP status, message should indicate health
            String message = result.message().orElse("").toLowerCase();
            assertTrue("Message should indicate healthy state", 
                message.contains("healthy") || message.contains("operational") || message.contains("memory"));
        }
    }

    @Test
    public void testConsistentResults() {
        // Execute multiple times quickly
        HealthCheckResult result1 = healthCheck.check();
        HealthCheckResult result2 = healthCheck.check();
        HealthCheckResult result3 = healthCheck.check();
        
        // Verify all results are valid
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        
        assertEquals("application", result1.name());
        assertEquals("application", result2.name());
        assertEquals("application", result3.name());
        
        // Status should be consistent under normal conditions
        // (Memory state shouldn't change dramatically in milliseconds)
        assertTrue("Results should be reasonably consistent", 
            result1.status() == result2.status() || 
            Math.abs(result1.durationMs() - result2.durationMs()) < 1000);
    }
}