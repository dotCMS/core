package com.dotcms.analytics.metrics;

/**
 * Metrics sender interface.
 *
 * @author vico
 */
public interface MetricsSender {

    /**
     * Sends metrics to analytics.
     *
     * @param metricsPayloadRequest {@link MetricsPayloadRequest} instance with metrics data
     */
    void sendMetrics(MetricsPayloadRequest metricsPayloadRequest);

}
