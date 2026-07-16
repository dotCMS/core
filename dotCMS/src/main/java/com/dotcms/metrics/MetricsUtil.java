package com.dotcms.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Utility class providing convenient methods for adding metrics throughout dotCMS.
 * 
 * This class provides simple, static methods for common metric patterns:
 * - Counters for tracking events
 * - Timers for measuring duration
 * - Gauges for monitoring values
 * - Tagged metrics for categorization
 */
public final class MetricsUtil {
    
    private MetricsUtil() {
        // Utility class
    }
    
    /**
     * Increment a counter with the dotCMS prefix.
     * 
     * @param name metric name (will be prefixed with "dotcms.")
     * @param tags optional tags as key-value pairs
     */
    public static void incrementCounter(String name, String... tags) {
        Counter.builder(MetricsConfig.getMetricName(name))
            .tags(tags)
            .register(Metrics.globalRegistry)
            .increment();
    }
    
    /**
     * Increment a counter by a specific amount.
     * 
     * @param name metric name
     * @param amount amount to increment
     * @param tags optional tags
     */
    public static void incrementCounter(String name, double amount, String... tags) {
        Counter.builder(MetricsConfig.getMetricName(name))
            .tags(tags)
            .register(Metrics.globalRegistry)
            .increment(amount);
    }
    
    /**
     * Record timing for an operation.
     * 
     * @param name metric name
     * @param callable operation to time
     * @param tags optional tags
     * @return result of the callable
     * @throws Exception if the callable throws
     */
    public static <T> T recordTime(String name, Callable<T> callable, String... tags) throws Exception {
        return Timer.builder(MetricsConfig.getMetricName(name))
            .tags(tags)
            .register(Metrics.globalRegistry)
            .recordCallable(callable);
    }
    
    /**
     * Time an operation without return value.
     * 
     * @param name metric name
     * @param runnable operation to time
     * @param tags optional tags
     */
    public static void recordTime(String name, Runnable runnable, String... tags) {
        Timer.Sample sample = Timer.start();
        try {
            runnable.run();
        } finally {
            sample.stop(Timer.builder(MetricsConfig.getMetricName(name))
                .tags(tags)
                .register(Metrics.globalRegistry));
        }
    }
    
    /**
     * Register a gauge to monitor a value.
     * 
     * @param name metric name
     * @param valueSupplier supplier for the gauge value
     * @param tags optional tags
     */
    public static void registerGauge(String name, Supplier<Number> valueSupplier, String... tags) {
        Gauge.builder(MetricsConfig.getMetricName(name), valueSupplier, supplier -> supplier.get().doubleValue())
            .tags(tags)
            .register(Metrics.globalRegistry);
    }
    
    /**
     * Register a gauge for a specific object property.
     * 
     * @param name metric name
     * @param object object to monitor
     * @param valueExtractor function to extract value from object
     * @param tags optional tags
     */
    public static <T> void registerGauge(String name, T object, 
                                       java.util.function.ToDoubleFunction<T> valueExtractor, 
                                       String... tags) {
        Gauge.builder(MetricsConfig.getMetricName(name), object, valueExtractor)
            .tags(tags)
            .register(Metrics.globalRegistry);
    }
    
    /**
     * Create a timer sample for manual timing.
     * 
     * @return Timer.Sample that can be stopped later
     */
    public static Timer.Sample startTimer() {
        return Timer.start();
    }
    
    /**
     * Stop a timer sample and record the duration.
     * 
     * @param sample timer sample to stop
     * @param name metric name
     * @param tags optional tags
     */
    public static void stopTimer(Timer.Sample sample, String name, String... tags) {
        sample.stop(Timer.builder(MetricsConfig.getMetricName(name))
            .tags(tags)
            .register(Metrics.globalRegistry));
    }
    
    // Convenience methods for common dotCMS operations
    
    /**
     * Record a contentlet operation.
     */
    public static void recordContentletOperation(String operation, String contentType) {
        incrementCounter("contentlet.operations", 
            "operation", operation,
            "content_type", contentType);
    }
    
    /**
     * Record API request metrics.
     */
    public static void recordApiRequest(String endpoint, String method, int statusCode) {
        incrementCounter("api.requests", 
            "endpoint", endpoint,
            "method", method,
            "status", String.valueOf(statusCode));
    }
    
    /**
     * Record cache operation metrics.
     */
    public static void recordCacheOperation(String cache, String operation, boolean hit) {
        incrementCounter("cache.operations",
            "cache", cache,
            "operation", operation,
            "result", hit ? "hit" : "miss");
    }
    
    /**
     * Record database operation timing.
     */
    public static <T> T recordDatabaseOperation(String operation, Callable<T> callable) throws Exception {
        return recordTime("database.operations", callable, "operation", operation);
    }
}