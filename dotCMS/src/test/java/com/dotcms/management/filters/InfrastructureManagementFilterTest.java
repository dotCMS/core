package com.dotcms.management.filters;

import com.dotcms.UnitTestBase;
import com.dotcms.management.config.InfrastructureConstants;
import com.dotcms.health.config.HealthEndpointConstants;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.mockito.Mockito.*;

public class InfrastructureManagementFilterTest extends UnitTestBase {

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
        
        // Create filter instance - simplified without CDI
        filter = new InfrastructureManagementFilter();
        filter.init(filterConfig);
    }

    @Test
    public void testManagementEndpointOnCorrectPort() throws IOException, ServletException {
        // Reset mocks for this test
        reset(request, response, chain, writer, requestDispatcher);
        when(response.getWriter()).thenReturn(writer);
        
        // Setup - management endpoint on correct port
        String requestURI = HealthEndpointConstants.Endpoints.LIVENESS;
        when(request.getRequestURI()).thenReturn(requestURI);
        when(request.getServerPort()).thenReturn(InfrastructureConstants.Ports.DEFAULT_MANAGEMENT_PORT);
        when(request.getRequestDispatcher(requestURI)).thenReturn(requestDispatcher);

        // Execute
        filter.doFilter(request, response, chain);

        // Verify request is forwarded directly to servlet (bypassing chain)
        verify(requestDispatcher).forward(request, response);
        verify(chain, never()).doFilter(request, response);
        verify(response, never()).sendError(anyInt());
    }

    @Test
    public void testManagementEndpointBlockedOnWrongPort() throws IOException, ServletException {
        // Reset mocks for this test
        reset(request, response, chain, writer);
        when(response.getWriter()).thenReturn(writer);
        
        // Setup - management endpoint on wrong port
        String requestURI = HealthEndpointConstants.Endpoints.LIVENESS;
        when(request.getRequestURI()).thenReturn(requestURI);
        when(request.getServerPort()).thenReturn(InfrastructureConstants.Ports.DEFAULT_APPLICATION_PORT); // Application port, not management port
        when(request.getHeader(InfrastructureConstants.Headers.X_FORWARDED_PORT)).thenReturn(null); // No proxy headers

        // Execute
        filter.doFilter(request, response, chain);

        // Verify request is blocked with 404 error
        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    public void testManagementEndpointWithProxyHeaders() throws IOException, ServletException {
        // Reset mocks for this test
        reset(request, response, chain, writer, requestDispatcher);
        when(response.getWriter()).thenReturn(writer);
        
        // Setup - management endpoint with proxy headers indicating management port
        String requestURI = HealthEndpointConstants.Endpoints.HEALTH;
        when(request.getRequestURI()).thenReturn(requestURI);
        when(request.getServerPort()).thenReturn(InfrastructureConstants.Ports.DEFAULT_APPLICATION_PORT); // Application port
        when(request.getHeader(InfrastructureConstants.Headers.X_FORWARDED_PORT)).thenReturn("8090"); // Proxy indicates management port
        when(request.getRequestDispatcher(requestURI)).thenReturn(requestDispatcher);

        // Execute
        filter.doFilter(request, response, chain);

        // Verify request is forwarded directly to servlet
        verify(requestDispatcher).forward(request, response);
        verify(chain, never()).doFilter(request, response);
        verify(response, never()).sendError(anyInt());
    }

    @Test
    public void testRegularEndpointContinuesChain() throws IOException, ServletException {
        // Reset mocks for this test
        reset(request, response, chain, writer);
        when(response.getWriter()).thenReturn(writer);
        
        // Setup - regular application endpoint (not /dotmgt)
        String requestURI = "/api/v1/content";
        when(request.getRequestURI()).thenReturn(requestURI);

        // Execute
        filter.doFilter(request, response, chain);

        // Verify chain continues for non-management requests
        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void testAllManagementEndpoints() throws IOException, ServletException {
        // Test all health endpoints (health service is aware of infrastructure)
        for (String endpoint : HealthEndpointConstants.getAllHealthEndpoints()) {
            // Reset mocks for each test
            reset(request, response, chain, writer, requestDispatcher);
            when(response.getWriter()).thenReturn(writer);
            
            // Setup - management endpoint on correct port
            when(request.getRequestURI()).thenReturn(endpoint);
            when(request.getServerPort()).thenReturn(InfrastructureConstants.Ports.DEFAULT_MANAGEMENT_PORT);
            when(request.getRequestDispatcher(endpoint)).thenReturn(requestDispatcher);

            // Execute
            filter.doFilter(request, response, chain);

            // Verify request is forwarded directly to servlet
            verify(requestDispatcher).forward(request, response);
            verify(chain, never()).doFilter(request, response);
            verify(response, never()).sendError(anyInt());
        }
    }
} 