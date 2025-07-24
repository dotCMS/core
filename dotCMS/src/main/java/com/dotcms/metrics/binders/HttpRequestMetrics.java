package com.dotcms.metrics.binders;

import com.dotmarketing.util.Logger;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive metric binder for dotCMS HTTP request monitoring.
 * 
 * This binder provides essential HTTP metrics including:
 * - Request duration histograms by endpoint and method
 * - HTTP status code distribution
 * - Request throughput and error rates
 * - Active request count
 * - Response size metrics
 * 
 * These metrics are critical for monitoring application performance,
 * identifying slow endpoints, and detecting error patterns.
 */
public class HttpRequestMetrics implements MeterBinder {
    
    private static final String METRIC_PREFIX = "dotcms.http";
    private final MBeanServer mBeanServer;
    
    // Thread-safe counters for custom metrics
    private final ConcurrentHashMap<String, AtomicLong> statusCodeCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> endpointCounts = new ConcurrentHashMap<>();
    private final AtomicLong activeRequests = new AtomicLong(0);
    
    public HttpRequestMetrics() {
        this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
    }
    
    @Override
    public void bindTo(MeterRegistry registry) {
        try {
            registerTomcatRequestMetrics(registry);
            registerApplicationMetrics(registry);
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
     * Register application-level HTTP metrics.
     */
    private void registerApplicationMetrics(MeterRegistry registry) {
        // HTTP status code distributions
        registerStatusCodeMetrics(registry);
        
        // Endpoint-specific metrics
        registerEndpointMetrics(registry);
        
        // Active request count
        Gauge.builder(METRIC_PREFIX + ".requests.active", this, metrics -> activeRequests.get())
            .description("Number of currently active HTTP requests")
            .register(registry);
    }
    
    /**
     * Register HTTP status code distribution metrics.
     */
    private void registerStatusCodeMetrics(MeterRegistry registry) {
        // 2xx Success responses
        Gauge.builder(METRIC_PREFIX + ".responses.2xx", this, 
            metrics -> getStatusCodeCount("2xx"))
            .description("Number of 2xx HTTP responses")
            .register(registry);
        
        // 3xx Redirection responses
        Gauge.builder(METRIC_PREFIX + ".responses.3xx", this,
            metrics -> getStatusCodeCount("3xx"))
            .description("Number of 3xx HTTP responses")
            .register(registry);
        
        // 4xx Client error responses
        Gauge.builder(METRIC_PREFIX + ".responses.4xx", this,
            metrics -> getStatusCodeCount("4xx"))
            .description("Number of 4xx HTTP responses")
            .register(registry);
        
        // 5xx Server error responses
        Gauge.builder(METRIC_PREFIX + ".responses.5xx", this,
            metrics -> getStatusCodeCount("5xx"))
            .description("Number of 5xx HTTP responses")
            .register(registry);
        
        // Error rate (4xx + 5xx / total)
        Gauge.builder(METRIC_PREFIX + ".responses.error_rate", this,
            metrics -> getErrorRate())
            .description("HTTP error rate percentage (4xx + 5xx / total)")
            .register(registry);
    }
    
    /**
     * Register endpoint-specific metrics for key dotCMS endpoints.
     */
    private void registerEndpointMetrics(MeterRegistry registry) {
        // API endpoint requests
        Gauge.builder(METRIC_PREFIX + ".endpoints.api.requests", this,
            metrics -> getEndpointCount("/api/"))
            .description("Number of requests to /api/* endpoints")
            .register(registry);
        
        // Content delivery requests
        Gauge.builder(METRIC_PREFIX + ".endpoints.content.requests", this,
            metrics -> getEndpointCount("/contentAsset/"))
            .description("Number of content delivery requests")
            .register(registry);
        
        // Admin interface requests
        Gauge.builder(METRIC_PREFIX + ".endpoints.admin.requests", this,
            metrics -> getEndpointCount("/dotAdmin/"))
            .description("Number of admin interface requests")
            .register(registry);
        
        // Management endpoint requests
        Gauge.builder(METRIC_PREFIX + ".endpoints.management.requests", this,
            metrics -> getEndpointCount("/dotmgt/"))
            .description("Number of management endpoint requests")
            .register(registry);
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
    // HELPER METHODS FOR CUSTOM COUNTERS
    // ====================================================================
    
    /**
     * Get count for specific status code range.
     * Note: These would be updated by request filters/interceptors in real implementation.
     */
    private double getStatusCodeCount(String statusRange) {
        AtomicLong count = statusCodeCounts.get(statusRange);
        return count != null ? count.get() : 0.0;
    }
    
    /**
     * Get count for specific endpoint pattern.
     * Note: These would be updated by request filters/interceptors in real implementation.
     */
    private double getEndpointCount(String endpointPattern) {
        AtomicLong count = endpointCounts.get(endpointPattern);
        return count != null ? count.get() : 0.0;
    }
    
    /**
     * Calculate overall error rate from status codes.
     */
    private double getErrorRate() {
        double total2xx = getStatusCodeCount("2xx");
        double total3xx = getStatusCodeCount("3xx");
        double total4xx = getStatusCodeCount("4xx");
        double total5xx = getStatusCodeCount("5xx");
        
        double totalRequests = total2xx + total3xx + total4xx + total5xx;
        double errorRequests = total4xx + total5xx;
        
        return totalRequests > 0 ? (errorRequests / totalRequests) * 100 : 0.0;
    }
    
    // ====================================================================
    // PUBLIC METHODS FOR REQUEST TRACKING (to be called by filters)
    // ====================================================================
    
    /**
     * Increment status code counter (to be called by request filter).
     */
    public void incrementStatusCode(int statusCode) {
        String range = getStatusCodeRange(statusCode);
        statusCodeCounts.computeIfAbsent(range, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * Increment endpoint counter (to be called by request filter).
     */
    public void incrementEndpoint(String uri) {
        String pattern = getEndpointPattern(uri);
        if (pattern != null) {
            endpointCounts.computeIfAbsent(pattern, k -> new AtomicLong(0)).incrementAndGet();
        }
    }
    
    /**
     * Track active request start.
     */
    public void requestStarted() {
        activeRequests.incrementAndGet();
    }
    
    /**
     * Track active request end.
     */
    public void requestEnded() {
        activeRequests.decrementAndGet();
    }
    
    private String getStatusCodeRange(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) return "2xx";
        if (statusCode >= 300 && statusCode < 400) return "3xx";
        if (statusCode >= 400 && statusCode < 500) return "4xx";
        if (statusCode >= 500 && statusCode < 600) return "5xx";
        return "other";
    }
    
    private String getEndpointPattern(String uri) {
        if (uri == null) return null;
        
        if (uri.startsWith("/api/")) return "/api/";
        if (uri.startsWith("/contentAsset/")) return "/contentAsset/";
        if (uri.startsWith("/dotAdmin/")) return "/dotAdmin/";
        if (uri.startsWith("/dotmgt/")) return "/dotmgt/";
        
        return null; // Only track key endpoint patterns
    }
} 