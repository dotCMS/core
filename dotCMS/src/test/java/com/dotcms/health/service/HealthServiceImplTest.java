package com.dotcms.health.service;

import com.dotcms.health.model.HealthCheckResult;
import com.dotcms.health.model.HealthStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HealthServiceImpl, focusing on the new convenience methods
 * for database and search service health checking.
 */
public class HealthServiceImplTest {

    private HealthServiceImpl healthService;
    
    @Mock
    private HealthStateManager mockHealthStateManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        healthService = new HealthServiceImpl();
        
        // Use reflection to set the mock health state manager
        try {
            java.lang.reflect.Field field = HealthServiceImpl.class.getDeclaredField("healthStateManager");
            field.setAccessible(true);
            field.set(healthService, mockHealthStateManager);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set mock health state manager", e);
        }
    }

    @Test
    public void testIsDatabaseHealthy_WhenDatabaseIsUp() {
        // Setup
        HealthCheckResult dbResult = HealthCheckResult.builder()
            .name("database")
            .status(HealthStatus.UP)
            .message("Database connection OK")
            .lastChecked(Instant.now())
            .durationMs(50L)
            .build();
            
        when(mockHealthStateManager.getHealthCheckResult("database"))
            .thenReturn(Optional.of(dbResult));

        // Execute & Verify
        assertTrue("Database should be healthy when status is UP", healthService.isDatabaseHealthy());
    }

    @Test
    public void testIsDatabaseHealthy_WhenDatabaseIsDown() {
        // Setup
        HealthCheckResult dbResult = HealthCheckResult.builder()
            .name("database")
            .status(HealthStatus.DOWN)
            .message("Database connection failed")
            .lastChecked(Instant.now())
            .durationMs(5000L)
            .build();
            
        when(mockHealthStateManager.getHealthCheckResult("database"))
            .thenReturn(Optional.of(dbResult));

        // Execute & Verify
        assertFalse("Database should not be healthy when status is DOWN", healthService.isDatabaseHealthy());
    }

    @Test
    public void testIsDatabaseHealthy_WhenDatabaseCheckNotFound() {
        // Setup
        when(mockHealthStateManager.getHealthCheckResult("database"))
            .thenReturn(Optional.empty());

        // Execute & Verify
        assertFalse("Database should not be healthy when check is not found", healthService.isDatabaseHealthy());
    }

    @Test
    public void testIsSearchServiceHealthy_WhenElasticsearchIsUp() {
        // Setup
        HealthCheckResult esResult = HealthCheckResult.builder()
            .name("elasticsearch")
            .status(HealthStatus.UP)
            .message("Elasticsearch API available")
            .lastChecked(Instant.now())
            .durationMs(100L)
            .build();
            
        when(mockHealthStateManager.getHealthCheckResult("elasticsearch"))
            .thenReturn(Optional.of(esResult));

        // Execute & Verify
        assertTrue("Search service should be healthy when Elasticsearch is UP", healthService.isSearchServiceHealthy());
    }

    @Test
    public void testIsSearchServiceHealthy_WhenElasticsearchIsDegraded() {
        // Setup
        HealthCheckResult esResult = HealthCheckResult.builder()
            .name("elasticsearch")
            .status(HealthStatus.DEGRADED)
            .message("Elasticsearch responding slowly")
            .lastChecked(Instant.now())
            .durationMs(3000L)
            .build();
            
        when(mockHealthStateManager.getHealthCheckResult("elasticsearch"))
            .thenReturn(Optional.of(esResult));

        // Execute & Verify
        assertFalse("Search service should not be healthy when Elasticsearch is DEGRADED", healthService.isSearchServiceHealthy());
    }

    @Test
    public void testGetDatabaseHealth_ReturnsCorrectResult() {
        // Setup
        HealthCheckResult dbResult = HealthCheckResult.builder()
            .name("database")
            .status(HealthStatus.UP)
            .message("Database connection OK")
            .lastChecked(Instant.now())
            .durationMs(75L)
            .build();
            
        when(mockHealthStateManager.getHealthCheckResult("database"))
            .thenReturn(Optional.of(dbResult));

        // Execute
        Optional<HealthCheckResult> result = healthService.getDatabaseHealth();

        // Verify
        assertTrue("Database health result should be present", result.isPresent());
        assertEquals("Database health status should match", HealthStatus.UP, result.get().status());
        assertEquals("Database health name should match", "database", result.get().name());
    }

    @Test
    public void testGetSearchServiceHealth_ReturnsCorrectResult() {
        // Setup
        HealthCheckResult esResult = HealthCheckResult.builder()
            .name("elasticsearch")
            .status(HealthStatus.DOWN)
            .message("Elasticsearch cluster unreachable")
            .lastChecked(Instant.now())
            .durationMs(5000L)
            .build();
            
        when(mockHealthStateManager.getHealthCheckResult("elasticsearch"))
            .thenReturn(Optional.of(esResult));

        // Execute
        Optional<HealthCheckResult> result = healthService.getSearchServiceHealth();

        // Verify
        assertTrue("Search service health result should be present", result.isPresent());
        assertEquals("Search service health status should match", HealthStatus.DOWN, result.get().status());
        assertEquals("Search service health name should match", "elasticsearch", result.get().name());
    }

    @Test
    public void testRefreshAndCheckDatabaseHealth_WithBlocking() {
        // Setup
        HealthCheckResult dbResult = HealthCheckResult.builder()
            .name("database")
            .status(HealthStatus.UP)
            .message("Database connection OK after refresh")
            .lastChecked(Instant.now())
            .durationMs(200L)
            .build();
            
        when(mockHealthStateManager.getHealthCheckResult("database"))
            .thenReturn(Optional.of(dbResult));

        // Execute
        boolean result = healthService.refreshAndCheckDatabaseHealth(true);

        // Verify
        assertTrue("Database should be healthy after refresh", result);
        verify(mockHealthStateManager).forceRefreshHealthCheck("database", true);
        verify(mockHealthStateManager).getHealthCheckResult("database");
    }

    @Test
    public void testRefreshAndCheckSearchServiceHealth_WithoutBlocking() {
        // Setup
        HealthCheckResult esResult = HealthCheckResult.builder()
            .name("elasticsearch")
            .status(HealthStatus.DEGRADED)
            .message("Elasticsearch partially available")
            .lastChecked(Instant.now())
            .durationMs(1500L)
            .build();
            
        when(mockHealthStateManager.getHealthCheckResult("elasticsearch"))
            .thenReturn(Optional.of(esResult));

        // Execute
        boolean result = healthService.refreshAndCheckSearchServiceHealth(false);

        // Verify
        assertFalse("Search service should not be healthy when DEGRADED", result);
        verify(mockHealthStateManager).forceRefreshHealthCheck("elasticsearch", false);
        verify(mockHealthStateManager).getHealthCheckResult("elasticsearch");
    }
}