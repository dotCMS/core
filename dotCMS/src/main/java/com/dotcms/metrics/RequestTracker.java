package com.dotcms.metrics;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    
    // Configurable endpoint patterns
    private final List<String> trackedEndpointPatterns;
    private final boolean endpointTrackingEnabled;
    private final int maxEndpointPatterns;
    
    private RequestTracker() {
        // Load configuration for endpoint tracking
        this.endpointTrackingEnabled = Config.getBooleanProperty("metrics.endpoints.tracking.enabled", true);
        this.maxEndpointPatterns = Config.getIntProperty("metrics.endpoints.tracking.max_patterns", 10);
        
        // Load tracked endpoint patterns from configuration
        String patternsConfig = Config.getStringProperty("metrics.endpoints.tracking.patterns", 
            "/api/,/contentAsset/,/dotAdmin/,/dotmgt/");
        
        if (UtilMethods.isSet(patternsConfig)) {
            this.trackedEndpointPatterns = Arrays.asList(patternsConfig.split(","));
            // Trim whitespace from patterns
            this.trackedEndpointPatterns.replaceAll(String::trim);
            
            Logger.info(this, "RequestTracker endpoint tracking enabled with patterns: " + this.trackedEndpointPatterns);
        } else {
            this.trackedEndpointPatterns = Collections.emptyList();
            Logger.info(this, "RequestTracker endpoint tracking disabled - no patterns configured");
        }
        
        // Log configuration
        Logger.info(this, String.format("RequestTracker initialized - endpoint tracking: %s, max patterns: %d, patterns: %s",
            endpointTrackingEnabled, maxEndpointPatterns, trackedEndpointPatterns));
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
        StatusCodeRange range = StatusCodeRange.fromStatusCode(statusCode);
        statusCodeCounts.computeIfAbsent(range.getLabel(), k -> new AtomicLong(0)).incrementAndGet();
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
        long totalRequests = 0;
        long errorRequests = 0;
        
        for (StatusCodeRange range : StatusCodeRange.values()) {
            if (range == StatusCodeRange.OTHER) {
                continue; // Skip OTHER category for rate calculation
            }
            
            long count = getStatusCodeCount(range.getLabel());
            totalRequests += count;
            
            if (range.isError()) {
                errorRequests += count;
            }
        }
        
        return totalRequests > 0 ? ((double) errorRequests / totalRequests) * 100 : 0.0;
    }
    
    // ====================================================================
    // HELPER METHODS
    // ====================================================================
    
    
    /**
     * Get endpoint pattern for a URI based on configured tracking patterns.
     * This method checks if the URI matches any of the configured endpoint patterns
     * and returns the matching pattern for metrics tracking.
     * 
     * @param uri the request URI
     * @return endpoint pattern or null if not a tracked pattern
     */
    private String getEndpointPattern(String uri) {
        if (uri == null || !endpointTrackingEnabled) {
            return null;
        }
        
        // Check current endpoint count against maximum to prevent explosion
        if (endpointCounts.size() >= maxEndpointPatterns) {
            Logger.debug(this, () -> "Maximum endpoint patterns reached (" + maxEndpointPatterns + 
                "). Not tracking new pattern for URI: " + uri);
            return null;
        }
        
        // Find the first matching pattern
        for (String pattern : trackedEndpointPatterns) {
            if (UtilMethods.isSet(pattern) && uri.startsWith(pattern)) {
                return pattern;
            }
        }
        
        return null; // No matching pattern found
    }
    
    /**
     * Get the list of currently configured endpoint patterns.
     * This is useful for debugging and monitoring configuration.
     * 
     * @return immutable list of configured endpoint patterns
     */
    public List<String> getConfiguredEndpointPatterns() {
        return Collections.unmodifiableList(trackedEndpointPatterns);
    }
    
    /**
     * Check if endpoint tracking is enabled.
     * 
     * @return true if endpoint tracking is enabled, false otherwise
     */
    public boolean isEndpointTrackingEnabled() {
        return endpointTrackingEnabled;
    }
    
    /**
     * Get the maximum number of endpoint patterns allowed.
     * 
     * @return maximum endpoint pattern limit
     */
    public int getMaxEndpointPatterns() {
        return maxEndpointPatterns;
    }
    
    /**
     * Get all status code counts for metrics collection.
     * Returns an immutable snapshot of current status code counts.
     * 
     * @return immutable copy of status code counts map
     */
    public Map<String, Long> getAllStatusCodeCounts() {
        Map<String, Long> result = new HashMap<>();
        statusCodeCounts.forEach((key, value) -> result.put(key, value.get()));
        return Collections.unmodifiableMap(result);
    }
    
    /**
     * Get all endpoint counts for metrics collection.
     * Returns an immutable snapshot of current endpoint counts.
     * 
     * @return immutable copy of endpoint counts map
     */
    public Map<String, Long> getAllEndpointCounts() {
        Map<String, Long> result = new HashMap<>();
        endpointCounts.forEach((key, value) -> result.put(key, value.get()));
        return Collections.unmodifiableMap(result);
    }
}