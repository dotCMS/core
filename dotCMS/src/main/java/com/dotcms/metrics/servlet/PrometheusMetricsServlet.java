package com.dotcms.metrics.servlet;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.metrics.MetricsConfig;
import com.dotcms.metrics.MetricsService;
import com.dotmarketing.util.Logger;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

/**
 * Servlet for exposing Prometheus metrics at the /metrics endpoint.
 * 
 * This servlet provides a traditional servlet-based approach for exposing
 * Prometheus metrics, which can be useful for monitoring systems that
 * expect metrics at a specific path without the REST API prefix.
 * 
 * The servlet is lightweight and designed for high-frequency scraping
 * by monitoring systems like Prometheus.
 */
public class PrometheusMetricsServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    private static final String CONTENT_TYPE_PROMETHEUS = "text/plain; version=0.0.4; charset=utf-8";
    private static final String CLASS_NAME = PrometheusMetricsServlet.class.getSimpleName();
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Set response headers
        response.setContentType(CONTENT_TYPE_PROMETHEUS);
        response.setCharacterEncoding("UTF-8");
        
        // Add cache control headers to prevent caching of metrics
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        
        try (PrintWriter writer = response.getWriter()) {
            
            // Check if metrics are enabled
            if (!MetricsConfig.ENABLED) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                writer.println("# Metrics collection is disabled");
                Logger.debug(this, "Metrics request denied - metrics disabled");
                return;
            }
            
            // Check if Prometheus is enabled
            if (!MetricsConfig.PROMETHEUS_ENABLED) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                writer.println("# Prometheus metrics are disabled");
                Logger.debug(this, "Prometheus metrics request denied - Prometheus disabled");
                return;
            }
            
            // Get the metrics service
            Optional<MetricsService> metricsServiceOpt = CDIUtils.getBean(MetricsService.class);
            if (!metricsServiceOpt.isPresent()) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                writer.println("# MetricsService not available");
                Logger.error(this, "MetricsService not available for metrics request");
                return;
            }
            
            MetricsService metricsService = metricsServiceOpt.get();
            PrometheusMeterRegistry prometheusRegistry = metricsService.getPrometheusRegistry();
            
            if (prometheusRegistry == null) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                writer.println("# Prometheus registry not configured");
                Logger.error(this, "Prometheus registry not configured for metrics request");
                return;
            }
            
            // Generate and return metrics
            String metricsContent = prometheusRegistry.scrape();
            writer.print(metricsContent);
            
            Logger.debug(this, "Served Prometheus metrics via servlet (" + 
                        metricsContent.length() + " characters) to " + 
                        request.getRemoteAddr());
                        
        } catch (Exception e) {
            Logger.error(this, "Error serving Prometheus metrics via servlet: " + e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            
            try (PrintWriter writer = response.getWriter()) {
                writer.println("# Error serving metrics: " + e.getMessage());
            }
        }
    }
    
    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Support HEAD requests for health checks
        response.setContentType(CONTENT_TYPE_PROMETHEUS);
        
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
        return "Prometheus Metrics Servlet for dotCMS - exposes application metrics in Prometheus format";
    }
}