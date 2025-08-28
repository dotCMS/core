package com.dotcms.management.servlet;

import com.dotcms.UnitTestBase;
import com.dotcms.health.config.HealthEndpointConstants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for AbstractManagementServlet to ensure path validation works correctly.
 */
public class AbstractManagementServletTest extends UnitTestBase {

    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    private TestManagementServlet servlet;
    private StringWriter responseWriter;
    private PrintWriter printWriter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        
        servlet = new TestManagementServlet();
        
        responseWriter = new StringWriter();
        printWriter = new PrintWriter(responseWriter);
        when(response.getWriter()).thenReturn(printWriter);
    }

    @Test
    public void testAllowsValidManagementPath() throws ServletException, IOException {
        // Setup - Valid management path
        when(request.getRequestURI()).thenReturn(HealthEndpointConstants.Endpoints.LIVENESS);
        when(request.getServletPath()).thenReturn(HealthEndpointConstants.Endpoints.LIVENESS);
        
        // Execute
        servlet.doGet(request, response);
        
        // Verify - Request was processed (not blocked)
        assertTrue("Should have called doManagementGet", servlet.wasManagementGetCalled);
        verify(response, never()).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void testBlocksNonManagementPath() throws ServletException, IOException {
        // Setup - Invalid non-management path
        when(request.getRequestURI()).thenReturn(HealthEndpointConstants.Endpoints.HEALTH_SUFFIX);
        when(request.getServletPath()).thenReturn(HealthEndpointConstants.Endpoints.HEALTH_SUFFIX);
        
        // Execute
        servlet.doGet(request, response);
        
        // Verify - Request was blocked
        assertFalse("Should NOT have called doManagementGet", servlet.wasManagementGetCalled);
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(response).setContentType("text/plain");
        verify(response).setCharacterEncoding("UTF-8");
        printWriter.flush();
        assertEquals("Not Found", responseWriter.toString());
    }

    @Test
    public void testBlocksSuspiciousHealthPath() throws ServletException, IOException {
        // Setup - Suspicious direct health access
        when(request.getRequestURI()).thenReturn(HealthEndpointConstants.Endpoints.LIVENESS_SUFFIX);
        when(request.getServletPath()).thenReturn(HealthEndpointConstants.Endpoints.LIVENESS_SUFFIX);
        
        // Execute
        servlet.doGet(request, response);
        
        // Verify - Request was blocked
        assertFalse("Should NOT have called doManagementGet", servlet.wasManagementGetCalled);
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void testAllowsManagementPathInRequestURI() throws ServletException, IOException {
        // Setup - Management path in request URI
        when(request.getRequestURI()).thenReturn("/app" + HealthEndpointConstants.Endpoints.HEALTH);
        when(request.getServletPath()).thenReturn(HealthEndpointConstants.Endpoints.HEALTH);
        
        // Execute
        servlet.doGet(request, response);
        
        // Verify - Request was processed
        assertTrue("Should have called doManagementGet", servlet.wasManagementGetCalled);
        verify(response, never()).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void testPostMethodValidation() throws ServletException, IOException {
        // Setup - Valid management path for POST
        when(request.getRequestURI()).thenReturn(HealthEndpointConstants.Endpoints.HEALTH);
        when(request.getServletPath()).thenReturn(HealthEndpointConstants.Endpoints.HEALTH);
        
        // Execute
        servlet.doPost(request, response);
        
        // Verify - Request was processed
        assertTrue("Should have called doManagementPost", servlet.wasManagementPostCalled);
        verify(response, never()).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void testDefaultMethodsReturnMethodNotAllowed() throws ServletException, IOException {
        // Setup - Valid management path
        when(request.getRequestURI()).thenReturn(HealthEndpointConstants.Endpoints.HEALTH);
        when(request.getServletPath()).thenReturn(HealthEndpointConstants.Endpoints.HEALTH);
        
        // Execute PUT (not overridden in test servlet)
        servlet.doPut(request, response);
        
        // Verify - Method not allowed
        verify(response).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Test implementation of AbstractManagementServlet for testing purposes.
     */
    private static class TestManagementServlet extends AbstractManagementServlet {
        boolean wasManagementGetCalled = false;
        boolean wasManagementPostCalled = false;

        @Override
        protected void doManagementGet(HttpServletRequest request, HttpServletResponse response) 
                throws ServletException, IOException {
            wasManagementGetCalled = true;
            response.setStatus(HttpServletResponse.SC_OK);
        }

        @Override
        protected void doManagementPost(HttpServletRequest request, HttpServletResponse response) 
                throws ServletException, IOException {
            wasManagementPostCalled = true;
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }
} 