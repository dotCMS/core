package com.dotcms.management.filters;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotmarketing.util.Config;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** Metrics cannot become public merely because a request claims the management proxy port. */
class MetricsManagementPortTest {

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final RequestDispatcher dispatcher = mock(RequestDispatcher.class);
    private final InfrastructureManagementFilter filter = new InfrastructureManagementFilter();
    private MockedStatic<Config> config;

    /** Models a real management connector independently of the forwarded server port. */
    @BeforeEach
    void setUp() {
        config = mockStatic(Config.class);
        config.when(() -> Config.getIntProperty("CMS_MANAGEMENT_PORT", 8090)).thenReturn(8090);
        config.when(() -> Config.getBooleanProperty("management.port.strict.check.enabled", true))
                .thenReturn(true);
        when(request.getRequestURI()).thenReturn("/dotmgt/metrics");
        when(request.getServletPath()).thenReturn("/dotmgt/metrics");
        when(request.getLocalPort()).thenReturn(8090);
        when(request.getServerPort()).thenReturn(8090);
        when(request.getRequestDispatcher("/dotmgt/metrics")).thenReturn(dispatcher);
    }

    /** Restores real configuration after each isolated request. */
    @AfterEach
    void tearDown() {
        config.close();
    }

    /** The actual management connector remains usable when a proxy changes the server port. */
    @Test
    void actualManagementConnector_allowsScrapeWithDifferentProxyPort() throws Exception {
        when(request.getServerPort()).thenReturn(443);

        filter.doFilter(request, response, chain);

        verify(dispatcher).forward(request, response);
        verify(response, never()).sendError(anyInt());
        verify(chain, never()).doFilter(request, response);
    }

    /** Neither forwarding-header spelling grants an application connector metrics access. */
    @Test
    void applicationConnector_rejectsForgedManagementHeaders() throws Exception {
        when(request.getLocalPort()).thenReturn(8080);
        when(request.getServerPort()).thenReturn(8080);
        when(request.getHeader("X-Forwarded-Port")).thenReturn("8090");
        when(request.getHeader("X-Original-Port")).thenReturn("8090");

        filter.doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(request, never()).getRequestDispatcher(anyString());
        verify(chain, never()).doFilter(request, response);
    }

    /** A proxy-rewritten server port is not evidence of the connector that accepted the request. */
    @Test
    void applicationConnector_rejectsRewrittenServerPort() throws Exception {
        when(request.getLocalPort()).thenReturn(8080);

        filter.doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(request, never()).getRequestDispatcher(anyString());
    }
}
