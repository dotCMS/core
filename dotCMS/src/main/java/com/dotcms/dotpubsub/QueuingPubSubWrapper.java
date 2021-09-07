package com.dotcms.dotpubsub;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
        submitter.submit(()->this.wrappedProvider.publish(event));
        
        
        Logger.debug(this.getClass(), ()->"sent/skipped: " + sent + "/" + skipped);
        Logger.debug(this.getClass(), ()->"active count:" + submitter.getActiveCount());
        

        return true;

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
