package com.dotcms.dotpubsub;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotConcurrentFactory.SubmitterConfig;
import com.dotcms.concurrent.DotSubmitter;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * This class wraps a pub/sub mechanism and de-dupes the messages sent through it. Once a second,
 * each topic/queue is reduced into a Set<DotPubSubEvent> before being transmitted over the wire
 * 
 * @author will
 *
 */
public class QueuingPubSubWrapper implements DotPubSubProvider {

    private final DotPubSubProvider wrappedProvider;

    private final Cache<Integer, Boolean> recentEvents ;

    private final DotSubmitter submitter;

    /**
     * Failed publish attempts per topic. Keyed by topic because one provider instance serves
     * every topic in the JVM, so a single total could not be attributed to the cache transport.
     *
     * Keys are normalized through {@link #topicKey(String)}; see that method for why.
     */
    private final Map<String, AtomicLong> failedByTopic = new ConcurrentHashMap<>();
    


    public QueuingPubSubWrapper(DotPubSubProvider providerIn) {
        this.wrappedProvider = providerIn instanceof QueuingPubSubWrapper 
                        ? ((QueuingPubSubWrapper) providerIn).wrappedProvider
                        : providerIn;

        
        final SubmitterConfig config = new DotConcurrentFactory.SubmitterConfigBuilder()
                        .poolSize(1)
                        .maxPoolSize(Config.getIntProperty("PUBSUB_QUEUE_DEDUPE_THREADS", 10))
                        .keepAliveMillis(1000)
                        .queueCapacity(Config.getIntProperty("PUBSUB_QUEUE_SIZE", 10000))
                        .rejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                        .build();
        
        this.submitter = DotConcurrentFactory.getInstance().getSubmitter("QueuingPubSubWrapperSubmitter", config);

        this.recentEvents = Caffeine.newBuilder()
                        .initialCapacity(10000)
                        .expireAfterWrite(Config.getIntProperty("PUBSUB_QUEUE_DEDUPE_TTL_MILLIS", 1500), TimeUnit.MILLISECONDS)
                        .maximumSize(50000)
                        .build();
        
        
    }

    @Override
    public String getProviderName() {
        return wrappedProvider.getProviderName();
    }
    
    
    public QueuingPubSubWrapper() {
        this(DotPubSubProviderLocator.provider.get());
    }


    @Override
    public DotPubSubProvider subscribe(final DotPubSubTopic topic) {
        return this.wrappedProvider.subscribe(topic);
    }

    @Override
    public DotPubSubProvider start() {

        this.wrappedProvider.start();
        return this;
    }

    @Override
    public void stop() {
        this.wrappedProvider.stop();
    }
    long skipped=0;
    long sent=0;

    /**
     * This publishes an event only if the same event has not been published within the last 
     * DOT_PUBSUB_QUEUE_DEDUPE_TTL_MILLIS milliseconds, e.g. 1500ms
     */
    @Override
    public boolean publish(final DotPubSubEvent event) {

        // if the same event has already been published in the last 1.5 seconds, skip it
        if(recentEvents.getIfPresent(event.hashCode())!=null){
            skipped++;
            Logger.debug(this.getClass(), ()->"Skipping:" + event);
            return true;
        }
        sent++;

        recentEvents.put(event.hashCode(), true);
        submitter.submit(()->publishAndRecordOutcome(event));


        Logger.debug(this.getClass(), ()->"sent/skipped: " + sent + "/" + skipped);
        Logger.debug(this.getClass(), ()->"active count:" + submitter.getActiveCount());
        

        return true;

    }

    /**
     * Runs the real publish on the submitter thread and records the outcome.
     *
     * {@link #publish(DotPubSubEvent)} has to return before this completes, so it always returns
     * {@code true} and the caller never learns whether the send actually worked. Before issue
     * #36803 the result of this task was discarded entirely, which meant a transport that was
     * initialized but failing every publish -- a dropped database connection, a stopped Redis
     * client -- reported no drops, stayed "initialized", and kept every health check green while
     * other nodes served stale content.
     */
    private void publishAndRecordOutcome(final DotPubSubEvent event) {
        try {
            if (!this.wrappedProvider.publish(event)) {
                recordFailure(event);
            }
        } catch (Exception e) {
            // providers are expected to swallow their own failures and return false; this is
            // the belt-and-braces path so an unexpected throw is still counted, not lost
            recordFailure(event);
            Logger.warnAndDebug(this.getClass(),
                    "Unable to publish pubsub event " + event + " : " + e.getMessage(), e);
        }
    }

    private void recordFailure(final DotPubSubEvent event) {
        failedByTopic.computeIfAbsent(topicKey(String.valueOf(event.getTopic())),
                t -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Normalizes a topic into the key {@link #failedByTopic} is indexed by.
     *
     * Writes and reads reach this map by different routes that do not agree on case.
     * {@code DotPubSubEvent.Builder.withTopic} lowercases, so anything recorded from an event is
     * already lowercase; the read side goes through {@code DotPubSubTopic.getTopic()}, which
     * returns {@code String.valueOf(getKey())} with no normalization at all. Those happen to match
     * today only because every current key is already lowercase
     * ({@code CacheTransportTopic.CACHE_TOPIC} is {@code "dotcache_topic"}).
     *
     * A future topic key with an uppercase character would make the lookup miss silently, and
     * {@link #getFailedPublishCount(String)} would report zero failures while invalidations were
     * being lost -- precisely the kind of blind spot issue #36803 exists to remove. Normalizing on
     * both sides here keeps the map self-consistent regardless of what a caller passes, rather than
     * relying on every caller knowing this convention.
     */
    private static String topicKey(final String topic) {
        return topic == null ? "null" : topic.toLowerCase();
    }

    /**
     * {@inheritDoc}
     *
     * Returns only this wrapper's own count. The wrapped provider published synchronously from
     * the submitter thread, so its {@code false} return has already been counted here -- adding
     * {@code wrappedProvider.getFailedPublishCount(topic)} would double count.
     */
    @Override
    public long getFailedPublishCount(final String topic) {
        final AtomicLong failed = failedByTopic.get(topicKey(topic));
        return failed == null ? 0 : failed.get();
    }

    @Override
    public DotPubSubProvider unsubscribe(DotPubSubTopic topic) {

        return this.wrappedProvider.unsubscribe(topic);
    }

    @Override
    public DotPubSubEvent lastEventIn() {
        return this.wrappedProvider.lastEventIn();
    }

    @Override
    public DotPubSubEvent lastEventOut() {
        return this.wrappedProvider.lastEventOut();
    }

}
