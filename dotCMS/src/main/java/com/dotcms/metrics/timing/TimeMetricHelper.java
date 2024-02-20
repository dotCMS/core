package com.dotcms.metrics.timing;

import io.vavr.Lazy;


/**
 * Utility class for formatting time metrics.
 *
 * @author vico
 */
public class TimeMetricHelper {

    private static final Lazy<TimeMetricHelper> INSTANCE = Lazy.of(TimeMetricHelper::new);

    private TimeMetricHelper() {}

    /**
     * Get an instance of the {@link TimeMetricHelper} class.
     *
     * @return The singleton instance of {@link TimeMetricHelper}.
     */
    public static TimeMetricHelper get() {
        return INSTANCE.get();
    }

    /**
     * Format a time metric in seconds using a custom mask.
     *
     * @param timeMetric The time metric to format.
     * @param mask       The custom format mask (e.g., "%.2f").
     * @return The formatted string representing the time metric.
     */
    public String formatDuration(final TimeMetric timeMetric, final String mask) {
        return String.format(mask, (float) timeMetric.getDuration() / 1000);
    }

    /**
     * Format a time metric in seconds using the default mask "%.4f".
     *
     * @param timeMetric The time metric to format.
     * @return The formatted string representing the time metric.
     */
    public String formatDuration(final TimeMetric timeMetric) {
        return formatDuration(timeMetric, "%.4f");
    }

}
