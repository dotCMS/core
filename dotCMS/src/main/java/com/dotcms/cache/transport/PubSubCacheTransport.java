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
     * Invalidations dropped before this transport was ever initialized.
     *
     * Boot order guarantees some of these: caches are invalidated by startup tasks and by the
     * starter import long before {@code ClusterFactory} wires the cluster and calls
     * {@link #init(Server)}, and a node that is alone at that point has nobody to notify anyway. A
     * first boot against an empty database was measured at ~2,800 of them.
     *
     * They are kept apart from {@link #droppedMessages} so the number an operator sees on a
     * healthy node is zero rather than thousands of benign startup drops -- a cumulative counter
     * dominated by boot noise is worthless as an alerting signal, which is the whole point of
     * issue #36803. Reported separately for diagnostics.
     */
    final AtomicLong startupDroppedMessages = new AtomicLong(0);

    /**
     * Whether {@link #init(Server)} has ever succeeded, which ends the startup accounting for the
     * life of this transport. Drops after that point were suffered by a transport that had been
     * working and are genuine invalidation loss, so a later re-init must not retire them.
     */
    private final AtomicBoolean everInitialized = new AtomicBoolean(false);

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

    /**
     * {@inheritDoc}
     *
     * {@code synchronized} so the guard below is a genuine test-and-set rather than a
     * check-then-act: two threads could otherwise both read {@code initialized == false} and each
     * run {@code start()} + {@code subscribe()}, double-subscribing the listener -- the opposite of
     * the churn this transport was changed to remove.
     *
     * Today every path here is already serialized -- {@code init()} is reached only through
     * {@code ChainableCacheAdministratorImpl.setCluster()} <- {@code addMeToCacheIfNeeded()} <-
     * {@code rewireCluster()} <- {@code ClusterFactory.rewireClusterIfNeeded()}, which is
     * {@code static synchronized}, and each of those has exactly one call site. This guards the
     * future: {@code rewireCluster()} is {@code public static} and not itself synchronized, so a
     * new caller could bypass the lock that currently makes the race unreachable. The method runs
     * once per cluster rewire, so the monitor costs nothing.
     *
     * Note that {@code initialized} is deliberately set only *after* {@code start()} and
     * {@code subscribe()} have returned. Marking it up front (for instance via a
     * {@code compareAndSet} guard) would leave a thrown {@code start()} permanently flagged as
     * initialized: {@link #isInitialized()} would report true, {@link #shouldReinit()} would report
     * false, {@code setCluster()} would never retry, and the health check would report a healthy
     * transport that never subscribed -- exactly the silent failure issue #36803 removes.
     */
    @Override
    public synchronized void init(final Server localServer) throws CacheTransportException {

        if (this.initialized.get()) {
            Logger.debug(this.getClass(), "PubSubCacheTransport already initialized, skipping re-init");
            return;
        }

        Logger.info(this.getClass(), "initing PubSubCacheTransport");
        this.pubsub.start();
        this.pubsub.subscribe(topic);

        this.initialized.set(true);

        retireStartupDrops();
    }

    /**
     * On the first successful init only, moves the drops accumulated so far into
     * {@link #startupDroppedMessages}, so {@link #getDroppedMessages()} counts only invalidations
     * lost while the transport was expected to be carrying them.
     *
     * Deliberately first-init-only. A transport that came up, went down, dropped invalidations and
     * recovered has lost real ones, and retiring those on every re-init would launder genuine loss
     * into the benign startup bucket -- exactly the blind spot issue #36803 exists to remove.
     *
     * The drop-warning throttle is cleared on every init: a genuine drop minutes later must log
     * immediately rather than be swallowed because an earlier drop consumed the window.
     *
     * A {@code send()} racing with this can have its increment land in either bucket. The total is
     * preserved either way, and both counters are diagnostics rather than exact accounting.
     */
    private void retireStartupDrops() {

        if (this.everInitialized.compareAndSet(false, true)) {
            final long startupDrops = this.droppedMessages.getAndSet(0);
            if (startupDrops > 0) {
                this.startupDroppedMessages.addAndGet(startupDrops);
                Logger.info(this.getClass(), "Cache transport initialized. " + startupDrops
                        + " cache invalidation(s) were dropped before it came up (expected during"
                        + " startup, when there is no cluster to notify yet).");
            }
        }
        this.lastDropWarnAt.set(0);
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

    /**
     * {@inheritDoc}
     *
     * Shares {@link #init(Server)}'s monitor so the two cannot interleave. Without it a shutdown
     * landing between {@code init()}'s {@code subscribe()} and its {@code initialized.set(true)}
     * would stop the provider and then be overwritten back to initialized, leaving a transport that
     * reports itself up with nothing listening.
     */
    @Override
    public synchronized void shutdown() throws CacheTransportException {
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

    /**
     * {@inheritDoc}
     *
     * Counts only drops recorded after the transport was first initialized; see
     * {@link #startupDroppedMessages} for the benign pre-init ones.
     */
    @Override
    public long getDroppedMessages() {
        return droppedMessages.get();
    }

    @Override
    public long getStartupDroppedMessages() {
        return startupDroppedMessages.get();
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
