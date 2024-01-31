package com.dotcms.metrics;

import com.dotcms.analytics.metrics.MetricsPayloadRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Metrics API to send metrics to analytics.
 *
 * @author vico
 */
public class MetricsAPIImpl implements MetricsAPI {

    private final Map<String, MetricsSender> metricsSender = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribeToMetrics(final String key, final MetricsSender sender) {
        metricsSender.put(key, sender);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unsubscribeFromMetrics(final String key) {
        metricsSender.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMetrics(final MetricsPayloadRequest metricsPayloadRequest) {
        metricsSender.values().forEach(metricsSender -> metricsSender.sendMetrics(metricsPayloadRequest));
    }

}
