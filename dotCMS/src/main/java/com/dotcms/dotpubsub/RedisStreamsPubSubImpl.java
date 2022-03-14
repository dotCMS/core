package com.dotcms.dotpubsub;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.dotcms.cache.lettuce.RedisClient;
import com.dotcms.cache.lettuce.RedisClientFactory;
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
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.vavr.control.Try;

/**
 * Redis groups Pub and sub
 * @author jsanca
 */
public class RedisStreamsPubSubImpl implements DotPubSubProvider {

    private final boolean testing;
    private final String clusterId;
    private final String serverId;
    private final Map<Comparable<String>, DotPubSubTopic> topicMap = new ConcurrentHashMap<>();

    private final RedisClient<String, Object> redisClient = RedisClientFactory.getClient("pubsub");
    
    
    
    private final long PUBSUB_THREAD_PAUSE_MS=Config.getLongProperty("PUBSUB_THREAD_PAUSE_MS", 200);
    public RedisStreamsPubSubImpl(String serverId, String clusterId, boolean testing) {
        super();
        this.testing = testing;
        this.serverId = serverId;
        this.clusterId = clusterId;
    }
    
    
    
    
    StatefulRedisConnection<String, String> getConn(){
        
        return (StatefulRedisConnection<String, String>) redisClient.getConnection();
        
    }


    public RedisStreamsPubSubImpl() {
        this(APILocator.getServerAPI().readServerId(), ClusterFactory.getClusterId(), false);

    }

    private final long maxStreamSize = Config.getLongProperty("REDIS_MAX_PUBSUB_STREAM_SIZE", 100000);

    private StreamListener listener = null;

    private boolean startListening() {
        if (this.listener != null) {
            Logger.info(RedisStreamsPubSubImpl.class, "Restarting our listener");
            this.listener.stop();
        }
        if (topicMap.isEmpty()) {
            Logger.info(RedisStreamsPubSubImpl.class, "No topics, nothing to listen for");
            return false;
        }
        this.listener = new StreamListener(topicMap, serverId);

        DotConcurrentFactory.getInstance().getSingleSubmitter(RedisStreamsPubSubImpl.class.getSimpleName())
                        .submit(this.listener);

        return true;

    }

    public class StreamListener implements Runnable {

        private boolean running = true;
        private final Map<Comparable<String>, DotPubSubTopic> topics;
        private final String serverId;

        public StreamListener(Map<Comparable<String>, DotPubSubTopic> topics, String serverId) {
            super();
            this.topics = topics;
            this.serverId = serverId;
        }

        public void stop() {
            this.running = false;
        }

        private boolean buildTopicStreams() {


            for (final DotPubSubTopic topic : topics.values()) {

                final String redisTopic = redisTopic(topic);

                try (StatefulRedisConnection<String, String> conn = getConn()){
                    
                    RedisCommands<String, String> commands = conn.sync();

                    
                    commands.xgroupCreate( XReadArgs.StreamOffset.from(redisTopic, "0-0"), serverId ,
                                    XGroupCreateArgs.Builder.mkstream(true) );
                    Logger.info(RedisStreamsPubSubImpl.class, "Creating/Connecting to Redis Stream : " + redisTopic);

 

                } catch (RedisBusyException redisBusyException) {
                    Logger.info(RedisStreamsPubSubImpl.class, "Redis Stream already exists: " + redisTopic);
                } catch (Exception e) {
                    Logger.warn(RedisStreamsPubSubImpl.class, e.getMessage(), e);
                    throw new DotRuntimeException(e);
                }
            }
            return true;

        }

        @Override
        public void run() {

            buildTopicStreams();

            while (running) {

                // sub the pub
                topics.values().forEach(v -> eventsIn(v));
                Try.run(() -> Thread.sleep(PUBSUB_THREAD_PAUSE_MS));

            }

        }

        void eventsIn(DotPubSubTopic topic) {
            final String redisTopic = redisTopic(topic);

            try (StatefulRedisConnection<String, String> conn = getConn()) {
                RedisCommands<String, String> commands = conn.sync();
                
                //we are a consumer group of 1
                final Consumer<String> consumer = Consumer.from(serverId, serverId);

                List<StreamMessage<String, String>> messages = commands.xreadgroup(consumer,
                                XReadArgs.StreamOffset.lastConsumed(redisTopic));
                // ACK Attack
                messages.forEach(m -> conn.async().xack(redisTopic, serverId, m.getId()));

                for (final StreamMessage<String, String> messageIn : messages) {
                    final List<DotPubSubEvent> bodyEvents = Try
                                    .of(() -> messageIn.getBody().entrySet().stream().map(e -> new DotPubSubEvent(e.getValue()))
                                                    .filter(e -> !serverId.startsWith(e.getOrigin()))
                                                    .collect(Collectors.toList()))
                                    .onFailure(e -> Logger.warnAndDebug(RedisStreamsPubSubImpl.class, e))
                                    .getOrElse(ImmutableList.of());

                    for (DotPubSubEvent event : bodyEvents) {
                        if (testing) {
                            Logger.info(this.getClass(), "server:" + serverId + " got message:" + event);
                        }

                        final DotPubSubTopic pubSubTopic = topicMap.get(redisTopic);

                        if (topic == null) {
                            continue;
                        }

                        pubSubTopic.incrementReceivedCounters(event);
                        pubSubTopic.notify(event);

                    }

                }
            } catch (Exception e) {
                Logger.warn(RedisStreamsPubSubImpl.class, e.getMessage(),e);
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
            Logger.info(RedisStreamsPubSubImpl.class, "Stopping our listener");
            this.listener.stop();
        }

    }

    @Override
    public boolean publish(final DotPubSubEvent eventIn) {
        final DotPubSubEvent eventOut = new DotPubSubEvent.Builder(eventIn).withOrigin(serverId).build();
        try (StatefulRedisConnection<String, String> conn = getConn()) {
            conn.async().xadd(redisTopic(eventOut.getTopic()), XAddArgs.Builder.maxlen(maxStreamSize), "e",
                            eventOut.toString());
        }
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
