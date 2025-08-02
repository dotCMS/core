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
 * This service manages the creation and validation of metric tags, with dynamic
 * environment variable discovery for flexible deployment configuration.
 * 
 * Key features:
 * - Centralized tag management for all metrics
 * - Dynamic discovery of environment variable-based tags
 * - Internal build information tags (version, commit)
 * - Validation of tag configuration at startup
 * 
 * Dynamic Environment Variable Tags:
 * Environment variables with the prefix DOT_METRICS_TAG_ are automatically
 * discovered and converted to metric tags:
 * 
 * Examples:
 * - DOT_METRICS_TAG_APP=dotcms → app=dotcms
 * - DOT_METRICS_TAG_ENV=production → env=production  
 * - DOT_METRICS_TAG_CUSTOMER=acme → customer=acme
 * - DOT_METRICS_TAG_DEPLOYMENT=web-cluster → deployment=web-cluster
 * - DOT_METRICS_TAG_NAMESPACE=default → namespace=default
 * - DOT_METRICS_TAG_POD=dotcms-0 → pod=dotcms-0
 * 
 * Internal Tags (always present):
 * - version: dotCMS version from build (e.g., "1.0.0-SNAPSHOT")
 * - commit: Git commit hash from build (e.g., "1f95051")
 * - host: System hostname
 * - app: Application name (defaults to "dotcms", can be overridden with DOT_METRICS_TAG_APP)
 * 
 * Optional Dynamic Tags:
 * - env: Environment name (only if DOT_METRICS_TAG_ENV is set)
 * - customer: Customer identifier (only if DOT_METRICS_TAG_CUSTOMER is set)
 * - deployment: Deployment name (only if DOT_METRICS_TAG_DEPLOYMENT is set)
 * - namespace: Kubernetes namespace (only if DOT_METRICS_TAG_NAMESPACE is set)
 * - pod: Kubernetes pod name (only if DOT_METRICS_TAG_POD is set)
 * - Any other tags from DOT_METRICS_TAG_* environment variables
 * 
 * The service is managed by CDI and automatically initialized during application startup.
 */
@ApplicationScoped
public class MetricsTaggingService {

    

    
    // Cached tag values
    private List<Tag> commonTags;
    private String hostname;
    
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
        
        // Internal tags that cannot be overridden
        tags.add(Tag.of("version", MetricsConfig.VERSION_TAG));
        tags.add(Tag.of("commit", MetricsConfig.BUILD_COMMIT_TAG));
        
        // Host tag (internal infrastructure)
        tags.add(Tag.of("host", hostname));
        
        // Default application tag (can be overridden by environment variable)
        tags.add(Tag.of("app", "dotcms"));
        
        // Dynamic environment-based tags (if enabled)
        if (MetricsConfig.DYNAMIC_TAGGING_ENABLED) {
            addDynamicEnvironmentTags(tags);
        }
        
        return tags;
    }
    
    /**
     * Dynamically discovers and adds environment variable-based tags.
     * Uses Config.subsetContainsAsList() to find all properties with DOT_METRICS_TAG_ prefix.
     * This discovers all such environment variables dynamically, not just predefined ones.
     * Dynamic tags can override default tags (like the default "app" tag).
     * 
     * Tag Name Conversion Rules (following Prometheus best practices):
     * - Environment variable suffixes are converted to lowercase
     * - Underscores are preserved (Prometheus standard)
     * - Multiple underscores are preserved as-is
     * - Only alphanumeric characters and underscores are allowed
     * 
     * Examples:
     * - DOT_METRICS_TAG_APP=myapp → app=myapp (overrides default "dotcms")
     * - DOT_METRICS_TAG_ENV=production → env=production
     * - DOT_METRICS_TAG_CUSTOMER_ID=acme → customer_id=acme
     * - DOT_METRICS_TAG_MY_CUSTOM_TAG=value → my_custom_tag=value
     * - DOT_METRICS_TAG_POD_NAME=web-1 → pod_name=web-1
     */
    private void addDynamicEnvironmentTags(List<Tag> tags) {
        // Use Config.subsetContainsAsList to find all properties with the metrics tag prefix
        String configPrefix = "METRICS_TAG";
        List<String> metricsTagProperties = Config.subsetContainsAsList(configPrefix);
        
        for (String propertyKey : metricsTagProperties) {
            // Only process properties that actually start with our prefix pattern
            if (propertyKey.contains("METRICS_TAG_")) {
                String tagValue = Config.getStringProperty(propertyKey, null);
                
                if (UtilMethods.isSet(tagValue)) {
                    // Extract tag name from property key
                    // Handle both DOT_METRICS_TAG_APP and METRICS_TAG_APP patterns
                    String tagName;
                    if (propertyKey.startsWith("DOT_METRICS_TAG_")) {
                        tagName = propertyKey.substring("DOT_METRICS_TAG_".length()).toLowerCase();
                    } else if (propertyKey.contains("METRICS_TAG_")) {
                        int index = propertyKey.indexOf("METRICS_TAG_");
                        tagName = propertyKey.substring(index + "METRICS_TAG_".length()).toLowerCase();
                    } else {
                        continue; // Skip if it doesn't match expected pattern
                    }
                    
                    // Validate tag name - only allow alphanumeric and underscores (Prometheus standard)
                    if (tagName.matches("^[a-z0-9_]+$") && !tagName.isEmpty()) {
                        // Remove any existing tag with the same name (to allow overriding defaults)
                        tags.removeIf(tag -> tag.getKey().equals(tagName));
                        
                        // Add the new tag
                        tags.add(Tag.of(tagName, tagValue));
                        Logger.debug(this, "Added dynamic tag: " + tagName + "=" + tagValue + " (from " + propertyKey + ")");
                    } else {
                        Logger.warn(this, "Skipping invalid tag name from property: " + propertyKey + 
                                   " (tag names must be lowercase alphanumeric with underscores only)");
                    }
                } else {
                    Logger.debug(this, "Skipping empty property: " + propertyKey);
                }
            }
        }
        
        // Special handling for replica extraction from hostname if not explicitly set
        if (tags.stream().noneMatch(tag -> "replica".equals(tag.getKey()))) {
            String extractedReplica = extractReplicaFromHostname(hostname);
            if (UtilMethods.isSet(extractedReplica)) {
                tags.add(Tag.of("replica", extractedReplica));
                Logger.debug(this, "Added replica tag from hostname: " + extractedReplica);
            }
        }
        
        // Fallback for environment if not set via DOT_METRICS_TAG_ENV
        if (tags.stream().noneMatch(tag -> "env".equals(tag.getKey()))) {
            String fallbackEnv = Config.getStringProperty("environment", "local");
            if (UtilMethods.isSet(fallbackEnv)) {
                tags.add(Tag.of("env", fallbackEnv));
                Logger.debug(this, "Added fallback env tag: " + fallbackEnv);
            }
        }
    }
    
    /**
     * Build minimal fallback tags when initialization fails.
     * 
     * @return List of minimal tags
     */
    private List<Tag> buildFallbackTags() {
        List<Tag> tags = new ArrayList<>();
        
        try {
            // Essential internal tags always available
            tags.add(Tag.of("version", MetricsConfig.VERSION_TAG));
            tags.add(Tag.of("commit", MetricsConfig.BUILD_COMMIT_TAG));
            tags.add(Tag.of("host", hostname != null ? hostname : "unknown"));
            
            // Add minimal app tag as fallback
            tags.add(Tag.of("app", "dotcms"));
            tags.add(Tag.of("env", "unknown"));
        } catch (Exception e) {
            Logger.warn(this, "Failed to create fallback tags: " + e.getMessage());
        }
        
        return tags;
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
        Logger.debug(this, "  Common Tags:");
        
        for (Tag tag : commonTags) {
            Logger.debug(this, "    " + tag.getKey() + " = " + tag.getValue());
        }
    }
    
    /**
     * Extract replica number from K8s StatefulSet hostname pattern.
     * Looks for patterns like "app-name-123-5" where 5 is the replica number.
     * 
     * @param hostname The hostname to analyze
     * @return replica number as string, or null if not found
     */
    private String extractReplicaFromHostname(String hostname) {
        if (!UtilMethods.isSet(hostname)) {
            return null;
        }
        
        // Pattern for K8s StatefulSet: app-name-hash-replica
        // Examples: dotcms-prod-abc123-0, my-app-staging-def456-2
        Pattern pattern = Pattern.compile(".*-(\\d+)$");
        Matcher matcher = pattern.matcher(hostname);
        
        if (matcher.find()) {
            String replica = matcher.group(1);
            Logger.debug(this, "Extracted replica '" + replica + "' from hostname: " + hostname);
            return replica;
        }
        
        Logger.debug(this, "No replica pattern found in hostname: " + hostname);
        return null;
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
        boolean hasHost = commonTags.stream().anyMatch(tag -> "host".equals(tag.getKey()));
        
        if (!hasApp) {
            Logger.warn(this, "Missing required 'app' tag in metrics configuration");
            valid = false;
        }
        
        if (!hasHost) {
            Logger.warn(this, "Missing required 'host' tag in metrics configuration");
            valid = false;
        }
        
        return valid;
    }
}