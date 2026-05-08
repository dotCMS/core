package com.dotcms.queue.provider;

import com.dotcms.queue.DotQueuePublisher;
import com.dotmarketing.util.Logger;

import java.util.Map;

/**
 * Default {@link DotQueuePublisher} that discards all messages. Active when no
 * queue provider is configured ({@code DOT_QUEUE_PROVIDER=noop} or unset).
 */
public final class NoOpQueuePublisher implements DotQueuePublisher {

    public static final NoOpQueuePublisher INSTANCE = new NoOpQueuePublisher();

    private NoOpQueuePublisher() {
    }

    @Override
    public void publish(final String queueName,
                        final String messageBody,
                        final Map<String, String> attributes) {
        Logger.debug(NoOpQueuePublisher.class,
                () -> "Queue publishing disabled, dropping message for queue: " + queueName);
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
