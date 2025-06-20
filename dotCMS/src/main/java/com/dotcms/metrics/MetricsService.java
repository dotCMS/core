package com.dotcms.metrics;

import com.dotcms.metrics.binders.CacheMetrics;
import com.dotcms.metrics.binders.ContentletMetrics;
import com.dotcms.metrics.binders.TomcatMetrics;
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
import java.net.InetAddress;
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
                prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
                
                // Add common tags
                if (MetricsConfig.INCLUDE_COMMON_TAGS) {
                    prometheusRegistry.config().commonTags(getCommonTags());
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
                if (MetricsConfig.INCLUDE_COMMON_TAGS) {
                    jmxRegistry.config().commonTags(getCommonTags());
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
                Logger.debug(this, "Contentlet metrics registered");
            } catch (Exception e) {
                Logger.error(this, "Failed to register contentlet metrics: " + e.getMessage(), e);
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
     * Get common tags to be applied to all metrics.
     * 
     * @return List of common tags
     */
    private List<Tag> getCommonTags() {
        List<Tag> tags = new ArrayList<>();
        
        try {
            // Add application tag
            tags.add(Tag.of("application", "dotcms"));
            
            // Add hostname tag
            String hostname = InetAddress.getLocalHost().getHostName();
            tags.add(Tag.of("host", hostname));
            
            // Add environment tag if configured
            String environment = System.getProperty("DOT_ENVIRONMENT", "local");
            tags.add(Tag.of("environment", environment));
            
        } catch (Exception e) {
            Logger.warn(this, "Failed to create some common tags: " + e.getMessage());
        }
        
        return tags;
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