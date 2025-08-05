package com.dotcms.health.config;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for HealthEndpointConstants to ensure proper configuration 
 * and correct dependency on infrastructure constants.
 */
public class HealthEndpointConstantsTest {

    @Test
    public void testHealthEndpointsUseCorrectPrefix() {
        // Verify all health endpoints use the shared infrastructure prefix
        assertTrue("Liveness endpoint should start with /dotmgt", 
                  HealthEndpointConstants.Endpoints.LIVENESS.startsWith("/dotmgt"));
        assertTrue("Readiness endpoint should start with /dotmgt", 
                  HealthEndpointConstants.Endpoints.READINESS.startsWith("/dotmgt"));
        assertTrue("Health endpoint should start with /dotmgt", 
                  HealthEndpointConstants.Endpoints.HEALTH.startsWith("/dotmgt"));
    }

    @Test
    public void testSpecificEndpointPaths() {
        // Verify specific endpoint paths are as expected
        assertEquals("Liveness endpoint path incorrect", 
                    "/dotmgt/livez", HealthEndpointConstants.Endpoints.LIVENESS);
        assertEquals("Readiness endpoint path incorrect", 
                    "/dotmgt/readyz", HealthEndpointConstants.Endpoints.READINESS);
        assertEquals("Health endpoint path incorrect", 
                    "/dotmgt/health", HealthEndpointConstants.Endpoints.HEALTH);
    }

    @Test
    public void testSuffixConstants() {
        // Verify suffix constants eliminate magic strings
        assertEquals("Liveness suffix incorrect", "/livez", HealthEndpointConstants.Endpoints.LIVENESS_SUFFIX);
        assertEquals("Readiness suffix incorrect", "/readyz", HealthEndpointConstants.Endpoints.READINESS_SUFFIX);
        assertEquals("Health suffix incorrect", "/health", HealthEndpointConstants.Endpoints.HEALTH_SUFFIX);
        
        // Verify that full endpoints are correctly constructed from prefix + suffix
        assertEquals("Liveness endpoint should be constructed from prefix + suffix",
                    "/dotmgt" + HealthEndpointConstants.Endpoints.LIVENESS_SUFFIX,
                    HealthEndpointConstants.Endpoints.LIVENESS);
        assertEquals("Readiness endpoint should be constructed from prefix + suffix",
                    "/dotmgt" + HealthEndpointConstants.Endpoints.READINESS_SUFFIX,
                    HealthEndpointConstants.Endpoints.READINESS);
        assertEquals("Health endpoint should be constructed from prefix + suffix",
                    "/dotmgt" + HealthEndpointConstants.Endpoints.HEALTH_SUFFIX,
                    HealthEndpointConstants.Endpoints.HEALTH);
    }

    @Test
    public void testGetAllHealthEndpoints() {
        String[] endpoints = HealthEndpointConstants.getAllHealthEndpoints();
        
        // Verify correct number of endpoints
        assertEquals("Should have exactly 3 health endpoints", 3, endpoints.length);
        
        // Verify all expected endpoints are present
        boolean hasLiveness = false, hasReadiness = false, hasHealth = false;
        for (String endpoint : endpoints) {
            if (endpoint.equals(HealthEndpointConstants.Endpoints.LIVENESS)) hasLiveness = true;
            if (endpoint.equals(HealthEndpointConstants.Endpoints.READINESS)) hasReadiness = true;
            if (endpoint.equals(HealthEndpointConstants.Endpoints.HEALTH)) hasHealth = true;
        }
        
        assertTrue("Should contain liveness endpoint", hasLiveness);
        assertTrue("Should contain readiness endpoint", hasReadiness);
        assertTrue("Should contain health endpoint", hasHealth);
    }

    @Test
    public void testResponseConstants() {
        // Verify response constants are defined
        assertNotNull("Alive response should be defined", 
                     HealthEndpointConstants.Responses.ALIVE_RESPONSE);
        assertNotNull("Ready response should be defined", 
                     HealthEndpointConstants.Responses.READY_RESPONSE);
        assertNotNull("JSON content type should be defined", 
                     HealthEndpointConstants.Responses.CONTENT_TYPE_JSON);
        
        // Verify expected values
        assertEquals("Alive response value incorrect", 
                    "alive", HealthEndpointConstants.Responses.ALIVE_RESPONSE);
        assertEquals("Ready response value incorrect", 
                    "ready", HealthEndpointConstants.Responses.READY_RESPONSE);
        assertEquals("JSON content type incorrect", 
                    "application/json", HealthEndpointConstants.Responses.CONTENT_TYPE_JSON);
    }
} 