package com.dotcms.health.filter;

import com.dotcms.health.util.HealthCheckEndpointUtil;
import com.dotmarketing.util.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter that handles health check endpoints.
 * 
 * CRITICAL: This filter must be configured AFTER essential security filters but BEFORE
 * expensive filters that might:
 * - Access the database (which could hang during DB outages)
 * - Perform expensive operations 
 * - Require external dependencies
 * - Block during system startup/shutdown
 * 
 * When a health check endpoint is detected, this filter will immediately forward
 * to the HealthProbeServlet, completely bypassing the remaining filter chain.
 * This ensures health checks remain fast, reliable, and resilient to system issues.
 * 
 * Filter Chain Order Requirements:
 * 1. NormalizationFilter (URI security validation)
 * 2. HttpHeaderSecurityFilter (Security headers)
 * 3. CookiesFilter (Cookie security handling)
 * 4. HealthCheckFilter (Health check bypass)
 * 5. All other filters (bypassed for health endpoints)...
 * 
 * IMPORTANT: This filter is registered programmatically via FilterRegistration.java
 * Do NOT add @WebFilter annotation - it would create duplicate registrations.
 * 
 * To add new filters that run AFTER the ordered filter chain, use @WebFilter annotation.
 * See FilterRegistration.java for the complete ordered filter chain configuration.
 */
public class HealthCheckFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Logger.info(this, "HealthCheckFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String servletPath = httpRequest.getServletPath();

        // Check if this is a health check endpoint using the centralized utility
        if (HealthCheckEndpointUtil.isHealthCheckEndpoint(servletPath)) {
            Logger.debug(this, "Health check endpoint detected, dispatching to HealthProbeServlet: " + servletPath);
            
            // Forward the request directly to HealthProbeServlet, bypassing remaining filters
            // This ensures health checks avoid expensive filters while still reaching the servlet
            try {
                RequestDispatcher dispatcher = request.getRequestDispatcher(servletPath);
                if (dispatcher != null) {
                    dispatcher.forward(request, response);
                } else {
                    Logger.error(this, "Unable to get RequestDispatcher for health endpoint: " + servletPath);
                    HttpServletResponse httpResponse = (HttpServletResponse) response;
                    httpResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    httpResponse.getWriter().write("Health check service unavailable");
                }
            } catch (Exception e) {
                Logger.error(this, "Error forwarding health check request: " + e.getMessage(), e);
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                httpResponse.getWriter().write("Health check error");
            }
            return;
        }
        
        // Not a health check endpoint, proceed with normal filter chain
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        Logger.info(this, "HealthCheckFilter destroyed");
    }
} 