package com.dotcms.analytics.metrics;

import com.dotcms.analytics.app.AnalyticsApp;

import java.util.Map;

/**
 * Metrics API to send metrics to analytics.
 *
 * @author vico
 */
public interface MetricsAPI {

    String TOKEN_QUERY_PARAM_NAME = "token";

    /**
     * Subscribes to metrics to be sent to analytics.
     *
     * @param key key to identify metrics, probably the sender implementation class name
     * @param sender sender instance to send metrics
     */
    void subscribeToMetrics(final String key, final MetricsSender sender);

    /**
     * Unsubscribes from metrics to be sent to analytics.
     *
     * @param key, key to identify metrics, probably the sender implementation class name
     */
    void unsubscribeFromMetrics(final String key);

    /**
     * Sends metrics to analytics.
     *
     * @param metricsPayloadRequest {@link MetricsPayloadRequest} instance with metrics data
     */
    void sendMetrics(final MetricsPayloadRequest metricsPayloadRequest);

    /**
     * Creates a {@link MetricsPayloadRequest} instance to be used when sending metrics data.
     *
     * @param url metrics endpoint url
     * @param payload metrics data
     * @param token token
     * @return {@link StringPayloadHttpRequest} instance
     */
    static MetricsPayloadRequest createMetricsRequest(final String url, final String payload, final String token) {
        return MetricsPayloadRequest.builder()
            .url(url)
            .payload(payload)
            .queryParams(Map.of(TOKEN_QUERY_PARAM_NAME, token))
            .build();
    }

    /**
     * Creates a {@link MetricsPayloadRequest} instance to be used when sending metrics data.
     *
     * @param analyticsApp {@link AnalyticsApp} instance
     * @param payload metrics data
     * @return {@link MetricsPayloadRequest} instance
     */
    static MetricsPayloadRequest createMetricsRequest(final AnalyticsApp analyticsApp, final String payload) {
        return createMetricsRequest(
            analyticsApp.getAnalyticsProperties().analyticsWriteUrl(),
            payload,
            analyticsApp.getAnalyticsProperties().analyticsKey());
    }

}
