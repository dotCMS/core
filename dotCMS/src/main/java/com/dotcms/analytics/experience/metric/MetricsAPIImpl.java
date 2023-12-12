package com.dotcms.analytics.experience.metric;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.experience.metric.collector.MetricCollectorType;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

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

    @Override
    public MetricResult getHotResults(final User user) throws DotSecurityException {

        if (!user.isAdmin()) {
            throw new DotSecurityException("Not Admin User is not allowed to get Matric Results");
        }

        final MetricResult.Builder builder = new MetricResult.Builder();

        for (final MetricCollectorType metricCollectorType : MetricCollectorType.values()) {

            metricCollectorType.getCollector().collect(user).ifPresent(value ->
                    builder.get(metricCollectorType.getCategory())
                            .get(metricCollectorType.getFeature())
                            .put(MetricCollectorType.valueOf(metricCollectorType.name()), value)
            );

        }

        return builder.build();
    }

}
