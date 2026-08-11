package com.dotcms.cache.transport;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import com.dotcms.cache.transport.CacheTransportTopic.CacheEventType;
import com.dotcms.cluster.bean.Server;
import com.dotcms.dotpubsub.DotPubSubEvent;
import com.dotcms.dotpubsub.DotPubSubProvider;
import com.dotcms.dotpubsub.DotPubSubProviderLocator;
import com.dotcms.dotpubsub.QueuingPubSubWrapper;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.cache.transport.CacheTransportException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.control.Try;

/**
 * This class uses the dotCMS pub/sub mechanism to send cache invalidations
 * to other servers in the cluster
 * @author will
 *
 */
public class PubSubCacheTransport implements CacheTransport {

    final DotPubSubProvider pubsub;

    final CacheTransportTopic topic;

    final AtomicBoolean initialized = new AtomicBoolean(false);

    final AtomicLong droppedMessages = new AtomicLong(0);

    /**
     * Sends that were attempted and reported as failed by a synchronous provider. Failures from
     * an asynchronous provider are counted by the provider itself and read back in
     * {@link #getFailedMessages()}; see {@link DotPubSubProvider#getFailedPublishCount(String)}.
     */
    final AtomicLong failedMessages = new AtomicLong(0);

    private final AtomicLong lastDropWarnAt = new AtomicLong(0);

    private final AtomicLong lastFailWarnAt = new AtomicLong(0);

    private static final long DROP_WARN_INTERVAL_MILLIS =
            Config.getLongProperty("CACHE_TRANSPORT_DROP_WARN_INTERVAL_MILLIS", 30000);

    @Override
    public boolean requiresAutowiring() {
        return false;
    }

    public PubSubCacheTransport() {
        this(DotPubSubProviderLocator.provider.get(), new CacheTransportTopic());
    }

    @VisibleForTesting
    PubSubCacheTransport(final DotPubSubProvider pubsub, final CacheTransportTopic topic) {
        this.pubsub = pubsub;
        this.topic = topic;
        Logger.debug(this.getClass(), "PubSubCacheTransport");
    }

    @Override
    public void init(final Server localServer) throws CacheTransportException {

        if (this.initialized.get()) {
            Logger.debug(this.getClass(), "PubSubCacheTransport already initialized, skipping re-init");
            return;
        }

        Logger.info(this.getClass(), "initing PubSubCacheTransport");
        this.pubsub.start();
        this.pubsub.subscribe(topic);

        this.initialized.set(true);

    }

    @Override
    public void send(final String message) throws CacheTransportException {
        if (!this.initialized.get()) {
            final long dropped = this.droppedMessages.incrementAndGet();
            warnThrottled(this.lastDropWarnAt,
                    "Cache transport is not initialized - dropping cluster cache invalidations. "
                            + "Other nodes may serve stale content. Total dropped: " + dropped);
            return;
        }

        final DotPubSubEvent event = new DotPubSubEvent.Builder().withTopic(this.topic)
                        .withType(CacheEventType.INVAL.name()).withMessage(message).build();

        // The boolean matters: every provider signals a failed send by returning false rather
        // than throwing, so ignoring it (as this did before issue #36803) made a transport that
        // was initialized but failing every publish completely invisible - no drops recorded,
        // isInitialized() still true, health checks green, other nodes stale. Asynchronous
        // providers cannot answer here and return true immediately; their failures are counted
        // provider-side and picked up by getFailedMessages().
        if (!this.pubsub.publish(event)) {
            final long failed = this.failedMessages.incrementAndGet();
            warnThrottled(this.lastFailWarnAt,
                    "Cache transport failed to publish cluster cache invalidations. "
                            + "Other nodes may serve stale content. Total failed: " + failed);
        }

    }

    /**
     * Logs at most one warning per {@link #DROP_WARN_INTERVAL_MILLIS} for the given throttle, so
     * a sustained failure does not flood the log at invalidation rate.
     */
    private void warnThrottled(final AtomicLong lastWarnAt, final String message) {
        final long now = System.currentTimeMillis();
        final long lastWarn = lastWarnAt.get();
        if (now - lastWarn > DROP_WARN_INTERVAL_MILLIS && lastWarnAt.compareAndSet(lastWarn, now)) {
            Logger.warn(this.getClass(), message);
        }
    }

    @Override
    public void testCluster() throws CacheTransportException {

        Logger.info(this.getClass(), "Sending PING to cluster ");
        final DotPubSubEvent event = new DotPubSubEvent.Builder().withType(CacheTransportTopic.CacheEventType.PING.name())
                        .withTopic(this.topic)

                        .build();

        this.pubsub.publish(event);

    }

    @Override
    public Map<String, Serializable> validateCacheInCluster(final int maxWaitInMillis) throws CacheTransportException {

        final DotPubSubEvent clusterStatusRequest = new DotPubSubEvent.Builder()
                        .withType(CacheTransportTopic.CacheEventType.CLUSTER_REQ.name()).withTopic(this.topic).build();

        final int numberOfOtherServers = Try.of(() -> APILocator.getServerAPI().getAliveServers().size()).getOrElse(0);
        this.topic.resetResponses();
        this.pubsub.publish(clusterStatusRequest);

        final long waitUntil = System.currentTimeMillis() + maxWaitInMillis;

        while (System.currentTimeMillis() < waitUntil) {

            if (this.topic.readResponses().size() >= numberOfOtherServers) {
                break;
            }

            Try.run(() -> Thread.sleep(50));

        }

        return this.topic.readResponses();
    }

    @Override
    public void shutdown() throws CacheTransportException {
        Logger.debug(this.getClass(), "shutdown()");
        this.pubsub.stop();
        if (initialized.get()) {
            initialized.set(false);
        }
    }

    @Override
    public boolean isInitialized() {
        Logger.debug(this.getClass(), "isInitialized");
        return initialized.get();
    }

    @Override
    public boolean shouldReinit() {

        return !initialized.get();
    }

    @Override
    public long getDroppedMessages() {
        return droppedMessages.get();
    }

    /**
     * {@inheritDoc}
     *
     * Sums the failures this transport observed synchronously with those the provider recorded on
     * its own thread. Exactly one of the two counts a given attempt: a synchronous provider
     * returns the real boolean to {@link #send(String)} and reports 0 here, while an
     * asynchronous one returns true immediately and counts the outcome itself.
     */
    @Override
    public long getFailedMessages() {
        return failedMessages.get() + this.pubsub.getFailedPublishCount(this.topic.getTopic());
    }

    @Override
    public CacheTransportInfo getInfo() {

        return new CacheTransportInfo() {
            @Override
            public String getClusterName() {
                return ClusterFactory.getClusterId();
            }

            
            
            @Override
            public String getCacheTransport() {

                return pubsub.getProviderName();
            }
            
            @Override
            public String getAddress() {
                return "n/a";
            }

            @Override
            public int getPort() {
                return -1;
            }

            @Override
            public boolean isOpen() {
                return true;
            }

            @Override
            public int getNumberOfNodes() {
                return Try.of(() -> APILocator.getServerAPI().getAliveServers().size()).getOrElse(-1);
            }

            @Override
            public long getReceivedBytes() {
                return topic.bytesReceived();
            }

            @Override
            public long getReceivedMessages() {
                return topic.messagesReceived();
            }

            @Override
            public long getSentBytes() {
                return topic.bytesSent();
            }

            @Override
            public long getSentMessages() {
                return topic.messagesSent();
            }
        };
    }

}
