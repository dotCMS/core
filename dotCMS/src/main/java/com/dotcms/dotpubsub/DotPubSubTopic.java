package com.dotcms.dotpubsub;

import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.system.event.local.model.KeyFilterable;
import com.dotmarketing.util.Logger;

public interface DotPubSubTopic extends EventSubscriber<DotPubSubEvent>, KeyFilterable {


    long messagesSent();

    long bytesSent();

    long messagesRecieved();

    long bytesRecieved();

    /**
     * Should this topic ignore messages sent by my own server
     * 
     * @return
     */
    default boolean ignoreMyOwnMessages() {
        return true;
    }

    /**
     * Override to respond to an incoming DotPubSubEvent.
     */
    @Override
    default void notify(DotPubSubEvent event) {
        Logger.info(this.getClass(), "got event:" + event);

    }

    /**
     * Implement this to increment the counters
     * @param event
     */
    void incrementSentCounters(DotPubSubEvent event);

    /**
     * Implement this to increment the counters
     * @param event
     */
    void incrementRecievedCounters(DotPubSubEvent event);



}
