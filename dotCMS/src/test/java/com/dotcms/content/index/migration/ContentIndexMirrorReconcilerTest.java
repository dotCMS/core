package com.dotcms.content.index.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.content.elasticsearch.business.IndiciesInfo;
import com.dotcms.content.index.IndexAPI;
import com.dotcms.content.index.domain.IndexStats;
import com.dotcms.content.index.migration.MirrorStatus.IndexKind;
import com.dotcms.content.index.migration.MirrorStatus.Verdict;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ContentIndexMirrorReconciler} — the content (working/live) half of the
 * migration-readiness report (issue #36360). Both engine leaves are mocked and the index names are
 * fed through an injected {@code IndiciesInfo}, so no live cluster is needed. The mocked ES leaf
 * strips a fixed {@code cluster_x.} prefix, matching {@code removeClusterIdFromName}.
 */
public class ContentIndexMirrorReconcilerTest extends UnitTestBase {

    private static final String PREFIX = "cluster_x.";

    private IndexAPI es;
    private IndexAPI os;

    @Before
    public void setUp() {
        es = mock(IndexAPI.class);
        os = mock(IndexAPI.class);
        when(es.removeClusterIdFromName(anyString())).thenAnswer(inv -> {
            final String n = inv.getArgument(0);
            return n.startsWith(PREFIX) ? n.substring(PREFIX.length()) : n;
        });
    }

    private static IndexStats stats(final long count) {
        final IndexStats s = mock(IndexStats.class);
        when(s.documentCount()).thenReturn(count);
        return s;
    }

    private static IndiciesInfo indicies(final String working, final String live) {
        return new IndiciesInfo.Builder().setWorking(working).setLive(live).build();
    }

    private ContentIndexMirrorReconciler reconciler(final IndiciesInfo info) {
        return new ContentIndexMirrorReconciler(es, os, () -> info);
    }

    /** Both content indices present on both engines with equal counts → two IN_SYNC rows. */
    @Test
    public void workingAndLive_inSync() {
        // Build the stats maps first: nesting stats() (a when()) inside a when().thenReturn(...) would
        // trip Mockito's UnfinishedStubbingException.
        final Map<String, IndexStats> esStats = Map.of("working_1", stats(100), "live_1", stats(50));
        final Map<String, IndexStats> osStats = Map.of("working_1.os", stats(100), "live_1.os", stats(50));
        when(es.getIndicesStats()).thenReturn(esStats);
        when(os.getIndicesStats()).thenReturn(osStats);

        final List<MirrorStatus> statuses =
                reconciler(indicies(PREFIX + "working_1", PREFIX + "live_1")).statuses();

        assertEquals(2, statuses.size());
        final MirrorStatus working = statuses.get(0);
        assertEquals(IndexKind.CONTENT_WORKING, working.kind());
        assertEquals("working_1", working.indexName()); // logical: cluster prefix stripped, no .os
        assertEquals(Verdict.IN_SYNC, working.verdict());
        assertEquals(100, working.es().docCount());
        assertEquals(100, working.os().docCount());
        // full physical names as stored: ES cluster-prefixed, OS additionally .os-tagged
        assertEquals("cluster_x.working_1", working.es().physicalName());
        assertEquals("cluster_x.working_1.os", working.os().physicalName());
        assertEquals(IndexKind.CONTENT_LIVE, statuses.get(1).kind());
        assertEquals(Verdict.IN_SYNC, statuses.get(1).verdict());
    }

    /** The OpenSearch counterpart of the working index is missing → MISSING_COUNTERPART. */
    @Test
    public void missingOsCounterpart_onWorking() {
        final Map<String, IndexStats> esStats = Map.of("working_1", stats(100), "live_1", stats(50));
        final Map<String, IndexStats> osStats = Map.of("live_1.os", stats(50)); // working_1.os absent
        when(es.getIndicesStats()).thenReturn(esStats);
        when(os.getIndicesStats()).thenReturn(osStats);

        final List<MirrorStatus> statuses =
                reconciler(indicies(PREFIX + "working_1", PREFIX + "live_1")).statuses();

        final MirrorStatus working = statuses.get(0);
        assertEquals(Verdict.MISSING_COUNTERPART, working.verdict());
        assertTrue(working.es().exists());
        assertFalse(working.os().exists());
        assertTrue(working.needsAttention());
        assertEquals(-100.0, working.driftPercent(), 0.001); // mirror empty vs original of 100 → -100%
        assertTrue(working.recommendation().contains("OpenSearch"));
    }

    /** Counts diverge on the live index (exact stats, no cap) → COUNT_DRIFT. */
    @Test
    public void countDrift_onLive() {
        final Map<String, IndexStats> esStats = Map.of("working_1", stats(100), "live_1", stats(50));
        final Map<String, IndexStats> osStats = Map.of("working_1.os", stats(100), "live_1.os", stats(40));
        when(es.getIndicesStats()).thenReturn(esStats);
        when(os.getIndicesStats()).thenReturn(osStats);

        final List<MirrorStatus> statuses =
                reconciler(indicies(PREFIX + "working_1", PREFIX + "live_1")).statuses();

        final MirrorStatus live = statuses.get(1);
        assertEquals(IndexKind.CONTENT_LIVE, live.kind());
        assertEquals(Verdict.COUNT_DRIFT, live.verdict());
        assertEquals(50, live.es().docCount());
        assertEquals(40, live.os().docCount());
        // mirror 10 docs behind the original of 50 → -20%
        assertEquals(-20.0, live.driftPercent(), 0.001);
    }

    /** A null IndiciesInfo (could not be loaded) yields no rows rather than throwing. */
    @Test
    public void nullIndicies_emptyList() {
        assertTrue(reconciler(null).statuses().isEmpty());
    }

    /** An unset working/live slot is skipped (no row, no NPE). */
    @Test
    public void unsetSlot_skipped() {
        final Map<String, IndexStats> esStats = Map.of("live_1", stats(50));
        final Map<String, IndexStats> osStats = Map.of("live_1.os", stats(50));
        when(es.getIndicesStats()).thenReturn(esStats);
        when(os.getIndicesStats()).thenReturn(osStats);

        final List<MirrorStatus> statuses = reconciler(indicies(null, PREFIX + "live_1")).statuses();

        assertEquals(1, statuses.size());
        assertEquals(IndexKind.CONTENT_LIVE, statuses.get(0).kind());
    }
}
