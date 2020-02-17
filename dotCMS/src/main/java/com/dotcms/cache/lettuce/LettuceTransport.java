package com.dotcms.cache.lettuce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import com.dotcms.cluster.bean.Server;
import com.dotcms.enterprise.LicenseUtil;
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
import com.dotmarketing.util.WebKeys;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.struts.MultiMessageResources;
import io.lettuce.core.Consumer;
import io.lettuce.core.RedisBusyException;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.vavr.control.Try;


/**
 * categorizes the type of messages that can be sent and recieved
 */
enum MessageType {
    INFO, INVALIDATE, CYCLE_KEY, PING, PONG, VALIDATE_CACHE_RESPONSE, VALIDATE_CACHE;

    static MessageType from(final String type) {
        if (type == null) {
            return INFO;
        }
        return valueOf(type);
    }
}


/**
 * A cache transport layer built ontop of redis streams. Each server in a cluster registers itself
 * with redis as a consumer on a streams "group" and then polls the stream for new messages coming
 * in. Redis tracks the latest messages for each consumer and only returns those that have not been
 * consumed already
 *
 */
public class LettuceTransport implements CacheTransport {



    private String FINAL_STREAMS_KEY;
    private static final String TEMP_STREAM="topic:temp";
    final AtomicBoolean isInitialized = new AtomicBoolean(false);

    private final AtomicLong receivedMessages = new AtomicLong(0);

    private final AtomicLong sentMessages = new AtomicLong(0);

    private final LinkedBlockingQueue<Message> queue;

    private final LettuceClient lettuce;

    private final String serverId;

    private final boolean testing;
    private final static String CACHE_ATTACHED_TO_CACHE = "CACHE_ATTACHED_TO_CACHE";
    private final long REDIS_THREAD_SLEEP =
                    Try.of(() -> Config.getLongProperty("redis.lettucetransport.readwrite.thread.sleep", 100l)).getOrElse(100l);

    @VisibleForTesting
    LettuceTransport(LettuceClient client, String serverId, String clusterId, boolean testing) {
        lettuce = client;
        this.serverId = serverId;
        this.testing = testing;
        FINAL_STREAMS_KEY = clusterId !=null ? "topic:" + clusterId : TEMP_STREAM;
        this.messagesIn = (this.testing) ? Collections.synchronizedList(new ArrayList<>()) : null;
        this.messagesOut = (this.testing) ? Collections.synchronizedList(new ArrayList<>()) : null;
        this.queue = new LinkedBlockingQueue<>();
        if(!testing && LicenseUtil.getLevel()<300) {
            throw new DotRuntimeException("Reverting to NullTransport");
        }
    }

    @VisibleForTesting
    LettuceTransport(LettuceClient client, String serverId, String clusterId) {
        this(client, serverId, clusterId, true);
    }

    public LettuceTransport() {
        this(LettuceClient.getInstance(), APILocator.getServerAPI().readServerId(), null, false);
    }

    public String streamsKey() {
        if (FINAL_STREAMS_KEY == TEMP_STREAM && System.getProperty(WebKeys.DOTCMS_STARTED_UP) != null) {
            FINAL_STREAMS_KEY = "topic:" + ClusterFactory.getClusterId();
        }
        return FINAL_STREAMS_KEY;

    }
    
    
    @Override
    public void init(Server localServer) throws CacheTransportException {
        if (!LicenseManager.getInstance().isEnterprise()) {
            return;
        }
        init();
    }


    void init() throws CacheTransportException {
        Logger.info(this, "LettuceTransport " + serverId + " joining cache " + streamsKey());

        registerStream();
        isInitialized.set(true);
        listenThread();

    }

    /**
     * makes sure the group is set up and registers this server as a consumer of that group
     */
    void registerStream() {
        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            conn.sync().xgroupCreate(XReadArgs.StreamOffset.from(streamsKey(), "$"), serverId,
                            XGroupCreateArgs.Builder.mkstream());

        } catch (RedisBusyException redisBusyException) {
            Logger.debug(this.getClass(), String.format("Server '%s already joined to cluster", serverId), redisBusyException);
        }

        send(MessageType.INFO, "Server Online:" + serverId + " time:" + new Date());

    }



    @Override
    public boolean isInitialized() {
        return isInitialized.get();
    }

    @Override
    public boolean shouldReinit() {
        return false;
    }

    @VisibleForTesting
    final List<Message> messagesIn;
    @VisibleForTesting
    final List<Message> messagesOut;

    /**
     * this thread pools redis for new messages and then posts any local messages that need to be sent
     * out
     * 
     */
    public void listenThread() {

        new Thread("LettuceTransport") {


            public void run() {

                while (isInitialized()) {
                    try {
                        messagesIn();
                        messagesOut();
                        Thread.sleep(REDIS_THREAD_SLEEP);
                    } catch (RedisCommandExecutionException e) {
                        Logger.warnEveryAndDebug(this.getClass(), e, 2000);
                        if (e.getMessage() != null && e.getMessage().contains("NOGROUP")) {
                            Logger.warn(this.getClass(), "Lost the redis stream, re-registering");
                            registerStream();
                        }

                    } catch (Exception e) {
                        Logger.warnEveryAndDebug(this.getClass(), e, 2000);
                    }
                }

            }
        }.start();
    }

    /**
     * reads incoming messages, sends an ACK to acknowledge them, parses them into Messages and sends
     * them on to be handled.
     */
    void messagesIn() {

        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            List<StreamMessage<String, Object>> messages = conn.sync().xreadgroup(Consumer.from(serverId, serverId),
                            XReadArgs.StreamOffset.lastConsumed(streamsKey()));
            // ACK Attack
            messages.forEach(m -> conn.sync().xack(streamsKey(), serverId, m.getId()));
            receivedMessages.addAndGet(messages.size());
            for (final StreamMessage<String, Object> messageIn : messages) {
                List<Message> bodyMessages =
                                messageIn.getBody().entrySet().stream().map(e -> new Message(e.getValue(), e.getKey()))
                                                .filter(m -> !m.serverId.equals(serverId)).collect(Collectors.toList());

                for (Message bodyMessage : bodyMessages) {
                    if (testing) {
                        Logger.info(this.getClass(), "server:" + serverId + " got message:" + bodyMessage);
                        messagesIn.add(bodyMessage);
                    }
                    handleIncoming(bodyMessage);
                }
            }
        }
    }

    /**
     * long lived set that is uses to pass messages from the queue to redis. It is cleared every loop
     */
    final Set<Message> outgoingMessages = new HashSet<>();

    /**
     * polls the queue for outgoing messages
     */
    private void messagesOut() {

        queue.drainTo(outgoingMessages);
        send(outgoingMessages);
        outgoingMessages.clear();
    }


    /**
     * Handles messages based on their MessageType
     * 
     * @param message
     */
    private void handleIncoming(Message message) {
        switch (message.type) {
            case PING:
                Logger.info(this, serverId + " got PING from: " + message + " sending PONG");
                send(MessageType.PONG, serverId);
                break;
            case INFO:
                Logger.info(this, String.format("server:%s got message:%s", serverId, message));
                break;
            case PONG:
                Logger.info(this, serverId + " got PONG from:" + message);
                break;
            case VALIDATE_CACHE:
                validateCacheResponse(message);
                break;
            case CYCLE_KEY:
                Logger.info(this, String.format("server:%s got CYCLE_KEY:%s", serverId, message));
                LettuceCache.prefixKey.set(Long.valueOf(message.message));
                break;
            default:
                CacheLocator.getCacheAdministrator().invalidateCacheMesageFromCluster(message.message);;
        }
    }



    /**
     * sends a respose back to another server's question as to who is out there.
     * 
     * @param message
     */
    private void validateCacheResponse(Message message) {
        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            conn.sync().sadd(CACHE_ATTACHED_TO_CACHE, serverId);
        }
    }

    /**
     * Legacy parsing of a message str
     */
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

    /**
     * sends a message of a specific type
     * 
     * @param type
     * @param messageStr
     * @throws CacheTransportException
     */
    public void send(final MessageType type, final String messageStr) throws CacheTransportException {
        send(new Message(serverId, type, messageStr));
    }

    /**
     * sends a Collection of Messages to redis
     * 
     * @param message
     * @throws CacheTransportException
     */
    public void send(final Collection<Message> messages) throws CacheTransportException {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        if (testing) {
            Logger.info(this.getClass(), String.format("Sending " + messages));
            messagesOut.addAll(messages);
        }

        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            List<String> strList = new ArrayList<>();
            messages.forEach(m -> {
                strList.add(m.encode());
                strList.add(m.type.name());
            });

            conn.sync().xadd(streamsKey(), strList.toArray());
        }
        sentMessages.addAndGet(messages.size());
    }

    /**
     * adds a message to the queue
     * 
     * @param message
     * @throws CacheTransportException
     */
    public void send(final Message message) throws CacheTransportException {
        queue.add(message);
    }


    /**
     * adds a message to the queue for redis. by default, this will be interpreted as an invalidate
     * message
     * 
     * @param message
     * @throws CacheTransportException
     */
    @Override
    public void send(String message) throws CacheTransportException {
        queue.add(new Message(serverId, MessageType.INVALIDATE, message));
    }

    /**
     * sends a PING out to all servers, expects a PONG back
     */
    @Override
    public void testCluster() throws CacheTransportException {

        Logger.info(this, "Sending Ping from " + serverId + " to Cluster" + new Date());
        this.send(MessageType.PING, serverId);

    }

    /**
     * sends a VALIDATE_CACHE request to all servers, expects active serverIds back from the group
     */
    @Override
    public Map<String, Boolean> validateCacheInCluster(String dateInMillis, int numberServers, int maxWaitSeconds)
                    throws CacheTransportException {

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
        cacheMembers.forEach(s -> mapToReturn.put(s, true));

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
                conn.sync().xgroupDestroy(streamsKey(), serverId);
            }
            isInitialized.set(false);
            messagesIn.clear();
            messagesOut.clear();
            queue.clear();
        }

    }



    @Override
    public CacheTransportInfo getInfo() {
        final Map<String, String> infoMap = new HashMap<>();
        try (StatefulRedisConnection<String, Object> conn = lettuce.get()) {
            String[] urls  = Config.getStringArrayProperty("redis.lettuceclient.uris", new String[] {"redis://localhost"});
            Logger.info(this.getClass(), "REDIS INFO ------------");
            String[] infos = conn.sync().info().split("\r\n|\n|\r");
            for (String info : infos) {
                Logger.info(this.getClass(), "  " + info);
                String[] splitter = info.split(":",2);
                if(splitter.length>1) {
                infoMap.put(splitter[0], splitter[1]);
                }
            }



            return new CacheTransportInfo() {
                @Override
                public String getClusterName() {
                    return "redis:" + streamsKey();
                }

                @Override
                public String getAddress() {
                    return "redis:" + infoMap.get("redis_version") + " " + infoMap.get("redis_mode") + " "
                                    + infoMap.get("role") ;
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
                    return Try.of(() -> Integer.parseInt(infoMap.get("connected_slaves")) +1).getOrElse(-1);
                }


                @Override
                public long getReceivedBytes() {
                    return Try.of(() -> Long.parseLong(infoMap.get("total_net_input_bytes"))).getOrElse(-1l);
                }

                @Override
                public long getReceivedMessages() {
                    return Try.of(() -> Long.parseLong(infoMap.get("total_commands_processed"))).getOrElse(-1l);
                }

                @Override
                public long getSentBytes() {
                    return Try.of(() -> Long.parseLong(infoMap.get("total_net_output_bytes"))).getOrElse(-1l);
                }

                @Override
                public long getSentMessages() {
                    return sentMessages.get();
                }
            };
        }
    }



}
