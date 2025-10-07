package com.dotcms.metrics.init;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.config.DotInitializer;
import com.dotcms.metrics.MetricsConfig;
import com.dotcms.metrics.MetricsService;
import com.dotmarketing.util.Logger;

import java.util.Optional;

/**
 * Initializer for the dotCMS metrics system.
 * 
 * This initializer ensures that the metrics service is properly initialized
 * during application startup. It's part of the dotCMS initialization sequence
 * and will be called automatically when the application starts.
 * 
 * The initializer:
 * - Checks if metrics are enabled
 * - Initializes the MetricsService via CDI
 * - Logs initialization status
 * - Handles any initialization errors gracefully
 */
public class MetricsInitializer implements DotInitializer {
    
    private static final String CLASS_NAME = MetricsInitializer.class.getSimpleName();
    
    @Override
    public void init() {
        if (!MetricsConfig.ENABLED) {
            Logger.info(this, "Metrics collection is disabled, skipping initialization");
            return;
        }
        
        Logger.info(this, "Initializing dotCMS Metrics System");
        
        try {
            // Get the MetricsService from CDI - this will trigger its @PostConstruct method
            Optional<MetricsService> metricsServiceOpt = CDIUtils.getBean(MetricsService.class);
            
            if (metricsServiceOpt.isPresent()) {
                MetricsService metricsService = metricsServiceOpt.get();
                
                if (metricsService.isEnabled()) {
                    Logger.info(this, "Metrics System initialized successfully");
                    
                    // Log enabled features
                    StringBuilder features = new StringBuilder("Enabled features: ");
                    if (MetricsConfig.PROMETHEUS_ENABLED) {
                        features.append("Prometheus(").append(MetricsConfig.PROMETHEUS_ENDPOINT).append(") ");
                    }
                    if (MetricsConfig.JMX_ENABLED) {
                        features.append("JMX(").append(MetricsConfig.JMX_DOMAIN).append(") ");
                    }
                    if (MetricsConfig.JVM_METRICS_ENABLED) {
                        features.append("JVM ");
                    }
                    if (MetricsConfig.SYSTEM_METRICS_ENABLED) {
                        features.append("System ");
                    }
                    
                    Logger.info(this, features.toString());
                } else {
                    Logger.warn(this, "Metrics System initialized but not enabled - check configuration");
                }
            } else {
                Logger.error(this, "Failed to get MetricsService from CDI container");
            }
            
        } catch (Exception e) {
            Logger.error(this, "Error during Metrics System initialization: " + e.getMessage(), e);
            // Don't rethrow - metrics failure shouldn't prevent application startup
        }
    }
    
    @Override
    public String getName() {
        return CLASS_NAME;
    }
}