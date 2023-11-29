package com.dotcms.analytics.metrics;

import com.dotcms.analytics.app.AnalyticsApp;

import java.io.Serializable;

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
     * @param analyticsApp app to be used to resolve analytics url
     * @param payload payload to send
     */
    void sendMetrics(final AnalyticsApp analyticsApp, final String payload);

    /**
     * Creates an {@link AnalyticsAppPayload} instance based on the given parameters.
     *
     * @param analyticsApp analytics app
     * @param payload payload to send
     * @return an {@link AnalyticsAppPayload} instance
     */
    static <P extends Serializable> AnalyticsAppPayload<P> createAnalyticsAppPayload(final AnalyticsApp analyticsApp,
                                                                                     final P payload) {
        return AnalyticsAppPayload.<P>builder()
                .analyticsApp(analyticsApp)
                .payload(payload)
                .build();
    }

}
