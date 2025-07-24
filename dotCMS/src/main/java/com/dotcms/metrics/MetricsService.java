package com.dotcms.metrics;

import com.dotcms.metrics.binders.CacheMetrics;
import com.dotcms.metrics.binders.ContentletMetrics;
import com.dotcms.metrics.binders.DatabaseMetrics;
import com.dotcms.metrics.binders.FileAssetMetrics;
import com.dotcms.metrics.binders.HttpRequestMetrics;
import com.dotcms.metrics.binders.SearchMetrics;
import com.dotcms.metrics.binders.TomcatMetrics;
import com.dotcms.metrics.binders.UserSessionMetrics;
import com.dotmarketing.util.Logger;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.jmx.JmxMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for configuring and managing Micrometer metrics registries
 * in the dotCMS application.
 * 
 * This service:
 * - Initializes configured metric registries (Prometheus, JMX)
 * - Registers common metric binders for JVM and system metrics
 * - Provides access to the global metrics registry
 * - Handles registry lifecycle management
 * 
 * The service is managed by CDI and automatically initialized during application startup.
 */
@ApplicationScoped
public class MetricsService {
    
    private static final String CLASS_NAME = MetricsService.class.getSimpleName();
    
    private PrometheusMeterRegistry prometheusRegistry;
    private JmxMeterRegistry jmxRegistry;
    private final List<MeterRegistry> registries = new ArrayList<>();
    
    @Inject
    private MetricsTaggingService taggingService;
    
    @Inject
    private MetricsValidator metricsValidator;
    
    /**
     * Initialize the metrics service and configure registries.
     * Called automatically by CDI after bean construction.
     */
    @PostConstruct
    public void init() {
        if (!MetricsConfig.ENABLED) {
            Logger.info(this, "Metrics collection is disabled");
            return;
        }
        
        Logger.info(this, "Initializing dotCMS Metrics Service");
        MetricsConfig.logConfiguration();
        
        try {
            configureRegistries();
            registerCommonMetrics();
            
            // Validate configuration after initialization
            if (!metricsValidator.isValidForProduction()) {
                Logger.warn(this, "Metrics Service initialized with configuration warnings - check logs for details");
            }
            
            Logger.info(this, "Metrics Service initialized successfully");
        } catch (Exception e) {
            Logger.error(this, "Failed to initialize Metrics Service: " + e.getMessage(), e);
        }
    }
    
    /**
     * Configure and register metric registries based on configuration.
     */
    private void configureRegistries() {
        // Configure Prometheus registry
        if (MetricsConfig.PROMETHEUS_ENABLED) {
            try {
                // Use default config - no automatic servlet registration
                prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
                
                // Add common tags
                if (MetricsConfig.INCLUDE_COMMON_TAGS && MetricsConfig.K8S_TAGGING_ENABLED) {
                    prometheusRegistry.config().commonTags(taggingService.getCommonTags());
                }
                
                Metrics.addRegistry(prometheusRegistry);
                registries.add(prometheusRegistry);
                
                Logger.info(this, "Prometheus metrics registry configured");
            } catch (Exception e) {
                Logger.error(this, "Failed to configure Prometheus registry: " + e.getMessage(), e);
            }
        }
        
        // Configure JMX registry
        if (MetricsConfig.JMX_ENABLED) {
            try {
                jmxRegistry = new JmxMeterRegistry(
                    io.micrometer.jmx.JmxConfig.DEFAULT,
                    io.micrometer.core.instrument.Clock.SYSTEM
                );
                
                // Add common tags
                if (MetricsConfig.INCLUDE_COMMON_TAGS && MetricsConfig.K8S_TAGGING_ENABLED) {
                    jmxRegistry.config().commonTags(taggingService.getCommonTags());
                }
                
                Metrics.addRegistry(jmxRegistry);
                registries.add(jmxRegistry);
                
                Logger.info(this, "JMX metrics registry configured with domain: " + MetricsConfig.JMX_DOMAIN);
            } catch (Exception e) {
                Logger.error(this, "Failed to configure JMX registry: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Register common metric binders for JVM and system monitoring.
     */
    private void registerCommonMetrics() {
        MeterRegistry globalRegistry = Metrics.globalRegistry;
        
        // JVM Metrics
        if (MetricsConfig.JVM_METRICS_ENABLED) {
            try {
                new JvmMemoryMetrics().bindTo(globalRegistry);
                new JvmGcMetrics().bindTo(globalRegistry);
                new JvmThreadMetrics().bindTo(globalRegistry);
                Logger.debug(this, "JVM metrics registered");
            } catch (Exception e) {
                Logger.error(this, "Failed to register JVM metrics: " + e.getMessage(), e);
            }
        }
        
        // System Metrics
        if (MetricsConfig.SYSTEM_METRICS_ENABLED) {
            try {
                new ProcessorMetrics().bindTo(globalRegistry);
                new UptimeMetrics().bindTo(globalRegistry);
                Logger.debug(this, "System metrics registered");
            } catch (Exception e) {
                Logger.error(this, "Failed to register system metrics: " + e.getMessage(), e);
            }
        }
        
        // dotCMS Application Metrics
        if (MetricsConfig.APPLICATION_METRICS_ENABLED) {
            try {
                new ContentletMetrics().bindTo(globalRegistry);
                Logger.debug(this, "Content metrics registered");
            } catch (Exception e) {
                Logger.error(this, "Failed to register content metrics: " + e.getMessage(), e);
            }
        }
        
        // Database Metrics
        if (MetricsConfig.DATABASE_METRICS_ENABLED) {
            try {
                new DatabaseMetrics().bindTo(globalRegistry);
                Logger.debug(this, "Database metrics registered");
            } catch (Exception e) {
                Logger.error(this, "Failed to register database metrics: " + e.getMessage(), e);
            }
        }
        
        // Cache Metrics
        if (MetricsConfig.CACHE_METRICS_ENABLED) {
            try {
                new CacheMetrics().bindTo(globalRegistry);
                Logger.debug(this, "Cache metrics registered");
            } catch (Exception e) {
                Logger.error(this, "Failed to register cache metrics: " + e.getMessage(), e);
            }
        }
        
        // Search Metrics
        if (MetricsConfig.SEARCH_METRICS_ENABLED) {
            try {
                new SearchMetrics().bindTo(globalRegistry);
                Logger.debug(this, "Search metrics registered");
            } catch (Exception e) {
                Logger.error(this, "Failed to register search metrics: " + e.getMessage(), e);
            }
        }
        
        // HTTP Request Metrics
        if (MetricsConfig.HTTP_METRICS_ENABLED) {
            try {
                new HttpRequestMetrics().bindTo(globalRegistry);
                Logger.debug(this, "HTTP request metrics registered");
            } catch (Exception e) {
                Logger.error(this, "Failed to register HTTP request metrics: " + e.getMessage(), e);
            }
        }
        
        // User Session Metrics
        if (MetricsConfig.USER_SESSION_METRICS_ENABLED) {
            try {
                new UserSessionMetrics().bindTo(globalRegistry);
                Logger.debug(this, "User session metrics registered");
            } catch (Exception e) {
                Logger.error(this, "Failed to register user session metrics: " + e.getMessage(), e);
            }
        }
        
        // File Asset Metrics
        if (MetricsConfig.FILE_ASSET_METRICS_ENABLED) {
            try {
                new FileAssetMetrics().bindTo(globalRegistry);
                Logger.debug(this, "File asset metrics registered");
            } catch (Exception e) {
                Logger.error(this, "Failed to register file asset metrics: " + e.getMessage(), e);
            }
        }
        
        // Tomcat Metrics
        if (MetricsConfig.TOMCAT_METRICS_ENABLED) {
            try {
                new TomcatMetrics().bindTo(globalRegistry);
                Logger.debug(this, "Tomcat metrics registered");
            } catch (Exception e) {
                Logger.error(this, "Failed to register Tomcat metrics: " + e.getMessage(), e);
            }
        }
    }
    
    
    /**
     * Get the Prometheus meter registry.
     * 
     * @return PrometheusMeterRegistry instance, or null if not configured
     */
    public PrometheusMeterRegistry getPrometheusRegistry() {
        return prometheusRegistry;
    }
    
    /**
     * Get the JMX meter registry.
     * 
     * @return JmxMeterRegistry instance, or null if not configured
     */
    public JmxMeterRegistry getJmxRegistry() {
        return jmxRegistry;
    }
    
    /**
     * Get the global meter registry.
     * 
     * @return Global MeterRegistry instance
     */
    public MeterRegistry getGlobalRegistry() {
        return Metrics.globalRegistry;
    }
    
    /**
     * Get the metrics tagging service for creating tagged metrics.
     * 
     * @return MetricsTaggingService instance
     */
    public MetricsTaggingService getTaggingService() {
        return taggingService;
    }
    
    /**
     * Check if metrics are enabled and initialized.
     * 
     * @return true if metrics are enabled, false otherwise
     */
    public boolean isEnabled() {
        return MetricsConfig.ENABLED && (prometheusRegistry != null || jmxRegistry != null);
    }
    
    /**
     * Clean up resources when the service is destroyed.
     * Called automatically by CDI during application shutdown.
     */
    @PreDestroy
    public void destroy() {
        if (!MetricsConfig.ENABLED) {
            return;
        }
        
        Logger.info(this, "Shutting down Metrics Service");
        
        for (MeterRegistry registry : registries) {
            try {
                registry.close();
            } catch (Exception e) {
                Logger.warn(this, "Error closing metric registry: " + e.getMessage());
            }
        }
        
        registries.clear();
        Logger.info(this, "Metrics Service shutdown complete");
    }
}