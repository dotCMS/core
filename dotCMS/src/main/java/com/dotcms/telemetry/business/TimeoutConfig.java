package com.dotcms.telemetry.business;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;

/**
 * Configuration for telemetry metric timeouts.
 *
 * <p>Reads timeout settings from configuration properties to protect the database
 * from runaway queries and ensure predictable collection times.</p>
 *
 * <p>Configuration properties:</p>
 * <ul>
 *     <li>{@code telemetry.metric.timeout.seconds} - Global timeout for any metric (default: 2)</li>
 *     <li>{@code telemetry.collection.timeout.seconds} - Total collection timeout (default: 30)</li>
 *     <li>{@code telemetry.metric.slow.threshold.ms} - Slow metric warning threshold (default: 500)</li>
 * </ul>
 *
 * <p><strong>Design Philosophy:</strong></p>
 * <ul>
 *     <li><strong>Global timeout</strong> - No metric can exceed this limit</li>
 *     <li><strong>Database protection</strong> - Prevents runaway queries</li>
 *     <li><strong>Predictable</strong> - Known worst-case collection time</li>
 *     <li><strong>Simple</strong> - One timeout rule, no exceptions</li>
 * </ul>
 *
 * @see com.dotcms.telemetry.collectors.MetricStatsCollector
 */
@ApplicationScoped
public class TimeoutConfig {

    private static final String METRIC_TIMEOUT_PROP = "telemetry.metric.timeout.seconds";
    private static final String COLLECTION_TIMEOUT_PROP = "telemetry.collection.timeout.seconds";
    private static final String SLOW_THRESHOLD_PROP = "telemetry.metric.slow.threshold.ms";

    private static final int DEFAULT_METRIC_TIMEOUT_SECONDS = 2;
    private static final int DEFAULT_COLLECTION_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_SLOW_THRESHOLD_MS = 500;

    /**
     * Gets the global timeout for any single metric collection.
     *
     * <p>This is a hard limit - no metric should ever exceed this timeout.
     * Metrics that timeout are skipped with an error logged.</p>
     *
     * @return the metric timeout in seconds (default: 2)
     */
    public int getMetricTimeoutSeconds() {
        return getPositiveInt(METRIC_TIMEOUT_PROP, DEFAULT_METRIC_TIMEOUT_SECONDS);
    }

    /**
     * Gets the global timeout for any single metric collection in milliseconds.
     *
     * @return the metric timeout in milliseconds (default: 2000)
     */
    public long getMetricTimeoutMillis() {
        return getMetricTimeoutSeconds() * 1000L;
    }

    /**
     * Gets the total timeout for the entire metric collection process.
     *
     * <p>This is a safety net to prevent the entire collection from hanging.
     * For ~100 metrics with 2s individual timeout, worst case would be 200s.
     * This timeout ensures we fail fast if something goes wrong.</p>
     *
     * @return the collection timeout in seconds (default: 30)
     */
    public int getCollectionTimeoutSeconds() {
        return getPositiveInt(COLLECTION_TIMEOUT_PROP, DEFAULT_COLLECTION_TIMEOUT_SECONDS);
    }

    /**
     * Gets the total timeout for the entire metric collection process in milliseconds.
     *
     * @return the collection timeout in milliseconds (default: 30000)
     */
    public long getCollectionTimeoutMillis() {
        return getCollectionTimeoutSeconds() * 1000L;
    }

    /**
     * Gets the warning threshold for slow metrics.
     *
     * <p>Metrics taking longer than this will be logged as warnings for optimization,
     * even if they complete successfully.</p>
     *
     * @return the slow threshold in milliseconds (default: 500)
     */
    public int getSlowThresholdMillis() {
        return getPositiveInt(SLOW_THRESHOLD_PROP, DEFAULT_SLOW_THRESHOLD_MS);
    }

    /**
     * Helper method to read a positive integer from configuration.
     *
     * @param propertyName the configuration property name
     * @param defaultValue the default value if property is not set or invalid
     * @return the property value or default
     */
    private int getPositiveInt(final String propertyName, final int defaultValue) {
        try {
            final int value = Config.getIntProperty(propertyName, defaultValue);
            if (value <= 0) {
                Logger.warn(this, String.format("Invalid value %d for property %s (must be positive), using default: %d",
                        value, propertyName, defaultValue));
                return defaultValue;
            }
            return value;
        } catch (final Exception e) {
            Logger.warn(this, String.format("Error reading property %s, using default: %d",
                    propertyName, defaultValue), e);
            return defaultValue;
        }
    }
}
