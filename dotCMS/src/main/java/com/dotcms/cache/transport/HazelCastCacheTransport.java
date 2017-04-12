package com.dotcms.cache.transport;

import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.business.HazelcastUtil;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.ChainableCacheAdministratorImpl;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.cache.transport.CacheTransportException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.hazelcast.config.TopicConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.liferay.portal.struts.MultiMessageResources;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jasontesser on 3/28/17.
 */
public class HazelCastCacheTransport implements CacheTransport{

    private Map<String, Map<String, Boolean>> cacheStatus;
    private final String topicName = "dotCMSClusterCacheInvalidation";
    private String topicId;

    @Override
    public void init(Server localServer) throws CacheTransportException {
        Logger.info(this,"Starting Hazelcast Cache Transport");
        Logger.debug(this,"Calling HazelUtil to ensure Hazelcast member is up");
        HazelcastInstance hazel = new HazelcastUtil().getHazel();
        TopicConfig topicConfig = new TopicConfig();
        topicConfig.setGlobalOrderingEnabled( false );
        topicConfig.setStatisticsEnabled( false );
        topicConfig.setName( topicName );
        hazel.getConfig().addTopicConfig(topicConfig);
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
        topicId = new HazelcastUtil().getHazel().getTopic(topicName).addMessageListener(messageListener);
    }



    public void receive(String msg){
        if ( msg.equals(ChainableCacheAdministratorImpl.TEST_MESSAGE) ) {

            Logger.info(this, "Received Message Ping " + new Date());
            try {
                new HazelcastUtil().getHazel().getTopic(topicName).publish("ACK");
            } catch ( Exception e ) {
                Logger.error(HazelCastCacheTransport.class, e.getMessage(), e);
            }

            //Handle when other server is responding to ping.
        } else if ( msg.startsWith(ChainableCacheAdministratorImpl.VALIDATE_CACHE_RESPONSE) ) {

            //Deletes the first part of the message, no longer needed.
            msg = msg.replace(ChainableCacheAdministratorImpl.VALIDATE_CACHE_RESPONSE, "");

            //Gets the part of the message that has the Data in Milli.
            String dateInMillis = msg.substring(0, msg.indexOf(ChainableCacheAdministratorImpl.VALIDATE_SEPARATOR));
            //Gets the last part of the message that has the Server ID.
            String serverID = msg.substring(msg.lastIndexOf(ChainableCacheAdministratorImpl.VALIDATE_SEPARATOR) + 1);

            synchronized (this) {
                //Creates or updates the Map inside the Map.
                Map<String, Boolean> localMap = cacheStatus.get(dateInMillis);

                if ( localMap == null ) {
                    localMap = new HashMap<String, Boolean>();
                }

                localMap.put(serverID, Boolean.TRUE);

                //Add the Info with the Date in Millis and the Map with Server Info.
                cacheStatus.put(dateInMillis, localMap);
            }

            Logger.debug(this, ChainableCacheAdministratorImpl.VALIDATE_CACHE_RESPONSE + " SERVER_ID: " + serverID + " DATE_MILLIS: " + dateInMillis);

            //Handle when other server is trying to ping local server.
        } else if ( msg.startsWith(ChainableCacheAdministratorImpl.VALIDATE_CACHE) ) {

            //Deletes the first part of the message, no longer needed.
            msg = msg.replace(ChainableCacheAdministratorImpl.VALIDATE_CACHE, "");

            //Gets the part of the message that has the Data in Milli.
            String dateInMillis = msg;
            //Sends the message back in order to alert the server we are alive.
            try {
                send(ChainableCacheAdministratorImpl.VALIDATE_CACHE_RESPONSE + dateInMillis + ChainableCacheAdministratorImpl.VALIDATE_SEPARATOR + APILocator.getServerAPI().readServerId());
            } catch ( CacheTransportException e ) {
                Logger.error(this.getClass(), "Error sending message", e);
                throw new DotRuntimeException("Error sending message", e);
            }

            Logger.debug(this, ChainableCacheAdministratorImpl.VALIDATE_CACHE + " DATE_MILLIS: " + dateInMillis);

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
        try {
            new HazelcastUtil().getHazel().getTopic(topicName).publish(message);
        } catch ( Exception e ) {
            Logger.error(HazelCastCacheTransport.class, "Unable to send message: " + e.getMessage(), e);
            throw new CacheTransportException("Unable to send message", e);
        }
    }

    @Override
    public void testCluster() throws CacheTransportException {
        try {
            send(ChainableCacheAdministratorImpl.TEST_MESSAGE);
            Logger.info(this, "Sending Ping to Cluster " + new Date());
        } catch ( Exception e ) {
            Logger.error(HazelCastCacheTransport.class, e.getMessage(), e);
            throw new CacheTransportException("Error testing cluster", e);
        }
    }

    @Override
    public Map<String, Boolean> validateCacheInCluster(String dateInMillis, int numberServers, int maxWaitSeconds) throws CacheTransportException {
        cacheStatus = new HashMap<>();

        //If we are already in Cluster.
        if ( numberServers > 0 ) {
            //Sends the message to the other servers.
            send(ChainableCacheAdministratorImpl.VALIDATE_CACHE + dateInMillis);

            //Waits for 2 seconds in order all the servers respond.
            int maxWaitTime = maxWaitSeconds * 1000;
            int passedWaitTime = 0;

            //Trying to NOT wait whole 2 seconds for returning the info.
            while ( passedWaitTime <= maxWaitTime ) {
                try {
                    Thread.sleep(10);
                    passedWaitTime += 10;

                    Map<String, Boolean> ourMap = cacheStatus.get(dateInMillis);

                    //No need to wait if we have all server results.
                    if ( ourMap != null && ourMap.size() == numberServers ) {
                        passedWaitTime = maxWaitTime + 1;
                    }

                } catch ( InterruptedException ex ) {
                    Thread.currentThread().interrupt();
                    passedWaitTime = maxWaitTime + 1;
                }
            }
        }

        //Returns the Map with all the info stored by receive() method.
        Map<String, Boolean> mapToReturn = new HashMap<String, Boolean>();

        if ( cacheStatus.get(dateInMillis) != null ) {
            mapToReturn = cacheStatus.get(dateInMillis);
        }

        return mapToReturn;
    }

    @Override
    public void shutdown() throws CacheTransportException {
        new HazelcastUtil().getHazel().getTopic(topicName).removeMessageListener(topicId);
    }

}
