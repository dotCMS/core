package com.dotcms.metrics.servlet;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.management.servlet.AbstractManagementServlet;
import com.dotcms.metrics.MetricsConfig;
import com.dotcms.metrics.MetricsService;
import com.dotcms.metrics.config.MetricsEndpointConstants;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.SecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Exposes the existing Prometheus registry on the management connector. Authentication is
 * optional by default and enforced when metrics.prometheus.require-authentication is enabled.
 */
public class ManagementMetricsServlet extends AbstractManagementServlet {

    private static final long serialVersionUID = 1L;

    /** Serves a scrape after applying the same authentication policy to GET and inherited HEAD. */
    @Override
    protected void doManagementGet(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {
        if (!requireAuthentication(request, response)) {
            return;
        }
        handlePrometheusMetrics(response);
    }

    /**
     * Requires normal dotCMS back-end authentication when requested, failing closed if unavailable.
     *
     * @return whether the request may reach the metrics service
     */
    private boolean requireAuthentication(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {
        try {
            if (!Config.getBooleanProperty(MetricsConfig.PROMETHEUS_REQUIRE_AUTH_PROPERTY, false)) {
                return true;
            }
            new WebResource.InitBuilder()
                    .requestAndResponse(request, response)
                    .rejectWhenNoUser(true)
                    .requiredBackendUser(true)
                    .init();
            return true;
        } catch (SecurityException e) {
            // sendError dispatches the site's HTML error page and can redirect scrapers to login.
            response.setStatus(e.getResponse().getStatus());
        } catch (Exception e) {
            Logger.error(this, "Authentication unavailable for metrics request", e);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
        return false;
    }

    /** Returns an exposition or an explicit unavailable response without closing the container writer. */
    private void handlePrometheusMetrics(final HttpServletResponse response) throws IOException {
        response.setContentType(MetricsEndpointConstants.Responses.CONTENT_TYPE_PROMETHEUS);
        response.setCharacterEncoding(MetricsEndpointConstants.Responses.CHARSET_UTF8);
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0L);

        try {
            if (!Config.getBooleanProperty(MetricsConfig.ENABLED_PROPERTY, true)) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.getWriter().write(MetricsEndpointConstants.Responses.METRICS_DISABLED_MESSAGE);
                return;
            }
            if (!Config.getBooleanProperty(MetricsConfig.PROMETHEUS_ENABLED_PROPERTY, true)) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.getWriter().write(MetricsEndpointConstants.Responses.PROMETHEUS_DISABLED_MESSAGE);
                return;
            }

            final Optional<MetricsService> service = CDIUtils.getBean(MetricsService.class);
            if (service.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.getWriter().write(MetricsEndpointConstants.Responses.SERVICE_UNAVAILABLE_MESSAGE);
                return;
            }
            final PrometheusMeterRegistry registry = service.get().getPrometheusRegistry();
            if (registry == null) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.getWriter().write(MetricsEndpointConstants.Responses.REGISTRY_NOT_CONFIGURED_MESSAGE);
                return;
            }

            // Complete collection before obtaining the writer so a failure cannot commit a partial 200.
            final String exposition = registry.scrape();
            response.getWriter().write(exposition);
        } catch (Exception e) {
            Logger.error(this, "Error serving Prometheus metrics", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("# Error serving metrics\n");
        }
    }

    /** Describes the management-only Prometheus endpoint. */
    @Override
    public String getServletInfo() {
        return "Management Metrics Servlet for dotCMS - exposes Prometheus metrics through management port infrastructure";
    }
}
