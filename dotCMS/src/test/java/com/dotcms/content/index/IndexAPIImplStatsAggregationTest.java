package com.dotcms.content.index;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.index.domain.ClusterIndexHealth;
import com.dotcms.content.index.domain.ImmutableClusterIndexHealth;
import com.dotcms.content.index.domain.ImmutableIndexStats;
import com.dotcms.content.index.domain.IndexStats;
import com.dotcms.content.index.opensearch.OSIndexAPIImpl;
import com.dotmarketing.util.Config;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link IndexAPIImpl#getIndicesStats()} and
 * {@link IndexAPIImpl#getClusterHealth()} dual-write aggregation.
 *
 * <p>Verifies that in dual-write phases (Phase 1/2) both ES and OS providers
 * are queried and their results merged — with OS winning on key collision —
 * matching the same pattern used by {@link IndexAPIImpl#getClusterHealth()}.</p>
 *
 * <p>No live cluster required: ES and OS are replaced by Mockito mocks.</p>
 */
public class IndexAPIImplStatsAggregationTest {

    private ESIndexAPI    esImpl;
    private OSIndexAPIImpl osImpl;

    @Before
    public void setUp() {
        esImpl = mock(ESIndexAPI.class);
        osImpl = mock(OSIndexAPIImpl.class);
    }

    @After
    public void tearDown() {
        Config.setProperty(FLAG_KEY, null);
    }

    // =========================================================================
    // getIndicesStats
    // =========================================================================

    /**
     * Given Scenario: Phase 0 (ES only, single write provider).
     * When : getIndicesStats() is called.
     * Then : result comes entirely from ES; OS is never consulted.
     */
    @Test
    public void test_getIndicesStats_phase0_delegatesToEsOnly() {
        Config.setProperty(FLAG_KEY, 0);
        final IndexStats esStats = statsOf("working_T0", 100, 1024);
        when(esImpl.getIndicesStats()).thenReturn(Map.of("working_T0", esStats));

        final IndexAPIImpl api = new IndexAPIImpl(esImpl, osImpl);
        final Map<String, IndexStats> result = api.getIndicesStats();

        assertEquals(1, result.size());
        assertEquals(esStats, result.get("working_T0"));
        verify(osImpl, never()).getIndicesStats();
    }

    /**
     * Given Scenario: Phase 1 (dual-write, ES reads). ES and OS have disjoint index names.
     * When : getIndicesStats() is called.
     * Then : result contains entries from both providers.
     */
    @Test
    public void test_getIndicesStats_phase1_mergesBothProviders() {
        Config.setProperty(FLAG_KEY, 1);
        when(esImpl.getIndicesStats()).thenReturn(
                Map.of("working_T0", statsOf("working_T0", 100, 1024),
                       "live_T0",    statsOf("live_T0",    200, 2048)));
        when(osImpl.getIndicesStats()).thenReturn(
                Map.of("working_T1", statsOf("working_T1", 101, 1025),
                       "live_T1",    statsOf("live_T1",    201, 2049)));

        final IndexAPIImpl api = new IndexAPIImpl(esImpl, osImpl);
        final Map<String, IndexStats> result = api.getIndicesStats();

        assertEquals(4, result.size());
        assertNotNull(result.get("working_T0"));
        assertNotNull(result.get("live_T0"));
        assertNotNull(result.get("working_T1"));
        assertNotNull(result.get("live_T1"));
    }

    /**
     * Given Scenario: Phase 1. ES and OS both report stats for the same index name
     *                 (same-timestamp bootstrap — deduplication case).
     * When : getIndicesStats() is called.
     * Then : OS entry overwrites the ES entry (OS wins on key collision).
     */
    @Test
    public void test_getIndicesStats_phase1_osWinsOnKeyCollision() {
        Config.setProperty(FLAG_KEY, 1);
        final IndexStats esStats = statsOf("working_T0", 100, 1024);
        final IndexStats osStats = statsOf("working_T0", 999, 9999);
        when(esImpl.getIndicesStats()).thenReturn(Map.of("working_T0", esStats));
        when(osImpl.getIndicesStats()).thenReturn(Map.of("working_T0", osStats));

        final IndexAPIImpl api = new IndexAPIImpl(esImpl, osImpl);
        final Map<String, IndexStats> result = api.getIndicesStats();

        assertEquals(1, result.size());
        assertEquals("OS entry must win on key collision", osStats, result.get("working_T0"));
    }

    /**
     * Given Scenario: Phase 1. OS getIndicesStats() throws an unexpected exception.
     * When : getIndicesStats() is called.
     * Then : ES entries are still returned (OS error handling is inside OSIndexAPIImpl itself —
     *        this test documents that IndexAPIImpl does not add extra try/catch).
     *        If OSIndexAPIImpl returns empty on error (as per its own implementation),
     *        the merge just omits OS entries.
     */
    @Test
    public void test_getIndicesStats_phase1_osReturnsEmptyOnError_stillReturnsEsEntries() {
        Config.setProperty(FLAG_KEY, 1);
        when(esImpl.getIndicesStats()).thenReturn(
                Map.of("working_T0", statsOf("working_T0", 100, 1024)));
        when(osImpl.getIndicesStats()).thenReturn(Map.of()); // OS degraded

        final IndexAPIImpl api = new IndexAPIImpl(esImpl, osImpl);
        final Map<String, IndexStats> result = api.getIndicesStats();

        assertEquals(1, result.size());
        assertNotNull(result.get("working_T0"));
    }

    /**
     * Given Scenario: Phase 3 (OS only, single write provider).
     * When : getIndicesStats() is called.
     * Then : result comes entirely from OS; ES is never consulted.
     */
    @Test
    public void test_getIndicesStats_phase3_delegatesToOsOnly() {
        Config.setProperty(FLAG_KEY, 3);
        final IndexStats osStats = statsOf("working_T1", 101, 1025);
        when(osImpl.getIndicesStats()).thenReturn(Map.of("working_T1", osStats));

        final IndexAPIImpl api = new IndexAPIImpl(esImpl, osImpl);
        final Map<String, IndexStats> result = api.getIndicesStats();

        assertEquals(1, result.size());
        assertEquals(osStats, result.get("working_T1"));
        verify(esImpl, never()).getIndicesStats();
    }

    // =========================================================================
    // getClusterHealth
    // =========================================================================

    /**
     * Given Scenario: Phase 0 (ES only).
     * When : getClusterHealth() is called.
     * Then : result comes entirely from ES; OS is never consulted.
     */
    @Test
    public void test_getClusterHealth_phase0_delegatesToEsOnly() {
        Config.setProperty(FLAG_KEY, 0);
        final ClusterIndexHealth esHealth = healthOf(1, 1, "GREEN");
        when(esImpl.getClusterHealth()).thenReturn(Map.of("working_T0", esHealth));

        final IndexAPIImpl api = new IndexAPIImpl(esImpl, osImpl);
        final Map<String, ClusterIndexHealth> result = api.getClusterHealth();

        assertEquals(1, result.size());
        assertEquals(esHealth, result.get("working_T0"));
        verify(osImpl, never()).getClusterHealth();
    }

    /**
     * Given Scenario: Phase 1. ES and OS have disjoint index names.
     * When : getClusterHealth() is called.
     * Then : result contains health entries from both providers.
     */
    @Test
    public void test_getClusterHealth_phase1_mergesBothProviders() {
        Config.setProperty(FLAG_KEY, 1);
        when(esImpl.getClusterHealth()).thenReturn(
                Map.of("working_T0", healthOf(1, 1, "GREEN"),
                       "live_T0",    healthOf(1, 1, "GREEN")));
        when(osImpl.getClusterHealth()).thenReturn(
                Map.of("working_T1", healthOf(1, 0, "YELLOW"),
                       "live_T1",    healthOf(1, 0, "YELLOW")));

        final IndexAPIImpl api = new IndexAPIImpl(esImpl, osImpl);
        final Map<String, ClusterIndexHealth> result = api.getClusterHealth();

        assertEquals(4, result.size());
        assertEquals("GREEN",  result.get("working_T0").status());
        assertEquals("GREEN",  result.get("live_T0").status());
        assertEquals("YELLOW", result.get("working_T1").status());
        assertEquals("YELLOW", result.get("live_T1").status());
    }

    /**
     * Given Scenario: Phase 1. Same index name in both providers (same-timestamp bootstrap).
     * When : getClusterHealth() is called.
     * Then : OS health entry overwrites the ES entry (OS wins).
     */
    @Test
    public void test_getClusterHealth_phase1_osWinsOnKeyCollision() {
        Config.setProperty(FLAG_KEY, 1);
        when(esImpl.getClusterHealth()).thenReturn(Map.of("working_T0", healthOf(1, 1, "GREEN")));
        when(osImpl.getClusterHealth()).thenReturn(Map.of("working_T0", healthOf(1, 0, "YELLOW")));

        final IndexAPIImpl api = new IndexAPIImpl(esImpl, osImpl);
        final Map<String, ClusterIndexHealth> result = api.getClusterHealth();

        assertEquals(1, result.size());
        assertEquals("OS health must win on key collision",
                "YELLOW", result.get("working_T0").status());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static IndexStats statsOf(final String name, final long count, final long bytes) {
        return ImmutableIndexStats.builder()
                .indexName(name)
                .documentCount(count)
                .sizeRaw(bytes)
                .size(bytes + "b")
                .build();
    }

    private static ClusterIndexHealth healthOf(final int shards, final int replicas,
            final String status) {
        return ImmutableClusterIndexHealth.builder()
                .numberOfShards(shards)
                .numberOfReplicas(replicas)
                .status(status)
                .build();
    }
}
