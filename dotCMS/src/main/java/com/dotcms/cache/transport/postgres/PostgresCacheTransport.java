package com.dotcms.cache.transport.postgres;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import com.dotcms.cache.transport.postgres.CachePubSubTopic.CacheEventType;
import com.dotcms.cluster.bean.Server;
import com.dotcms.dotpubsub.DotPubSubEvent;
import com.dotcms.dotpubsub.PostgresPubSubImpl;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.cache.transport.CacheTransportException;
import com.dotmarketing.util.Logger;
import jersey.repackaged.com.google.common.collect.ImmutableMap;

public class PostgresCacheTransport implements CacheTransport {

    final PostgresPubSubImpl pubsub;

    final CachePubSubTopic topic;
    final ServerResponseTopic serverResponseTopic;
    final AtomicBoolean initialized = new AtomicBoolean(false);


    @Override
    public boolean requiresAutowiring() {
        return false;
    }



    public PostgresCacheTransport() {
        this.pubsub = new PostgresPubSubImpl();
        this.topic = new CachePubSubTopic();
        this.serverResponseTopic = new ServerResponseTopic();
        Logger.debug(this.getClass(), "PostgresCacheTransport");

    }



    @Override
    public void init(final Server localServer) throws CacheTransportException {
        
        Logger.info(this.getClass(), "initing PostgresCacheTransport");
        this.pubsub.subscribe(topic);
        this.pubsub.subscribe(serverResponseTopic);
        this.pubsub.start();
        this.initialized.set(true);
    }



    @Override
    public void send(final String message) throws CacheTransportException {
        if(!this.initialized.get()) {
            return;
        }

        final DotPubSubEvent event =
                        new DotPubSubEvent.Builder().withType(CacheEventType.INVAL.name()).withMessage(message).build();

        this.pubsub.publish(this.topic, event);


    }



    @Override
    public void testCluster() throws CacheTransportException {


        Logger.info(this.getClass(), "Querying servers in cluster: " +ClusterFactory.getClusterId() + "..." );
        Set<String> servers = new HashSet<>();
        servers.addAll(validateCacheInCluster(2).keySet());
        servers.add(pubsub.serverId + "(me)");
        

        
        Logger.info(this.getClass()," Found "+ servers.size() + " servers in cluster: " + String.join( ", " , servers.toArray(new String[servers.size()])) );
    }


    
    
    


    @Override
    public Map<String, Boolean> validateCacheInCluster(final String dateInMillis, final int numberServers, final int maxWaitSeconds)
                    throws CacheTransportException {

        final DotPubSubEvent event = new DotPubSubEvent.Builder()
                        .withType(CachePubSubTopic.CacheEventType.CLUSTER_REQ.name()).build();
        
        serverResponseTopic.resetMap();
        
        
        this.pubsub.publish(this.topic, event);
        
        final long waitUntil = System.currentTimeMillis() + (1000*maxWaitSeconds);
        while(System.currentTimeMillis()< waitUntil) {
            if(serverResponseTopic.size()>=numberServers) {
                return serverResponseTopic.readResponses();
            }
            
            
        }
        
        
        return ImmutableMap.of();
    }



    @Override
    public void shutdown() throws CacheTransportException {
        Logger.debug(this.getClass(), "shutdown()");
        this.pubsub.stop();
    }



    @Override
    public boolean isInitialized() {
        Logger.debug(this.getClass(), "isInitialized");
        return initialized.get();
    }



    @Override
    public boolean shouldReinit() {

        return !initialized.get();
    }



    @Override
    public CacheTransportInfo getInfo() {
        

        return new CacheTransportInfo(){
            @Override
            public String getClusterName() {
                return ClusterFactory.getClusterId();
            }

            @Override
            public String getAddress() {
                return "n/a";
            }

            @Override
            public int getPort() {
                return -1;
            }


            @Override
            public boolean isOpen() {
                return true;
            }

            @Override
            public int getNumberOfNodes() {
                return serverResponseTopic.size()+1;
            }


            @Override
            public long getReceivedBytes() {
                return topic.bytesRecieved();
            }

            @Override
            public long getReceivedMessages() {
                return topic.messagesRecieved();
            }

            @Override
            public long getSentBytes() {
                return topic.bytesSent();
            }

            @Override
            public long getSentMessages() {
                return topic.messagesSent();
            }
        };
    }


}
