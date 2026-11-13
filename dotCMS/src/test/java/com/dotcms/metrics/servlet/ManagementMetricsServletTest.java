package com.dotcms.metrics.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.metrics.MetricsService;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.SecurityException;
import com.dotmarketing.util.Config;
import com.liferay.portal.auth.PrincipalThreadLocal;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** Checks scrape availability, configuration, and authentication without booting dotCMS. */
class ManagementMetricsServletTest {

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final MetricsService service = mock(MetricsService.class);
    private final PrometheusMeterRegistry registry = mock(PrometheusMeterRegistry.class);
    private final StringWriter body = new StringWriter();
    private final ManagementMetricsServlet servlet = new ManagementMetricsServlet();
    private MockedStatic<Config> config;
    private MockedStatic<CDIUtils> cdi;

    /** Uses the existing configuration defaults and a registry with a representative sample. */
    @BeforeEach
    void setUp() throws Exception {
        config = mockStatic(Config.class);
        config.when(() -> Config.getBooleanProperty(anyString(), anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        cdi = mockStatic(CDIUtils.class);
        cdi.when(() -> CDIUtils.getBean(MetricsService.class)).thenReturn(Optional.of(service));
        when(service.getPrometheusRegistry()).thenReturn(registry);
        when(registry.scrape()).thenReturn("jvm_memory_used_bytes{area=\"heap\"} 123\n");
        when(request.getServletPath()).thenReturn("/dotmgt/metrics");
        when(request.getRequestURI()).thenReturn("/dotmgt/metrics");
        when(request.getMethod()).thenReturn("GET");
        when(response.getWriter()).thenReturn(new PrintWriter(body));
        when(response.getCharacterEncoding()).thenReturn("UTF-8");
    }

    /** Releases thread-local static mocks so other tests retain their configuration and CDI. */
    @AfterEach
    void tearDown() {
        cdi.close();
        config.close();
    }

    /** Defaults expose metrics without extra enablement or authentication flags. */
    @Test
    void defaultConfiguration_returnsRegistryExpositionWithoutAuthentication() throws Exception {
        try (var authentication = mockConstruction(WebResource.InitBuilder.class)) {
            servlet.service(request, response);

            assertEquals("jvm_memory_used_bytes{area=\"heap\"} 123\n", body.toString());
            verify(response).setContentType("text/plain; version=0.0.4; charset=utf-8");
            verify(response, never()).sendError(org.mockito.ArgumentMatchers.anyInt());
            assertEquals(0, authentication.constructed().size());
        }
    }

    /** Disabled metrics are unavailable and never resolve the CDI service. */
    @Test
    void disabledMetrics_return503WithoutResolvingService() throws Exception {
        config.when(() -> Config.getBooleanProperty("metrics.enabled", true)).thenReturn(false);

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        assertEquals("# Metrics collection is disabled\n", body.toString());
        cdi.verifyNoInteractions();
    }

    /** Disabling just Prometheus preserves the explicit unavailable response. */
    @Test
    void disabledPrometheus_returns503WithoutResolvingService() throws Exception {
        config.when(() -> Config.getBooleanProperty("metrics.prometheus.enabled", true))
                .thenReturn(false);

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        assertEquals("# Prometheus metrics are disabled\n", body.toString());
        cdi.verifyNoInteractions();
    }

    /** Missing CDI services must not look like a successful empty scrape. */
    @Test
    void absentService_returns503() throws Exception {
        cdi.when(() -> CDIUtils.getBean(MetricsService.class)).thenReturn(Optional.empty());

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        assertEquals("# MetricsService not available\n", body.toString());
    }

    /** A service without an initialized Prometheus registry is unavailable. */
    @Test
    void absentRegistry_returns503() throws Exception {
        when(service.getPrometheusRegistry()).thenReturn(null);

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        assertEquals("# Prometheus registry not configured\n", body.toString());
    }

    /** Scrape exceptions cannot put internal exception details in a response. */
    @Test
    void scrapeFailure_returnsGenericErrorBeforeWritingExposition() throws Exception {
        when(registry.scrape()).thenThrow(new IllegalStateException("private diagnostic detail"));

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertEquals("# Error serving metrics\n", body.toString());
        assertFalse(body.toString().contains("private diagnostic detail"));
        assertFalse(body.toString().contains("jvm_memory_used_bytes"));
    }

    /** Required authentication rejects anonymous GET and HEAD before looking up metrics. */
    @Test
    void requiredAuthentication_rejectsAnonymousGetAndHead() throws Exception {
        config.when(() -> Config.getBooleanProperty(
                "metrics.prometheus.require-authentication", false)).thenReturn(true);
        try (var authentication = mockConstruction(WebResource.InitBuilder.class,
                (builder, context) -> {
                    when(builder.requestAndResponse(request, response)).thenReturn(builder);
                    when(builder.rejectWhenNoUser(true)).thenReturn(builder);
                    when(builder.requiredBackendUser(true)).thenReturn(builder);
                    when(builder.requestAndResponse(eq(request), any(HttpServletResponse.class)))
                            .thenReturn(builder);
                    when(builder.init()).thenThrow(new SecurityException(
                            "Authentication required", Response.Status.UNAUTHORIZED));
                })) {
            servlet.service(request, response);
            when(request.getMethod()).thenReturn("HEAD");
            servlet.service(request, response);

            verify(response, org.mockito.Mockito.times(2))
                    .setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(response, never()).sendError(org.mockito.ArgumentMatchers.anyInt());
            assertEquals(2, authentication.constructed().size());
            cdi.verifyNoInteractions();
            assertEquals("", body.toString());
        }
    }

    /** Authenticated scrapes use normal dotCMS authentication and the existing registry. */
    @Test
    void requiredAuthentication_acceptsAuthenticatedRequest() throws Exception {
        config.when(() -> Config.getBooleanProperty(
                "metrics.prometheus.require-authentication", false)).thenReturn(true);
        try (var authentication = mockConstruction(WebResource.InitBuilder.class,
                (builder, context) -> {
                    when(builder.requestAndResponse(request, response)).thenReturn(builder);
                    when(builder.rejectWhenNoUser(true)).thenReturn(builder);
                    when(builder.requiredBackendUser(true)).thenReturn(builder);
                    when(builder.init()).thenReturn(new InitDataObject());
                })) {
            servlet.service(request, response);

            assertEquals(1, authentication.constructed().size());
            verify(authentication.constructed().get(0)).rejectWhenNoUser(true);
            verify(authentication.constructed().get(0)).requiredBackendUser(true);
            assertEquals("jvm_memory_used_bytes{area=\"heap\"} 123\n", body.toString());
        }
    }

    /** Opt-in authentication also requires a back-end role, not just a front-end login. */
    @Test
    void requiredAuthentication_rejectsFrontendOnlyUser() throws Exception {
        config.when(() -> Config.getBooleanProperty(
                "metrics.prometheus.require-authentication", false)).thenReturn(true);
        try (var authentication = mockConstruction(WebResource.InitBuilder.class,
                (builder, context) -> {
                    when(builder.requestAndResponse(request, response)).thenReturn(builder);
                    when(builder.rejectWhenNoUser(true)).thenReturn(builder);
                    when(builder.requiredBackendUser(true)).thenReturn(builder);
                    when(builder.init()).thenThrow(new SecurityException(
                            "Back-end role required", Response.Status.FORBIDDEN));
                })) {
            servlet.service(request, response);

            verify(authentication.constructed().get(0)).requiredBackendUser(true);
            verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            cdi.verifyNoInteractions();
        }
    }

    /** Authentication outages fail closed and do not reach the metrics service. */
    @Test
    void authenticationFailure_returns503WithoutResolvingService() throws Exception {
        config.when(() -> Config.getBooleanProperty(
                "metrics.prometheus.require-authentication", false)).thenReturn(true);
        try (var authentication = mockConstruction(WebResource.InitBuilder.class,
                (builder, context) -> {
                    when(builder.requestAndResponse(request, response)).thenReturn(builder);
                    when(builder.rejectWhenNoUser(true)).thenReturn(builder);
                    when(builder.requiredBackendUser(true)).thenReturn(builder);
                    when(builder.init()).thenThrow(new IllegalStateException("private auth detail"));
                })) {
            servlet.service(request, response);

            assertEquals(1, authentication.constructed().size());
            verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            verify(response, never()).sendError(org.mockito.ArgumentMatchers.anyInt());
            cdi.verifyNoInteractions();
            assertEquals("", body.toString());
        }
    }

    /** Authentication may change the pooled thread's identity even when a later check fails. */
    @Test
    void authentication_restoresPreviousPrincipalOnSuccessAndFailure() throws Exception {
        config.when(() -> Config.getBooleanProperty(
                "metrics.prometheus.require-authentication", false)).thenReturn(true);
        final String original = PrincipalThreadLocal.getName();
        try {
            for (final String previous : new String[]{null, "caller-context"}) {
                for (final RuntimeException failure : new RuntimeException[]{
                        null, new SecurityException("Denied", Response.Status.FORBIDDEN),
                        new IllegalStateException("Authentication unavailable")}) {
                    PrincipalThreadLocal.setName(previous);
                    try (var authentication = mockConstruction(WebResource.InitBuilder.class,
                            (builder, context) -> {
                                when(builder.requestAndResponse(request, response)).thenReturn(builder);
                                when(builder.rejectWhenNoUser(true)).thenReturn(builder);
                                when(builder.requiredBackendUser(true)).thenReturn(builder);
                                when(builder.init()).thenAnswer(ignored -> {
                                    PrincipalThreadLocal.setName("scrape-user");
                                    if (failure != null) {
                                        throw failure;
                                    }
                                    return new InitDataObject();
                                });
                            })) {
                        servlet.service(request, response);
                        assertEquals(previous, PrincipalThreadLocal.getName());
                    }
                }
            }
        } finally {
            PrincipalThreadLocal.setName(original);
        }
    }

    /** HEAD checks use the same availability and access rules but return no sample body. */
    @Test
    void enabledHead_returnsNoExpositionBody() throws Exception {
        when(request.getMethod()).thenReturn("HEAD");

        servlet.service(request, response);

        verify(response, never()).sendError(org.mockito.ArgumentMatchers.anyInt());
        assertEquals("", body.toString());
    }
}
