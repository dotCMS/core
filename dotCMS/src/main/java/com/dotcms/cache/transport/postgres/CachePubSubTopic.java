package com.dotcms.cache.transport.postgres;

import java.io.Serializable;
import com.dotcms.dotpubsub.DotPubSubEvent;
import com.dotcms.dotpubsub.DotPubSubProviderLocator;
import com.dotcms.dotpubsub.DotPubSubTopic;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import io.vavr.Lazy;

public class CachePubSubTopic implements DotPubSubTopic {

    final static String CACHE_TOPIC = "dotcache_topic";
    
    static long bytesSent, bytesRecieved, messagesSent, messagesRecieved = 0;

    
    
    final Lazy<String[]> serverIds = Lazy.of(() -> {
        final String serverId = APILocator.getServerAPI().readServerId();
        final String shorty = APILocator.getShortyAPI().shortify(serverId);
        return new String[] {shorty,serverId};
        
    });

    
    @VisibleForTesting
    public enum CacheEventType {
        INVAL, PING, PONG, CLUSTER_REQ, CLUSTER_RES,UKN;

        static public CacheEventType from(Serializable name) {
            for(CacheEventType type : CacheEventType.values()) {
                if(type.name().equals(name)) {
                    return type;
                }
            }
            return UKN;
            
        }

    }
    
    

    @Override
    public Comparable getKey() {
        return CACHE_TOPIC;
    }

    @Override
    public void notify(DotPubSubEvent event) {
       
        // if this is my event
        for(String serverId: serverIds.get()) {
            if(serverId.equals(event.getOrigin())){
                Logger.debug(getClass(), ()-> "pub/sub event sent from me, ignoring:" + event );
                return;
            }
        }
        Logger.debug(getClass(), ()-> "got pub/sub event:" + event );
        final String message = (String) event.getMessage();
        final CacheEventType type = CacheEventType.from(event.getType());


        switch (type) {
            case INVAL:
                Logger.info(getClass(), ()-> "got cache invalidation event:" + event );
                CacheLocator.getCacheAdministrator().invalidateCacheMesageFromCluster(message);
                return;
                
            case PING:
                Logger.info(this.getClass(), ()-> "Got PING from server:" + event.getOrigin() + ". sending PONG");
                DotPubSubProviderLocator.provider.get().publish(this,new DotPubSubEvent.Builder(event).withType(CacheEventType.PONG.name()).build());
                return;
                
            case PONG:
                Logger.info(this.getClass(), ()-> "Got PONG from server:" + event.getOrigin() + ".");
                return;
                
            case CLUSTER_REQ:
                

                Logger.info(this.getClass(), ()-> "Got CLUSTER_REQ from server:" + event.getOrigin() + ". sending response");
                DotPubSubProviderLocator.provider.get().publish(new ServerResponseTopic(),
                                new DotPubSubEvent.Builder(event)
                                .withPayload(ImmutableMap.of(APILocator.getServerAPI().readServerId(),Boolean.TRUE))
                                .withType(CacheEventType.CLUSTER_RES.name()).build());
                return;
                
            default:
                
        }

    }

    @Override
    public long messagesSent() {

        return messagesSent;
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
