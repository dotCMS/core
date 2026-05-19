package com.dotcms.analytics.queue;

import com.dotcms.queue.DotQueueException;
import com.dotcms.queue.DotQueuePublisher;
import com.dotcms.queue.DotQueuePublisherLocator;
import com.dotmarketing.util.Logger;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Publishes analytics collector events to the configured queue provider (SQS, no-op, etc.)
 * as JSON matching the event manager's {@code UserEventPayload} schema.
 *
 * <p>Fire-and-forget: failures are logged but never propagated to the caller.
 */
public final class AnalyticsEventQueuePublisher {

    public static final String QUEUE_NAME = "ANALYTICS_EVENTS";
    static final String ATTR_TENANT = "tenant";
    static final String ATTR_PROJECT = "project";

    private AnalyticsEventQueuePublisher() {}

    /**
     * Transforms and publishes collector payloads to the analytics events queue.
     *
     * @param payloads collector output — one map per event
     * @param tenant   client/customer identifier written as an SQS message attribute
     * @param project  deployment project written as an SQS message attribute
     */
    public static void publish(final List<Map<String, Serializable>> payloads,
                               final String tenant,
                               final String project) {
        final String json = CollectorPayloadTransformer.toJson(payloads);
        if (json == null) {
            Logger.debug(AnalyticsEventQueuePublisher.class, "No events to publish — empty payload");
            return;
        }

        final Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put(ATTR_TENANT, tenant);
        attributes.put(ATTR_PROJECT, project);

        try {
            final DotQueuePublisher publisher = DotQueuePublisherLocator.get();
            publisher.publish(QUEUE_NAME, json, attributes);
            Logger.debug(AnalyticsEventQueuePublisher.class,
                    () -> "Published " + payloads.size() + " event(s) to queue " + QUEUE_NAME);
        } catch (final DotQueueException e) {
            Logger.warnAndDebug(AnalyticsEventQueuePublisher.class,
                    "Failed to publish analytics events to queue: " + e.getMessage(), e);
        }
    }
}
