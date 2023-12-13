package com.dotcms.analytics.experience.metric;

/**
 * Metrics sender interface.
 *
 * @author vico
 */
public interface MetricsSender {

    /**
     * Sends metrics to analytics.
     *
     * @param analyticsAppPayload app to be used to resolve analytics url plus the payload
     */
    void sendMetrics(final AnalyticsAppPayload<String> analyticsAppPayload);

}
