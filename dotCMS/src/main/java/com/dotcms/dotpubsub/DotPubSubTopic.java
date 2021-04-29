package com.dotcms.dotpubsub;

import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.system.event.local.model.KeyFilterable;
import com.dotmarketing.util.Logger;

public interface DotPubSubTopic extends EventSubscriber<DotPubSubEvent>, KeyFilterable{

    
    long messagesSent() ;
    long bytesSent();
    long messagesRecieved();
    long bytesRecieved();

    default boolean ignoreMyOwnMessages() {
        return true;
        
    }
    
    
    @Override
    default void notify(DotPubSubEvent event) {
        Logger.info(this.getClass(), "got event:" +event);
        
    }
    
    void incrementSentCounters(DotPubSubEvent event);
    
    void incrementRecievedCounters(DotPubSubEvent event);


    
    
}
