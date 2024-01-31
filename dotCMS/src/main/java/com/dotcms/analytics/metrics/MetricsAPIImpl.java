package com.dotcms.analytics.metrics;

import com.dotcms.analytics.app.AnalyticsApp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Metrics API to send metrics to analytics.
 *
 * @author vico
 */
public class MetricsAPIImpl implements MetricsAPI {

    private final Map<String, MetricsSender> metricsSenders = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribeToMetrics(final String key, final MetricsSender sender) {
        metricsSenders.put(key, sender);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unsubscribeFromMetrics(final String key) {
        metricsSenders.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMetrics(final AnalyticsApp analyticsApp, final String payload) {
        metricsSenders
                .values()
                .forEach(metricsSender ->
                        metricsSender.sendMetrics(
                                MetricsAPI.createAnalyticsAppPayload(
                                        analyticsApp,
                                        payload)));
    }

}
