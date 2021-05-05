package com.dotcms.cache.transport;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.dotcms.dotpubsub.DotPubSubEvent;
import com.dotcms.dotpubsub.DotPubSubProvider;
import com.dotcms.dotpubsub.DotPubSubProviderLocator;
import com.dotcms.dotpubsub.DotPubSubTopic;
import com.dotcms.enterprise.ClusterUtil;
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

    private final Map<String, Serializable> serverResponses = new ConcurrentHashMap<>();

    public HashMap<String, Serializable> readResponses() {
        return new HashMap<>(serverResponses);

    }

    public void resetResponses() {
        serverResponses.clear();

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

    final private Function2<DotPubSubEvent, CacheTransportTopic, Boolean> clusterRequest = (event, topic) -> {
        serverResponses.clear();
        Logger.info(this.getClass(), () -> "Got CLUSTER_REQ from server:" + event.getOrigin() + ". sending response");

        final Map<String, Serializable> cacheInfo = Try.of(() -> ClusterUtil.getNodeInfo()).onFailure(e -> {
            Logger.warnAndDebug(CacheTransportTopic.class, e.getMessage(), e);
        }).getOrElse(ImmutableMap.of("serverId", event.getOrigin()));

        cacheInfo.putAll(ClusterUtil.getNodeInfo());

        topic.provider.publish(new DotPubSubEvent.Builder(event).withTopic(this).withPayload(cacheInfo)
                        .withType(CacheEventType.CLUSTER_RES.name()).withTopic(this)

                        .build());
        return true;
    };

    final private Function2<DotPubSubEvent, CacheTransportTopic, Boolean> clusterResponse = (event, topic) -> {

        Logger.info(this.getClass(),
                        () -> "Got CLUSTER_RESPONSE from server:" + event.getOrigin() + ". Saving response");

        final String origin = (String) event.getPayload().get("serverId");
        serverResponses.put(origin, (Serializable) event.getPayload());
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
