package com.dotcms.queue;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Cloud-agnostic interface for publishing messages to an external queue.
 * Implementations handle provider-specific details (SQS, Pub/Sub, Service Bus, etc.)
 * and are selected at runtime via {@link DotQueuePublisherLocator}.
 *
 * <p><b>Usage pattern:</b> Callers should always call {@link #publish} directly.
 * The {@link #isAvailable} method is a configuration/capability check, not a
 * prerequisite guard. With the {@link com.dotcms.queue.provider.NoOpQueuePublisher}
 * default, {@code publish()} safely discards messages with a debug log — do not
 * short-circuit with {@code if (isAvailable) publish()} as that suppresses even
 * the debug audit trail.
 */
public interface DotQueuePublisher {

    /**
     * Publishes a message to the specified queue.
     *
     * @param queueName  logical queue name, mapped to a provider-specific destination
     *                   by the implementation (e.g. {@code ANALYTICS_EVENTS})
     * @param messageBody the message payload, typically JSON
     * @param attributes  optional key-value metadata attached to the message;
     *                    implementations may map these to provider-specific message attributes.
     *                    Entries with null or empty keys/values are silently filtered.
     * @throws DotQueueException if publishing fails
     */
    void publish(String queueName, String messageBody, @Nullable Map<String, String> attributes);

    /**
     * Returns {@code true} if the given queue name has a provider-specific destination
     * configured. This is a configuration check, not a connectivity or health check —
     * it does not probe the remote service. A {@code true} result does not guarantee
     * that a subsequent {@link #publish} call will succeed.
     *
     * @param queueName logical queue name to check
     */
    boolean isAvailable(String queueName);
}
