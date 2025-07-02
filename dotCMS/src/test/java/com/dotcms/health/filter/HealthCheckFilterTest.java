package com.dotcms.health.filter;

import com.dotcms.health.util.HealthCheckEndpointUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.*;

public class HealthCheckFilterTest {

    private HealthCheckFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        filter = new HealthCheckFilter();
    }

    @Test
    public void testHealthCheckEndpoint() throws IOException, ServletException {
        // Setup
        String healthCheckPath = "/api/v1/health";
        when(request.getRequestURI()).thenReturn(healthCheckPath);

        // Execute
        filter.doFilter(request, response, chain);

        // Verify
        verify(chain).doFilter(request, response);
        verifyNoMoreInteractions(chain);
    }

    @Test
    public void testNonHealthCheckEndpoint() throws IOException, ServletException {
        // Setup
        String regularPath = "/api/v1/content";
        when(request.getRequestURI()).thenReturn(regularPath);

        // Execute
        filter.doFilter(request, response, chain);

        // Verify
        verify(chain).doFilter(request, response);
        verifyNoMoreInteractions(chain);
    }
} 