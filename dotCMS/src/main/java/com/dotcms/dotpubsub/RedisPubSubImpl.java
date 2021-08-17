package com.dotcms.dotpubsub;

import com.dotcms.cache.lettuce.RedisClient;
import com.dotcms.cache.lettuce.RedisClientFactory;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis pub and sub
 * @author jsanca
 */
public class RedisPubSubImpl implements DotPubSubProvider {

    private final AtomicBoolean start = new AtomicBoolean(false);
    private final RedisClient<String, Object> redisClient = RedisClientFactory.getClient("pubsub");

    @VisibleForTesting
    private static DotPubSubEvent lastEventIn, lastEventOut;

    @Override
    public DotPubSubProvider subscribe(final DotPubSubTopic topic) {

        Logger.debug(this, ()-> "Subscribing topic: " + topic.getTopic());
        this.start();
        this.redisClient.subscribe(message-> {

            final DotPubSubEvent event = (DotPubSubEvent) message;
            lastEventIn = event;
            topic.notify(event);
        }, topic.getTopic());
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
    public boolean publish(final DotPubSubEvent event) {

        if (this.start.get()) {

            this.redisClient.publishMessage(event, event.getTopic());
            lastEventOut = event;
            return true;
        } else {

            Logger.debug(this, ()->"(PubSub stopped) Message Filtered: " + event);
        }

        return false;
    }

    @Override
    public DotPubSubProvider unsubscribe(final DotPubSubTopic topic) {

        Logger.debug(this, ()-> "Unsubscribing topic: " + topic.getTopic());
        this.redisClient.unsubscribe(topic.getTopic());
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
