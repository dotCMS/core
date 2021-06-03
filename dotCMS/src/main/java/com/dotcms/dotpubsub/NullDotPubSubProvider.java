package com.dotcms.dotpubsub;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.google.common.annotations.VisibleForTesting;

public class NullDotPubSubProvider implements DotPubSubProvider {

    private Map<Comparable<String>, DotPubSubTopic> topicMap = new ConcurrentHashMap<>();

    @VisibleForTesting
    public DotPubSubEvent lastEvent;

    @Override
    public DotPubSubProvider subscribe(DotPubSubTopic topic) {
        topicMap.put(topic.getKey().toString(), topic);
        return this;
    }

    @Override
    public DotPubSubProvider start() {

        return this;
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean publish(DotPubSubEvent event) {
        this.lastEvent = event;
        return true;
    }

    @Override
    public DotPubSubProvider unsubscribe(DotPubSubTopic topic) {
        topicMap.remove(topic.getKey().toString());
        return this;
    }

    @Override
    public DotPubSubEvent lastEventIn() {

        return this.lastEvent;
    }

    @Override
    public DotPubSubEvent lastEventOut() {
        return this.lastEvent;
    }

}
