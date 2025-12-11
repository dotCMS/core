package com.dotcms.telemetry.cache;

import com.dotcms.telemetry.ProfileType;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;

/**
 * Configuration for telemetry metric collection.
 * 
 * <p>Reads the active profile from configuration properties. The active profile
 * determines which metrics are collected based on their {@link com.dotcms.telemetry.MetricsProfile}
 * annotations.</p>
 * 
 * <p>Configuration properties:</p>
 * <ul>
 *     <li>{@code telemetry.default.profile} - Profile for dashboard/API endpoints (default: MINIMAL)</li>
 *     <li>{@code telemetry.cron.profile} - Profile for scheduled cron job collection (default: FULL)</li>
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
}

