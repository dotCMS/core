package com.dotcms.management.servlet;

import com.dotcms.health.servlet.HealthProbeServlet;
import com.dotcms.health.service.HealthStateManager;
import com.dotcms.health.model.HealthResponse;
import com.dotcms.health.model.HealthStatus;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Management servlet providing infrastructure monitoring endpoints on a dedicated port.
 * This servlet is designed to be lightweight and serve only infrastructure endpoints
 * without authentication requirements, specifically for Kubernetes and monitoring tools.
 * 
 * Management Endpoints (unauthenticated, for infrastructure):
 * - /dotmgt/livez - Kubernetes liveness probe (minimal text: "alive" | "unhealthy")
 * - /dotmgt/readyz - Kubernetes readiness probe (minimal text: "ready" | "not ready")
 * - /dotmgt/metrics - Prometheus metrics endpoint (text/plain format)
 * - /dotmgt/health - Basic health status (minimal JSON)
 * 
 * This servlet is only active when management.port.enabled=true.
 * When disabled, these endpoints are served by the main application port.
 */
public class ManagementServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_TEXT = "text/plain";
    private static final String CHARSET_UTF8 = "UTF-8";
    
    private HealthStateManager healthStateManager;
    private ObjectMapper objectMapper;
    private boolean managementPortEnabled;
    private HealthProbeServlet healthProbeServlet;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        // Check if management port is enabled
        managementPortEnabled = Config.getBooleanProperty("management.port.enabled", true);
        
        if (!managementPortEnabled) {
            Logger.info(this, "Management port is disabled - ManagementServlet will not serve requests");
            return;
        }
        
        // Use the centralized ObjectMapper provider for consistent JSON serialization
        objectMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
        
        // Get singleton health state manager instance
        healthStateManager = HealthStateManager.getInstance();
        
        // Initialize health probe servlet for delegation
        healthProbeServlet = new HealthProbeServlet();
        try {
            healthProbeServlet.init(config);
        } catch (ServletException e) {
            Logger.error(this, "Failed to initialize HealthProbeServlet: " + e.getMessage(), e);
            throw e;
        }
        
        Logger.info(this, "ManagementServlet initialized for infrastructure monitoring endpoints");
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // If management port is disabled, return 404
        if (!managementPortEnabled) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType(CONTENT_TYPE_TEXT);
            response.setCharacterEncoding(CHARSET_UTF8);
            response.getWriter().write("Management port is disabled");
            return;
        }

        // Extract the actual endpoint from the request
        String endpoint = request.getServletPath();
        Logger.debug(this, "ManagementServlet.doGet called with endpoint: " + endpoint + ", URI: " + request.getRequestURI());
        
        // Route to appropriate handler based on endpoint
        switch (endpoint) {
            case "/dotmgt/livez":
                handleLivenessProbe(request, response);
                break;
            case "/dotmgt/readyz":
                handleReadinessProbe(request, response);
                break;
            case "/dotmgt/metrics":
                handleMetrics(request, response);
                break;
            case "/dotmgt/health":
                handleBasicHealth(request, response);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType(CONTENT_TYPE_TEXT);
                response.setCharacterEncoding(CHARSET_UTF8);
                response.getWriter().write("Endpoint not found. Available: /dotmgt/livez, /dotmgt/readyz, /dotmgt/metrics, /dotmgt/health");
                break;
        }
    }
    
    /**
     * Liveness probe endpoint - delegates to HealthProbeServlet for full health logic.
     * Returns minimal text response for Kubernetes compatibility.
     */
    private void handleLivenessProbe(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        // Delegate to HealthProbeServlet for sophisticated health checking and logging
        healthProbeServlet.handleLivenessProbe(request, response);
    }
    
    /**
     * Readiness probe endpoint - delegates to HealthProbeServlet for full health logic.
     * Returns minimal text response for Kubernetes compatibility.
     */
    private void handleReadinessProbe(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        // Delegate to HealthProbeServlet for sophisticated health checking and logging
        healthProbeServlet.handleReadinessProbe(request, response);
    }
    
    /**
     * Metrics endpoint - provides Prometheus-compatible metrics.
     * Currently returns basic health metrics in text format.
     */
    private void handleMetrics(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        // For now, provide basic health metrics in Prometheus format
        // This can be extended to include more detailed metrics
        HealthResponse livenessHealth = healthStateManager.getLivenessHealth();
        HealthResponse readinessHealth = healthStateManager.getReadinessHealth();
        
        StringBuilder metrics = new StringBuilder();
        
        // Add basic health metrics
        metrics.append("# HELP dotcms_health_status Health status of dotCMS (0=DOWN, 1=DEGRADED, 2=UP)\n");
        metrics.append("# TYPE dotcms_health_status gauge\n");
        
        // Liveness metrics
        int livenessStatus = getHealthStatusValue(livenessHealth.status());
        metrics.append("dotcms_health_status{type=\"liveness\"} ").append(livenessStatus).append("\n");
        
        // Readiness metrics
        int readinessStatus = getHealthStatusValue(readinessHealth.status());
        metrics.append("dotcms_health_status{type=\"readiness\"} ").append(readinessStatus).append("\n");
        
        // Add health check counts
        metrics.append("# HELP dotcms_health_checks_total Total number of health checks\n");
        metrics.append("# TYPE dotcms_health_checks_total gauge\n");
        metrics.append("dotcms_health_checks_total{type=\"liveness\"} ").append(livenessHealth.checks().size()).append("\n");
        metrics.append("dotcms_health_checks_total{type=\"readiness\"} ").append(readinessHealth.checks().size()).append("\n");
        
        // Add individual health check metrics
        for (var check : livenessHealth.checks()) {
            int checkStatus = getHealthStatusValue(check.status());
            metrics.append("dotcms_health_check_status{type=\"liveness\",name=\"").append(check.name()).append("\"} ").append(checkStatus).append("\n");
        }
        
        for (var check : readinessHealth.checks()) {
            int checkStatus = getHealthStatusValue(check.status());
            metrics.append("dotcms_health_check_status{type=\"readiness\",name=\"").append(check.name()).append("\"} ").append(checkStatus).append("\n");
        }
        
        // Add uptime metric
        long uptimeSeconds = healthStateManager.getUptimeSeconds();
        metrics.append("# HELP dotcms_uptime_seconds Uptime of the dotCMS instance in seconds\n");
        metrics.append("# TYPE dotcms_uptime_seconds counter\n");
        metrics.append("dotcms_uptime_seconds ").append(uptimeSeconds).append("\n");
        
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(CONTENT_TYPE_TEXT);
        response.setCharacterEncoding(CHARSET_UTF8);
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        
        try (PrintWriter writer = response.getWriter()) {
            writer.write(metrics.toString());
            writer.flush();
        }
        
        Logger.debug(this, "Metrics endpoint served " + metrics.length() + " characters");
    }
    
    /**
     * Basic health endpoint - delegates to HealthProbeServlet for full health logic.
     * This is different from the detailed /api/v1/health endpoint.
     */
    private void handleBasicHealth(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        // Delegate to HealthProbeServlet for sophisticated health checking and logging
        healthProbeServlet.handleFullHealth(request, response);
    }
    
    /**
     * Writes a text response with appropriate headers.
     */
    private void writeTextResponse(HttpServletResponse response, String content) throws IOException {
        response.setContentType(CONTENT_TYPE_TEXT);
        response.setCharacterEncoding(CHARSET_UTF8);
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        
        try (PrintWriter writer = response.getWriter()) {
            writer.write(content);
            writer.flush();
        }
    }
    
    /**
     * Converts HealthStatus to numeric value for Prometheus metrics.
     */
    private int getHealthStatusValue(HealthStatus status) {
        switch (status) {
            case DOWN:
                return 0;
            case DEGRADED:
            case UNKNOWN:
                return 1;
            case UP:
                return 2;
            default:
                return 0;
        }
    }
    
    @Override
    public void destroy() {
        if (healthProbeServlet != null) {
            healthProbeServlet.destroy();
        }
        super.destroy();
        Logger.info(this, "ManagementServlet destroyed");
    }
}