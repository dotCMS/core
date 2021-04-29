package com.dotcms.cache.transport.postgres;

import java.util.HashMap;
import java.util.Map;
import com.dotcms.dotpubsub.DotPubSubEvent;
import com.dotcms.dotpubsub.DotPubSubTopic;
import com.dotmarketing.business.APILocator;
import com.google.common.collect.ImmutableMap;

public class ServerResponseTopic implements DotPubSubTopic {

    private final Map<String, Boolean> serverResponses = new HashMap<>();

    final static String KEY = "serverresponse";
    
    static long bytesSent, bytesRecieved, messagesSent, messagesRecieved = 0;

    
    public ServerResponseTopic() {
        serverResponses.clear();

    }


    public void resetMap() {
        serverResponses.clear();
        //serverResponses.put(APILocator.getServerAPI().readServerId(), true);
    }

    public Map<String, Boolean> readResponses() {
        return ImmutableMap.copyOf(serverResponses);
    }

    public int size() {
        return serverResponses.size(); 
    }
    
    @Override
    public Comparable getKey() {
        return KEY;
    }

    @Override
    public void notify(DotPubSubEvent event) {
        serverResponses.put(event.getOrigin(), true);


    }
    @Override
    public long messagesSent() {

        return new Long(messagesSent);
    }

    @Override
    public long bytesSent() {
        return bytesSent;
    }

    @Override
    public long messagesRecieved() {
        return messagesRecieved;
    }

    @Override
    public long bytesRecieved() {
        return bytesRecieved;
    }
    
    @Override
    public void incrementSentCounters(final DotPubSubEvent event) {
        bytesSent+=event.toString().getBytes().length;
        messagesSent++;
    }
    
    @Override
    public void incrementRecievedCounters (final DotPubSubEvent event) {
        bytesRecieved+=event.toString().getBytes().length;
        messagesRecieved++;
    }
    


}
