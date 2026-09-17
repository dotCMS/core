package com.dotcms.dotpubsub;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.Lazy;
import io.vavr.control.Try;

/**
 * Resolves the {@link DotPubSubProvider} used to broadcast cache and cluster events.
 * <p>
 * The default is {@link JDBCPubSubImpl}. It can be replaced by setting the
 * {@code DOT_PUBSUB_PROVIDER_OVERRIDE} system property or config property to the fully qualified
 * name of an alternative implementation, for example {@code RedisPubSubImpl}.
 * <p>
 * If the configured class cannot be loaded or constructed - for instance because it belongs to a
 * dependency that is no longer shipped - the default provider is used instead and a warning is
 * logged. A stale override therefore degrades to the built-in behaviour rather than preventing
 * the node from starting.
 */
public class DotPubSubProviderLocator {

    public final static String DOT_PUBSUB_PROVIDER_OVERRIDE = "DOT_PUBSUB_PROVIDER_OVERRIDE";
    public final static String DOT_PUBSUB_USE_QUEUE = "DOT_PUBSUB_USE_QUEUE";

    /**
     * Default provider is JDBCPubSubImpl, can be overriden by setting config: DOT_PUBSUB_PROVIDER_OVERRIDE
     * DOT_PUBSUB_USE_QUEUE is a boolean, and will wrap the Pubsub in a queue
     */
    public static Lazy<DotPubSubProvider> provider = Lazy.of(() -> {

        final boolean useQueue = System.getProperty(DOT_PUBSUB_USE_QUEUE) != null
                        ? Boolean.valueOf(System.getProperty(DOT_PUBSUB_USE_QUEUE))
                        : Config.getBooleanProperty(DOT_PUBSUB_USE_QUEUE, true);

        final String pubsubClazz = System.getProperty(DOT_PUBSUB_PROVIDER_OVERRIDE) != null
                ? System.getProperty(DOT_PUBSUB_PROVIDER_OVERRIDE)
                : Config.getStringProperty(DOT_PUBSUB_PROVIDER_OVERRIDE,
                JDBCPubSubImpl.class.getCanonicalName());

        final DotPubSubProvider provider = resolveProvider(pubsubClazz);

        return (useQueue) ? new QueuingPubSubWrapper(provider) : provider;

    });

    /**
     * Resolves the provider named by {@code DOT_PUBSUB_PROVIDER_OVERRIDE}, falling back to
     * {@link JDBCPubSubImpl} when that class is unavailable.
     *
     * @param pubsubClazz fully qualified name of the requested provider implementation
     * @return the resolved provider
     * @throws DotRuntimeException if neither the requested nor the default provider can be created
     */
    private static DotPubSubProvider resolveProvider(final String pubsubClazz) {
        return resolveProvider(pubsubClazz, JDBCPubSubImpl.class.getCanonicalName());
    }

    /**
     * Resolves the provider named by {@code pubsubClazz}, falling back to {@code defaultClazz} when
     * the requested class is missing, exposes no no-arg constructor, throws from its constructor, or
     * does not implement {@link DotPubSubProvider}.
     * <p>
     * A failure to build the <em>default</em> provider is not recoverable and is rethrown, since
     * there is nothing left to fall back to.
     *
     * @param pubsubClazz  fully qualified name of the requested provider implementation
     * @param defaultClazz fully qualified name of the provider to fall back to
     * @return the resolved provider
     * @throws DotRuntimeException if the fallback (or an explicitly requested default) is unusable
     */
    @VisibleForTesting
    static DotPubSubProvider resolveProvider(final String pubsubClazz, final String defaultClazz) {

        final Try<DotPubSubProvider> configured = Try.of(() -> newProviderInstance(pubsubClazz));

        if (configured.isSuccess() || defaultClazz.equals(pubsubClazz)) {
            return configured.getOrElseThrow(DotRuntimeException::new);
        }

        Logger.warn(DotPubSubProviderLocator.class,
                () -> "Unable to instantiate DOT_PUBSUB_PROVIDER_OVERRIDE '" + pubsubClazz
                        + "', falling back to " + defaultClazz,
                configured.getCause());

        return Try.of(() -> newProviderInstance(defaultClazz))
                .getOrElseThrow(DotRuntimeException::new);
    }

    /**
     * Creates a provider from its fully qualified class name.
     *
     * @param className fully qualified name of the provider implementation
     * @return a new provider instance
     * @throws ReflectiveOperationException if the class is missing, has no no-arg constructor, or
     *                                      the constructor itself fails
     * @throws ClassCastException           if the class does not implement {@link DotPubSubProvider}
     */
    private static DotPubSubProvider newProviderInstance(final String className)
            throws ReflectiveOperationException {
        return (DotPubSubProvider) Class.forName(className).getDeclaredConstructor().newInstance();
    }

}
