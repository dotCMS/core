package com.dotcms.dotpubsub;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.dotmarketing.util.Logger;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;


/**
 * This class wraps a pub/sub mechanism and de-dupes the messages sent through it. Once a second,
 * each topic/queue is reduced into a Set<DotPubSubEvent> before being transmitted over
 * the wire
 * 
 * @author will
 *
 */
public class QueuingPubSubWrapper implements DotPubSubProvider {

    private final ScheduledExecutorService executorService;
    private final DotPubSubProvider wrappedProvider;
    private final Set<DotPubSubEvent> outgoingMessages = new LinkedHashSet<>();
    private final Map<String,Tuple2<DotPubSubTopic, LinkedBlockingQueue<DotPubSubEvent>>> topicQueues =
                    new ConcurrentHashMap<>();

    boolean logDedupes = true;
    
    
    
    public QueuingPubSubWrapper(DotPubSubProvider providerIn) {
        this.wrappedProvider = providerIn instanceof QueuingPubSubWrapper 
                        ? ((QueuingPubSubWrapper) providerIn).wrappedProvider
                        : providerIn;

        this.executorService = Executors.newSingleThreadScheduledExecutor();

    }

    public QueuingPubSubWrapper() {
        this(DotPubSubProviderLocator.provider.get());
    }


    @Override
    public DotPubSubProvider subscribe(final DotPubSubTopic topic) {
        topicQueues.putIfAbsent(topic.getKey().toString(), Tuple.of( topic, new LinkedBlockingQueue<DotPubSubEvent>()));
        return this.wrappedProvider.subscribe(topic);
    }

    @Override
    public DotPubSubProvider start() {
        this.executorService.scheduleAtFixedRate(this::publishQueue, 5, 1, TimeUnit.SECONDS);
        this.wrappedProvider.start();
        return this;
    }

    @Override
    public void stop() {
        this.wrappedProvider.stop();
        this.executorService.shutdown();
    }


    /**
     * this method is run every second in a loop. It takes the entries in the topic queues and drains
     * them into a set to de-dupe them. It then publishes all the outgoing messages in the set
     * 
     * @return
     */
    private boolean publishQueue() {
        Logger.debug(this.getClass(), ()-> "Running QueuingPubSubWrapper.publishQueue");
        for (Map.Entry<String,Tuple2<DotPubSubTopic, LinkedBlockingQueue<DotPubSubEvent>>> topicTuple : topicQueues.entrySet()) {
            final DotPubSubTopic topic = topicTuple.getValue()._1;
            final LinkedBlockingQueue<DotPubSubEvent> topicQueue =topicTuple.getValue()._2;
            
            final int drainedMessages = topicQueue.drainTo(outgoingMessages);
            
            if(this.logDedupes && drainedMessages > 0) {
                final int dedupedMessages = outgoingMessages.size() ;
                if(drainedMessages>dedupedMessages) {
                    Logger.info(this.getClass(), ()-> "Topic: " + topic.getKey() + " = " + drainedMessages + " total / "+ dedupedMessages + " unique sent");
                }
            }
            
            for (DotPubSubEvent event : outgoingMessages) {
                this.wrappedProvider.publish(event);
            }
            outgoingMessages.clear();
        }

        return true;
    }


    /**
     * Publishing an event only adds the DotPubSubEvent to the queue, which will be processed async once every seccond
     */
    @Override
    public boolean publish(final DotPubSubEvent event) {
        
        LinkedBlockingQueue<DotPubSubEvent> topicQueue = topicQueues.get(event.getTopic())._2;
        Try.run(() -> topicQueue.add(event)).onFailure(e->Logger.warn(QueuingPubSubWrapper.class,e.getMessage(),e));
        return true;

    }

    @Override
    public DotPubSubProvider unsubscribe(DotPubSubTopic topic) {
        topicQueues.remove(topic);
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
