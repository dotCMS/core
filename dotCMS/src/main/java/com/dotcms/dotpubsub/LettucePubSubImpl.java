package com.dotcms.dotpubsub;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.dotcms.cache.lettuce.LettuceClient;
import com.dotcms.cache.lettuce.MasterReplicaLettuceClient;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.lettuce.core.Consumer;
import io.lettuce.core.RedisBusyException;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XAddArgs;
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.XReadArgs.StreamOffset;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.vavr.control.Try;
public class LettucePubSubImpl implements DotPubSubProvider {

    private final boolean testing;
    private final String clusterId;
    private final String serverId;
    private final LettuceClient<String, String> lettuce;
    private final Map<Comparable<String>, DotPubSubTopic> topicMap = new ConcurrentHashMap<>();

    public LettucePubSubImpl(String serverId, String clusterId, boolean testing) {
        super();
        this.testing = testing;
        this.serverId = serverId;
        this.clusterId = clusterId;
        this.lettuce = new MasterReplicaLettuceClient<>();
    }


    public LettucePubSubImpl() {
        this(APILocator.getServerAPI().readServerId(), ClusterFactory.getClusterId(), false);

    }

    private final long maxStreamSize = Config.getLongProperty("REDIS_MAX_PUBSUB_STREAM_SIZE", 100000);

    private StreamListener listener = null;

    private boolean startListening() {
        if (this.listener != null) {
            Logger.info(LettucePubSubImpl.class, "Restarting our listener");
            this.listener.stop();
        }

        this.listener = new StreamListener(topicMap, serverId, clusterId);

        DotConcurrentFactory.getInstance().getSingleSubmitter(LettucePubSubImpl.class.getSimpleName())
                        .submit(this.listener);

        return true;

    }

    public class StreamListener implements Runnable {

        private boolean running = true;
        private final Map<Comparable<String>, DotPubSubTopic> topics;
        private final String serverId, clusterId;

        public StreamListener(Map<Comparable<String>, DotPubSubTopic> topics, String serverId, String clusterId) {
            super();
            this.topics = topics;
            this.serverId = serverId;
            this.clusterId = clusterId;
        }

        public void stop() {
            this.running = false;
        }

        private boolean buildTopicStreams() {


            for (final DotPubSubTopic topic : topics.values()) {

                final String redisTopic = redisTopic(topic);

                try {
                    RedisAsyncCommands<String, String> asyncCommands = lettuce.get().async();
                    Logger.info(LettucePubSubImpl.class, "Creating Redis Stream : " + redisTopic);
                    asyncCommands.xgroupCreate(StreamOffset.latest(redisTopic), serverId,
                                    XGroupCreateArgs.Builder.mkstream(true));

                } catch (RedisBusyException redisBusyException) {
                    Logger.info(LettucePubSubImpl.class, "Redis Stream already exists: " + redisTopic);
                } catch (Exception e) {
                    Logger.warnAndDebug(LettucePubSubImpl.class, e);
                    throw new DotRuntimeException(e);
                }
            }
            return true;

        }

        @Override
        public void run() {

            if (topics.isEmpty()) {
                Logger.info(LettucePubSubImpl.class, "No topics, nothing to listen for");
                return;
            }

            buildTopicStreams();

            while (running) {

                for (final DotPubSubTopic topic : topics.values()) {
                    eventsIn(topic);
                    Try.run(() -> Thread.sleep(100));
                }

            }

        }

        void eventsIn(DotPubSubTopic topic) {
            final String redisTopic = redisTopic(topic);
            List<StreamMessage<String, String>> messages = lettuce.get().sync().xreadgroup(
                            Consumer.from(serverId, serverId), XReadArgs.StreamOffset.lastConsumed(redisTopic));
            // ACK Attack
            messages.forEach(m -> lettuce.get().async().xack(redisTopic, serverId, m.getId()));

            for (final StreamMessage<String, String> messageIn : messages) {
                List<DotPubSubEvent> bodyEvents = Try.of(() -> messageIn.getBody().entrySet().stream()
                                .map(e -> new DotPubSubEvent(e.getValue()))
                                .filter(e -> !serverId.startsWith(e.getOrigin())).collect(Collectors.toList()))
                                .onFailure(e -> Logger.warnAndDebug(LettucePubSubImpl.class, e))
                                .getOrElse(ImmutableList.of());

                for (DotPubSubEvent event : bodyEvents) {
                    if (testing) {
                        Logger.info(this.getClass(), "server:" + serverId + " got message:" + event);
                    }

                    DotPubSubTopic pubSubTopic = topicMap.get(redisTopic);
                    
                    if (topic == null) {
                        continue;
                    }

                    pubSubTopic.incrementReceivedCounters(event);
                    pubSubTopic.notify(event);

                }

            }
        }

    }

    @VisibleForTesting
    Map<Comparable<String>, DotPubSubTopic> getTopics() {
        return ImmutableMap.copyOf(topicMap);
    }

    final String redisTopic(String topic) {
        
        return topic.startsWith(clusterId + "-") ? topic : clusterId + "-" + topic;
    }

    final String redisTopic(DotPubSubTopic topic) {
        return redisTopic(topic.getTopic());
    }

    @Override
    public DotPubSubProvider subscribe(final DotPubSubTopic topic) {

        final String topicKey = redisTopic(topic);
        
        
        
        if (topicMap.containsKey(topicKey)) {
            return this;
        }

        topicMap.put(topicKey, topic);

        startListening();

        return this;
    }

    @Override
    public DotPubSubProvider start() {
        startListening();
        return this;
    }

    @Override
    public void stop() {
        if (this.listener != null) {
            Logger.info(LettucePubSubImpl.class, "Stopping our listener");
            this.listener.stop();
        }

    }

    @Override
    public boolean publish(final DotPubSubEvent eventIn) {
        final DotPubSubEvent eventOut = new DotPubSubEvent.Builder(eventIn).withOrigin(serverId).build();

        lettuce.get().async().xadd(redisTopic(eventOut.getTopic()), XAddArgs.Builder.maxlen(maxStreamSize), "e",
                        eventOut.toString());

        return true;
    }

    @Override
    public DotPubSubProvider unsubscribe(DotPubSubTopic topic) {
        final String redisTopic = redisTopic(topic);
        topicMap.remove(topic.getTopic());
        topicMap.remove(redisTopic);
        startListening();
        return this;
    }

}
