package com.dotcms.dotpubsub;

public interface DotPubSubProvider {

    
    DotPubSubProvider subscribe(DotPubSubTopic topic);

    DotPubSubProvider init();

    void shutdown();
    
    boolean publish(DotPubSubTopic topic, DotPubSubEvent event);
    

    
    
}
