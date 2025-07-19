package com.dotcms.metrics;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

/**
 * Configuration for the dotCMS metrics collection system using Micrometer.
 * 
 * This class provides centralized access to metrics system configuration including
 * registry settings, metric collection intervals, and export configurations.
 * 
 * Key Features:
 * - Metrics collection enable/disable controls
 * - Registry configuration (Prometheus, JMX, etc.)
 * - Collection intervals and export settings
 * - Custom metric prefix management
 * 
 * Usage:
 * - For global settings: Use the constants directly
 *   Example: if (MetricsConfig.ENABLED) { ... }
 * 
 * - For registry configuration: Use the registry-specific settings
 *   Example: MetricsConfig.PROMETHEUS_ENDPOINT
 * 
 * Configuration Properties:
 * - metrics.enabled: Enable/disable metrics collection (default: true)
 * - metrics.prefix: Prefix for all metric names (default: "dotcms")
 * - metrics.collection.interval-seconds: How often to collect metrics (default: 30)
 * - metrics.prometheus.enabled: Enable Prometheus registry (default: true)
 * - metrics.prometheus.endpoint: Prometheus metrics endpoint (default: "/metrics")
 * - metrics.jmx.enabled: Enable JMX registry (default: true)
 * - metrics.jvm.enabled: Enable JVM metrics (default: true)
 * - metrics.system.enabled: Enable system metrics (default: true)
 * - metrics.application.enabled: Enable application-specific metrics (default: true)
 */
public final class MetricsConfig {
    
    // ====================================================================
    // GLOBAL METRICS SYSTEM SETTINGS
    // ====================================================================
    
    /**
     * Whether metrics collection is enabled globally.
     * When disabled, no metrics will be collected or exported.
     * Default: true
     */
    public static final boolean ENABLED = 
        Config.getBooleanProperty("metrics.enabled", true);
    
    /**
     * Prefix for all metric names to namespace dotCMS metrics.
     * Default: "dotcms"
     */
    public static final String METRIC_PREFIX = 
        Config.getStringProperty("metrics.prefix", "dotcms");
    
    /**
     * How often to collect metrics (in seconds).
     * Default: 30 seconds
     */
    public static final int COLLECTION_INTERVAL_SECONDS = 
        Config.getIntProperty("metrics.collection.interval-seconds", 30);
    
    /**
     * Whether to include common tags like host, environment, etc.
     * Default: true
     */
    public static final boolean INCLUDE_COMMON_TAGS = 
        Config.getBooleanProperty("metrics.include.common-tags", true);
    
    // ====================================================================
    // PROMETHEUS REGISTRY CONFIGURATION
    // ====================================================================
    
    /**
     * Whether to enable Prometheus metrics registry.
     * Default: true
     */
    public static final boolean PROMETHEUS_ENABLED = 
        Config.getBooleanProperty("metrics.prometheus.enabled", true);
    
    /**
     * Endpoint path for Prometheus metrics scraping.
     * Default: "/metrics"
     */
    public static final String PROMETHEUS_ENDPOINT = 
        Config.getStringProperty("metrics.prometheus.endpoint", "/metrics");
    
    /**
     * Whether Prometheus endpoint requires authentication.
     * Default: false (to allow scraping by monitoring systems)
     */
    public static final boolean PROMETHEUS_REQUIRE_AUTH = 
        Config.getBooleanProperty("metrics.prometheus.require-authentication", false);
    
    // ====================================================================
    // JMX REGISTRY CONFIGURATION
    // ====================================================================
    
    /**
     * Whether to enable JMX metrics registry.
     * Default: true
     */
    public static final boolean JMX_ENABLED = 
        Config.getBooleanProperty("metrics.jmx.enabled", true);
    
    /**
     * JMX domain for metrics registration.
     * Default: "dotcms.metrics"
     */
    public static final String JMX_DOMAIN = 
        Config.getStringProperty("metrics.jmx.domain", "dotcms.metrics");
    
    // ====================================================================
    // METRIC CATEGORIES CONFIGURATION
    // ====================================================================
    
    /**
     * Whether to collect JVM metrics (memory, GC, threads, etc.).
     * Default: true
     */
    public static final boolean JVM_METRICS_ENABLED = 
        Config.getBooleanProperty("metrics.jvm.enabled", true);
    
    /**
     * Whether to collect system metrics (CPU, disk, network).
     * Default: true
     */
    public static final boolean SYSTEM_METRICS_ENABLED = 
        Config.getBooleanProperty("metrics.system.enabled", true);
    
    /**
     * Whether to collect application-specific metrics (requests, cache, etc.).
     * Default: true
     */
    public static final boolean APPLICATION_METRICS_ENABLED = 
        Config.getBooleanProperty("metrics.application.enabled", true);
    
    /**
     * Whether to collect database metrics (connection pool, query performance).
     * Default: true
     */
    public static final boolean DATABASE_METRICS_ENABLED = 
        Config.getBooleanProperty("metrics.database.enabled", true);
    
    /**
     * Whether to collect cache metrics (hit rates, evictions, etc.).
     * Default: true
     */
    public static final boolean CACHE_METRICS_ENABLED = 
        Config.getBooleanProperty("metrics.cache.enabled", true);
    
    /**
     * Whether to collect HTTP request metrics.
     * Default: true
     */
    public static final boolean HTTP_METRICS_ENABLED = 
        Config.getBooleanProperty("metrics.http.enabled", true);
    
    /**
     * Whether to collect Tomcat server metrics (thread pools, connectors, etc.).
     * Default: true
     */
    public static final boolean TOMCAT_METRICS_ENABLED = 
        Config.getBooleanProperty("metrics.tomcat.enabled", true);
    
    // ====================================================================
    // PERFORMANCE SETTINGS
    // ====================================================================
    
    /**
     * Maximum number of metric tags to prevent cardinality explosion.
     * Default: 10000
     */
    public static final int MAX_METRIC_TAGS = 
        Config.getIntProperty("metrics.max-tags", 10000);
    
    /**
     * Whether to use meter filters to limit metric cardinality.
     * Default: true
     */
    public static final boolean USE_METER_FILTERS = 
        Config.getBooleanProperty("metrics.use-meter-filters", true);
    
    /**
     * Buffer size for metric publishing (number of measurements).
     * Default: 1000
     */
    public static final int PUBLISH_BUFFER_SIZE = 
        Config.getIntProperty("metrics.publish.buffer-size", 1000);
    
    // ====================================================================
    // UTILITY METHODS
    // ====================================================================
    
    /**
     * Get a prefixed metric name.
     * 
     * @param metricName The base metric name
     * @return The metric name with configured prefix
     */
    public static String getMetricName(String metricName) {
        if (METRIC_PREFIX == null || METRIC_PREFIX.trim().isEmpty()) {
            return metricName;
        }
        return METRIC_PREFIX + "." + metricName;
    }
    
    /**
     * Check if a specific metric category is enabled.
     * 
     * @param category The metric category to check (e.g., "jvm", "system", "http")
     * @return true if the category is enabled, false otherwise
     */
    public static boolean isCategoryEnabled(String category) {
        if (!ENABLED) {
            return false;
        }
        
        String property = "metrics." + category.toLowerCase() + ".enabled";
        return Config.getBooleanProperty(property, true);
    }
    
    /**
     * Log the current metrics configuration for debugging.
     */
    public static void logConfiguration() {
        if (!Logger.isDebugEnabled(MetricsConfig.class)) {
            return;
        }
        
        Logger.debug(MetricsConfig.class, "Metrics Configuration:");
        Logger.debug(MetricsConfig.class, "  Enabled: " + ENABLED);
        Logger.debug(MetricsConfig.class, "  Prefix: " + METRIC_PREFIX);
        Logger.debug(MetricsConfig.class, "  Collection Interval: " + COLLECTION_INTERVAL_SECONDS + "s");
        Logger.debug(MetricsConfig.class, "  Prometheus: " + PROMETHEUS_ENABLED + " (endpoint: " + PROMETHEUS_ENDPOINT + ")");
        Logger.debug(MetricsConfig.class, "  JMX: " + JMX_ENABLED + " (domain: " + JMX_DOMAIN + ")");
        Logger.debug(MetricsConfig.class, "  JVM Metrics: " + JVM_METRICS_ENABLED);
        Logger.debug(MetricsConfig.class, "  System Metrics: " + SYSTEM_METRICS_ENABLED);
        Logger.debug(MetricsConfig.class, "  Application Metrics: " + APPLICATION_METRICS_ENABLED);
        Logger.debug(MetricsConfig.class, "  Database Metrics: " + DATABASE_METRICS_ENABLED);
        Logger.debug(MetricsConfig.class, "  Cache Metrics: " + CACHE_METRICS_ENABLED);
        Logger.debug(MetricsConfig.class, "  HTTP Metrics: " + HTTP_METRICS_ENABLED);
        Logger.debug(MetricsConfig.class, "  Tomcat Metrics: " + TOMCAT_METRICS_ENABLED);
    }
}