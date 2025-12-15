package com.dotcms.telemetry.cache;

import com.dotcms.telemetry.ProfileType;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;

/**
 * Configuration for telemetry metric collection and caching.
 *
 * <p>Reads the active profile from configuration properties. The active profile
 * determines which metrics are collected based on their {@link com.dotcms.telemetry.MetricsProfile}
 * annotations.</p>
 *
 * <p>Also provides caching configuration for metrics, controlling cache TTL and
 * per-metric cache settings.</p>
 *
 * <p>Configuration properties:</p>
 * <ul>
 *     <li>{@code telemetry.default.profile} - Profile for dashboard/API endpoints (default: MINIMAL)</li>
 *     <li>{@code telemetry.cron.profile} - Profile for scheduled cron job collection (default: FULL)</li>
 *     <li>{@code telemetry.cache.enabled} - Enable/disable metric caching globally (default: true)</li>
 *     <li>{@code telemetry.cache.default.ttl.seconds} - Default cache TTL in seconds (default: 300)</li>
 *     <li>{@code telemetry.cache.max.size} - Maximum cache size (default: 1000)</li>
 *     <li>{@code telemetry.cache.metric.{name}.enabled} - Enable/disable caching for specific metric</li>
 *     <li>{@code telemetry.cache.metric.{name}.ttl.seconds} - Custom TTL for specific metric</li>
 * </ul>
 * 
 * @see ProfileType
 * @see com.dotcms.telemetry.MetricsProfile
 */
@ApplicationScoped
public class MetricCacheConfig {
    
    private static final String DEFAULT_PROFILE_PROP = "telemetry.default.profile";
    private static final String CRON_PROFILE_PROP = "telemetry.cron.profile";
    private static final ProfileType DEFAULT_PROFILE = ProfileType.MINIMAL;
    private static final ProfileType DEFAULT_CRON_PROFILE = ProfileType.FULL;

    // Cache configuration properties
    private static final String CACHE_ENABLED_PROP = "telemetry.cache.enabled";
    private static final String DEFAULT_TTL_PROP = "telemetry.cache.default.ttl.seconds";
    private static final String MAX_SIZE_PROP = "telemetry.cache.max.size";

    private static final boolean DEFAULT_CACHE_ENABLED = true;
    private static final long DEFAULT_TTL_SECONDS = 300; // 5 minutes
    private static final long DEFAULT_MAX_SIZE = 1000;
    
    /**
     * Gets the active profile for dashboard/API endpoints from configuration.
     * 
     * <p>Reads from {@code telemetry.default.profile} property. If the property
     * is not set or contains an invalid value, returns {@link ProfileType#MINIMAL}.</p>
     * 
     * <p>This profile is used by the Usage Portlet dashboard for fast loading.</p>
     * 
     * @return the active profile type for dashboard/API
     */
    public ProfileType getActiveProfile() {
        return getProfile(DEFAULT_PROFILE_PROP, DEFAULT_PROFILE, "dashboard/API");
    }
    
    /**
     * Gets the profile for scheduled cron job collection from configuration.
     * 
     * <p>Reads from {@code telemetry.cron.profile} property. If the property
     * is not set or contains an invalid value, returns {@link ProfileType#FULL}.</p>
     * 
     * <p>This profile is used by the {@link com.dotcms.telemetry.job.MetricsStatsJob}
     * to collect all metrics for persistence, regardless of dashboard performance requirements.</p>
     * 
     * @return the profile type for cron job collection
     */
    public ProfileType getCronProfile() {
        return getProfile(CRON_PROFILE_PROP, DEFAULT_CRON_PROFILE, "cron job");
    }
    
    /**
     * Helper method to read profile from configuration.
     * 
     * @param propertyName the configuration property name
     * @param defaultValue the default profile if property is not set or invalid
     * @param context description of where this profile is used (for logging)
     * @return the profile type
     */
    private ProfileType getProfile(final String propertyName, final ProfileType defaultValue, final String context) {
        final String profileStr = Config.getStringProperty(propertyName, defaultValue.name());
        try {
            final ProfileType profile = ProfileType.valueOf(profileStr.toUpperCase());
            Logger.debug(this, () -> String.format("Telemetry profile for %s: %s (from property: %s)",
                context, profile, profileStr));
            return profile;
        } catch (final IllegalArgumentException e) {
            Logger.warn(this, String.format("Invalid telemetry profile '%s' in property %s, using default: %s",
                profileStr, propertyName, defaultValue), e);
            return defaultValue;
        }
    }

    /**
     * Check if metric caching is enabled globally.
     *
     * <p>Reads from {@code telemetry.cache.enabled} property.</p>
     *
     * @return true if caching is enabled globally, false otherwise
     */
    public boolean isCachingEnabled() {
        return Config.getBooleanProperty(CACHE_ENABLED_PROP, DEFAULT_CACHE_ENABLED);
    }

    /**
     * Check if caching is enabled for a specific metric.
     *
     * <p>First checks if global caching is enabled. If disabled globally, returns false.
     * Then checks for per-metric override via {@code telemetry.cache.metric.{name}.enabled}.</p>
     *
     * @param metricName the metric name
     * @return true if caching is enabled for this metric, false otherwise
     */
    public boolean isCachingEnabled(final String metricName) {
        if (!isCachingEnabled()) {
            return false; // Global disable
        }

        // Check per-metric override
        final String prop = "telemetry.cache.metric." + metricName + ".enabled";
        return Config.getBooleanProperty(prop, true); // Default: enabled if global enabled
    }

    /**
     * Get cache TTL in milliseconds for a specific metric.
     *
     * <p>Checks for per-metric override via {@code telemetry.cache.metric.{name}.ttl.seconds}.
     * If not found, uses default TTL from {@code telemetry.cache.default.ttl.seconds}.</p>
     *
     * @param metricName the metric name
     * @return cache TTL in milliseconds
     */
    public long getCacheTTL(final String metricName) {
        // Check per-metric override
        final String prop = "telemetry.cache.metric." + metricName + ".ttl.seconds";
        final long ttlSeconds = Config.getLongProperty(prop, 0);

        if (ttlSeconds > 0) {
            return ttlSeconds * 1000; // Convert to milliseconds
        }

        // Use default
        final long defaultTtlSeconds = Config.getLongProperty(DEFAULT_TTL_PROP, DEFAULT_TTL_SECONDS);
        return defaultTtlSeconds * 1000; // Convert to milliseconds
    }

    /**
     * Get maximum cache size.
     *
     * <p>Reads from {@code telemetry.cache.max.size} property.</p>
     *
     * @return maximum number of entries in the cache
     */
    public long getMaxCacheSize() {
        return Config.getLongProperty(MAX_SIZE_PROP, DEFAULT_MAX_SIZE);
    }
}

