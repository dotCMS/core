package com.dotcms.dotpubsub;

public interface DotPubSubProvider {

    void subscribe(DotPubSubTopic topic);

    DotPubSubProvider init();

    void shutdown();
    
    boolean publish(DotPubSubEvent event);
    

}
