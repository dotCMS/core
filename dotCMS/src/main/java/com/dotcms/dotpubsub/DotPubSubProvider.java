package com.dotcms.dotpubsub;

public interface DotPubSubProvider {

    /**
     * Pass a DotPubSubTopic in listen/respond to events that match your topics key
     * 
     * @param topic
     * @return
     */
    DotPubSubProvider subscribe(DotPubSubTopic topic);

    /**
     * Starts the Provider (if needed)
     * @return
     */
    DotPubSubProvider start();
    
    /**
     * Shuts down the Provider (if needed)
     * @return
     */
    void stop();
    
    
    /**
     * Publish an event on a given topic
     * @param topic
     * @param event
     * @return
     */
    boolean publish(DotPubSubTopic topic, DotPubSubEvent event);
    

    
    
}
