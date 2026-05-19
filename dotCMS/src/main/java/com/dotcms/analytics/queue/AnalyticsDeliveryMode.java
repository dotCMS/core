package com.dotcms.analytics.queue;

import com.dotmarketing.util.Config;

import java.util.Locale;

/**
 * Controls how server-side analytics events are delivered to the event manager.
 *
 * <ul>
 *   <li>{@link #HTTP} — POST to the configured {@code analyticsWriteUrl} (default, legacy)</li>
 *   <li>{@link #SQS} — publish to SQS via {@link AnalyticsEventQueuePublisher}</li>
 * </ul>
 *
 * <p>Resolved from {@code DOT_ANALYTICS_DELIVERY_MODE} config property.
 */
public enum AnalyticsDeliveryMode {

    HTTP,
    SQS;

    private static final String CONFIG_KEY = "DOT_ANALYTICS_DELIVERY_MODE";

    public static AnalyticsDeliveryMode resolve() {
        final String raw = Config.getStringProperty(CONFIG_KEY, HTTP.name());
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException e) {
            return HTTP;
        }
    }
}
