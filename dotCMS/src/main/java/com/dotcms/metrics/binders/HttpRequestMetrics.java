package com.dotcms.metrics.binders;

import com.dotcms.metrics.RequestTracker;
import com.dotcms.metrics.StatusCodeRange;
import com.dotmarketing.util.Logger;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive metric binder for dotCMS HTTP request monitoring.
 * 
 * This binder integrates with the RequestTracker to provide essential HTTP metrics including:
 * - Request duration histograms and averages
 * - HTTP status code distribution
 * - Request throughput and error rates
 * - Active request count
 * - Endpoint-specific request counts
 * - Tomcat-level request processor metrics
 * 
 * These metrics are critical for monitoring application performance,
 * identifying slow endpoints, and detecting error patterns.
 */
public class HttpRequestMetrics implements MeterBinder {
    
    private static final String METRIC_PREFIX = "dotcms.http";
    private final MBeanServer mBeanServer;
    private final RequestTracker requestTracker;
    
    public HttpRequestMetrics() {
        this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
        this.requestTracker = RequestTracker.getInstance();
    }
    
    @Override
    public void bindTo(MeterRegistry registry) {
        try {
            registerTomcatRequestMetrics(registry);
            registerRequestTrackerMetrics(registry);
            registerPerformanceMetrics(registry);
            
            Logger.info(this, "HTTP request metrics registered successfully");
            
        } catch (Exception e) {
            Logger.error(this, "Failed to register HTTP request metrics: " + e.getMessage(), e);
        }
    }
    
    /**
     * Register Tomcat-level HTTP request metrics from MBeans.
     */
    private void registerTomcatRequestMetrics(MeterRegistry registry) {
        try {
            Set<ObjectName> processors = mBeanServer.queryNames(
                new ObjectName("Catalina:type=GlobalRequestProcessor,name=*"), null);
            
            for (ObjectName processor : processors) {
                String processorName = processor.getKeyProperty("name");
                
                // Total requests
                Gauge.builder(METRIC_PREFIX + ".requests.total", this,
                    metrics -> getRequestProcessorAttribute(processor, "requestCount"))
                    .description("Total HTTP requests processed")
                    .tag("processor", processorName)
                    .register(registry);
                
                // Error count (4xx + 5xx responses)
                Gauge.builder(METRIC_PREFIX + ".requests.errors", this,
                    metrics -> getRequestProcessorAttribute(processor, "errorCount"))
                    .description("Total HTTP request errors")
                    .tag("processor", processorName)
                    .register(registry);
                
                // Average processing time
                Gauge.builder(METRIC_PREFIX + ".requests.duration.avg_ms", this,
                    metrics -> getAverageProcessingTime(processor))
                    .description("Average HTTP request processing time in milliseconds")
                    .tag("processor", processorName)
                    .register(registry);
                
                // Max processing time
                Gauge.builder(METRIC_PREFIX + ".requests.duration.max_ms", this,
                    metrics -> getRequestProcessorAttribute(processor, "maxTime"))
                    .description("Maximum HTTP request processing time in milliseconds")
                    .tag("processor", processorName)
                    .register(registry);
                
                // Bytes received
                Gauge.builder(METRIC_PREFIX + ".requests.bytes.received", this,
                    metrics -> getRequestProcessorAttribute(processor, "bytesReceived"))
                    .description("Total bytes received in HTTP requests")
                    .tag("processor", processorName)
                    .register(registry);
                
                // Bytes sent  
                Gauge.builder(METRIC_PREFIX + ".requests.bytes.sent", this,
                    metrics -> getRequestProcessorAttribute(processor, "bytesSent"))
                    .description("Total bytes sent in HTTP responses")
                    .tag("processor", processorName)
                    .register(registry);
                
                // Request rate (requests per second)
                Gauge.builder(METRIC_PREFIX + ".requests.rate.rps", this,
                    metrics -> getRequestRate(processor))
                    .description("HTTP requests per second")
                    .tag("processor", processorName)
                    .register(registry);
            }
            
        } catch (Exception e) {
            Logger.warn(this, "Failed to register Tomcat request metrics: " + e.getMessage());
        }
    }
    
    /**
     * Register RequestTracker-based HTTP metrics (real-time application metrics).
     */
    private void registerRequestTrackerMetrics(MeterRegistry registry) {
        // Active request count from RequestTracker
        Gauge.builder(METRIC_PREFIX + ".requests.active", requestTracker, RequestTracker::getActiveRequests)
            .description("Number of currently active HTTP requests")
            .register(registry);
        
        // Total request count from RequestTracker
        Gauge.builder(METRIC_PREFIX + ".requests.total_tracked", requestTracker, RequestTracker::getTotalRequests)
            .description("Total number of HTTP requests tracked by RequestTracker")
            .register(registry);
        
        // Average request duration from RequestTracker
        Gauge.builder(METRIC_PREFIX + ".requests.duration.avg_ms_tracked", requestTracker, RequestTracker::getAverageDuration)
            .description("Average HTTP request duration in milliseconds from RequestTracker")
            .register(registry);
        
        // Error rate from RequestTracker
        Gauge.builder(METRIC_PREFIX + ".requests.error_rate_tracked", requestTracker, RequestTracker::getErrorRate)
            .description("HTTP error rate percentage from RequestTracker")
            .register(registry);
        
        // HTTP status code distributions from RequestTracker
        registerRequestTrackerStatusCodeMetrics(registry);
        
        // Endpoint-specific metrics from RequestTracker
        registerRequestTrackerEndpointMetrics(registry);
    }
    
    /**
     * Register HTTP status code distribution metrics from RequestTracker.
     * Uses dynamic registration based on actual status code ranges encountered.
     */
    private void registerRequestTrackerStatusCodeMetrics(MeterRegistry registry) {
        // Register metrics for all defined status code ranges
        for (StatusCodeRange range : StatusCodeRange.values()) {
            final String rangeLabel = range.getLabel();
            
            Gauge.builder(METRIC_PREFIX + ".responses." + rangeLabel, requestTracker,
                tracker -> tracker.getStatusCodeCount(rangeLabel))
                .description("Number of " + rangeLabel + " HTTP responses from RequestTracker")
                .tag("status_range", rangeLabel)
                .tag("is_error", String.valueOf(range.isError()))
                .register(registry);
        }
        
        // Also register dynamic metrics for any status codes that might be tracked at runtime
        // This catches any status codes that might be encountered but not in the enum
        Map<String, Long> currentStatusCounts = requestTracker.getAllStatusCodeCounts();
        for (String statusRange : currentStatusCounts.keySet()) {
            // Skip if already registered by the enum loop above
            boolean alreadyRegistered = false;
            for (StatusCodeRange range : StatusCodeRange.values()) {
                if (range.getLabel().equals(statusRange)) {
                    alreadyRegistered = true;
                    break;
                }
            }
            
            if (!alreadyRegistered) {
                Gauge.builder(METRIC_PREFIX + ".responses." + statusRange, requestTracker,
                    tracker -> tracker.getStatusCodeCount(statusRange))
                    .description("Number of " + statusRange + " HTTP responses from RequestTracker (dynamic)")
                    .tag("status_range", statusRange)
                    .tag("is_error", "unknown")
                    .register(registry);
            }
        }
    }
    
    /**
     * Register endpoint-specific metrics for key dotCMS endpoints from RequestTracker.
     * Uses dynamic registration based on actual endpoints being tracked.
     */
    private void registerRequestTrackerEndpointMetrics(MeterRegistry registry) {
        // Get all currently tracked endpoints dynamically
        Map<String, Long> currentEndpointCounts = requestTracker.getAllEndpointCounts();
        
        for (String endpoint : currentEndpointCounts.keySet()) {
            // Create a safe metric name from the endpoint pattern
            String metricName = sanitizeEndpointForMetric(endpoint);
            
            Gauge.builder(METRIC_PREFIX + ".endpoints.requests", requestTracker,
                tracker -> tracker.getEndpointCount(endpoint))
                .description("Number of requests to " + endpoint + " endpoints from RequestTracker")
                .tag("endpoint", endpoint)
                .tag("endpoint_name", metricName)
                .register(registry);
        }
        
        // Also register configured endpoint patterns even if no requests have been seen yet
        // This ensures consistent metrics even during startup
        for (String configuredEndpoint : requestTracker.getConfiguredEndpointPatterns()) {
            if (!currentEndpointCounts.containsKey(configuredEndpoint)) {
                String metricName = sanitizeEndpointForMetric(configuredEndpoint);
                
                Gauge.builder(METRIC_PREFIX + ".endpoints.requests", requestTracker,
                    tracker -> tracker.getEndpointCount(configuredEndpoint))
                    .description("Number of requests to " + configuredEndpoint + " endpoints from RequestTracker")
                    .tag("endpoint", configuredEndpoint)
                    .tag("endpoint_name", metricName)
                    .register(registry);
            }
        }
    }
    
    /**
     * Convert endpoint pattern to a safe metric name.
     * Removes slashes and special characters to create valid metric names.
     * 
     * @param endpoint the endpoint pattern (e.g., "/api/", "/contentAsset/")
     * @return sanitized name for use in metric names (e.g., "api", "contentAsset")
     */
    private String sanitizeEndpointForMetric(String endpoint) {
        if (endpoint == null) {
            return "unknown";
        }
        
        // Remove leading/trailing slashes and convert to lowercase
        String sanitized = endpoint.replaceAll("^/+|/+$", "").toLowerCase();
        
        // Replace remaining slashes with underscores
        sanitized = sanitized.replaceAll("/", "_");
        
        // Handle empty string
        if (sanitized.isEmpty()) {
            return "root";
        }
        
        return sanitized;
    }
    
    /**
     * Register performance and throughput metrics.
     */
    private void registerPerformanceMetrics(MeterRegistry registry) {
        // Overall error rate from Tomcat
        Gauge.builder(METRIC_PREFIX + ".performance.error_rate_tomcat", this,
            metrics -> getTomcatErrorRate())
            .description("Overall error rate from Tomcat metrics")
            .register(registry);
        
        // Throughput (requests per minute)
        Gauge.builder(METRIC_PREFIX + ".performance.throughput_rpm", this,
            metrics -> getThroughputRPM())
            .description("Request throughput in requests per minute")
            .register(registry);
    }
    
    // ====================================================================
    // HELPER METHODS FOR ACCESSING TOMCAT METRICS
    // ====================================================================
    
    private double getRequestProcessorAttribute(ObjectName objectName, String attributeName) {
        try {
            Object value = mBeanServer.getAttribute(objectName, attributeName);
            return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get request processor attribute " + attributeName + ": " + e.getMessage());
            return 0.0;
        }
    }
    
    private double getAverageProcessingTime(ObjectName processor) {
        try {
            double totalTime = getRequestProcessorAttribute(processor, "processingTime");
            double requestCount = getRequestProcessorAttribute(processor, "requestCount");
            return requestCount > 0 ? totalTime / requestCount : 0.0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to calculate average processing time: " + e.getMessage());
            return 0.0;
        }
    }
    
    private double getRequestRate(ObjectName processor) {
        try {
            // Simple approximation - would need time tracking for accurate rate
            double requestCount = getRequestProcessorAttribute(processor, "requestCount");
            // Assuming server uptime, this is very rough - ideally track over time windows
            return requestCount > 0 ? requestCount / 3600 : 0.0; // Very rough RPS estimate
        } catch (Exception e) {
            Logger.debug(this, "Failed to calculate request rate: " + e.getMessage());
            return 0.0;
        }
    }
    
    private double getTomcatErrorRate() {
        try {
            Set<ObjectName> processors = mBeanServer.queryNames(
                new ObjectName("Catalina:type=GlobalRequestProcessor,name=*"), null);
            
            double totalRequests = 0;
            double totalErrors = 0;
            
            for (ObjectName processor : processors) {
                totalRequests += getRequestProcessorAttribute(processor, "requestCount");
                totalErrors += getRequestProcessorAttribute(processor, "errorCount");
            }
            
            return totalRequests > 0 ? (totalErrors / totalRequests) * 100 : 0.0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to calculate Tomcat error rate: " + e.getMessage());
            return 0.0;
        }
    }
    
    private double getThroughputRPM() {
        try {
            Set<ObjectName> processors = mBeanServer.queryNames(
                new ObjectName("Catalina:type=GlobalRequestProcessor,name=*"), null);
            
            double totalRequests = 0;
            for (ObjectName processor : processors) {
                totalRequests += getRequestProcessorAttribute(processor, "requestCount");
            }
            
            // Very rough estimate - would need proper time tracking
            return totalRequests / 60; // Rough RPM estimate
        } catch (Exception e) {
            Logger.debug(this, "Failed to calculate throughput: " + e.getMessage());
            return 0.0;
        }
    }
    
    // ====================================================================
    // LEGACY METHODS (now handled by RequestTracker)
    // ====================================================================
    
    /**
     * @deprecated Use RequestTracker.getInstance().incrementStatusCode() instead.
     * This method is maintained for backwards compatibility.
     */
    @Deprecated
    public void incrementStatusCode(int statusCode) {
        requestTracker.incrementStatusCode(statusCode);
    }
    
    /**
     * @deprecated Use RequestTracker.getInstance().incrementEndpoint() instead.
     * This method is maintained for backwards compatibility.
     */
    @Deprecated
    public void incrementEndpoint(String uri) {
        requestTracker.incrementEndpoint(uri);
    }
    
    /**
     * @deprecated Use RequestTracker.getInstance().requestStarted() instead.
     * This method is maintained for backwards compatibility.
     */
    @Deprecated
    public void requestStarted() {
        // Note: RequestTracker.requestStarted() requires HttpServletRequest parameter
        // This legacy method only increments active count for compatibility
        Logger.warn(this, "Using deprecated requestStarted() method - consider using RequestTracker directly");
    }
    
    /**
     * @deprecated Use RequestTracker.getInstance().requestEnded() instead.
     * This method is maintained for backwards compatibility.
     */
    @Deprecated
    public void requestEnded() {
        // Note: RequestTracker.requestEnded() requires HttpServletRequest/Response parameters
        // This legacy method only decrements active count for compatibility
        Logger.warn(this, "Using deprecated requestEnded() method - consider using RequestTracker directly");
    }
} 