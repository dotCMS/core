package com.dotcms.cache.lettuce;

import com.dotmarketing.util.Logger;
import io.lettuce.core.pubsub.RedisPubSubListener;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Default listener for redis PUB/SUB
 * @author jsanca
 * @param <V>
 */
public class DotPubSubListener<K,V> implements RedisPubSubListener<K, V> {

    private final Consumer<V> messageConsumer;
    private final Set<K> channelSet;

    public DotPubSubListener(final Consumer<V> messageConsumer, final K... channels) {

        this.messageConsumer = messageConsumer;
        this.channelSet      = new HashSet<>(Arrays.asList(channels));
    }

    @Override
    public void message(K channel, V message) {

        if (this.channelSet.contains(channel)) {

            this.messageConsumer.accept(message);
        }
    }

    @Override
    public void message(K pattern, K channel, V message) {

        message(channel, message);
    }

    @Override
    public void subscribed(K channel, long count) {

        Logger.info(this, "subscribed " + channel);
    }

    @Override
    public void psubscribed(K pattern, long count) {

        Logger.info(this, "psubscribed " + pattern);
    }

    @Override
    public void unsubscribed(K channel, long count) {

        Logger.info(this, "unsubscribed " + channel);
    }

    @Override
    public void punsubscribed(K pattern, long count) {

        Logger.info(this, "punsubscribed " + pattern);
    }

}
