package com.dotcms.rest.api.v1.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.dotcms.content.index.domain.SearchClusterHealth.Engine;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * Unit tests for {@link ClusterNodeMapper}: the backend status rule, and the conversion of a
 * server's status report into the typed sections of {@code GET /api/v1/cluster/nodes} (#37875).
 *
 * @author hassandotcms
 */
public class ClusterNodeMapperTest {

    /**
     * The status rule at its boundaries, with a 180 s threshold: a fresh heartbeat is at most 180
     * s old. Responding and fresh is UP, either one alone is LAGGING, neither is DOWN. A server
     * that answered but failed its assets check is DOWN whatever its heartbeat, as the legacy
     * Network tab shows it red.
     */
    @Test
    public void status_followsTheRuleAtItsBoundaries() {
        assertEquals(NodeStatus.UP, ClusterNodeMapper.status(true, false, 180, 180));
        assertEquals(NodeStatus.LAGGING, ClusterNodeMapper.status(true, false, 181, 180));
        assertEquals(NodeStatus.LAGGING, ClusterNodeMapper.status(false, false, 180, 180));
        assertEquals(NodeStatus.DOWN, ClusterNodeMapper.status(false, false, 181, 180));
        assertEquals(NodeStatus.DOWN, ClusterNodeMapper.status(true, true, 10, 180));
    }

    /** The assets check failed only when the report says so, with the key the legacy dot reads. */
    @Test
    public void assetsFailed_readsTheLegacyAssetsStatus() {
        assertEquals(true, ClusterNodeMapper.assetsFailed(Map.of("assetsStatus", "red")));
        assertEquals(false, ClusterNodeMapper.assetsFailed(Map.of("assetsStatus", "green")));
        assertEquals(false, ClusterNodeMapper.assetsFailed(Map.of()));
    }

    /**
     * A report as a current server sends it, after the JSON round trip of the pub/sub channel:
     * numbers arrive as Integer, Long or Double, and assetsCanRead keeps its historic "true/" form.
     * Every search value is distinct, so two swapped arguments in the section's constructor fail.
     */
    @Test
    public void currentReport_mapsToTypedSections() {
        final Map<String, Serializable> report = new HashMap<>();
        report.put("cacheTransportClass", "PubSub");
        report.put("clusterName", "dotcms-cluster");
        report.put("ipAddress", "10.0.0.7");   // the node IP, never the cache address
        report.put("port", -1);
        report.put("open", true);
        report.put("numberOfNodes", 2);
        report.put("receivedBytes", 1024);
        report.put("sentBytes", 4096L);
        report.put("esEngine", "OPENSEARCH");
        report.put("esClusterName", "os-cluster");
        report.put("esTimedout", false);
        report.put("esNumberOfNodes", 3);
        report.put("esNumberOfDataNodes", 4);
        report.put("esActivePrimaryShards", 5);
        report.put("esActiveShards", 6);
        report.put("esRelocatingShards", 7);
        report.put("esInitializingShards", 8);
        report.put("esUnassignedShards", 1);
        report.put("esDelayedUnasignedShards", 0);
        report.put("esNumberOfPendingTasks", 9);
        report.put("esNumberOfInFlightFetch", 10);
        report.put("esTaskMaxWaitingTimeMillis", 25);
        report.put("esTaskMaxWaitingInQueueMillis", "25ms");
        report.put("esActiveShardsPercentAsNumber", 99.5);
        report.put("assetsPath", "/data/shared/assets");
        report.put("assetsCanRead", "true/");
        report.put("assetsCanWrite", "false");

        final CacheSectionView cache = ClusterNodeMapper.cache(report);
        assertEquals("PubSub", cache.transport());
        assertEquals("dotcms-cluster", cache.clusterName());
        assertNull("the cache address is never the node IP", cache.address());
        assertNull("-1 means the transport has no port", cache.port());
        assertEquals(Integer.valueOf(2), cache.numberOfNodes());
        assertEquals(Long.valueOf(1024), cache.receivedBytes());
        assertEquals(Long.valueOf(4096), cache.sentBytes());
        assertEquals(Boolean.TRUE, cache.open());

        final SearchSectionView search = ClusterNodeMapper.search(report);
        assertEquals(Engine.OPENSEARCH, search.engine());
        assertEquals("os-cluster", search.clusterName());
        assertEquals(Boolean.FALSE, search.timedOut());
        assertEquals(Integer.valueOf(3), search.numberOfNodes());
        assertEquals(Integer.valueOf(4), search.numberOfDataNodes());
        assertEquals(Integer.valueOf(5), search.activePrimaryShards());
        assertEquals(Integer.valueOf(6), search.activeShards());
        assertEquals(Integer.valueOf(7), search.relocatingShards());
        assertEquals(Integer.valueOf(8), search.initializingShards());
        assertEquals(Integer.valueOf(1), search.unassignedShards());
        assertEquals(Integer.valueOf(0), search.delayedUnassignedShards());
        assertEquals(Integer.valueOf(9), search.numberOfPendingTasks());
        assertEquals(Integer.valueOf(10), search.numberOfInFlightFetch());
        assertEquals(Long.valueOf(25), search.taskMaxWaitingInQueueMillis());
        assertEquals(99.5, search.activeShardsPercent(), 0.0001);

        final AssetsSectionView assets = ClusterNodeMapper.assets(report);
        assertEquals("/data/shared/assets", assets.path());
        assertEquals(Boolean.TRUE, assets.canRead());
        assertEquals(Boolean.FALSE, assets.canWrite());
    }

    /**
     * A report from a server on an older version: no engine, the queue wait only as the "0s" time
     * string, and garbage where a number is expected. Values that can't be converted become null,
     * and nothing throws.
     */
    @Test
    public void olderReport_unconvertibleValuesBecomeNull() {
        final Map<String, Serializable> report = new HashMap<>();
        report.put("esClusterName", "elastic");
        report.put("esTaskMaxWaitingInQueueMillis", "0s");
        report.put("esNumberOfNodes", "not-a-number");
        report.put("assetsCanRead", "maybe");

        final SearchSectionView search = ClusterNodeMapper.search(report);
        assertNull(search.engine());
        assertNull(search.taskMaxWaitingInQueueMillis());
        assertNull(search.numberOfNodes());
        assertEquals("elastic", search.clusterName());
        assertNull(ClusterNodeMapper.assets(report).canRead());
    }

    /**
     * A server that answered but whose search check failed sends every search key as "": the
     * search section is still present, with every value null.
     */
    @Test
    public void failedSearchCheck_givesAnAllNullSearchSection() {
        final Map<String, Serializable> report = new HashMap<>();
        for (final String key : new String[]{"esEngine", "esClusterName", "esTimedout", "esNumberOfNodes",
                "esTaskMaxWaitingTimeMillis", "esActiveShardsPercentAsNumber"}) {
            report.put(key, "");
        }

        final SearchSectionView search = ClusterNodeMapper.search(report);
        assertNull(search.engine());
        assertNull(search.clusterName());
        assertNull(search.timedOut());
        assertNull(search.numberOfNodes());
        assertNull(search.taskMaxWaitingInQueueMillis());
        assertNull(search.activeShardsPercent());
    }
}
