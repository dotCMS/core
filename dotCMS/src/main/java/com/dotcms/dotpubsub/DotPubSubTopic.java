package com.dotcms.dotpubsub;

import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.system.event.local.model.KeyFilterable;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;

public interface DotPubSubTopic extends EventSubscriber<DotPubSubEvent>, KeyFilterable {


    default long messagesSent() {
        return -1;
    }

    default long bytesSent() {
        return -1;
    }


    default long messagesReceived() {
        return -1;
    }


    default long bytesReceived() {
        return -1;
    }

    /**
     * Implement this to increment the counters
     * 
     * @param event
     */
    default void incrementSentCounters(DotPubSubEvent event) {


    }

    /**
     * Implement this to increment the counters
     * 
     * @param event
     */
    default void incrementReceivedCounters(DotPubSubEvent event) {


    }



    /**
     * Override to respond to an incoming DotPubSubEvent.
     */
    @Override
    default void notify(DotPubSubEvent event) {
        incrementReceivedCounters(event);
        Logger.info(this.getClass(), "got event:" + event);

    }



    


}
