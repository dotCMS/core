package com.dotcms.cache.transport;

import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.business.HazelcastUtil;
import com.dotcms.cluster.business.HazelcastUtil.HazelcastInstanceType;
import com.dotcms.dotpubsub.DotPubSubEvent;
import com.dotcms.enterprise.ClusterUtil;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.ChainableCacheAdministratorImpl;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.cache.transport.CacheTransportException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.liferay.portal.struts.MultiMessageResources;
import io.vavr.control.Try;
import jersey.repackaged.com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by jasontesser on 3/28/17.
 */
public abstract class AbstractHazelcastCacheTransport implements CacheTransport {

    final private Map<String, Serializable> cacheStatus = new ConcurrentHashMap<>();

    private final AtomicLong receivedMessages = new AtomicLong(0);
    private final AtomicLong receivedBytes = new AtomicLong(0);
    private final AtomicLong sentMessages = new AtomicLong(0);
    private final AtomicLong sentBytes = new AtomicLong(0);

    private final String topicName = "dotCMSClusterCacheInvalidation";
    private String topicId;

    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    protected abstract HazelcastInstanceType getHazelcastInstanceType();
    
    @Override
    public void init(Server localServer) throws CacheTransportException {
    	if(!LicenseManager.getInstance().isEnterprise()){
    		return;
    	}
        Logger.info(this,"Starting Hazelcast Cache Transport");
        Logger.debug(this,"Calling HazelUtil to ensure Hazelcast member is up");

        HazelcastInstance hazel = getHazelcastInstance(true);

        MessageListener<Object> messageListener = new MessageListener<Object>() {
            @Override
            public void onMessage( Message<Object> message ) {
                if ( message == null ) {
                    return;
                }
                String msg = message.getMessageObject().toString();

                if ( msg == null ) {
                    return;
                }
                receive(msg);
            }
        };
        for(int i=0;i<50;i++){
            try{
                topicId = hazel.getTopic(topicName).addMessageListener(messageListener);
                break;
            }
            catch(NullPointerException npe){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Logger.error(this.getClass(), e.getMessage());
                }
            }
            if(i==49){
                Logger.error(this.getClass(), "Unable to register HAZELCAST Listener");
            }
        }
        

        isInitialized.set(true);
    }

    @Override
    public boolean isInitialized() {
        return isInitialized.get();
    }

    @Override
    public boolean shouldReinit() {
        return true;
    }
    public void receive(String msg){

    	receivedMessages.addAndGet(1);
    	receivedBytes.addAndGet(msg.length());

        if ( msg.equals(ChainableCacheAdministratorImpl.TEST_MESSAGE) ) {

            Logger.info(this, "Received Message Ping " + new Date());
            try {
                getHazelcastInstance().getTopic(topicName).publish("ACK");
            } catch ( Exception e ) {
                Logger.error(AbstractHazelcastCacheTransport.class, e.getMessage(), e);
            }

            //Handle when other server is responding to ping.
        } else if ( msg.startsWith(ChainableCacheAdministratorImpl.VALIDATE_CACHE_RESPONSE) ) {

            //Deletes the first part of the message, no longer needed.
            msg = msg.replace(ChainableCacheAdministratorImpl.VALIDATE_CACHE_RESPONSE, "");

            Logger.info(this.getClass(), "got a response to my question: VALIDATE_CACHE? ");
            synchronized (this) {
                final String incomingMessage = msg;

                HashMap<String, Serializable> incomingMap =
                                Try.of(() -> DotObjectMapperProvider.getInstance().getDefaultObjectMapper()
                                                .readValue(incomingMessage, HashMap.class)).getOrElse(new HashMap<>());
                final String origin = (String) incomingMap.get("serverId");
                cacheStatus.put(origin, incomingMap);
                Logger.debug(this, ChainableCacheAdministratorImpl.VALIDATE_CACHE_RESPONSE + " SERVER_ID: " + origin
                                + " Message: " + msg);
            }



            //Handle when other server is trying to ping local server.
        } else if ( msg.startsWith(ChainableCacheAdministratorImpl.VALIDATE_CACHE) ) {

            final DotPubSubEvent event = new DotPubSubEvent.Builder()
                            .withOrigin(APILocator.getServerAPI().readServerId())
                            .withType(CacheTransportTopic.CacheEventType.CLUSTER_REQ.name())
                            .withTopic(topicName)
                            .withPayload(ClusterUtil.getNodeInfo())
                            .build();

            Logger.info(this.getClass(), "got asked to VALIDATE_CACHE?, sending response");
            try {
                send(ChainableCacheAdministratorImpl.VALIDATE_CACHE_RESPONSE + event.toString());
            } catch ( CacheTransportException e ) {
                Logger.error(this.getClass(), "Error sending message", e);
                throw new DotRuntimeException("Error sending message", e);
            }

            Logger.debug(this, ChainableCacheAdministratorImpl.VALIDATE_CACHE +event.getMessage());

        } else if ( msg.equals("ACK") ) {
            Logger.info(this, "ACK Received " + new Date());
        } else if ( msg.equals("MultiMessageResources.reload") ) {
            MultiMessageResources messages = (MultiMessageResources) Config.CONTEXT.getAttribute(Globals.MESSAGES_KEY);
            messages.reloadLocally();
        } else if ( msg.equals(ChainableCacheAdministratorImpl.DUMMY_TEXT_TO_SEND) ) {
            //Don't do anything is we are only checking sending.
        } else {
            CacheLocator.getCacheAdministrator().invalidateCacheMesageFromCluster(msg);
        }
    }

    @Override
    public void send(String message) throws CacheTransportException {

    	sentMessages.addAndGet(1);
    	sentBytes.addAndGet(message.length());

    	try {
            getHazelcastInstance().getTopic(topicName).publish(message);
        } catch ( Exception e ) {
            Logger.error(AbstractHazelcastCacheTransport.class, "Unable to send message: " + e.getMessage(), e);
            throw new CacheTransportException("Unable to send message", e);
        }
    }

    @Override
    public void testCluster() throws CacheTransportException {
        try {
            send(ChainableCacheAdministratorImpl.TEST_MESSAGE);
            Logger.info(this, "Sending Ping to Cluster " + new Date());
        } catch ( Exception e ) {
            Logger.error(AbstractHazelcastCacheTransport.class, e.getMessage(), e);
            throw new CacheTransportException("Error testing cluster", e);
        }
    }

    @Override
    public Map<String, Serializable> validateCacheInCluster(int maxWaitMillis) throws CacheTransportException {

        
        cacheStatus.clear();

        final int numberServers = Try.of(()-> APILocator.getServerAPI().getAliveServers().size()).getOrElse(0);
        
        final Map<String,Serializable> map = ClusterUtil.getNodeInfo();
        cacheStatus.put(APILocator.getServerAPI().readServerId(),(Serializable) map);
        
        //If we are already in Cluster.
        if ( numberServers > 1 ) {
            //Sends the message to the other servers.
            send(ChainableCacheAdministratorImpl.VALIDATE_CACHE );

            //Waits for 2 seconds in order all the servers respond.

            final long endTime = System.currentTimeMillis() + maxWaitMillis;

            //Trying to NOT wait whole 2 seconds for returning the info.
            while ( System.currentTimeMillis() <= endTime ) {
                try {
                    Thread.sleep(10);

                    if(cacheStatus.size()>=numberServers) {
                        return ImmutableMap.copyOf(cacheStatus);
                    }
                } catch ( InterruptedException ex ) {
                    Thread.currentThread().interrupt();
    
                }
            }
        }

        return ImmutableMap.copyOf(cacheStatus);


    }

    @Override
    public void shutdown() throws CacheTransportException {
    	if (isInitialized.get()) {
    		getHazelcastInstance().getTopic(topicName).removeMessageListener(topicId);

    		isInitialized.set(false);
    	}
    }

    protected HazelcastInstance getHazelcastInstance() {
    	return getHazelcastInstance(false);
    }

    protected HazelcastInstance getHazelcastInstance(boolean reInitialize) {
    	return HazelcastUtil.getInstance().getHazel(getHazelcastInstanceType(), reInitialize);
    }


    @Override
    public CacheTransportInfo getInfo() {
    	HazelcastInstance hazel = getHazelcastInstance();

    	return new CacheTransportInfo(){
    		@Override
    		public String getClusterName() {
    			return hazel.getName();
    		}

    		@Override
        	public String getAddress() {
    			return ((InetSocketAddress) hazel.getLocalEndpoint().getSocketAddress()).getHostString();
    		}

    		@Override
    		public int getPort() {
    			return ((InetSocketAddress) hazel.getLocalEndpoint().getSocketAddress()).getPort();
    		}


    		@Override
    		public boolean isOpen() {
    			return hazel.getLifecycleService().isRunning();
    		}

    		@Override
    		public int getNumberOfNodes() {
        		return hazel.getCluster().getMembers().size();
        	}


    		@Override
    		public long getReceivedBytes() {
    			return receivedBytes.get();
    		}

    		@Override
    		public long getReceivedMessages() {
    			return receivedMessages.get();
    		}

    		@Override
    		public long getSentBytes() {
    			return sentBytes.get();
    		}

    		@Override
    		public long getSentMessages() {
    			return sentMessages.get();
    		}
    	};
    }
}
