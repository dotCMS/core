package com.dotcms.queue;

import com.dotcms.queue.provider.NoOpQueuePublisher;
import com.dotcms.queue.provider.SqsQueuePublisher;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;

/**
 * Configuration-driven factory that returns the active {@link DotQueuePublisher}
 * implementation. Set {@code DOT_QUEUE_PROVIDER} to select a provider:
 *
 * <ul>
 *   <li>{@code sqs}  — {@link SqsQueuePublisher}</li>
 *   <li>{@code noop} — {@link NoOpQueuePublisher} (default)</li>
 *   <li>A fully-qualified class name — instantiated via reflection
 *       (must implement {@link DotQueuePublisher} and have a no-arg constructor)</li>
 * </ul>
 */
public final class DotQueuePublisherLocator {

    public static final String DOT_QUEUE_PROVIDER = "DOT_QUEUE_PROVIDER";

    private static final Lazy<DotQueuePublisher> INSTANCE = Lazy.of(() -> {
        final String provider = Config.getStringProperty(DOT_QUEUE_PROVIDER, "noop");
        Logger.info(DotQueuePublisherLocator.class,
                "Initializing queue publisher with provider: " + provider);

        if ("sqs".equalsIgnoreCase(provider)) {
            return new SqsQueuePublisher();
        }

        if ("noop".equalsIgnoreCase(provider)) {
            return NoOpQueuePublisher.INSTANCE;
        }

        // Treat as a fully-qualified class name for custom/plugin providers
        try {
            final Class<?> clazz = Class.forName(provider);
            final Object instance = clazz.getDeclaredConstructor().newInstance();
            if (instance instanceof DotQueuePublisher) {
                return (DotQueuePublisher) instance;
            }
            Logger.error(DotQueuePublisherLocator.class,
                    "Class '" + provider + "' does not implement DotQueuePublisher, falling back to no-op");
        } catch (final Exception e) {
            Logger.error(DotQueuePublisherLocator.class,
                    "Failed to instantiate queue provider '" + provider + "': " + e.getMessage(), e);
        }

        return NoOpQueuePublisher.INSTANCE;
    });

    private DotQueuePublisherLocator() {
    }

    public static DotQueuePublisher get() {
        return INSTANCE.get();
    }
}
