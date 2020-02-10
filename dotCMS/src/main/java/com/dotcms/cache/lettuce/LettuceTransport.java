package com.dotcms.cache.lettuce;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import com.dotcms.cluster.bean.Server;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ChainableCacheAdministratorImpl;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.cache.transport.CacheTransportException;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import io.lettuce.core.Consumer;
import io.lettuce.core.RedisBusyException;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.vavr.control.Try;



enum MessageType {
    INFO, ACTION
}



public abstract class LettuceTransport implements CacheTransport {

    private Map<String, Map<String, Boolean>> cacheStatus = new HashMap<>();

    private final String topicName;

    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    private final LettuceClient lettuce;

    private final String serverId;
    private final String clusterId;

    public LettuceTransport(LettuceClient client, String serverId) {
        lettuce = client;
        this.serverId = serverId;
        this.clusterId = ClusterFactory.getClusterId();
        this.topicName = "dotCMS:Cache:" + clusterId;
    }

    public LettuceTransport() {
        this(LettuceClient.getInstance.apply(), APILocator.getServerAPI().readServerId());
    }


    Map<MessageType, Object> info(String message) {
        return ImmutableMap.of(MessageType.INFO, message);
    }

    Map<MessageType, Object> action(String message) {
        return ImmutableMap.of(MessageType.ACTION, message);
    }

    @Override
    public void init(Server localServer) throws CacheTransportException {
        if (!LicenseManager.getInstance().isEnterprise()) {
            return;
        }
        Logger.info(this, "LettuceTransport joining cache :" + this.topicName);



        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            conn.sync().xadd(topicName, info("Server Online: + " + serverId + " time:" + new Date()));
        }

        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            // WARNING: Streams must exist before creating the group
            // This will not be necessary in Lettuce 5.2, see
            // https://github.com/lettuce-io/lettuce-core/issues/898
            conn.sync().xgroupCreate(XReadArgs.StreamOffset.from(topicName, "0-0"), serverId);

        } catch (RedisBusyException redisBusyException) {
            Logger.warn(this.getClass(), String.format("\t Server '%s already joined to cluster", serverId));
        }


        isInitialized.set(true);
        listenThread();
    }

    @Override
    public boolean isInitialized() {
        return isInitialized.get();
    }

    @Override
    public boolean shouldReinit() {
        return true;
    }

    public void listenThread() {
        System.out.println("Waiting for new messages");

        while(true) {
            try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
                List<StreamMessage<String, Object>> messages = conn.sync().xreadgroup(
                                Consumer.from(topicName, serverId),
                                XReadArgs.StreamOffset.lastConsumed(topicName)
                        );
                
                if (!messages.isEmpty()) {
                    for (StreamMessage<String, Object> message : messages) {
                        System.out.println(message);
                        // Confirm that the message has been processed using XACK
                        conn.sync().xack(topicName, serverId,  message.getId());
                    }
                }
            }
            Try.run(() -> Thread.sleep(100L));
        }
        
        
        
        
        /*
            
        if (msg.equals(ChainableCacheAdministratorImpl.TEST_MESSAGE)) {

            Logger.info(this, "Received Message Ping " + new Date());
            try {
                getHazelcastInstance().getTopic(topicName).publish("ACK");
            } catch (Exception e) {
                Logger.error(LettuceCacheTransport.class, e.getMessage(), e);
            }

            // Handle when other server is responding to ping.
        } else if (msg.startsWith(ChainableCacheAdministratorImpl.VALIDATE_CACHE_RESPONSE)) {

            // Deletes the first part of the message, no longer needed.
            msg = msg.replace(ChainableCacheAdministratorImpl.VALIDATE_CACHE_RESPONSE, "");

            // Gets the part of the message that has the Data in Milli.
            String dateInMillis = msg.substring(0, msg.indexOf(ChainableCacheAdministratorImpl.VALIDATE_SEPARATOR));
            // Gets the last part of the message that has the Server ID.
            String serverID = msg.substring(msg.lastIndexOf(ChainableCacheAdministratorImpl.VALIDATE_SEPARATOR) + 1);

            synchronized (this) {
                // Creates or updates the Map inside the Map.
                Map<String, Boolean> localMap = cacheStatus.get(dateInMillis);

                if (localMap == null) {
                    localMap = new HashMap<String, Boolean>();
                }

                localMap.put(serverID, Boolean.TRUE);

                // Add the Info with the Date in Millis and the Map with Server Info.
                cacheStatus.put(dateInMillis, localMap);
            }

            Logger.debug(this, ChainableCacheAdministratorImpl.VALIDATE_CACHE_RESPONSE + " SERVER_ID: " + serverID
                            + " DATE_MILLIS: " + dateInMillis);

            // Handle when other server is trying to ping local server.
        } else if (msg.startsWith(ChainableCacheAdministratorImpl.VALIDATE_CACHE)) {

            // Deletes the first part of the message, no longer needed.
            msg = msg.replace(ChainableCacheAdministratorImpl.VALIDATE_CACHE, "");

            // Gets the part of the message that has the Data in Milli.
            String dateInMillis = msg;
            // Sends the message back in order to alert the server we are alive.
            try {
                send(ChainableCacheAdministratorImpl.VALIDATE_CACHE_RESPONSE + dateInMillis
                                + ChainableCacheAdministratorImpl.VALIDATE_SEPARATOR + APILocator.getServerAPI().readServerId());
            } catch (CacheTransportException e) {
                Logger.error(this.getClass(), "Error sending message", e);
                throw new DotRuntimeException("Error sending message", e);
            }

            Logger.debug(this, ChainableCacheAdministratorImpl.VALIDATE_CACHE + " DATE_MILLIS: " + dateInMillis);

        } else if (msg.equals("ACK")) {
            Logger.info(this, "ACK Received " + new Date());
        } else if (msg.equals("MultiMessageResources.reload")) {
            MultiMessageResources messages = (MultiMessageResources) Config.CONTEXT.getAttribute(Globals.MESSAGES_KEY);
            messages.reloadLocally();
        } else if (msg.equals(ChainableCacheAdministratorImpl.DUMMY_TEXT_TO_SEND)) {
            // Don't do anything is we are only checking sending.
        } else {
            CacheLocator.getCacheAdministrator().invalidateCacheMesageFromCluster(msg);
        }
        
        */
    }

    @Override
    public void send(String message) throws CacheTransportException {

        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            conn.sync().xadd(topicName, info(message));
        }

    }

    @Override
    public void testCluster() throws CacheTransportException {
        try {
            Logger.info(this, "Sending Ping to Cluster " + new Date());
            send(ChainableCacheAdministratorImpl.TEST_MESSAGE);

        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage(), e);
            throw new CacheTransportException("Error testing cluster", e);
        }
    }

    @Override
    public Map<String, Boolean> validateCacheInCluster(String dateInMillis, int numberServers, int maxWaitSeconds)
                    throws CacheTransportException {
        cacheStatus = new HashMap<>();

        // If we are already in Cluster.
        if (numberServers > 0) {
            // Sends the message to the other servers.
            send(ChainableCacheAdministratorImpl.VALIDATE_CACHE + dateInMillis);

            // Waits for 2 seconds in order all the servers respond.
            int maxWaitTime = maxWaitSeconds * 1000;
            int passedWaitTime = 0;

            // Trying to NOT wait whole 2 seconds for returning the info.
            while (passedWaitTime <= maxWaitTime) {
                try {
                    Thread.sleep(10);
                    passedWaitTime += 10;

                    Map<String, Boolean> ourMap = cacheStatus.get(dateInMillis);

                    // No need to wait if we have all server results.
                    if (ourMap != null && ourMap.size() == numberServers) {
                        passedWaitTime = maxWaitTime + 1;
                    }

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    passedWaitTime = maxWaitTime + 1;
                }
            }
        }

        // Returns the Map with all the info stored by receive() method.
        Map<String, Boolean> mapToReturn = new HashMap<String, Boolean>();

        if (cacheStatus.get(dateInMillis) != null) {
            mapToReturn = cacheStatus.get(dateInMillis);
        }

        return mapToReturn;
    }

    @Override
    public void shutdown() throws CacheTransportException {
        if (isInitialized.get()) {
            Logger.info(this.getClass(), "Removing Server:" + serverId + " from cache cluster");
            try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
                conn.sync().xgroupDestroy(topicName, serverId);
            }
            isInitialized.set(false);
        }
    }



    @Override
    public CacheTransportInfo getInfo() {
        final Map<String,String> infoMap = new HashMap<>();
        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            
            Logger.info(this.getClass(), "REDIS INFO ------------" );
            String[] infos = conn.sync().info().split(System.getProperty("line.separator"));
            for(String info : infos) {
                Logger.info(this.getClass(), "  " + info);
                infoMap.put(info.split(":")[0], info.split(":",2)[1]);
            }
            

        
        

        return new CacheTransportInfo() {
            @Override
            public String getClusterName() {
                return topicName;
            }

            @Override
            public String getAddress() {
                return infoMap.get("redis:" + infoMap.get("role") + " " + infoMap.get("redis_version") + " " + infoMap.get("redis_mode")+ " " + infoMap.get("redis_mode"));
            }

            @Override
            public int getPort() {
                return Try.of(()->Integer.parseInt(infoMap.get("tcp_port"))).getOrElse(-1);
            }


            @Override
            public boolean isOpen() {
                return !infoMap.isEmpty();
            }

            @Override
            public int getNumberOfNodes() {
                return Try.of(()->Integer.parseInt(infoMap.get("connected_clients"))).getOrElse(-1);
            }


            @Override
            public long getReceivedBytes() {
                return Try.of(()->Integer.parseInt(infoMap.get("tcp_port"))).getOrElse(-1);
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
}
