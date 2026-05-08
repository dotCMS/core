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

        if (!"noop".equalsIgnoreCase(provider)) {
            Logger.warn(DotQueuePublisherLocator.class,
                    "Unrecognized queue provider '" + provider
                            + "', falling back to no-op. Valid values: sqs, noop");
        }

        return NoOpQueuePublisher.INSTANCE;
    });

    private DotQueuePublisherLocator() {
    }

    public static DotQueuePublisher get() {
        return INSTANCE.get();
    }
}
