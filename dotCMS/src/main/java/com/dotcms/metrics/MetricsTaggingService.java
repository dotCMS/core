package com.dotcms.metrics;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.micrometer.core.instrument.Tag;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service responsible for providing consistent tagging across all metrics in dotCMS.
 * 
 * This service manages the creation and validation of metric tags, with special
 * support for Kubernetes environment variables and deployment-specific metadata.
 * 
 * Key features:
 * - Centralized tag management for all metrics
 * - Kubernetes-aware tag extraction from environment variables
 * - Automatic replica detection from pod names
 * - Fallback to default values when environment variables are not set
 * - Validation of tag configuration at startup
 * 
 * Supported Environment Variables:
 * - DOT_K8S_APP: Application name (default: "dotcms")
 * - DOT_K8S_ENV: Environment name (default: "local")
 * - DOT_K8S_VERSION: Application version (default: "unknown")
 * - DOT_K8S_CUSTOMER: Customer identifier (default: "default")
 * - DOT_K8S_DEPLOYMENT: Full deployment name (default: hostname)
 * - DOT_ENVIRONMENT: Legacy environment variable (fallback for DOT_K8S_ENV)
 * 
 * The service is managed by CDI and automatically initialized during application startup.
 */
@ApplicationScoped
public class MetricsTaggingService {
    
    private static final String CLASS_NAME = MetricsTaggingService.class.getSimpleName();
    
    // Pattern to extract replica number from pod names like "dotcms-prod-abc123-xyz789"
    private static final Pattern REPLICA_PATTERN = Pattern.compile(".*-([0-9]+)$");
    
    // Configuration property names
    private static final String K8S_APP_PROPERTY = "k8s.tags.app";
    private static final String K8S_ENV_PROPERTY = "k8s.tags.env";
    private static final String K8S_VERSION_PROPERTY = "k8s.tags.version";
    private static final String K8S_CUSTOMER_PROPERTY = "k8s.tags.customer";
    private static final String K8S_DEPLOYMENT_PROPERTY = "k8s.tags.deployment";
    
    // Default values
    private static final String DEFAULT_APP = "dotcms";
    private static final String DEFAULT_ENV = "local";
    private static final String DEFAULT_VERSION = "unknown";
    private static final String DEFAULT_CUSTOMER = "default";
    
    // Cached tag values
    private List<Tag> commonTags;
    private String hostname;
    private String replica;
    
    /**
     * Initialize the tagging service and prepare common tags.
     * Called automatically by CDI after bean construction.
     */
    @PostConstruct
    public void init() {
        Logger.info(this, "Initializing Metrics Tagging Service");
        
        try {
            // Get hostname for fallback values
            hostname = InetAddress.getLocalHost().getHostName();
            
            // Extract replica information from hostname if available
            replica = extractReplicaFromHostname(hostname);
            
            // Build common tags
            commonTags = buildCommonTags();
            
            // Log configuration for debugging
            logTagConfiguration();
            
            Logger.info(this, "Metrics Tagging Service initialized successfully");
        } catch (Exception e) {
            Logger.error(this, "Failed to initialize Metrics Tagging Service: " + e.getMessage(), e);
            // Fallback to minimal tags to ensure metrics still work
            commonTags = buildFallbackTags();
        }
    }
    
    /**
     * Get the common tags that should be applied to all metrics.
     * 
     * @return Immutable list of common tags
     */
    public List<Tag> getCommonTags() {
        return commonTags != null ? Collections.unmodifiableList(commonTags) : Collections.emptyList();
    }
    
    /**
     * Create additional tags for specific metrics.
     * 
     * @param additionalTags Variable number of key-value pairs for additional tags
     * @return List of tags including common tags and additional tags
     */
    public List<Tag> createTags(String... additionalTags) {
        List<Tag> tags = new ArrayList<>(getCommonTags());
        
        if (additionalTags != null && additionalTags.length > 0) {
            if (additionalTags.length % 2 != 0) {
                Logger.warn(this, "Invalid number of additional tag arguments. Expected key-value pairs.");
                return tags;
            }
            
            for (int i = 0; i < additionalTags.length; i += 2) {
                String key = additionalTags[i];
                String value = additionalTags[i + 1];
                
                if (UtilMethods.isSet(key) && UtilMethods.isSet(value)) {
                    tags.add(Tag.of(key, value));
                }
            }
        }
        
        return tags;
    }
    
    /**
     * Build the standard set of common tags from configuration.
     * 
     * @return List of common tags
     */
    private List<Tag> buildCommonTags() {
        List<Tag> tags = new ArrayList<>();
        
        // Application tag
        String app = Config.getStringProperty(K8S_APP_PROPERTY, DEFAULT_APP);
        tags.add(Tag.of("app", app));
        
        // Environment tag - check new property first, then fall back to legacy
        String env = Config.getStringProperty(K8S_ENV_PROPERTY, null);
        if (!UtilMethods.isSet(env)) {
            env = Config.getStringProperty("environment", DEFAULT_ENV);
        }
        tags.add(Tag.of("env", env));
        
        // Version tag
        String version = Config.getStringProperty(K8S_VERSION_PROPERTY, DEFAULT_VERSION);
        tags.add(Tag.of("version", version));
        
        // Customer tag
        String customer = Config.getStringProperty(K8S_CUSTOMER_PROPERTY, DEFAULT_CUSTOMER);
        tags.add(Tag.of("customer", customer));
        
        // Deployment tag - use configured value or hostname as fallback
        String deployment = Config.getStringProperty(K8S_DEPLOYMENT_PROPERTY, hostname);
        tags.add(Tag.of("deployment", deployment));
        
        // Host tag
        tags.add(Tag.of("host", hostname));
        
        // Replica tag (if extracted from hostname)
        if (UtilMethods.isSet(replica)) {
            tags.add(Tag.of("replica", replica));
        }
        
        return tags;
    }
    
    /**
     * Build minimal fallback tags when initialization fails.
     * 
     * @return List of minimal tags
     */
    private List<Tag> buildFallbackTags() {
        List<Tag> tags = new ArrayList<>();
        
        try {
            tags.add(Tag.of("app", DEFAULT_APP));
            tags.add(Tag.of("env", DEFAULT_ENV));
            tags.add(Tag.of("host", hostname != null ? hostname : "unknown"));
        } catch (Exception e) {
            Logger.warn(this, "Failed to create fallback tags: " + e.getMessage());
        }
        
        return tags;
    }
    
    /**
     * Extract replica number from hostname using pattern matching.
     * 
     * @param hostname The hostname to analyze
     * @return Replica identifier or null if not found
     */
    private String extractReplicaFromHostname(String hostname) {
        if (!UtilMethods.isSet(hostname)) {
            return null;
        }
        
        Matcher matcher = REPLICA_PATTERN.matcher(hostname);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Log the current tag configuration for debugging purposes.
     */
    private void logTagConfiguration() {
        if (!Logger.isDebugEnabled(this.getClass())) {
            return;
        }
        
        Logger.debug(this, "Metrics Tags Configuration:");
        Logger.debug(this, "  Hostname: " + hostname);
        Logger.debug(this, "  Replica: " + (replica != null ? replica : "not detected"));
        Logger.debug(this, "  Common Tags:");
        
        for (Tag tag : commonTags) {
            Logger.debug(this, "    " + tag.getKey() + " = " + tag.getValue());
        }
    }
    
    /**
     * Validate that essential tags are properly configured.
     * 
     * @return true if configuration is valid, false otherwise
     */
    public boolean validateTagConfiguration() {
        boolean valid = true;
        
        if (commonTags == null || commonTags.isEmpty()) {
            Logger.warn(this, "No common tags configured - metrics may not be properly labeled");
            valid = false;
        }
        
        // Check for required tags
        boolean hasApp = commonTags.stream().anyMatch(tag -> "app".equals(tag.getKey()));
        boolean hasEnv = commonTags.stream().anyMatch(tag -> "env".equals(tag.getKey()));
        boolean hasHost = commonTags.stream().anyMatch(tag -> "host".equals(tag.getKey()));
        
        if (!hasApp) {
            Logger.warn(this, "Missing required 'app' tag in metrics configuration");
            valid = false;
        }
        
        if (!hasEnv) {
            Logger.warn(this, "Missing required 'env' tag in metrics configuration");
            valid = false;
        }
        
        if (!hasHost) {
            Logger.warn(this, "Missing required 'host' tag in metrics configuration");
            valid = false;
        }
        
        return valid;
    }
}