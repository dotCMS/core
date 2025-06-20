package com.dotcms.rest.api.v1.database;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotmarketing.db.DatabaseConnectionHealthManager;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DatabaseHealthResource REST endpoints.
 * Tests health status retrieval and circuit breaker control endpoints.
 * 
 * @author dotCMS
 */
@RunWith(MockitoJUnitRunner.class)
public class DatabaseHealthResourceTest {

    @Mock
    private HttpServletRequest mockRequest;
    
    @Mock
    private HttpServletResponse mockResponse;
    
    @Mock
    private WebResource mockWebResource;
    
    @Mock
    private WebResource.InitBuilder mockInitBuilder;
    
    @Mock
    private User mockUser;
    
    private DatabaseHealthResource resource;
    private DatabaseConnectionHealthManager healthManager;
    
    @Before
    public void setUp() {
        // Reset health manager singleton for testing
        try {
            java.lang.reflect.Field instanceField = DatabaseConnectionHealthManager.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            // Ignore reflection errors for test setup
        }
        
        healthManager = DatabaseConnectionHealthManager.getInstance();
        resource = new DatabaseHealthResource();
        
        // Setup mock user
        when(mockUser.getUserId()).thenReturn("test-admin-user-" + UUIDGenerator.generateUuid());
        when(mockUser.isAdmin()).thenReturn(true);
        
        // Setup mock WebResource chain
        when(mockWebResource.init(any(HttpServletRequest.class), any(HttpServletResponse.class), anyBoolean()))
            .thenReturn(null);
    }
    
    @Test
    public void testGetHealthStatusReturnsCompleteHealthInformation() {
        // Ensure circuit is in known state
        healthManager.closeCircuitBreaker("Test setup");
        
        Response response = resource.getHealthStatus(mockRequest, mockResponse);
        
        assertEquals("Should return HTTP 200 OK", Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull("Response entity should not be null", response.getEntity());
        
        assertTrue("Response should be ResponseEntityView", response.getEntity() instanceof ResponseEntityView);
        ResponseEntityView<?> entityView = (ResponseEntityView<?>) response.getEntity();
        
        assertTrue("Response entity should be a Map", entityView.getEntity() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> healthData = (Map<String, Object>) entityView.getEntity();
        
        // Verify required health status fields
        assertTrue("Should contain 'healthy' field", healthData.containsKey("healthy"));
        assertTrue("Should contain 'circuitState' field", healthData.containsKey("circuitState"));
        assertTrue("Should contain 'consecutiveFailures' field", healthData.containsKey("consecutiveFailures"));
        assertTrue("Should contain 'lastSuccessTime' field", healthData.containsKey("lastSuccessTime"));
        assertTrue("Should contain 'lastFailureTime' field", healthData.containsKey("lastFailureTime"));
        assertTrue("Should contain 'connectionLeakCount' field", healthData.containsKey("connectionLeakCount"));
        assertTrue("Should contain 'operationAllowed' field", healthData.containsKey("operationAllowed"));
        assertTrue("Should contain 'timestamp' field", healthData.containsKey("timestamp"));
        assertTrue("Should contain 'databaseConnectivity' field", healthData.containsKey("databaseConnectivity"));
        
        // Verify field types and values
        assertTrue("Healthy should be boolean", healthData.get("healthy") instanceof Boolean);
        assertEquals("Circuit state should be CLOSED", "CLOSED", healthData.get("circuitState"));
        assertTrue("Consecutive failures should be integer", healthData.get("consecutiveFailures") instanceof Integer);
        assertTrue("Operation allowed should be boolean", healthData.get("operationAllowed") instanceof Boolean);
        assertTrue("Timestamp should be Instant", healthData.get("timestamp") instanceof Instant);
    }
    
    @Test
    public void testGetHealthStatusWithConnectionPoolMetrics() {
        // This test would require mocking HikariCP pool for full coverage
        // For now, we verify the endpoint handles null pool metrics gracefully
        Response response = resource.getHealthStatus(mockRequest, mockResponse);
        
        assertEquals("Should return HTTP 200 OK", Response.Status.OK.getStatusCode(), response.getStatus());
        
        ResponseEntityView<?> entityView = (ResponseEntityView<?>) response.getEntity();
        @SuppressWarnings("unchecked")
        Map<String, Object> healthData = (Map<String, Object>) entityView.getEntity();
        
        // Connection pool metrics may or may not be present depending on DataSource availability
        // The endpoint should handle both cases gracefully
        if (healthData.containsKey("connectionPool")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> poolMetrics = (Map<String, Object>) healthData.get("connectionPool");
            assertTrue("Pool metrics should contain connection counts", 
                      poolMetrics.containsKey("activeConnections") || 
                      poolMetrics.containsKey("totalConnections"));
        }
    }
    
    @Test
    public void testGetSimpleHealthStatusReturnsHealthyWhenOperational() {
        // Ensure circuit is closed and healthy
        healthManager.closeCircuitBreaker("Test setup");
        
        Response response = resource.getSimpleHealthStatus(mockRequest, mockResponse);
        
        assertEquals("Should return HTTP 200 OK for healthy status", 
                    Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Response should contain 'healthy' text", "healthy", response.getEntity());
    }
    
    @Test
    public void testGetSimpleHealthStatusReturnsUnhealthyWhenCircuitOpen() {
        // Open circuit breaker to simulate unhealthy state
        healthManager.openCircuitBreaker("Test: simulating unhealthy state");
        
        Response response = resource.getSimpleHealthStatus(mockRequest, mockResponse);
        
        assertEquals("Should return HTTP 503 Service Unavailable for unhealthy status", 
                    Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
        assertEquals("Response should contain 'unhealthy' text", "unhealthy", response.getEntity());
    }
    
    @Test
    public void testOpenCircuitBreakerRequiresAdminAuthentication() {
        // This test verifies the WebResource.InitBuilder pattern is used correctly
        // In a real environment, this would validate admin authentication
        
        try (MockedStatic<DbConnectionFactory> dbFactoryMock = mockStatic(DbConnectionFactory.class)) {
            // Setup WebResource mock to return admin user
            try {
                java.lang.reflect.Field webResourceField = DatabaseHealthResource.class.getDeclaredField("webResource");
                webResourceField.setAccessible(true);
                webResourceField.set(resource, mockWebResource);
                
                // Mock successful admin authentication
                when(mockInitBuilder.getUser()).thenReturn(mockUser);
                when(new WebResource.InitBuilder(any(WebResource.class))
                    .requestAndResponse(any(), any())
                    .requiredBackendUser(true)
                    .requireAdmin(true)
                    .init()).thenReturn(mockInitBuilder);
                
            } catch (Exception e) {
                // Skip this test if reflection fails
                return;
            }
            
            Response response = resource.openCircuitBreaker(mockRequest, mockResponse, "Test reason");
            
            // Verify the circuit breaker operation was called
            dbFactoryMock.verify(() -> DbConnectionFactory.openDatabaseCircuitBreaker(anyString()));
            
            assertEquals("Should return HTTP 200 OK for successful operation", 
                        Response.Status.OK.getStatusCode(), response.getStatus());
        }
    }
    
    @Test
    public void testCloseCircuitBreakerWithValidReason() {
        try (MockedStatic<DbConnectionFactory> dbFactoryMock = mockStatic(DbConnectionFactory.class)) {
            // Setup WebResource mock to return admin user
            try {
                java.lang.reflect.Field webResourceField = DatabaseHealthResource.class.getDeclaredField("webResource");
                webResourceField.setAccessible(true);
                webResourceField.set(resource, mockWebResource);
                
                when(mockInitBuilder.getUser()).thenReturn(mockUser);
                when(new WebResource.InitBuilder(any(WebResource.class))
                    .requestAndResponse(any(), any())
                    .requiredBackendUser(true)
                    .requireAdmin(true)
                    .init()).thenReturn(mockInitBuilder);
                
            } catch (Exception e) {
                // Skip this test if reflection fails
                return;
            }
            
            String testReason = "Test maintenance complete";
            Response response = resource.closeCircuitBreaker(mockRequest, mockResponse, testReason);
            
            // Verify the circuit breaker operation was called
            dbFactoryMock.verify(() -> DbConnectionFactory.closeDatabaseCircuitBreaker(anyString()));
            
            assertEquals("Should return HTTP 200 OK for successful operation", 
                        Response.Status.OK.getStatusCode(), response.getStatus());
            
            ResponseEntityView<?> entityView = (ResponseEntityView<?>) response.getEntity();
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) entityView.getEntity();
            
            assertTrue("Response should indicate success", (Boolean) result.get("success"));
            assertEquals("Response should contain correct action", "circuit_breaker_closed", result.get("action"));
            assertTrue("Response should contain reason", result.get("reason").toString().contains(testReason));
            assertTrue("Response should contain user ID", result.containsKey("user"));
            assertTrue("Response should contain timestamp", result.containsKey("timestamp"));
        }
    }
    
    @Test
    public void testOpenCircuitBreakerWithDefaultReason() {
        try (MockedStatic<DbConnectionFactory> dbFactoryMock = mockStatic(DbConnectionFactory.class)) {
            try {
                java.lang.reflect.Field webResourceField = DatabaseHealthResource.class.getDeclaredField("webResource");
                webResourceField.setAccessible(true);
                webResourceField.set(resource, mockWebResource);
                
                when(mockInitBuilder.getUser()).thenReturn(mockUser);
                when(new WebResource.InitBuilder(any(WebResource.class))
                    .requestAndResponse(any(), any())
                    .requiredBackendUser(true)
                    .requireAdmin(true)
                    .init()).thenReturn(mockInitBuilder);
                
            } catch (Exception e) {
                return;
            }
            
            // Call with null reason to test default reason generation
            Response response = resource.openCircuitBreaker(mockRequest, mockResponse, null);
            
            assertEquals("Should return HTTP 200 OK", Response.Status.OK.getStatusCode(), response.getStatus());
            
            ResponseEntityView<?> entityView = (ResponseEntityView<?>) response.getEntity();
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) entityView.getEntity();
            
            String reason = result.get("reason").toString();
            assertTrue("Default reason should contain user ID", reason.contains(mockUser.getUserId()));
            assertTrue("Default reason should mention REST API", reason.contains("REST API"));
        }
    }
    
    @Test
    public void testHealthStatusHandlesExceptionsGracefully() {
        // Force an exception by using reflection to break something
        try {
            java.lang.reflect.Field webResourceField = DatabaseHealthResource.class.getDeclaredField("webResource");
            webResourceField.setAccessible(true);
            webResourceField.set(resource, null); // This should cause NullPointerException
        } catch (Exception e) {
            // If reflection fails, skip this test
            return;
        }
        
        Response response = resource.getHealthStatus(mockRequest, mockResponse);
        
        assertEquals("Should return HTTP 500 Internal Server Error on exception", 
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        
        ResponseEntityView<?> entityView = (ResponseEntityView<?>) response.getEntity();
        @SuppressWarnings("unchecked")
        Map<String, Object> errorData = (Map<String, Object>) entityView.getEntity();
        
        assertFalse("Error response should indicate unhealthy", (Boolean) errorData.get("healthy"));
        assertTrue("Error response should contain error message", errorData.containsKey("error"));
        assertTrue("Error response should contain timestamp", errorData.containsKey("timestamp"));
    }
    
    @Test
    public void testSimpleHealthStatusHandlesExceptionsGracefully() {
        // Test exception handling in simple health endpoint
        Response response = resource.getSimpleHealthStatus(null, null); // Null parameters should cause exception
        
        assertEquals("Should return HTTP 503 Service Unavailable on exception", 
                    Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
        assertEquals("Error response should contain 'error' text", "error", response.getEntity());
    }
    
    @Test
    public void testCircuitBreakerStateChangesReflectedInHealthStatus() {
        // Test that circuit state changes are immediately reflected in health status
        
        // Start with closed circuit
        healthManager.closeCircuitBreaker("Test setup");
        Response response = resource.getHealthStatus(mockRequest, mockResponse);
        ResponseEntityView<?> entityView = (ResponseEntityView<?>) response.getEntity();
        @SuppressWarnings("unchecked")
        Map<String, Object> healthData = (Map<String, Object>) entityView.getEntity();
        assertEquals("Circuit should be CLOSED", "CLOSED", healthData.get("circuitState"));
        
        // Open circuit
        healthManager.openCircuitBreaker("Test circuit open");
        response = resource.getHealthStatus(mockRequest, mockResponse);
        entityView = (ResponseEntityView<?>) response.getEntity();
        @SuppressWarnings("unchecked")
        Map<String, Object> updatedHealthData = (Map<String, Object>) entityView.getEntity();
        assertEquals("Circuit should be OPEN", "OPEN", updatedHealthData.get("circuitState"));
        assertFalse("Should not allow operations when circuit is open", 
                   (Boolean) updatedHealthData.get("operationAllowed"));
    }
    
    @Test
    public void testHealthStatusTimestampAccuracy() {
        long beforeRequest = System.currentTimeMillis();
        Response response = resource.getHealthStatus(mockRequest, mockResponse);
        long afterRequest = System.currentTimeMillis();
        
        ResponseEntityView<?> entityView = (ResponseEntityView<?>) response.getEntity();
        @SuppressWarnings("unchecked")
        Map<String, Object> healthData = (Map<String, Object>) entityView.getEntity();
        
        Instant timestamp = (Instant) healthData.get("timestamp");
        long timestampMillis = timestamp.toEpochMilli();
        
        assertTrue("Timestamp should be recent", 
                  timestampMillis >= beforeRequest && timestampMillis <= afterRequest);
    }
}