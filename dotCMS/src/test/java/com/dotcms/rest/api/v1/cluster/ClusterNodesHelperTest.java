package com.dotcms.rest.api.v1.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.cluster.bean.Server;
import com.dotcms.content.index.domain.SearchClusterHealth.Engine;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link ClusterNodesHelper}, which builds the {@code GET /api/v1/cluster/nodes}
 * entity from the alive servers and one cluster status request (#37875).
 *
 * @author hassandotcms
 */
public class ClusterNodesHelperTest {

    private static final long NOW = 1_800_000_000_000L;

    private static Server server(final String id, final long secondsSinceHeartbeat) {
        return Server.builder()
                .withServerId(id)
                .withName("name-" + id)
                .withClusterId("cluster-1")
                .withIpAddress("10.0.0." + id.length())
                .withKey("26.10.01")
                .withLastHeartBeat(NOW - secondsSinceHeartbeat * 1000)
                .build();
    }

    private static ClusterNodesView nodes(final List<Server> alive, final Map<String, Serializable> replies,
            final CacheTransport transport) throws Exception {
        final ServerAPI serverAPI = mock(ServerAPI.class);
        when(serverAPI.readServerId()).thenReturn("aaaa");
        when(serverAPI.getAliveServers()).thenReturn(alive);
        when(transport.validateCacheInCluster(anyInt())).thenReturn(replies);
        try (MockedStatic<LicenseUtil> license = mockStatic(LicenseUtil.class)) {
            license.when(() -> LicenseUtil.getDisplayServerId(anyString()))
                    .thenAnswer(call -> "short-" + call.getArgument(0));
            return new ClusterNodesHelper(serverAPI, () -> transport,
                    Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)).nodes();
        }
    }

    /**
     * Given : three alive servers; the first answers, the second doesn't, the third answers with
     *         a failed assets check, and a server that is not in the alive list also answers.
     * When  : the nodes are built.
     * Then  : exactly one status request is sent, the nodes keep the alive-list order, identity
     *         comes from the database row, the server that answered has its sections, the other
     *         has none, the one with a failed assets check is DOWN, and the stray reply is ignored.
     */
    @Test
    public void answeredAndUnansweredServers() throws Exception {
        final Map<String, Serializable> report = new HashMap<>();
        report.put("serverId", "aaaa");
        report.put("cacheTransportClass", "PubSub");
        report.put("esEngine", "ELASTICSEARCH");
        report.put("assetsCanWrite", true);
        final Map<String, Serializable> replies = new HashMap<>();
        replies.put("aaaa", (Serializable) report);
        replies.put("stray", new HashMap<>(Map.of("serverId", "stray")));
        replies.put("cccc", new HashMap<>(Map.of("serverId", "cccc", "assetsStatus", "red")));
        final CacheTransport transport = mock(CacheTransport.class);

        final ClusterNodesView view = nodes(List.of(server("aaaa", 10), server("bbbb", 400), server("cccc", 10)),
                replies, transport);

        verify(transport, times(1)).validateCacheInCluster(anyInt());
        assertEquals("aaaa", view.currentServerId());
        assertEquals(3, view.nodes().size());

        final ClusterNodeView answered = view.nodes().get(0);
        assertEquals("aaaa", answered.serverId());
        assertEquals("short-aaaa", answered.displayServerId());
        assertEquals("name-aaaa", answered.name());
        assertEquals("cluster-1", answered.clusterName());
        assertEquals("26.10.01", answered.version());
        assertEquals("10.0.0.4", answered.ipAddress());
        assertEquals(10, answered.lastContactSeconds());
        assertTrue(answered.responded());
        assertEquals(NodeStatus.UP, answered.status());
        assertEquals("PubSub", answered.cache().transport());
        assertEquals(Engine.ELASTICSEARCH, answered.search().engine());
        assertEquals(Boolean.TRUE, answered.assets().canWrite());

        final ClusterNodeView silent = view.nodes().get(1);
        assertEquals("bbbb", silent.serverId());
        assertFalse(silent.responded());
        assertEquals(NodeStatus.DOWN, silent.status());
        assertNull(silent.cache());
        assertNull(silent.search());
        assertNull(silent.assets());

        final ClusterNodeView brokenAssets = view.nodes().get(2);
        assertTrue(brokenAssets.responded());
        assertEquals("answered with a failed assets check: red on the legacy tab", NodeStatus.DOWN, brokenAssets.status());
    }

    /**
     * Given : a cache transport that returns no replies at all (null, as NullTransport does), and
     *         a heartbeat a little ahead of this server's clock.
     * When  : the nodes are built.
     * Then  : the call succeeds, the server is listed as not responding, and its heartbeat age is
     *         0 rather than negative.
     */
    @Test
    public void noReplies_andClockSkew() throws Exception {
        final ClusterNodesView view = nodes(List.of(server("aaaa", -5)), null, mock(CacheTransport.class));

        final ClusterNodeView node = view.nodes().get(0);
        assertFalse(node.responded());
        assertEquals(0, node.lastContactSeconds());
        assertEquals(NodeStatus.LAGGING, node.status());
    }

    /**
     * Given : the alive servers can't be read from the database.
     * When  : the nodes are built.
     * Then  : the call fails with an unchecked error, which the REST layer turns into a 500,
     *         instead of the 400 a {@code DotDataException} would give or a 200 with an empty
     *         list; its message carries no SQL, because the database error is only logged.
     */
    @Test
    public void databaseFailure_isAServerErrorWithoutTheSql() throws Exception {
        final ServerAPI serverAPI = mock(ServerAPI.class);
        when(serverAPI.getAliveServers()).thenThrow(new DotDataException("select * from sitelic failed"));

        try {
            new ClusterNodesHelper(serverAPI, () -> mock(CacheTransport.class),
                    Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)).nodes();
            fail("A database failure must not produce a list");
        } catch (final DotRuntimeException e) {
            assertFalse("the SQL must not reach the response", e.getMessage().contains("select"));
            assertNull("the database error must not be the cause the REST layer reports", e.getCause());
        }
    }
}
