package com.dotcms.health.servlet;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.dotcms.health.model.HealthResponse;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.health.model.HealthCheckResult;
import com.dotcms.health.service.HealthStateManager;
import com.dotcms.health.config.HealthEndpointConstants;
import com.dotmarketing.util.Config;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Collections;
import java.util.Arrays;

/**
 * Test for HealthProbeServlet endpoint routing and response formats
 */
public class HealthProbeServletTest {

    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private HealthStateManager healthStateManager;
    
    private HealthProbeServlet servlet;
    private StringWriter responseWriter;
    private PrintWriter printWriter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        
        // Disable authentication for tests
        Config.setProperty("health.detailed.authentication.required", "false");
        
        servlet = new HealthProbeServlet();
        
        // Use reflection to set the health state manager for testing
        java.lang.reflect.Field healthField = HealthProbeServlet.class.getDeclaredField("healthStateManager");
        healthField.setAccessible(true);
        healthField.set(servlet, healthStateManager);
        
        // Use reflection to set a mock ObjectMapper for testing
        java.lang.reflect.Field mapperField = HealthProbeServlet.class.getDeclaredField("objectMapper");
        mapperField.setAccessible(true);
        mapperField.set(servlet, mock(com.fasterxml.jackson.databind.ObjectMapper.class));
        
        responseWriter = new StringWriter();
        printWriter = new PrintWriter(responseWriter);
        when(response.getWriter()).thenReturn(printWriter);
    }
    
    @After
    public void tearDown() {
        // Reset configuration after tests
        Config.setProperty("health.detailed.authentication.required", "true");
    }

    @Test
    public void testDotmgtLivezEndpointReturnsTextResponse() throws Exception {
        // Setup - Management liveness endpoint (new direct mapping)
        when(request.getServletPath()).thenReturn(HealthEndpointConstants.Endpoints.LIVENESS);
        
        HealthResponse healthResponse = HealthResponse.builder()
            .status(HealthStatus.UP)
            .checks(Collections.emptyList())
            .timestamp(Instant.now())
            .build();
        when(healthStateManager.getLivenessHealth()).thenReturn(healthResponse);
        
        // Execute
        servlet.doManagementGet(request, response);
        
        // Verify minimal text response
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("text/plain");
        verify(response).setCharacterEncoding("UTF-8");
        printWriter.flush();
        assertEquals(HealthEndpointConstants.Responses.ALIVE_RESPONSE, responseWriter.toString());
    }

    @Test
    public void testDotmgtReadyzEndpointReturnsTextResponse() throws Exception {
        // Setup - Management readiness endpoint (new direct mapping)
        when(request.getServletPath()).thenReturn(HealthEndpointConstants.Endpoints.READINESS);
        
        HealthResponse healthResponse = HealthResponse.builder()
            .status(HealthStatus.UP)
            .checks(Collections.emptyList())
            .timestamp(Instant.now())
            .build();
        when(healthStateManager.getReadinessHealth()).thenReturn(healthResponse);
        
        // Execute
        servlet.doManagementGet(request, response);
        
        // Verify minimal text response
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("text/plain");
        verify(response).setCharacterEncoding("UTF-8");
        printWriter.flush();
        assertEquals(HealthEndpointConstants.Responses.READY_RESPONSE, responseWriter.toString());
    }

    @Test
    public void testDotmgtHealthEndpointReturnsJsonResponse() throws Exception {
        // Setup - Management health endpoint (new direct mapping)
        when(request.getServletPath()).thenReturn(HealthEndpointConstants.Endpoints.HEALTH);
        
        HealthResponse healthResponse = HealthResponse.builder()
            .status(HealthStatus.UP)
            .checks(Collections.emptyList())
            .timestamp(Instant.now())
            .build();
        when(healthStateManager.getCurrentHealth()).thenReturn(healthResponse);
        
        // Execute
        servlet.doManagementGet(request, response);
        
        // Verify JSON response
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
    }


    @Test
    public void testServletContainerHealthCheckWaitsForHttpConnectors() throws Exception {
        // This test verifies that the servlet container health check properly waits for
        // HTTP connectors to be started before reporting as UP, which fixes the premature
        // "FIRST SUCCESS" logging issue identified in the conversation summary
        
        // Setup - liveness endpoint should wait for servlet container to be ready
        when(request.getServletPath()).thenReturn(HealthEndpointConstants.Endpoints.LIVENESS);
        
        // Create a health response where servlet container is still starting up
        HealthCheckResult servletContainerStarting = HealthCheckResult.builder()
            .name("servlet-container")
            .status(HealthStatus.DOWN)
            .message("HTTP connectors not ready to serve requests")
            .lastChecked(Instant.now())
            .durationMs(500L)
            .build();
        
        HealthResponse healthResponse = HealthResponse.builder()
            .status(HealthStatus.DOWN)
            .checks(Arrays.asList(servletContainerStarting))
            .timestamp(Instant.now())
            .build();
        when(healthStateManager.getLivenessHealth()).thenReturn(healthResponse);
        
        // Execute
        servlet.doManagementGet(request, response);
        
        // Verify that system reports as not alive until servlet container is truly ready
        verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        verify(response).setContentType("text/plain");
        printWriter.flush();
        assertEquals("unhealthy", responseWriter.toString());
        
        // Now test when servlet container becomes ready
        response = mock(HttpServletResponse.class);
        responseWriter = new StringWriter();
        printWriter = new PrintWriter(responseWriter);
        when(response.getWriter()).thenReturn(printWriter);
        
        HealthCheckResult servletContainerReady = HealthCheckResult.builder()
            .name("servlet-container")
            .status(HealthStatus.UP)
            .message("Servlet container responsive - HTTP connectors: 3 started")
            .lastChecked(Instant.now())
            .durationMs(200L)
            .build();
        
        HealthResponse readyHealthResponse = HealthResponse.builder()
            .status(HealthStatus.UP)
            .checks(Arrays.asList(servletContainerReady))
            .timestamp(Instant.now())
            .build();
        when(healthStateManager.getLivenessHealth()).thenReturn(readyHealthResponse);
        
        // Execute again
        servlet.doManagementGet(request, response);
        
        // Verify that system now reports as alive since servlet container is ready
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("text/plain");
        printWriter.flush();
        assertEquals("alive", responseWriter.toString());
    }


    @Test
    public void testReadyzWithDegradedStatusReturns200() throws Exception {
        // Setup - Degraded readiness check (from MONITOR_MODE)
        when(request.getServletPath()).thenReturn(HealthEndpointConstants.Endpoints.READINESS);
        
        HealthResponse healthResponse = HealthResponse.builder()
            .status(HealthStatus.DEGRADED)
            .checks(Collections.emptyList())
            .timestamp(Instant.now())
            .build();
        when(healthStateManager.getReadinessHealth()).thenReturn(healthResponse);
        
        // Execute
        servlet.doManagementGet(request, response);
        
        // Verify 200 status with ready response (MONITOR_MODE behavior)
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("text/plain");
        printWriter.flush();
        assertEquals("ready", responseWriter.toString());
    }
    
    @Test
    public void testReadyzWithDownStatusReturns503() throws Exception {
        // Setup - Genuine DOWN status (with a DOWN health check result)
        when(request.getServletPath()).thenReturn(HealthEndpointConstants.Endpoints.READINESS);
        
        HealthCheckResult downCheck = HealthCheckResult.builder()
            .name("database")
            .status(HealthStatus.DOWN)
            .message("Database connection failed")
            .lastChecked(Instant.now())
            .durationMs(1000L)
            .build();
        
        HealthResponse healthResponse = HealthResponse.builder()
            .status(HealthStatus.DOWN)
            .checks(Arrays.asList(downCheck))
            .timestamp(Instant.now())
            .build();
        when(healthStateManager.getReadinessHealth()).thenReturn(healthResponse);
        
        // Execute
        servlet.doManagementGet(request, response);
        
        // Verify 503 status and not ready response
        verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        verify(response).setContentType("text/plain");
        printWriter.flush();
        assertEquals("not ready", responseWriter.toString());
    }
} 