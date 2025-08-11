package com.dotcms.metrics.servlet;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.management.servlet.AbstractManagementServlet;
import com.dotcms.metrics.MetricsConfig;
import com.dotcms.metrics.MetricsService;
import com.dotcms.metrics.config.MetricsEndpointConstants;
import com.dotmarketing.util.Logger;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

/**
 * Management servlet for exposing Prometheus metrics at the /dotmgt/metrics endpoint.
 * 
 * This servlet extends AbstractManagementServlet to ensure it can only be accessed
 * through the management port infrastructure. It provides secure, high-performance
 * metrics scraping for monitoring systems.
 * 
 * The servlet serves metrics in Prometheus text exposition format and includes
 * proper caching headers to prevent metric caching while ensuring high performance.
 * 
 * Security:
 * - Only accessible through management port (8090 by default)
 * - Protected by InfrastructureManagementFilter
 * - No authentication required to allow scraping by monitoring tools
 * 
 * Performance:
 * - Lightweight processing with minimal filter chain
 * - Direct CDI service access
 * - Efficient response streaming
 */
public class ManagementMetricsServlet extends AbstractManagementServlet {
    
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = ManagementMetricsServlet.class.getSimpleName();
    
    @Override
    protected void doManagementGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String servletPath = request.getServletPath();
        
        // Route to appropriate handler based on path
        if (MetricsEndpointConstants.Endpoints.METRICS.equals(servletPath)) {
            handlePrometheusMetrics(request, response);
        } else if (servletPath != null && servletPath.startsWith(MetricsEndpointConstants.Endpoints.METRICS)) {
            // Handle any sub-paths under /dotmgt/metrics as Prometheus metrics for simplicity
            handlePrometheusMetrics(request, response);
        } else {
            // Default to Prometheus metrics for the base /dotmgt/metrics path
            handlePrometheusMetrics(request, response);
        }
    }
    
    /**
     * Handle Prometheus metrics scraping requests.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws IOException if response writing fails
     */
    private void handlePrometheusMetrics(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        // Set response headers
        response.setContentType(MetricsEndpointConstants.Responses.CONTENT_TYPE_PROMETHEUS);
        response.setCharacterEncoding(MetricsEndpointConstants.Responses.CHARSET_UTF8);
        
        // Add cache control headers to prevent caching of metrics
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        
        try (PrintWriter writer = response.getWriter()) {
            
            // Check if metrics are enabled
            if (!MetricsConfig.ENABLED) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                writer.print(MetricsEndpointConstants.Responses.METRICS_DISABLED_MESSAGE);
                Logger.debug(this, "Metrics request denied - metrics disabled");
                return;
            }
            
            // Check if Prometheus is enabled
            if (!MetricsConfig.PROMETHEUS_ENABLED) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                writer.print(MetricsEndpointConstants.Responses.PROMETHEUS_DISABLED_MESSAGE);
                Logger.debug(this, "Prometheus metrics request denied - Prometheus disabled");
                return;
            }
            
            // Get the metrics service
            Optional<MetricsService> metricsServiceOpt = CDIUtils.getBean(MetricsService.class);
            if (!metricsServiceOpt.isPresent()) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                writer.print(MetricsEndpointConstants.Responses.SERVICE_UNAVAILABLE_MESSAGE);
                Logger.error(this, "MetricsService not available for metrics request");
                return;
            }
            
            MetricsService metricsService = metricsServiceOpt.get();
            PrometheusMeterRegistry prometheusRegistry = metricsService.getPrometheusRegistry();
            
            if (prometheusRegistry == null) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                writer.print(MetricsEndpointConstants.Responses.REGISTRY_NOT_CONFIGURED_MESSAGE);
                Logger.error(this, "Prometheus registry not configured for metrics request");
                return;
            }
            
            // Generate and return metrics
            String metricsContent = prometheusRegistry.scrape();
            writer.print(metricsContent);
            
            Logger.debug(this, "Served Prometheus metrics via management servlet (" + 
                        metricsContent.length() + " characters) to " + 
                        request.getRemoteAddr());
                        
        } catch (Exception e) {
            Logger.error(this, "Error serving Prometheus metrics via management servlet: " + e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            
            try (PrintWriter writer = response.getWriter()) {
                writer.print("# Error serving metrics: " + e.getMessage() + "\n");
            }
        }
    }
    
    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Support HEAD requests for health checks
        response.setContentType(MetricsEndpointConstants.Responses.CONTENT_TYPE_PROMETHEUS);
        
        if (!MetricsConfig.ENABLED || !MetricsConfig.PROMETHEUS_ENABLED) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        
        Optional<MetricsService> metricsServiceOpt = CDIUtils.getBean(MetricsService.class);
        if (!metricsServiceOpt.isPresent() || metricsServiceOpt.get().getPrometheusRegistry() == null) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        
        response.setStatus(HttpServletResponse.SC_OK);
    }
    
    @Override
    public String getServletInfo() {
        return "Management Metrics Servlet for dotCMS - exposes Prometheus metrics through management port infrastructure";
    }
} 