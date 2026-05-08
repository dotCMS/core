package com.dotcms.queue;

import com.dotcms.queue.provider.NoOpQueuePublisher;
import com.dotcms.queue.provider.SqsQueuePublisher;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.Lazy;

/**
 * Configuration-driven factory that returns the active {@link DotQueuePublisher}
 * implementation. Set {@code DOT_QUEUE_PROVIDER} to select a provider:
 *
 * <ul>
 *   <li>{@code sqs}  — {@link SqsQueuePublisher}</li>
 *   <li>{@code noop} — {@link NoOpQueuePublisher} (default)</li>
 *   <li>A fully-qualified class name — instantiated via reflection
 *       (must implement {@link DotQueuePublisher} and have a no-arg constructor).
 *       This is a <b>trusted-operator setting</b>; it allows arbitrary class
 *       loading and must not be exposed to end users.</li>
 * </ul>
 *
 * <p>The provider is resolved once on first access via {@link Lazy}. Subsequent
 * calls return the cached instance. If resolution fails (bad class name, missing
 * constructor), the error is <em>not</em> cached — {@code get()} will re-attempt
 * on the next call, allowing ops to fix config without a restart.
 */
public final class DotQueuePublisherLocator {

    public static final String DOT_QUEUE_PROVIDER = "DOT_QUEUE_PROVIDER";

    private static final Lazy<DotQueuePublisher> INSTANCE = Lazy.of(() -> {
        final String raw = Config.getStringProperty(DOT_QUEUE_PROVIDER, "noop");
        return resolve(raw);
    });

    private DotQueuePublisherLocator() {
    }

    public static DotQueuePublisher get() {
        return INSTANCE.get();
    }

    @VisibleForTesting
    static DotQueuePublisher resolve(final String rawProvider) {
        final String provider = (rawProvider == null || rawProvider.trim().isEmpty())
                ? "noop"
                : rawProvider.trim();

        Logger.info(DotQueuePublisherLocator.class,
                "Initializing queue publisher with provider: " + provider);

        if ("sqs".equalsIgnoreCase(provider)) {
            return new SqsQueuePublisher();
        }

        if ("noop".equalsIgnoreCase(provider)) {
            return NoOpQueuePublisher.INSTANCE;
        }

        try {
            final Class<?> clazz = Class.forName(provider);
            final Object instance = clazz.getDeclaredConstructor().newInstance();
            if (instance instanceof DotQueuePublisher) {
                return (DotQueuePublisher) instance;
            }
            throw new DotQueueException(
                    "Class '" + provider + "' does not implement DotQueuePublisher");
        } catch (final DotQueueException e) {
            throw e;
        } catch (final Exception e) {
            throw new DotQueueException(
                    "Failed to instantiate queue provider '" + provider + "': " + e.getMessage(), e);
        }
    }
}
