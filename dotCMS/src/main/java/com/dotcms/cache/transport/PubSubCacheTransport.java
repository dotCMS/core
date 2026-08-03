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

    private final AtomicLong lastDropWarnAt = new AtomicLong(0);

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
            final long now = System.currentTimeMillis();
            final long lastWarn = this.lastDropWarnAt.get();
            if (now - lastWarn > DROP_WARN_INTERVAL_MILLIS
                    && this.lastDropWarnAt.compareAndSet(lastWarn, now)) {
                Logger.warn(this.getClass(),
                        "Cache transport is not initialized - dropping cluster cache invalidations. "
                                + "Other nodes may serve stale content. Total dropped: " + dropped);
            }
            return;
        }

        final DotPubSubEvent event = new DotPubSubEvent.Builder().withTopic(this.topic)
                        .withType(CacheEventType.INVAL.name()).withMessage(message).build();

        this.pubsub.publish(event);

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
