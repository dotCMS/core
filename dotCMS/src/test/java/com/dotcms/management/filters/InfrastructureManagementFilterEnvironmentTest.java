package com.dotcms.management.filters;

import com.dotcms.UnitTestBase;
import com.dotcms.management.config.InfrastructureConstants;
import com.dotcms.health.config.HealthEndpointConstants;
import com.dotmarketing.util.Config;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.After;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.mockito.Mockito.*;

/**
 * Tests for InfrastructureManagementFilter environment variable integration.
 * 
 * Specifically tests that the filter correctly reads the CMS_MANAGEMENT_PORT
 * environment variable that server.xml uses, ensuring no configuration duplication.
 */
public class InfrastructureManagementFilterEnvironmentTest extends UnitTestBase {

    private static InfrastructureManagementFilter filter;
    private static HttpServletRequest request;
    private static HttpServletResponse response;
    private static FilterChain chain;
    private static PrintWriter writer;
    private static FilterConfig filterConfig;
    private static RequestDispatcher requestDispatcher;

    @BeforeClass
    public static void setup() throws Exception {
        // Mock servlet components
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        writer = mock(PrintWriter.class);
        filterConfig = mock(FilterConfig.class);
        requestDispatcher = mock(RequestDispatcher.class);
        
        when(response.getWriter()).thenReturn(writer);
        
        // Create filter instance
        filter = new InfrastructureManagementFilter();
        filter.init(filterConfig);
    }

    @After
    public void cleanup() {
        // Clean up any Config property changes
        Config.setProperty(InfrastructureConstants.Ports.MANAGEMENT_PORT_PROPERTY, null);
    }

    @Test
    public void testUsesEnvironmentVariableFromServerXml() throws IOException, ServletException {
        // Setup - Set the same property that the filter reads as fallback from Config
        Config.setProperty(InfrastructureConstants.Ports.MANAGEMENT_PORT_PROPERTY, "9090");
        
        // Reset mocks for this test
        reset(request, response, chain, writer, requestDispatcher);
        when(response.getWriter()).thenReturn(writer);
        
        // Setup - Request on the environment-configured port
        String requestURI = HealthEndpointConstants.Endpoints.LIVENESS;
        when(request.getRequestURI()).thenReturn(requestURI);
        when(request.getServerPort()).thenReturn(9090); // Port from configuration
        when(request.getRequestDispatcher(requestURI)).thenReturn(requestDispatcher);

        // Execute
        filter.doFilter(request, response, chain);

        // Verify request is forwarded directly to servlet (port validation passed)
        verify(requestDispatcher).forward(request, response);
        verify(chain, never()).doFilter(request, response);
        verify(response, never()).sendError(anyInt());
    }

    @Test
    public void testBlocksWhenNotOnEnvironmentConfiguredPort() throws IOException, ServletException {
        // Setup - Set custom port via Config property
        Config.setProperty(InfrastructureConstants.Ports.MANAGEMENT_PORT_PROPERTY, "9090");
        
        // Reset mocks for this test
        reset(request, response, chain, writer);
        when(response.getWriter()).thenReturn(writer);
        
        // Setup - Request on different port than configured
        String requestURI = HealthEndpointConstants.Endpoints.LIVENESS;
        when(request.getRequestURI()).thenReturn(requestURI);
        when(request.getServerPort()).thenReturn(8080); // Not the management port
        when(request.getHeader(InfrastructureConstants.Headers.X_FORWARDED_PORT)).thenReturn(null);

        // Execute
        filter.doFilter(request, response, chain);

        // Verify request is blocked with 404 error
        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    public void testFallsBackToDefaultWhenEnvironmentVariableInvalid() throws IOException, ServletException {
        // Setup - Invalid Config property value
        Config.setProperty(InfrastructureConstants.Ports.MANAGEMENT_PORT_PROPERTY, "invalid");
        
        // Reset mocks for this test
        reset(request, response, chain, writer, requestDispatcher);
        when(response.getWriter()).thenReturn(writer);
        
        // Setup - Request on default port (should work despite invalid config)
        String requestURI = HealthEndpointConstants.Endpoints.HEALTH;
        when(request.getRequestURI()).thenReturn(requestURI);
        when(request.getServerPort()).thenReturn(InfrastructureConstants.Ports.DEFAULT_MANAGEMENT_PORT); // Default port
        when(request.getRequestDispatcher(requestURI)).thenReturn(requestDispatcher);

        // Execute
        filter.doFilter(request, response, chain);

        // Verify request is forwarded directly to servlet (falls back to default port)
        verify(requestDispatcher).forward(request, response);
        verify(chain, never()).doFilter(request, response);
        verify(response, never()).sendError(anyInt());
    }

    @Test
    public void testFallsBackToDefaultWhenEnvironmentVariableNotSet() throws IOException, ServletException {
        // Setup - No Config property set (normal default case)
        Config.setProperty(InfrastructureConstants.Ports.MANAGEMENT_PORT_PROPERTY, null);
        
        // Reset mocks for this test
        reset(request, response, chain, writer, requestDispatcher);
        when(response.getWriter()).thenReturn(writer);
        
        // Setup - Request on default port
        String requestURI = HealthEndpointConstants.Endpoints.READINESS;
        when(request.getRequestURI()).thenReturn(requestURI);
        when(request.getServerPort()).thenReturn(InfrastructureConstants.Ports.DEFAULT_MANAGEMENT_PORT);
        when(request.getRequestDispatcher(requestURI)).thenReturn(requestDispatcher);

        // Execute
        filter.doFilter(request, response, chain);

        // Verify request is forwarded directly to servlet (uses default)
        verify(requestDispatcher).forward(request, response);
        verify(chain, never()).doFilter(request, response);
        verify(response, never()).sendError(anyInt());
    }
} 