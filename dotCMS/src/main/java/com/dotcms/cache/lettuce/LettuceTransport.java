package com.dotcms.cache.lettuce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import com.dotcms.cluster.bean.Server;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.ChainableCacheAdministratorImpl;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.cache.transport.CacheTransportException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.struts.MultiMessageResources;
import io.lettuce.core.Consumer;
import io.lettuce.core.RedisBusyException;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.vavr.control.Try;



enum MessageType {
    INFO, INVALIDATE, TEST, PING, PONG, VALIDATE_CACHE_RESPONSE, VALIDATE_CACHE;

    static MessageType from(final String type) {
        if (type == null) {
            return INFO;
        }
        return valueOf(type);
    }
}

public class LettuceTransport implements CacheTransport {

    private Map<String, Map<String, Boolean>> cacheStatus = new HashMap<>();

    private final String STREAMS_KEY;

    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    private final LettuceClient lettuce;

    private final String serverId;

    private final boolean testing;
    private final static String CACHE_ATTACHED_TO_CACHE="CACHE_ATTACHED_TO_CACHE";
    @VisibleForTesting
    LettuceTransport(LettuceClient client, String serverId, String clusterId, boolean testing) {
        lettuce = client;
        this.serverId = serverId;
        this.testing = testing;
        this.STREAMS_KEY = "topic:" + clusterId;
        this.messagesIn = (this.testing) ? Collections.synchronizedList(new ArrayList<>()) : null;
        this.messagesOut = (this.testing) ? Collections.synchronizedList(new ArrayList<>()) : null;
    }

    @VisibleForTesting
    LettuceTransport(LettuceClient client, String serverId, String clusterId) {
        this(client, serverId, clusterId, true);

    }

    public LettuceTransport() {
        this(LettuceClient.getInstance(), APILocator.getServerAPI().readServerId(), ClusterFactory.getClusterId(), false);
    }


    @Override
    public void init(Server localServer) throws CacheTransportException {
        if (!LicenseManager.getInstance().isEnterprise()) {
            return;
        }
        init();

    }


    void init() throws CacheTransportException {
        Logger.info(this, "LettuceTransport " + serverId + " joining cache " + this.STREAMS_KEY);



        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            conn.sync().xgroupCreate(XReadArgs.StreamOffset.from(STREAMS_KEY, "$"), serverId);

        } catch (RedisBusyException redisBusyException) {
            Logger.debug(this.getClass(), String.format("Server '%s already joined to cluster", serverId), redisBusyException);
        }
        
        send(MessageType.INFO,"Server Online:" + serverId + " time:" + new Date());


        
        isInitialized.set(true);
        listenThread();

    }

    @Override
    public boolean isInitialized() {
        return isInitialized.get();
    }

    @Override
    public boolean shouldReinit() {
        return false;
    }

    /**
     * Only used for tests
     */
    final List<Message> messagesIn;
    final List<Message> messagesOut;
    
    public void listenThread() {

        new Thread("LettuceTransport") {
            public void run() {
                while (isInitialized()) {
                    try {
                        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
                            List<StreamMessage<String, Object>> messages = conn.sync().xreadgroup(
                                            Consumer.from(serverId, serverId), XReadArgs.StreamOffset.lastConsumed(STREAMS_KEY));
                            // ACK Attack
                            messages.forEach(m->conn.sync().xack(STREAMS_KEY, serverId, m.getId()));

                            for (final StreamMessage<String, Object> messageIn : messages) {
                                final Message message = new Message(messageIn.getBody());
                                if(serverId.equals(message.serverId)) {
                                    continue;
                                }
                                if (testing) {
                                    Logger.info(this.getClass(),"server:" + serverId + " got message:" + message);
                                    messagesIn.add(message);
                                }
                                
                                handleIncoming(message);
                            }
                            
                        }

                    } catch (Exception e) {
                        Logger.warn(this.getClass(), e.getMessage(),e);
                    }
                    Try.run(() -> Thread.sleep(100L));
                }
            }
        }.start();

    }


    private void handleIncoming(Message message) {
        
        switch (message.type) {

            case PING:
                Logger.info(this, serverId + " got PING from: " + message + " sending PONG" );
                send(MessageType.PONG, serverId);
                break;
            case INFO:
                Logger.info(this, String.format("server:%s got message:%s", serverId, message));
                break;
            case PONG:
                Logger.info(this, serverId + " got PONG from:" + message);
                break;
            case VALIDATE_CACHE_RESPONSE:
                validateCacheResponse(message);
                break;
            case VALIDATE_CACHE:
                validateCacheResponse(message);
                break;
            default:
                CacheLocator.getCacheAdministrator().invalidateCacheMesageFromCluster(message.message);;
        }


    }




    private void validateCacheResponse(Message message) {


        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            conn.sync().sadd(CACHE_ATTACHED_TO_CACHE, serverId);
        }


    }


    public void parseMessage(final String msg) {


        if (msg.equals("ACK")) {
            Logger.info(this, "ACK Received " + new Date());
        } else if (msg.equals("MultiMessageResources.reload")) {
            MultiMessageResources messages = (MultiMessageResources) Config.CONTEXT.getAttribute(Globals.MESSAGES_KEY);
            messages.reloadLocally();
        } else if (msg.equals(ChainableCacheAdministratorImpl.DUMMY_TEXT_TO_SEND)) {
            // Don't do anything is we are only checking sending.
        } else {
            CacheLocator.getCacheAdministrator().invalidateCacheMesageFromCluster(msg);
        }
    }


    public void send(final MessageType type, final String messageStr) throws CacheTransportException {

        send(new Message(serverId, type, messageStr));


    }

    public void send(final Message message) throws CacheTransportException {
        if (testing) {
            Logger.info(this.getClass(), String.format("Sending " + message));
            messagesOut.add(message);
        }

        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            conn.sync().xadd(STREAMS_KEY, message.type.name(),message.encode());
        }

    }

    @Override
    public void send(String message) throws CacheTransportException {
        send(MessageType.INVALIDATE, message);
    }

    @Override
    public void testCluster() throws CacheTransportException {

        Logger.info(this, "Sending Ping from " + serverId + " to Cluster" + new Date());
        this.send(MessageType.PING, serverId);

    }

    @Override
    public Map<String, Boolean> validateCacheInCluster(String dateInMillis, int numberServers, int maxWaitSeconds)
                    throws CacheTransportException {
        cacheStatus = new HashMap<>();

        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            conn.sync().spop(CACHE_ATTACHED_TO_CACHE, 1000000L);
            conn.sync().sadd(CACHE_ATTACHED_TO_CACHE, serverId);
        }
        Set<String> cacheMembers = new HashSet<>();
        
        // If we are already in Cluster.
        if (numberServers > 0) {
            // Sends the message to the other servers.
            send(MessageType.VALIDATE_CACHE, ChainableCacheAdministratorImpl.VALIDATE_CACHE + dateInMillis);

            // Waits for 2 seconds in order all the servers respond.
            int maxWaitTime = maxWaitSeconds * 1000;
            int passedWaitTime = 0;

            // Trying to NOT wait whole 2 seconds for returning the info.
            while (passedWaitTime <= maxWaitTime) {

                try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
                    cacheMembers.addAll(conn.sync().smembers(CACHE_ATTACHED_TO_CACHE).stream().map(o -> o.toString())
                                    .collect(Collectors.toSet()));
                }
                if (cacheMembers.size() >= numberServers) {
                    break;
                }



                Try.run(() -> Thread.sleep(10L));
                passedWaitTime += 10;

            }
        }


        Map<String, Boolean> mapToReturn = new HashMap<String, Boolean>();
        cacheMembers.forEach(s->mapToReturn.put(s,true));

        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            conn.sync().spop(CACHE_ATTACHED_TO_CACHE, 1000000L);

        }
        return mapToReturn;
    }

    @Override
    public void shutdown() throws CacheTransportException {
        if (isInitialized.get()) {
            Logger.info(this.getClass(), "Removing Server:" + serverId + " from cache cluster");
            try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
                conn.sync().xgroupDestroy(STREAMS_KEY, serverId);
            }
            isInitialized.set(false);
        }
    }



    @Override
    public CacheTransportInfo getInfo() {
        final Map<String, String> infoMap = new HashMap<>();
        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {

            Logger.info(this.getClass(), "REDIS INFO ------------");
            String[] infos = conn.sync().info().split(System.getProperty("line.separator"));
            for (String info : infos) {
                Logger.info(this.getClass(), "  " + info);
                infoMap.put(info.split(":")[0], info.split(":", 2)[1]);
            }



            return new CacheTransportInfo() {
                @Override
                public String getClusterName() {
                    return STREAMS_KEY;
                }

                @Override
                public String getAddress() {
                    return infoMap.get("redis:" + infoMap.get("role") + " " + infoMap.get("redis_version") + " "
                                    + infoMap.get("redis_mode") + " " + infoMap.get("redis_mode"));
                }

                @Override
                public int getPort() {
                    return Try.of(() -> Integer.parseInt(infoMap.get("tcp_port"))).getOrElse(-1);
                }


                @Override
                public boolean isOpen() {
                    return !infoMap.isEmpty();
                }

                @Override
                public int getNumberOfNodes() {
                    return Try.of(() -> Integer.parseInt(infoMap.get("connected_clients"))).getOrElse(-1);
                }


                @Override
                public long getReceivedBytes() {
                    return Try.of(() -> Integer.parseInt(infoMap.get("tcp_port"))).getOrElse(-1);
                }

                @Override
                public long getReceivedMessages() {
                    return -1l;
                }

                @Override
                public long getSentBytes() {
                    return -1l;
                }

                @Override
                public long getSentMessages() {
                    return -1l;
                }
            };
        }
    }
    
    
    
    
}
