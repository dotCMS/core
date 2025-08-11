package com.dotcms.filters;

import com.dotcms.metrics.RequestTracker;
import com.dotmarketing.util.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Request tracking filter that provides unified metrics collection and
 * shutdown coordination for HTTP requests.
 * 
 * This filter should be placed early in the filter chain (after NormalizationFilter
 * but before other processing filters) to ensure accurate tracking of all requests.
 * 
 * Key responsibilities:
 * - Track active request count for graceful shutdown via ShutdownCoordinator
 * - Collect HTTP metrics for monitoring via RequestTracker
 * - Measure request durations and status code distributions
 * - Support endpoint-specific tracking for key dotCMS endpoints
 * 
 * This filter replaces the RequestTrackingInterceptor to provide more accurate
 * tracking at the filter level and better integration with metrics collection.
 * 
 * @author dotCMS Team
 */
public class RequestTrackingFilter implements Filter {
    
    private RequestTracker requestTracker;
    private boolean trackingEnabled = true;
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            requestTracker = RequestTracker.getInstance();
            
            // Check if tracking should be disabled via init parameter
            String trackingParam = filterConfig.getInitParameter("requestTrackingEnabled");
            if ("false".equalsIgnoreCase(trackingParam)) {
                trackingEnabled = false;
                Logger.info(this, "Request tracking disabled via init parameter");
            } else {
                Logger.info(this, "RequestTrackingFilter initialized - unified request tracking enabled");
            }
            
        } catch (Exception e) {
            Logger.error(this, "Failed to initialize RequestTrackingFilter: " + e.getMessage(), e);
            throw new ServletException("RequestTrackingFilter initialization failed", e);
        }
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        // Skip processing if request should not be evaluated
        if (!shouldEvaluateRequest(request, response)) {
            chain.doFilter(request, response);
            return;
        }
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Mark request start for tracking
        requestTracker.requestStarted(httpRequest);
        
        try {
            // Process the request through the filter chain
            chain.doFilter(request, response);
            
        } finally {
            // Always mark request end, even if an exception occurred
            try {
                requestTracker.requestEnded(httpRequest, httpResponse);
            } catch (Exception e) {
                // Don't let tracking errors affect the response
                Logger.debug(this, "Error during request end tracking: " + e.getMessage());
            }
        }
    }
    
    /**
     * Determines if the request should be evaluated for tracking.
     * Returns false if the request is not HTTP or if tracking is disabled.
     * 
     * @param request the servlet request
     * @param response the servlet response
     * @return true if the request should be tracked, false otherwise
     */
    private boolean shouldEvaluateRequest(ServletRequest request, ServletResponse response) {
        // Only process HTTP requests
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            return false;
        }
        
        // Skip tracking if disabled
        return trackingEnabled && requestTracker != null;
    }
    
    @Override
    public void destroy() {
        try {
            Logger.info(this, "RequestTrackingFilter destroyed");
            
            // Log final statistics
            if (requestTracker != null && trackingEnabled) {
                long totalRequests = requestTracker.getTotalRequests();
                long activeRequests = requestTracker.getActiveRequests();
                double averageDuration = requestTracker.getAverageDuration();
                double errorRate = requestTracker.getErrorRate();
                
                Logger.info(this, String.format("Request tracking statistics - Total: %d, Active: %d, Avg Duration: %.2fms, Error Rate: %.2f%%",
                    totalRequests, activeRequests, averageDuration, errorRate));
            }
            
        } catch (Exception e) {
            Logger.warn(this, "Error during RequestTrackingFilter destruction: " + e.getMessage());
        }
    }
}