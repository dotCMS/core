package com.dotcms.cache.transport;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.dotcms.dotpubsub.DotPubSubEvent;
import com.dotcms.dotpubsub.DotPubSubProvider;
import com.dotcms.dotpubsub.DotPubSubProviderLocator;
import com.dotcms.dotpubsub.DotPubSubTopic;
import com.dotcms.enterprise.ClusterUtilProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import io.vavr.Function2;
import io.vavr.control.Try;

public class CacheTransportTopic implements DotPubSubTopic {

    final static String CACHE_TOPIC = "dotcache_topic";

    static long bytesSent, bytesRecieved, messagesSent, messagesRecieved = 0;

    /** Payload key that ties a cluster status reply to the request it answers. */
    static final String CLUSTER_REQUEST_ID = "clusterRequestId";

    /**
     * Replies to this server's open cluster status requests: request id, then responding server
     * id, then that server's reply. Every server receives every reply, so only ids opened here are
     * kept, and each is removed when its request ends.
     */
    private final Map<String, Map<String, Serializable>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * Starts collecting replies for a cluster status request this server is about to send.
     *
     * @param requestId the id the request carries
     */
    void openRequest(final String requestId) {
        pendingRequests.put(requestId, new ConcurrentHashMap<>());
    }

    /**
     * Returns the replies collected so far for an open request, keyed by responding server id.
     *
     * @param requestId the request id
     * @return a copy of the replies; empty if the request isn't open
     */
    Map<String, Serializable> readResponses(final String requestId) {
        final Map<String, Serializable> replies = pendingRequests.get(requestId);
        return replies != null ? new HashMap<>(replies) : new HashMap<>();
    }

    /**
     * Stops collecting replies for a request and returns what arrived. Later replies to it are
     * ignored.
     *
     * @param requestId the request id
     * @return the replies keyed by responding server id; empty if the request wasn't open
     */
    Map<String, Serializable> closeRequest(final String requestId) {
        final Map<String, Serializable> replies = pendingRequests.remove(requestId);
        return replies != null ? new HashMap<>(replies) : new HashMap<>();
    }

    @VisibleForTesting
    final String serverId;


    final DotPubSubProvider provider;

    public CacheTransportTopic() {
        this(APILocator.getServerAPI().readServerId());
    }

    @VisibleForTesting
    public CacheTransportTopic(String serverId) {
        this(serverId, DotPubSubProviderLocator.provider.get());
    }

    @VisibleForTesting
    public CacheTransportTopic(String serverId, DotPubSubProvider provider) {
        this.serverId = StringUtils.shortify(serverId, 10);
        this.provider = provider;
    }

    @Override
    public String getInstanceId () {
        return serverId;
    }

    public enum CacheEventType {
        INVAL, PING, PONG, CLUSTER_REQ, CLUSTER_RES, UKN;

        static public CacheEventType from(Serializable name) {
            for (CacheEventType type : CacheEventType.values()) {
                if (type.name().equals(name)) {
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
    public void notify(final DotPubSubEvent event) {

        Logger.debug(getClass(), () -> "got pub/sub event:" + event);

        functionMap.getOrDefault(event.getType(), doNothing).apply(event, this);

    }

    final private Function2<DotPubSubEvent, CacheTransportTopic, Boolean> doNothing = (event, topic) -> {
        Logger.debug(getClass(), () -> "got an non-event:" + event);

        return true;
    };

    final private Function2<DotPubSubEvent, CacheTransportTopic, Boolean> inval = (event, topic) -> {
        if (topic.serverId.equals(event.getOrigin())) {
            Logger.debug(getClass(), () -> "pub/sub event sent from me, ignoring:" + event);
            return true;
        }

        Logger.debug(getClass(), () -> "got cache invalidation event:" + event);
        CacheLocator.getCacheAdministrator().invalidateCacheMesageFromCluster(event.getMessage());
        return true;
    };

    final private Function2<DotPubSubEvent, CacheTransportTopic, Boolean> ping = (event, topic) -> {
        Logger.info(this.getClass(), () -> "Got PING from server:" + event.getOrigin() + ". sending PONG");
        topic.provider.publish(
                        new DotPubSubEvent.Builder(event).withTopic(this).withType(CacheEventType.PONG.name()).build());
        return true;
    };

    final private Function2<DotPubSubEvent, CacheTransportTopic, Boolean> pong = (event, topic) -> {
        Logger.info(this.getClass(), () -> "Got PONG from server:" + event + ".");
        return true;
    };

    /**
     * Answers a cluster status request ({@code CLUSTER_REQ}). The reply is built from the request,
     * so it echoes the request id, and carries this server's report, built once. If the report
     * can't be built, the failure is logged and no reply is sent.
     */
    final private Function2<DotPubSubEvent, CacheTransportTopic, Boolean> clusterRequest = (event, topic) -> {
        Logger.info(this.getClass(), () -> "Got CLUSTER_REQ from server:" + event.getOrigin() + ". sending response");

        // Built once. If it fails, no reply is sent: the requester then lists this server as not
        // responding, as it always has. A reply without a report would make it look healthy.
        final Try<Map<String, Serializable>> cacheInfo = Try.of(ClusterUtilProxy::getNodeInfo);
        if (cacheInfo.isFailure()) {
            Logger.warnAndDebug(CacheTransportTopic.class,
                    "Could not build the cluster status report: " + cacheInfo.getCause(), cacheInfo.getCause());
            return true;
        }

        topic.provider.publish(new DotPubSubEvent.Builder(event).withTopic(this).withPayload(cacheInfo.get())
                        .withType(CacheEventType.CLUSTER_RES.name()).withTopic(this)

                        .build());
        return true;
    };

    /**
     * Collects a reply to a cluster status request ({@code CLUSTER_RES}) under its request id.
     * Replies without an id, or for a request this server didn't open, are ignored.
     */
    final private Function2<DotPubSubEvent, CacheTransportTopic, Boolean> clusterResponse = (event, topic) -> {

        Logger.info(this.getClass(),
                        () -> "Got CLUSTER_RESPONSE from server:" + event.getOrigin() + ". Saving response");

        final Serializable requestId = event.getPayload().get(CLUSTER_REQUEST_ID);
        final String origin = (String) event.getPayload().get("serverId");
        if (requestId == null || origin == null) {
            Logger.debug(this.getClass(), () -> "Ignoring a cluster response without a request id: " + event);
            return true;
        }
        // computeIfPresent: replies to requests this server didn't send, or already closed, are dropped
        topic.pendingRequests.computeIfPresent(String.valueOf(requestId), (id, replies) -> {
            replies.put(origin, (Serializable) event.getPayload());
            return replies;
        });
        return true;
    };

    // Map <CacheEventType,Function2<DotPubSubEvent,CachePubSubTopic, Boolean>> functionMap
    final Map<String, Function2<DotPubSubEvent, CacheTransportTopic, Boolean>> functionMap =
                    new ImmutableMap.Builder<String, Function2<DotPubSubEvent, CacheTransportTopic, Boolean>>()
                                    .put(CacheEventType.INVAL.name(), this.inval)
                                    .put(CacheEventType.PING.name(), this.ping)
                                    .put(CacheEventType.PONG.name(), this.pong)
                                    .put(CacheEventType.CLUSTER_REQ.name(), this.clusterRequest)
                                    .put(CacheEventType.CLUSTER_RES.name(), this.clusterResponse).build();

    @Override
    public long messagesSent() {
        return messagesSent;
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
        bytesSent += event.toString().getBytes().length;
        messagesSent++;
    }

    @Override
    public void incrementReceivedCounters(final DotPubSubEvent event) {
        bytesRecieved += event.toString().getBytes().length;
        messagesRecieved++;
    }

}
