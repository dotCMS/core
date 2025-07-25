package com.dotcms.metrics;

import com.dotmarketing.util.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unified request tracking utility that provides metrics collection and
 * shutdown coordination for HTTP requests.
 * 
 * This class is thread-safe and designed to be used by both the RequestTrackingFilter
 * and the HttpRequestMetrics binder to avoid duplication and ensure consistency.
 * 
 * Key responsibilities:
 * - Track active request count for graceful shutdown
 * - Collect HTTP status code distributions 
 * - Track endpoint-specific request counts
 * - Measure request durations
 * - Support both metrics collection and shutdown coordination
 * 
 * @author dotCMS Team
 */
public class RequestTracker {
    
    private static volatile RequestTracker instance;
    
    // Thread-safe counters for metrics
    private final ConcurrentHashMap<String, AtomicLong> statusCodeCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> endpointCounts = new ConcurrentHashMap<>();
    private final AtomicLong activeRequests = new AtomicLong(0);
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalDuration = new AtomicLong(0);
    
    // Thread-local for request timing
    private final ThreadLocal<Long> requestStartTime = new ThreadLocal<>();
    
    private RequestTracker() {
        // Private constructor for singleton
    }
    
    /**
     * Get the singleton instance of the request tracker.
     * 
     * @return RequestTracker instance
     */
    public static RequestTracker getInstance() {
        if (instance == null) {
            synchronized (RequestTracker.class) {
                if (instance == null) {
                    instance = new RequestTracker();
                }
            }
        }
        return instance;
    }
    
    /**
     * Mark the start of a request. This method should be called at the beginning
     * of request processing to track active requests and timing.
     * 
     * @param request the HTTP request
     */
    public void requestStarted(HttpServletRequest request) {
        try {
            // Track active requests for shutdown coordination
            long currentActive = activeRequests.incrementAndGet();
            totalRequests.incrementAndGet();
            
            // Store start time for duration tracking
            requestStartTime.set(System.currentTimeMillis());
            
            Logger.debug(this, () -> String.format("Request started: %s %s (active: %d)", 
                request.getMethod(), request.getRequestURI(), currentActive));
                
        } catch (Exception e) {
            Logger.warn(this, "Failed to track request start: " + e.getMessage(), e);
        }
    }
    
    /**
     * Mark the end of a request. This method should be called at the end
     * of request processing to update metrics and active request count.
     * 
     * @param request the HTTP request
     * @param response the HTTP response
     */
    public void requestEnded(HttpServletRequest request, HttpServletResponse response) {
        try {
            // Calculate request duration
            Long startTime = requestStartTime.get();
            final long duration;
            if (startTime != null) {
                duration = System.currentTimeMillis() - startTime;
                totalDuration.addAndGet(duration);
                requestStartTime.remove();
            } else {
                duration = 0;
            }
            
            // Track active requests
            final long currentActive = activeRequests.decrementAndGet();
            
            // Track status code distribution
            incrementStatusCode(response.getStatus());
            
            // Track endpoint-specific counts
            incrementEndpoint(request.getRequestURI());
            
            Logger.debug(this, () -> String.format("Request ended: %s %s -> %d (duration: %dms, active: %d)", 
                request.getMethod(), request.getRequestURI(), response.getStatus(), duration, currentActive));
                
        } catch (Exception e) {
            Logger.warn(this, "Failed to track request end: " + e.getMessage(), e);
        }
    }
    
    /**
     * Increment status code counter for metrics collection.
     * 
     * @param statusCode the HTTP status code
     */
    public void incrementStatusCode(int statusCode) {
        String range = getStatusCodeRange(statusCode);
        statusCodeCounts.computeIfAbsent(range, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * Increment endpoint counter for metrics collection.
     * 
     * @param uri the request URI
     */
    public void incrementEndpoint(String uri) {
        String pattern = getEndpointPattern(uri);
        if (pattern != null) {
            endpointCounts.computeIfAbsent(pattern, k -> new AtomicLong(0)).incrementAndGet();
        }
    }
    
    // ====================================================================
    // GETTER METHODS FOR METRICS COLLECTION
    // ====================================================================
    
    /**
     * Get the current number of active requests.
     * 
     * @return active request count
     */
    public long getActiveRequests() {
        return activeRequests.get();
    }
    
    /**
     * Get the total number of requests processed.
     * 
     * @return total request count
     */
    public long getTotalRequests() {
        return totalRequests.get();
    }
    
    /**
     * Get the total duration of all requests in milliseconds.
     * 
     * @return total duration in milliseconds
     */
    public long getTotalDuration() {
        return totalDuration.get();
    }
    
    /**
     * Get the average request duration in milliseconds.
     * 
     * @return average duration in milliseconds
     */
    public double getAverageDuration() {
        long total = totalRequests.get();
        return total > 0 ? (double) totalDuration.get() / total : 0.0;
    }
    
    /**
     * Get count for specific status code range.
     * 
     * @param statusRange the status code range (e.g., "2xx", "4xx")
     * @return count of responses in that range
     */
    public long getStatusCodeCount(String statusRange) {
        AtomicLong count = statusCodeCounts.get(statusRange);
        return count != null ? count.get() : 0;
    }
    
    /**
     * Get count for specific endpoint pattern.
     * 
     * @param endpointPattern the endpoint pattern (e.g., "/api/")
     * @return count of requests to that endpoint pattern
     */
    public long getEndpointCount(String endpointPattern) {
        AtomicLong count = endpointCounts.get(endpointPattern);
        return count != null ? count.get() : 0;
    }
    
    /**
     * Calculate overall error rate percentage.
     * 
     * @return error rate as percentage (0-100)
     */
    public double getErrorRate() {
        long total2xx = getStatusCodeCount("2xx");
        long total3xx = getStatusCodeCount("3xx");
        long total4xx = getStatusCodeCount("4xx");
        long total5xx = getStatusCodeCount("5xx");
        
        long totalRequests = total2xx + total3xx + total4xx + total5xx;
        long errorRequests = total4xx + total5xx;
        
        return totalRequests > 0 ? ((double) errorRequests / totalRequests) * 100 : 0.0;
    }
    
    // ====================================================================
    // HELPER METHODS
    // ====================================================================
    
    /**
     * Get status code range category for a status code.
     * 
     * @param statusCode the HTTP status code
     * @return status code range (2xx, 3xx, 4xx, 5xx, other)
     */
    private String getStatusCodeRange(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) return "2xx";
        if (statusCode >= 300 && statusCode < 400) return "3xx";
        if (statusCode >= 400 && statusCode < 500) return "4xx";
        if (statusCode >= 500 && statusCode < 600) return "5xx";
        return "other";
    }
    
    /**
     * Get endpoint pattern for a URI.
     * 
     * @param uri the request URI
     * @return endpoint pattern or null if not a tracked pattern
     */
    private String getEndpointPattern(String uri) {
        if (uri == null) return null;
        
        if (uri.startsWith("/api/")) return "/api/";
        if (uri.startsWith("/contentAsset/")) return "/contentAsset/";
        if (uri.startsWith("/dotAdmin/")) return "/dotAdmin/";
        if (uri.startsWith("/dotmgt/")) return "/dotmgt/";
        
        return null; // Only track key endpoint patterns
    }
    
    /**
     * Get all status code counts for debugging/monitoring.
     * 
     * @return copy of status code counts map
     */
    public ConcurrentHashMap<String, Long> getAllStatusCodeCounts() {
        ConcurrentHashMap<String, Long> result = new ConcurrentHashMap<>();
        statusCodeCounts.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }
    
    /**
     * Get all endpoint counts for debugging/monitoring.
     * 
     * @return copy of endpoint counts map
     */
    public ConcurrentHashMap<String, Long> getAllEndpointCounts() {
        ConcurrentHashMap<String, Long> result = new ConcurrentHashMap<>();
        endpointCounts.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }
}