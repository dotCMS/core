package com.dotcms.publisher.pusher;

import com.dotcms.rest.RestClientBuilder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;

/**
 * Builds the REST {@link Client} used by Push Publishing on the sender side: {@link PushPublisher}
 * uploads bundles with it and {@code PublisherQueueJob} polls the receivers' audit status with it.
 * <p>
 * It is the plain {@link RestClientBuilder} client plus a <b>connect timeout</b>. Jersey's default
 * connect timeout is 0, which means "wait forever"; the only thing that ended a connection attempt
 * to an endpoint that never answers was the operating system, after about two minutes. Because the
 * publisher job processes bundles one at a time, that wait blocked every bundle queued behind the
 * unreachable one (see GitHub issue #37449).
 * <p>
 * Configuration:
 * <ul>
 *   <li>{@value #CONNECT_TIMEOUT_MS_PROPERTY}: connect timeout in milliseconds. Default
 *   {@value #DEFAULT_CONNECT_TIMEOUT_MS}. A value of {@code 0} restores the unbounded behavior.</li>
 * </ul>
 * Only the TCP handshake wait is bounded. No read timeout is applied, so large bundle uploads and
 * slow responses are unaffected. Other users of {@link RestClientBuilder} are not touched.
 *
 * @author hassandotcms
 * @since Sep 2026
 */
public final class PushPublishClientFactory {

    /** Config key for the connect timeout applied to the push-publish REST client, in milliseconds. */
    public static final String CONNECT_TIMEOUT_MS_PROPERTY = "PUSH_PUBLISH_CONNECT_TIMEOUT_MS";

    /** Default connect timeout: ten seconds. */
    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 10_000;

    private PushPublishClientFactory() {
        // static factory
    }

    /**
     * Returns a new REST client for talking to Push Publishing endpoints, with the configured connect
     * timeout applied. The value is read on every call so a configuration change takes effect on the
     * next publisher run without a restart.
     *
     * @return a new {@link Client}; callers own it and must close it
     */
    public static Client newClient() {
        final Client client = RestClientBuilder.newClient();
        final int connectTimeoutMs = Config.getIntProperty(CONNECT_TIMEOUT_MS_PROPERTY, DEFAULT_CONNECT_TIMEOUT_MS);
        if (connectTimeoutMs > 0) {
            client.property(ClientProperties.CONNECT_TIMEOUT, connectTimeoutMs);
        } else {
            Logger.debug(PushPublishClientFactory.class, () -> CONNECT_TIMEOUT_MS_PROPERTY
                    + " is " + connectTimeoutMs + ": push-publish connection attempts are unbounded");
        }
        return client;
    }

}
