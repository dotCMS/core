package org.apache.felix.framework;

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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * This topic alerts the other nodes that one of the nodes has restarted the OSGI Framework and
 * all of them needs to do the restart too.
 * @author jsanca
 */
public class OsgiRestartTopic implements DotPubSubTopic {

    final static String OSGI_RESTART_TOPIC = "osgi_restart_topic";

    static long bytesSent, bytesRecieved, messagesSent, messagesRecieved = 0;

    private final Map<String, Serializable> serverResponses = new ConcurrentHashMap<>();

    public HashMap<String, Serializable> readResponses() {
        return new HashMap<>(serverResponses);

    }

    public void resetResponses() {
        serverResponses.clear();
    }

    @VisibleForTesting
    private final String serverId;

    private final DotPubSubProvider provider;

    /**
     * Events types to handle
     * request: made on the content api
     * response: handle by the Pub/Sub
     * unknown
     */
    public enum EventType {

        OGSI_RESTART_REQUEST, OGSI_RESTART_RESPONSE, UNKNOWN;

        private static final EventType [] types = values();

        public static  EventType from(final Serializable name) {

            for (final EventType type : types) {
                if (type.name().equals(name)) {
                    return type;
                }
            }

            return UNKNOWN;
        }
    }

    final Map<String, Consumer<DotPubSubEvent>> consumerMap =
            new ImmutableMap.Builder<String, Consumer<DotPubSubEvent>>()
                    .put(EventType.OGSI_RESTART_REQUEST.name(),  (event) -> {

                        Logger.info(this.getClass(), () -> "Got OGSI_RESTART_REQUEST from server:" + event.getOrigin() + ". sending response");

                        OsgiRestartTopic.this.provider.publish(new DotPubSubEvent.Builder(event)
                                // we set response b/c the nodes subscribed will wait this kind of msg
                                .withType(EventType.OGSI_RESTART_RESPONSE.name()).withTopic(OsgiRestartTopic.this)
                                .build());
                    })
                    .put(EventType.OGSI_RESTART_RESPONSE.name(), (event) -> {

                        Logger.info(this.getClass(),
                                () -> "Got OGSI_RESTART_RESPONSE from server:" + event.getOrigin());

                        final String origin = (String) event.getPayload().get("sourceNode");
                        Logger.info(this, "Event received: " + event + ", origin node: " + origin);

                        // just in case we double check the origin is not itself to avoid double osgi restart
                        if (!OsgiRestartTopic.this.serverId.equals(origin)) {
                            // Restart the current instance
                            OSGIUtil.getInstance().restartOsgiOnlyLocal();
                        }
                    }).build();


    public OsgiRestartTopic() {
        this(APILocator.getServerAPI().readServerId());
    }

    @VisibleForTesting
    public OsgiRestartTopic(String serverId) {
        this(serverId, DotPubSubProviderLocator.provider.get());
    }

    @VisibleForTesting
    public OsgiRestartTopic(String serverId, DotPubSubProvider provider) {
        this.serverId = StringUtils.shortify(serverId, 10);
        this.provider = provider;
    }

    @Override
    public String getInstanceId () {
        return serverId;
    }

    public enum CacheEventType {
        CLUSTER_REQ, CLUSTER_RES, UKN;

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
        return OSGI_RESTART_TOPIC;
    }

    @Override
    public void notify(final DotPubSubEvent event) {

        Logger.info(this.getClass(), () -> "Got CLUSTER_REQ from server:" + event.getOrigin() + ". sending response");

        this.consumerMap.getOrDefault(event.getType(), doNothing).accept(event);
    }

    final private Consumer<DotPubSubEvent> doNothing = (event) -> {
        Logger.debug(getClass(), () -> "got an non-event:" + event);
    };

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
