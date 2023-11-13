package com.dotcms.metrics;

import com.dotcms.analytics.metrics.MetricsPayloadRequest;

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
