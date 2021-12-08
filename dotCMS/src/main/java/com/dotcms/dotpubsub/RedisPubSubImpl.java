package com.dotcms.dotpubsub;

import com.dotcms.cache.lettuce.RedisClient;
import com.dotcms.cache.lettuce.RedisClientFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.google.common.annotations.VisibleForTesting;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis pub and sub
 * This implementation is based on the redis Streaming Pub/Sub implementation
 *
 * The topic is used to subscribe to a channel, and it the Provider is started, can publish
 * a message to a channel (aka topic)
 * @author jsanca
 */
public class RedisPubSubImpl implements DotPubSubProvider {

    public final String serverId;
    private final AtomicBoolean start = new AtomicBoolean(false);
    private final RedisClient<String, Object> redisClient = RedisClientFactory.getClient("pubsub");

    @VisibleForTesting
    private static DotPubSubEvent lastEventIn, lastEventOut;

    public RedisPubSubImpl() {
        this(APILocator.getServerAPI().readServerId());

    }

    public RedisPubSubImpl(final String serverId) {
        this.serverId = StringUtils.shortify(serverId, 10);
    }

    @Override
    public DotPubSubProvider subscribe(final DotPubSubTopic topic) {

        Logger.debug(this, ()-> "Subscribing topic: " + topic.getTopic());
        this.start();
        this.redisClient.subscribe(message-> {

            final DotPubSubEvent event = (DotPubSubEvent) message;
            lastEventIn = event;
            topic.incrementReceivedCounters(event);
            topic.notify(event);
        }, topic.getTopic(), topic.getInstanceId());

        return this;
    }

    @Override
    public DotPubSubProvider start() {

        Logger.debug(this, ()->"PubSub has been started...");
        this.start.set(true);
        return this;
    }

    @Override
    public void stop() {

        Logger.debug(this, ()->"PubSub has been stopped...");
        this.start.set(false);
    }

    @Override
    public boolean publish(final DotPubSubEvent eventIn) {

        if (this.start.get()) {

            final DotPubSubEvent eventOut = new DotPubSubEvent.Builder(eventIn).withOrigin(serverId).build();
            this.redisClient.publishMessage(eventOut, eventOut.getTopic());
            lastEventOut = eventOut;
            return true;
        } else {

            Logger.debug(this, ()->"(PubSub stopped) Message Filtered: " + eventIn);
        }

        return false;
    }

    @Override
    public DotPubSubProvider unsubscribe(final DotPubSubTopic topic) {

        Logger.debug(this, ()-> "Unsubscribing topic: " + topic.getTopic());
        this.redisClient.unsubscribeSubscriber(topic.getInstanceId(), topic.getTopic());
        return this;
    }

    @Override
    public DotPubSubEvent lastEventIn() {
        return lastEventIn;
    }

    @Override
    public DotPubSubEvent lastEventOut() {
        return lastEventOut;
    }
}
