package com.dotmarketing.portlets.contentlet.business;

import com.dotcms.dotpubsub.DotPubSubEvent;
import com.dotcms.dotpubsub.DotPubSubProvider;
import com.dotcms.dotpubsub.DotPubSubProviderLocator;
import com.dotcms.dotpubsub.DotPubSubTopic;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.Map;
import java.util.function.Consumer;

//public class HostCacheTopic implements DotPubSubTopic {
//
//    public static final String HOST_CACHE_TOPIC = "dothostcache_topic";
//
//    private static long bytesSent, bytesReceived, messagesSent, messagesReceived = 0;
//
//    @VisibleForTesting
//    private final String serverId;
//
//    private final DotPubSubProvider provider;
//
//    final Map<String, Consumer<DotPubSubEvent>> consumerMap;
//
//    /**
//     * Events types to handle
//     * request: made on the content api
//     * response: handle by the Pub/Sub
//     * unknown
//     */
//    public enum EventType {
//
//        HOST_CACHE_REQUEST, HOST_CACHE_RESPONSE, UNKNOWN;
//
//        private static final EventType [] types = values();
//
//        public static  EventType from(final Serializable name) {
//
//            for (final EventType type : types) {
//                if (type.name().equals(name)) {
//                    return type;
//                }
//            }
//
//            return UNKNOWN;
//        }
//    }
//
//    public HostCacheTopic() {
//        this(APILocator.getServerAPI().readServerId());
//    }
//
//    @VisibleForTesting
//    public HostCacheTopic(final String serverId) {
//        this(serverId, DotPubSubProviderLocator.provider.get());
//    }
//
//    @VisibleForTesting
//    public HostCacheTopic(final String serverId, final DotPubSubProvider provider) {
//        this.serverId = StringUtils.shortify(serverId, 10);
//        this.provider = provider;
//        this.consumerMap = new ImmutableMap.Builder<String, Consumer<DotPubSubEvent>>()
//                .put(EventType.HOST_CACHE_REQUEST.name(),  (event) -> {
//
//                    Logger.info(this.getClass(), () -> "Got HOST_CACHE_REQUEST from server:" + event.getOrigin() + ". sending response");
//
//                    HostCacheTopic.this.provider.publish(new DotPubSubEvent.Builder(event)
//                            // we set response b/c the nodes subscribed will wait this kind of msg
//                            .withType(EventType.HOST_CACHE_RESPONSE.name()).withTopic(HostCacheTopic.this)
//                            .build());
//                })
//                .put(EventType.HOST_CACHE_RESPONSE.name(), (event) -> {
//
//                    Logger.info(this.getClass(),
//                            () -> "Got HOST_CACHE_RESPONSE from server:" + event.getOrigin() + ". Saving response");
//
//                    final String origin = (String) event.getOrigin();
//                    Logger.info(this, "Event received: " + event.toString());
//                    if(!this.serverId.equals(origin)) {
//                        APILocator.getHostAPI()
//                                .updateCache(null);
//                    }
//                }).build();
//    }
//
//    public String getInstanceId () {
//        return serverId;
//    }
//
//    @Override
//    public Comparable getKey() {
//        return HOST_CACHE_TOPIC;
//    }
//
//    @Override
//    public long messagesSent() {
//        return messagesSent;
//    }
//
//    @Override
//    public long bytesSent() {
//        return bytesSent;
//    }
//
//    @Override
//    public long messagesReceived() {
//        return messagesReceived;
//    }
//
//    @Override
//    public long bytesReceived() {
//        return bytesReceived;
//    }
//
//    @Override
//    public void incrementSentCounters(final DotPubSubEvent event) {
//        bytesSent += event.toString().getBytes().length;
//        messagesSent++;
//    }
//
//    @Override
//    public void incrementReceivedCounters(final DotPubSubEvent event) {
//        bytesReceived += event.toString().getBytes().length;
//        messagesReceived++;
//    }
//
//    @Override
//    public void notify(final DotPubSubEvent event) {
//
//        Logger.info(this.getClass(), () -> "Got CLUSTER_REQ from server:" + event.getOrigin() + ". sending response");
//
//        this.consumerMap.getOrDefault(event.getType(), doNothing).accept(event);
//    }
//
//    final private Consumer<DotPubSubEvent> doNothing = (event) -> {
//        Logger.debug(getClass(), () -> "got an non-event:" + event);
//    };
//
//
//}
