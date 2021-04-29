package com.dotcms.dotpubsub;

public interface DotPubSubProvider {

    
    DotPubSubProvider subscribe(DotPubSubTopic topic);

    DotPubSubProvider start();

    void stop();
    
    boolean publish(DotPubSubTopic topic, DotPubSubEvent event);
    

    
    
}
