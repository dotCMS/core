package com.dotcms.metrics.timing;

import com.dotmarketing.util.Logger;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * A utility class for measuring and formatting time metrics.
 *
 * <p>This class allows you to create and track time metrics, measure the duration,
 * and format the duration in seconds using the default format. It also provides
 * methods for equality comparison based on the metric's name.</p>
 *
 * @author vico
 */
public class TimeMetric {

    private final String name;
    private long start = Long.MIN_VALUE;
    private long stop = Long.MIN_VALUE;

    private TimeMetric(final String name) {
        final String metricId = UUID.randomUUID().toString();
        this.name = (StringUtils.isNotBlank(name) ? name + "_" : "") + metricId;
    }

    public long getStart() {
        return start;
    }

    public long getStop() {
        return stop;
    }

    /**
     * Factory method to create a new TimeMetric instance with a given name and start timing.
     *
     * @param name The name of the time metric.
     * @return A new TimeMetric instance with the specified name and started timer.
     */
    public static TimeMetric mark(final String name) {
        return new TimeMetric(name).start();
    }

    /**
     * Start the timer for the TimeMetric instance.
     *
     * @return The TimeMetric instance with the timer started.
     */
    public TimeMetric start() {
        start = System.currentTimeMillis();
        reportStart();
        return this;
    }

    /**
     * Stop the timer for the TimeMetric instance.
     *
     * @return The TimeMetric instance with the timer stopped.
     */
    public TimeMetric stop() {
        stop = System.currentTimeMillis();
        reportStop();
        return this;
    }

    /**
     * Get the name of the TimeMetric.
     *
     * @return The name of the TimeMetric.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the duration of the TimeMetric in milliseconds.
     *
     * @return The duration of the TimeMetric in milliseconds.
     * @throws IllegalStateException if the TimeMetric is not started or stopped.
     */
    public long getDuration() {
        if (start == Long.MIN_VALUE || stop == Long.MIN_VALUE) {
            throw new IllegalStateException("TimeMetric not started or stopped");
        }
        return stop - start;
    }

    /**
     * Report the start of a time metric.
     */
    private void reportStart() {
        Logger.debug(this, String.format(">>>>> START [%s] at [%d]", getName(), getStart()));
    }

    /**
     * Report the stop of a time metric.
     */
    private void reportStop() {
        Logger.debug(
            this,
            String.format(
                "<<<<< STOP [%s] at [%d] / duration [%s]",
                getName(),
                getStop(),
                TimeMetricHelper.get().formatDuration(this)));
    }

}
