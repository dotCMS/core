package com.dotcms.dotpubsub;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.jetbrains.annotations.NotNull;

/**
 * This class wraps a pub/sub mechanism and de-dupes the messages sent through it. Once a second,
 * each topic/queue is reduced into a Set<DotPubSubEvent> before being transmitted over the wire
 *
 * @author will
 *
 */
public class QueuingPubSubWrapper implements DotPubSubProvider {

    private ScheduledExecutorService executorService;
    private final DotPubSubProvider wrappedProvider;
    private final Map<String, Tuple2<DotPubSubTopic, LinkedBlockingQueue<DotPubSubEvent>>> topicQueues =
            new ConcurrentHashMap<>();

    final boolean logDedupes;
    final ObjectPool<Collection<DotPubSubEvent>> pool;
    private DotSubmitter submitter;

    public QueuingPubSubWrapper(DotPubSubProvider providerIn) {
        this.wrappedProvider =
                providerIn instanceof QueuingPubSubWrapper
                        ? ((QueuingPubSubWrapper) providerIn).wrappedProvider
                        : providerIn;

        this.executorService = Executors.newSingleThreadScheduledExecutor();

        this.logDedupes = Config.getBooleanProperty("DOT_PUBSUB_QUEUE_DEDUPE_LOG", false);

        PooledObjectFactory<Collection<DotPubSubEvent>> factory = new BasePooledObjectFactory<Collection<DotPubSubEvent>>(){

            @Override
            public Collection<DotPubSubEvent> create() {
                final Collection<DotPubSubEvent> rawCollection =
                        Config.getBooleanProperty("DOT_PUBSUB_QUEUE_DEDUPE", true) ?
                                new LinkedHashSet<>() : new ArrayList<>();
                return new CollectionWrapper(rawCollection);
            }

            @Override
            public PooledObject<Collection<DotPubSubEvent>> wrap(
                    Collection<DotPubSubEvent> outgoingMessages) {
                return new DefaultPooledObject<>(outgoingMessages);
            }
        };

        GenericObjectPool genericObjectPool = new GenericObjectPool<>(factory);
        genericObjectPool.setMaxTotal(-1);
        genericObjectPool.setMinIdle(1);
        genericObjectPool.setMaxIdle(1);

        pool = genericObjectPool;

        submitter = DotConcurrentFactory.getInstance().getSubmitter("QueuingPubSubWrapperSubmitter",
        new DotConcurrentFactory.SubmitterConfigBuilder()
                        .poolSize(Config.getIntProperty("MIN_NUMBER_THREAD_TO_PUBLISHER_WRAPPER", 10))
                        .maxPoolSize(Config.getIntProperty("MAX_NUMBER_THREAD_TO_PUBLISHER_WRAPPER", 40))
                        .queueCapacity(Config.getIntProperty("QUEUE_CAPACITY_TO_PUBLISHER_WRAPPER", Integer.MAX_VALUE))
                        .build()
        );
    }

    public QueuingPubSubWrapper() {
        this(DotPubSubProviderLocator.provider.get());
    }

    private final ScheduledExecutorService executorService() {
        if (this.executorService == null || this.executorService.isShutdown() || this.executorService.isTerminated()) {
            executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(this::publishQueue, 5, 1, TimeUnit.SECONDS);
        }
        return executorService;

    }

    @Override
    public DotPubSubProvider subscribe(final DotPubSubTopic topic) {
        topicQueues.putIfAbsent(topic.getKey().toString(), Tuple.of(topic, new LinkedBlockingQueue<DotPubSubEvent>()));
        return this.wrappedProvider.subscribe(topic);
    }

    @Override
    public DotPubSubProvider start() {
        executorService().scheduleAtFixedRate(this::publishQueue, 5, 1, TimeUnit.SECONDS);
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

        Logger.debug(this.getClass(), () -> "Running QueuingPubSubWrapper.publishQueue " + pool.getNumActive() + " " + pool.getNumIdle() + " " + submitter.getTaskCount() + " " + submitter.getActiveCount());

        try {
            for (Map.Entry<String, Tuple2<DotPubSubTopic, LinkedBlockingQueue<DotPubSubEvent>>> topicTuple : topicQueues
                    .entrySet()) {

                final DotPubSubTopic topic = topicTuple.getValue()._1;
                final LinkedBlockingQueue<DotPubSubEvent> topicQueue = topicTuple.getValue()._2;

                if (!topicQueue.isEmpty()) {
                    final Collection<DotPubSubEvent> outgoingMessages = pool.borrowObject();
                    final int drainedMessages = topicQueue.drainTo(outgoingMessages);

                    if (this.logDedupes && drainedMessages > 0) {
                        final int dedupedMessages = outgoingMessages.size();
                        if (drainedMessages > dedupedMessages) {
                            Logger.info(this.getClass(),
                                    () -> "Topic: " + topic.getKey() + " = " + drainedMessages
                                            + " total / " + dedupedMessages + " unique sent");
                        }
                    }
                    Logger.debug(this.getClass(), () -> "Running QueuingPubSubWrapper.publishQueue after drainTo " + topicQueue.size());

                    submitter.submit(new DotPubSubEventPublisher(outgoingMessages));
                }
            }
        } catch (Exception e) {
            Logger.warn(this.getClass(), () -> "Running QueuingPubSubWrapper.publishQueue ERROR: " + e.getMessage());
        }

        return true;
    }

    /**
     * Publishing an event only adds the DotPubSubEvent to the queue, which will be processed async once
     * every seccond
     */
    @Override
    public boolean publish(final DotPubSubEvent event) {

        LinkedBlockingQueue<DotPubSubEvent> topicQueue = topicQueues.get(event.getTopic())._2;
        Try.run(() -> topicQueue.add(event)).onFailure(e -> Logger.warn(QueuingPubSubWrapper.class, e.getMessage(), e));
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

    private class DotPubSubEventPublisher implements Runnable {

        final  Collection<DotPubSubEvent> outgoingMessages;

        DotPubSubEventPublisher(final Collection<DotPubSubEvent> outgoingMessages) {
            this.outgoingMessages = outgoingMessages;
        }

        @Override
        public void run() {
            try {
                Logger.debug(this.getClass(), () -> "Running QueuingPubSubWrapper.publishQueue Thread" + Thread.currentThread().getName() + " " + outgoingMessages.size());

                for (Iterator<DotPubSubEvent> iterator = outgoingMessages.iterator();
                        iterator.hasNext(); ) {
                    wrappedProvider.publish(iterator.next());
                    iterator.remove();
                }

                outgoingMessages.clear();
            } finally {
                if (outgoingMessages != null) {
                    try {
                        pool.returnObject(outgoingMessages);
                    } catch (Exception e) {
                        Logger.error(QueuingPubSubWrapper.class, e.getMessage());

                        try {
                            pool.invalidateObject(outgoingMessages);
                        } catch (Exception exception) {
                            Logger.error(QueuingPubSubWrapper.class, e.getMessage());
                        }
                    }
                }
            }

            Logger.debug(this.getClass(), () -> "Running QueuingPubSubWrapper.publishQueue Finish Thread" + Thread.currentThread().getName());

        }
    }

    private static class CollectionWrapper<E> implements Collection<E> {

        private final Collection<E> rootCollection;

        public CollectionWrapper(final Collection<E> rootCollection) {
            this.rootCollection = rootCollection;
        }

        @Override
        public Iterator<E> iterator() {
            return rootCollection.iterator();
        }

        @NotNull
        @Override
        public Object[] toArray() {
            return rootCollection.toArray();
        }

        @NotNull
        @Override
        public <T> T[] toArray(T[] array) {
            return rootCollection.toArray(array);
        }

        @Override
        public boolean add(E element) {
            return rootCollection.add(element);
        }

        @Override
        public boolean remove(Object element) {
            return rootCollection.remove(element);
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return rootCollection.containsAll(collection);
        }

        @Override
        public boolean addAll(Collection<? extends E> collection) {
            return rootCollection.addAll(collection);
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            return rootCollection.removeAll(collection);
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            return rootCollection.retainAll(collection);
        }

        @Override
        public void clear() {
            rootCollection.clear();
        }

        @Override
        public int size() {
            return rootCollection.size();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean contains(final Object element) {
            return rootCollection.contains(element);
        }

        @Override
        public boolean equals(final Object another){
            return this == another;
        }
    }

}
