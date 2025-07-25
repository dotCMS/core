package com.dotcms.metrics;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for validating the metrics configuration at application startup.
 * 
 * This validator ensures that:
 * - Essential metrics configuration is present and valid
 * - Kubernetes tags are properly configured when k8s deployment is detected
 * - Performance settings are within acceptable ranges
 * - Registry configurations are consistent
 * 
 * Validation occurs during CDI initialization and logs warnings for any
 * configuration issues that could impact metrics collection or export.
 * 
 * The service is managed by CDI and automatically initialized during application startup.
 */
@ApplicationScoped
public class MetricsValidator {
    
    private static final String CLASS_NAME = MetricsValidator.class.getSimpleName();
    
    @Inject
    private MetricsTaggingService taggingService;
    
    /**
     * Validate the metrics configuration at startup.
     * Called automatically by CDI after bean construction.
     */
    @PostConstruct
    public void validateConfiguration() {
        if (!MetricsConfig.ENABLED) {
            Logger.info(this, "Metrics validation skipped - metrics collection is disabled");
            return;
        }
        
        Logger.info(this, "Validating metrics configuration");
        
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        // Validate basic configuration
        validateBasicConfiguration(warnings, errors);
        
        // Validate Kubernetes tags if enabled
        if (MetricsConfig.K8S_TAGGING_ENABLED) {
            validateKubernetesConfiguration(warnings, errors);
        }
        
        // Validate registry configuration
        validateRegistryConfiguration(warnings, errors);
        
        // Validate performance settings
        validatePerformanceSettings(warnings, errors);
        
        // Validate tagging service
        validateTaggingService(warnings, errors);
        
        // Log results
        logValidationResults(warnings, errors);
        
        Logger.info(this, "Metrics configuration validation completed");
    }
    
    /**
     * Validate basic metrics configuration.
     */
    private void validateBasicConfiguration(List<String> warnings, List<String> errors) {
        // Check metric prefix
        if (!UtilMethods.isSet(MetricsConfig.METRIC_PREFIX)) {
            warnings.add("Metric prefix is empty - metrics may not be properly namespaced");
        }
        
        // Check collection interval
        if (MetricsConfig.COLLECTION_INTERVAL_SECONDS < 1) {
            errors.add("Collection interval is too low: " + MetricsConfig.COLLECTION_INTERVAL_SECONDS + 
                      " seconds (minimum: 1 second)");
        } else if (MetricsConfig.COLLECTION_INTERVAL_SECONDS > 300) {
            warnings.add("Collection interval is very high: " + MetricsConfig.COLLECTION_INTERVAL_SECONDS + 
                        " seconds (consider reducing for better monitoring)");
        }
        
        // Check if at least one registry is enabled
        if (!MetricsConfig.PROMETHEUS_ENABLED && !MetricsConfig.JMX_ENABLED) {
            errors.add("No metric registries enabled - metrics will not be exported");
        }
    }
    
    /**
     * Validate Kubernetes-specific configuration.
     */
    private void validateKubernetesConfiguration(List<String> warnings, List<String> errors) {
        // Check for kubernetes environment indicators
        boolean inKubernetesEnvironment = isKubernetesEnvironment();
        
        if (inKubernetesEnvironment) {
            Logger.debug(this, "Kubernetes environment detected, validating k8s tags");
            
            // Validate essential k8s tags
            if ("unknown".equals(MetricsConfig.K8S_VERSION_TAG)) {
                warnings.add("K8s version tag is 'unknown' - consider setting DOT_K8S_VERSION environment variable");
            }
            
            if ("default".equals(MetricsConfig.K8S_CUSTOMER_TAG)) {
                warnings.add("K8s customer tag is 'default' - consider setting DOT_K8S_CUSTOMER for multi-tenant deployments");
            }
            
            if (!UtilMethods.isSet(MetricsConfig.K8S_DEPLOYMENT_TAG)) {
                warnings.add("K8s deployment tag is not set - will use hostname as fallback");
            }
            
            // Check for proper environment configuration
            if ("local".equals(MetricsConfig.K8S_ENV_TAG)) {
                warnings.add("K8s environment is 'local' in kubernetes deployment - consider setting DOT_K8S_ENV");
            }
        } else {
            Logger.debug(this, "Non-Kubernetes environment detected");
        }
    }
    
    /**
     * Validate registry configuration.
     */
    private void validateRegistryConfiguration(List<String> warnings, List<String> errors) {
        // Validate Prometheus configuration
        if (MetricsConfig.PROMETHEUS_ENABLED) {
            if (!UtilMethods.isSet(MetricsConfig.PROMETHEUS_ENDPOINT)) {
                errors.add("Prometheus endpoint is not configured");
            } else if (!MetricsConfig.PROMETHEUS_ENDPOINT.startsWith("/")) {
                warnings.add("Prometheus endpoint should start with '/' for proper URL mapping");
            }
            
            if (MetricsConfig.PROMETHEUS_REQUIRE_AUTH) {
                warnings.add("Prometheus endpoint requires authentication - ensure monitoring systems can authenticate");
            }
        }
        
        // Validate JMX configuration
        if (MetricsConfig.JMX_ENABLED) {
            if (!UtilMethods.isSet(MetricsConfig.JMX_DOMAIN)) {
                warnings.add("JMX domain is not configured - using default");
            }
        }
    }
    
    /**
     * Validate performance settings.
     */
    private void validatePerformanceSettings(List<String> warnings, List<String> errors) {
        // Check max tags setting
        if (MetricsConfig.MAX_METRIC_TAGS < 1000) {
            warnings.add("Max metric tags is very low: " + MetricsConfig.MAX_METRIC_TAGS + 
                        " (consider increasing for better metric granularity)");
        } else if (MetricsConfig.MAX_METRIC_TAGS > 100000) {
            warnings.add("Max metric tags is very high: " + MetricsConfig.MAX_METRIC_TAGS + 
                        " (consider reducing to prevent cardinality explosion)");
        }
        
        // Check buffer size
        if (MetricsConfig.PUBLISH_BUFFER_SIZE < 100) {
            warnings.add("Publish buffer size is very low: " + MetricsConfig.PUBLISH_BUFFER_SIZE + 
                        " (may impact performance)");
        } else if (MetricsConfig.PUBLISH_BUFFER_SIZE > 10000) {
            warnings.add("Publish buffer size is very high: " + MetricsConfig.PUBLISH_BUFFER_SIZE + 
                        " (may increase memory usage)");
        }
    }
    
    /**
     * Validate the tagging service configuration.
     */
    private void validateTaggingService(List<String> warnings, List<String> errors) {
        if (taggingService == null) {
            errors.add("MetricsTaggingService is not initialized");
            return;
        }
        
        // Validate tagging service configuration
        if (!taggingService.validateTagConfiguration()) {
            errors.add("MetricsTaggingService configuration validation failed");
        }
        
        // Check if common tags are available
        if (MetricsConfig.INCLUDE_COMMON_TAGS && taggingService.getCommonTags().isEmpty()) {
            warnings.add("Common tags are enabled but no tags are configured");
        }
    }
    
    /**
     * Detect if running in a Kubernetes environment.
     * 
     * @return true if kubernetes environment is detected
     */
    private boolean isKubernetesEnvironment() {
        // Check for kubernetes service account
        if (System.getProperty("user.dir", "").contains("/opt/dotcms")) {
            return true;
        }
        
        // Check for kubernetes environment variables
        if (UtilMethods.isSet(System.getenv("KUBERNETES_SERVICE_HOST"))) {
            return true;
        }
        
        // Check for common kubernetes file system indicators
        if (System.getProperty("os.name", "").toLowerCase().contains("linux")) {
            // Check for kubernetes service account token
            java.io.File tokenFile = new java.io.File("/var/run/secrets/kubernetes.io/serviceaccount/token");
            if (tokenFile.exists()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Log validation results.
     */
    private void logValidationResults(List<String> warnings, List<String> errors) {
        if (errors.isEmpty() && warnings.isEmpty()) {
            Logger.info(this, "Metrics configuration validation passed with no issues");
            return;
        }
        
        // Log errors
        if (!errors.isEmpty()) {
            Logger.error(this, "Metrics configuration validation found " + errors.size() + " error(s):");
            for (String error : errors) {
                Logger.error(this, "  ERROR: " + error);
            }
        }
        
        // Log warnings
        if (!warnings.isEmpty()) {
            Logger.warn(this, "Metrics configuration validation found " + warnings.size() + " warning(s):");
            for (String warning : warnings) {
                Logger.warn(this, "  WARNING: " + warning);
            }
        }
        
        // Summary
        String summary = String.format("Validation completed: %d error(s), %d warning(s)", 
                                      errors.size(), warnings.size());
        
        if (!errors.isEmpty()) {
            Logger.error(this, summary);
        } else {
            Logger.info(this, summary);
        }
    }
    
    /**
     * Check if the metrics configuration is valid for production use.
     * 
     * @return true if configuration is valid, false if there are critical errors
     */
    public boolean isValidForProduction() {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        validateBasicConfiguration(warnings, errors);
        validateRegistryConfiguration(warnings, errors);
        validatePerformanceSettings(warnings, errors);
        validateTaggingService(warnings, errors);
        
        return errors.isEmpty();
    }
}