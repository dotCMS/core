package com.dotcms.queue;

import java.util.Map;

/**
 * Cloud-agnostic interface for publishing messages to an external queue.
 * Implementations handle provider-specific details (SQS, Pub/Sub, Service Bus, etc.)
 * and are selected at runtime via {@link DotQueuePublisherLocator}.
 */
public interface DotQueuePublisher {

    /**
     * Publishes a message to the specified queue.
     *
     * @param queueName  logical queue name, mapped to a provider-specific destination
     *                   by the implementation (e.g. {@code ANALYTICS_EVENTS})
     * @param messageBody the message payload, typically JSON
     * @param attributes  optional key-value metadata attached to the message;
     *                    implementations may map these to provider-specific message attributes
     * @throws DotQueueException if publishing fails
     */
    void publish(String queueName, String messageBody, Map<String, String> attributes);

    /**
     * Returns {@code true} if this publisher is configured and able to accept messages.
     */
    boolean isAvailable();
}
