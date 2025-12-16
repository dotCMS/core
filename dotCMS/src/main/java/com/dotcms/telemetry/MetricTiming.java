package com.dotcms.telemetry;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Stores execution timing information for a telemetry metric.
 *
 * <p>This class captures performance data for each metric collection,
 * including execution duration, cache hit status, timeout status, and warnings for slow metrics.</p>
 *
 * <p>Timing breakdown:</p>
 * <ul>
 *     <li><strong>durationMs</strong>: Total time including cache lookup (always present)</li>
 *     <li><strong>cacheHit</strong>: Whether the value was served from cache</li>
 *     <li><strong>computationMs</strong>: Actual computation time (only for cache misses, 0 for cache hits)</li>
 * </ul>
 *
 * <p>Used for:</p>
 * <ul>
 *     <li>Performance monitoring and optimization</li>
 *     <li>Cache effectiveness analysis (hit rate, time savings)</li>
 *     <li>Identifying slow metrics that need optimization</li>
 *     <li>Detecting metrics that exceed timeout thresholds</li>
 *     <li>Providing visibility into metric collection performance</li>
 * </ul>
 */
public class MetricTiming {

    private final String metricName;
    private final long durationMs;
    private final boolean cacheHit;
    private final long computationMs;
    private final boolean timedOut;
    private final boolean slow;

    /**
     * Creates a new MetricTiming instance.
     *
     * @param metricName the name of the metric
     * @param durationMs the total execution duration in milliseconds (including cache lookup)
     * @param cacheHit whether the value was served from cache
     * @param computationMs the actual computation time in milliseconds (0 for cache hits)
     * @param timedOut whether the metric exceeded its timeout
     * @param slow whether the metric exceeded the slow threshold
     */
    public MetricTiming(final String metricName, final long durationMs, final boolean cacheHit,
                        final long computationMs, final boolean timedOut, final boolean slow) {
        this.metricName = metricName;
        this.durationMs = durationMs;
        this.cacheHit = cacheHit;
        this.computationMs = computationMs;
        this.timedOut = timedOut;
        this.slow = slow;
    }

    /**
     * Gets the metric name.
     *
     * @return the metric name
     */
    @JsonProperty("metricName")
    public String getMetricName() {
        return metricName;
    }

    /**
     * Gets the total execution duration in milliseconds (including cache lookup).
     *
     * @return the duration in milliseconds
     */
    @JsonProperty("durationMs")
    public long getDurationMs() {
        return durationMs;
    }

    /**
     * Checks if the value was served from cache.
     *
     * @return true if cache hit, false if cache miss
     */
    @JsonProperty("cacheHit")
    public boolean isCacheHit() {
        return cacheHit;
    }

    /**
     * Gets the actual computation time in milliseconds.
     *
     * <p>For cache hits, this will be 0. For cache misses, this shows the actual
     * time taken to compute the metric value.</p>
     *
     * @return the computation time in milliseconds (0 for cache hits)
     */
    @JsonProperty("computationMs")
    public long getComputationMs() {
        return computationMs;
    }

    /**
     * Checks if the metric exceeded its timeout.
     *
     * @return true if the metric timed out, false otherwise
     */
    @JsonProperty("timedOut")
    public boolean isTimedOut() {
        return timedOut;
    }

    /**
     * Checks if the metric exceeded the slow threshold.
     *
     * <p>A metric can be slow without timing out - it completed successfully
     * but took longer than the warning threshold.</p>
     *
     * @return true if the metric was slow, false otherwise
     */
    @JsonProperty("slow")
    public boolean isSlow() {
        return slow;
    }

    @Override
    public String toString() {
        return String.format("MetricTiming{name='%s', durationMs=%d, cacheHit=%s, computationMs=%d, timedOut=%s, slow=%s}",
                metricName, durationMs, cacheHit, computationMs, timedOut, slow);
    }
}
