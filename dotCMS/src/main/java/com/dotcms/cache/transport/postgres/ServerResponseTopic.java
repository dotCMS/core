package com.dotcms.cache.transport.postgres;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import com.dotcms.dotpubsub.DotPubSubEvent;
import com.dotcms.dotpubsub.DotPubSubTopic;
import com.dotcms.enterprise.ClusterUtil;
import com.dotmarketing.business.APILocator;
import com.google.common.collect.ImmutableMap;

/**
 * This Topic collects server responses when a request for all servers is sent out
 * 
 * @author will
 *
 */
public class ServerResponseTopic implements DotPubSubTopic {

    private final Map<String, Serializable> serverResponses = new HashMap<>();

    final static String KEY = "serverresponse";
    
    static long bytesSent, bytesRecieved, messagesSent, messagesRecieved = 0;

    
    public ServerResponseTopic() {
        serverResponses.clear();

    }


    public void resetMap() {
        serverResponses.clear();
        final HashMap<String,Serializable> map = new HashMap<>();
        map.putAll(ClusterUtil.getNodeInfo());
        serverResponses.put(APILocator.getServerAPI().readServerId(), map);
    }

    public Map<String, Serializable> readResponses() {
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
        final String origin = (String) event.getPayload().get("serverId");
        serverResponses.put(origin, (Serializable) event.getPayload());


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
    public long messagesReceived() {
        return messagesRecieved;
    }

    @Override
    public long bytesReceived() {
        return bytesRecieved;
    }
    
    @Override
    public void incrementSentCounters(final DotPubSubEvent event) {
        bytesSent+=event.toString().getBytes().length;
        messagesSent++;
    }
    
    @Override
    public void incrementReceivedCounters (final DotPubSubEvent event) {
        bytesRecieved+=event.toString().getBytes().length;
        messagesRecieved++;
    }


    
    
    


}
