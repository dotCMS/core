package com.dotcms.dotpubsub;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import io.vavr.Lazy;
import io.vavr.control.Try;

public class DotPubSubProviderLocator {

    public final static String DOT_PUBSUB_PROVIDER_OVERRIDE = "DOT_PUBSUB_PROVIDER_OVERRIDE";
    public final static String DOT_PUBSUB_USE_QUEUE = "DOT_PUBSUB_USE_QUEUE";
    /**
     * Default provider is postgres, can be overriden by setting config: DOT_PUBSUB_PROVIDER_OVERRIDE
     * DOT_PUBSUB_USE_QUEUE is a boolean, and will wrap the Pubsub in a queue
     */
    public static Lazy<DotPubSubProvider> provider = Lazy.of(() -> {

        final boolean useQueue = System.getProperty(DOT_PUBSUB_USE_QUEUE) != null
                        ? Boolean.valueOf(System.getProperty(DOT_PUBSUB_USE_QUEUE))
                        : Config.getBooleanProperty(DOT_PUBSUB_USE_QUEUE, true);

        final String pubsubClazz = System.getProperty(DOT_PUBSUB_PROVIDER_OVERRIDE) != null
                        ? System.getProperty(DOT_PUBSUB_PROVIDER_OVERRIDE)
                        : Config.getStringProperty(DOT_PUBSUB_PROVIDER_OVERRIDE,
                                        PostgresPubSubImpl.class.getCanonicalName());

        DotPubSubProvider provider = (DotPubSubProvider) Try.of(() -> Class.forName(pubsubClazz).newInstance())
                        .getOrElseThrow(e -> new DotRuntimeException(e));

        return (useQueue) ? new QueuingPubSubWrapper(provider) : provider;

    });

}
