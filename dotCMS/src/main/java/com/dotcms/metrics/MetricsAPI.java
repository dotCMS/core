package com.dotcms.metrics;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.http.request.StringPayloadHttpRequest;

import java.util.Map;

/**
 * Metrics API to send metrics to analytics.
 *
 * @author vico
 */
public interface MetricsAPI {

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
     * @param httpRequest {@link StringPayloadHttpRequest} instance with metrics data
     */
    void sendMetrics(final StringPayloadHttpRequest httpRequest);

    /**
     * Creates a {@link StringPayloadHttpRequest} instance to be used when sending metrics data.
     *
     * @param url metrics endpoint url
     * @param payload metrics data
     * @param token token
     * @return {@link StringPayloadHttpRequest} instance
     */
    static StringPayloadHttpRequest createMetricsRequest(final String url, final String payload, final String token) {
        return StringPayloadHttpRequest.builder()
            .url(url)
            .payload(payload)
            .queryParams(Map.of(MetricsSender.TOKEN_QUERY_PARAM_NAME, token))
            .build();
    }

    /**
     * Creates a {@link StringPayloadHttpRequest} instance to be used when sending metrics data.
     *
     * @param analyticsApp {@link AnalyticsApp} instance
     * @param payload metrics data
     * @return {@link StringPayloadHttpRequest} instance
     */
    static StringPayloadHttpRequest createMetricsRequest(final AnalyticsApp analyticsApp, final String payload) {
        return createMetricsRequest(
            analyticsApp.getAnalyticsProperties().analyticsWriteUrl(),
            payload,
            analyticsApp.getAnalyticsProperties().analyticsKey());
    }

}
