package com.dotcms.cache.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import java.util.HashMap;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import com.dotcms.enterprise.ClusterUtilProxy;
import com.dotcms.cache.transport.CacheTransportTopic;
import com.dotcms.cache.transport.CacheTransportTopic.CacheEventType;
import com.dotcms.dotpubsub.DotPubSubEvent;
import com.dotcms.dotpubsub.DotPubSubProvider;
import com.dotcms.dotpubsub.DotPubSubProviderLocator;
import com.dotcms.dotpubsub.DotPubSubTopic;
import com.dotcms.dotpubsub.NullDotPubSubProvider;
import com.dotcms.dotpubsub.QueuingPubSubWrapper;

public class CacheTransportTopicTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // override the provider for testing
        System.setProperty(DotPubSubProviderLocator.DOT_PUBSUB_PROVIDER_OVERRIDE,NullDotPubSubProvider.class.getCanonicalName());
        
    }

    /**
     * tests if we can use a property to override the default provider
     */
    @Test
    public void test_provider_override() {
        DotPubSubProvider provider = DotPubSubProviderLocator.provider.get();
        assert provider instanceof NullDotPubSubProvider || provider instanceof QueuingPubSubWrapper;

    }

    @Test
    public void test_provider_topic_sending__a_ping_gets_a_pong() throws InterruptedException {
        NullDotPubSubProvider provider = new NullDotPubSubProvider();
        provider.start();

        DotPubSubTopic topic = new CacheTransportTopic("fakeServer",provider);
        DotPubSubEvent event= new DotPubSubEvent.Builder().withType(CacheEventType.PING.name()).withTopic(topic).build();
        topic.notify(event);
        Thread.sleep(2000);
        assert "PONG".equalsIgnoreCase(provider.lastEventIn().getType());
        
    }

    /** A server id longer than the 10 characters the topic keeps for its pub/sub origin. */
    private static final String FULL_SERVER_ID = "6f0c2a9e-1d4b-4c8e-9a51-7b3e2d1f0a99";

    private static DotPubSubEvent clusterRequest(final DotPubSubTopic topic) {
        return new DotPubSubEvent.Builder().withType(CacheEventType.CLUSTER_REQ.name()).withTopic(topic)
                .withOrigin("requester-server-id").addPayload(CacheTransportTopic.CLUSTER_REQUEST_ID, "req-1").build();
    }

    /**
     * Given : a status request carrying a request id.
     * When  : this server handles it.
     * Then  : it builds its report once and replies with a CLUSTER_RES that carries the report
     *         and echoes the request id, which is how the requester matches replies to its
     *         request (issues #37876, #37877).
     */
    @Test
    public void clusterRequest_repliesOnceWithTheReportAndTheRequestId() {
        final NullDotPubSubProvider provider = new NullDotPubSubProvider();
        final CacheTransportTopic topic = new CacheTransportTopic(FULL_SERVER_ID, provider);

        try (MockedStatic<ClusterUtilProxy> proxy = mockStatic(ClusterUtilProxy.class)) {
            proxy.when(ClusterUtilProxy::getNodeInfo)
                    .thenReturn(new HashMap<>(Map.of("serverId", FULL_SERVER_ID, "status", "green")));

            topic.notify(clusterRequest(topic));

            proxy.verify(ClusterUtilProxy::getNodeInfo, times(1));
        }
        final DotPubSubEvent reply = provider.lastEventOut();
        assertEquals(CacheEventType.CLUSTER_RES.name(), reply.getType());
        assertEquals("green", reply.getPayload().get("status"));
        assertEquals("req-1", reply.getPayload().get(CacheTransportTopic.CLUSTER_REQUEST_ID));
    }

    /**
     * Given : a status request, and a report that fails to build on this server.
     * When  : this server handles the request.
     * Then  : nothing is published and nothing escapes, so the requester lists this server as
     *         not responding, as the legacy Network tab always has (a reply without a report
     *         would make the server look healthy on both screens).
     */
    @Test
    public void clusterRequest_reportFails_sendsNoReply() {
        final NullDotPubSubProvider provider = new NullDotPubSubProvider();
        final CacheTransportTopic topic = new CacheTransportTopic(FULL_SERVER_ID, provider);

        try (MockedStatic<ClusterUtilProxy> proxy = mockStatic(ClusterUtilProxy.class)) {
            proxy.when(ClusterUtilProxy::getNodeInfo).thenThrow(new IllegalStateException("no cluster_server row"));

            topic.notify(clusterRequest(topic));

            proxy.verify(ClusterUtilProxy::getNodeInfo, times(1));
        }
        assertNull("no reply may be published when the report fails", provider.lastEventOut());
    }

    private static DotPubSubEvent reply(final DotPubSubTopic topic, final String requestId, final String serverId) {
        final DotPubSubEvent.Builder builder = new DotPubSubEvent.Builder()
                .withType(CacheEventType.CLUSTER_RES.name()).withTopic(topic).addPayload("serverId", serverId);
        if (requestId != null) {
            builder.addPayload(CacheTransportTopic.CLUSTER_REQUEST_ID, requestId);
        }
        return builder.build();
    }

    /**
     * Given : an open status request r1.
     * When  : replies arrive for r1, for an id this server never opened, and with no id at all
     *         (every server receives every reply), and then another server's status request.
     * Then  : only the r1 reply is collected, the incoming request doesn't wipe it, and closing
     *         r1 hands it back and forgets the request (issue #37877).
     */
    @Test
    public void replies_areCollectedOnlyForOpenRequests() {
        final NullDotPubSubProvider provider = new NullDotPubSubProvider();
        final CacheTransportTopic topic = new CacheTransportTopic(FULL_SERVER_ID, provider);
        topic.openRequest("r1");

        topic.notify(reply(topic, "r1", "server-a"));
        topic.notify(reply(topic, "unknown", "server-b"));
        topic.notify(reply(topic, null, "server-c"));
        try (MockedStatic<ClusterUtilProxy> proxy = mockStatic(ClusterUtilProxy.class)) {
            proxy.when(ClusterUtilProxy::getNodeInfo).thenReturn(new HashMap<>(Map.of("serverId", FULL_SERVER_ID)));
            topic.notify(clusterRequest(topic));
        }

        assertEquals(java.util.Set.of("server-a"), topic.readResponses("r1").keySet());
        assertEquals(java.util.Set.of("server-a"), topic.closeRequest("r1").keySet());
        topic.notify(reply(topic, "r1", "server-late"));
        assertEquals("a closed request collects nothing", 0, topic.readResponses("r1").size());
    }
}
