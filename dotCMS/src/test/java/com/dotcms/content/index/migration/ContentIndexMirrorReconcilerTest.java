package com.dotcms.content.index.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.content.elasticsearch.business.IndiciesInfo;
import com.dotcms.content.index.ContentletIndexOperations;
import com.dotcms.content.index.migration.ContentIndexMirrorReconciler.ExpectedCounts;
import com.dotcms.content.index.IndexAPI;
import com.dotmarketing.exception.DotRuntimeException;
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
    private ContentletIndexOperations esOps;
    private ContentletIndexOperations osOps;

    @Before
    public void setUp() {
        es = mock(IndexAPI.class);
        os = mock(IndexAPI.class);
        esOps = mock(ContentletIndexOperations.class);
        osOps = mock(ContentletIndexOperations.class);
        when(es.removeClusterIdFromName(anyString())).thenAnswer(inv -> {
            final String n = inv.getArgument(0);
            return n.startsWith(PREFIX) ? n.substring(PREFIX.length()) : n;
        });
        // Mirror each leaf's physical-name convention: ES cluster-prefixes, OS also tags with .os.
        when(esOps.toPhysicalName(anyString())).thenAnswer(inv -> PREFIX + inv.getArgument(0));
        when(osOps.toPhysicalName(anyString())).thenAnswer(inv -> PREFIX + inv.getArgument(0) + ".os");
    }

    /**
     * A stats entry that marks an index as PRESENT. Its {@code documentCount} is deliberately a poison
     * value: existence comes from the stats snapshot but the reported count must come from a live count
     * query, because the stats counter trails a just-written document by seconds (issue #36983). If the
     * implementation ever reads the count from here again, every assertion below fails loudly instead of
     * silently reintroducing the lag.
     */
    private static IndexStats present() {
        final IndexStats s = mock(IndexStats.class);
        when(s.documentCount()).thenReturn(-999L);
        return s;
    }

    /** Stubs the live count query of one engine leaf for a logical index name. */
    private void count(final ContentletIndexOperations ops, final String logicalName, final long n) {
        when(ops.getIndexDocumentCount(ops.toPhysicalName(logicalName))).thenReturn(n);
    }

    private static IndiciesInfo indicies(final String working, final String live) {
        return new IndiciesInfo.Builder().setWorking(working).setLive(live).build();
    }

    private ContentIndexMirrorReconciler reconciler(final IndiciesInfo info) {
        return reconciler(info, null);
    }

    /** @param expected the database denominator behind the coverage percentages, or null when absent */
    private ContentIndexMirrorReconciler reconciler(final IndiciesInfo info,
            final ExpectedCounts expected) {
        return new ContentIndexMirrorReconciler(es, os, esOps, osOps, () -> info, () -> expected);
    }

    /** Both content indices present on both engines with equal counts → two IN_SYNC rows. */
    @Test
    public void workingAndLive_inSync() {
        // Build the stats maps first: nesting stats() (a when()) inside a when().thenReturn(...) would
        // trip Mockito's UnfinishedStubbingException.
        final Map<String, IndexStats> esStats = Map.of("working_1", present(), "live_1", present());
        final Map<String, IndexStats> osStats = Map.of("working_1.os", present(), "live_1.os", present());
        when(es.getIndicesStats()).thenReturn(esStats);
        when(os.getIndicesStats()).thenReturn(osStats);
        count(esOps, "working_1", 100); count(osOps, "working_1", 100);
        count(esOps, "live_1", 50);     count(osOps, "live_1", 50);

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
        final Map<String, IndexStats> esStats = Map.of("working_1", present(), "live_1", present());
        final Map<String, IndexStats> osStats = Map.of("live_1.os", present()); // working_1.os absent
        when(es.getIndicesStats()).thenReturn(esStats);
        when(os.getIndicesStats()).thenReturn(osStats);
        count(esOps, "working_1", 100);
        count(esOps, "live_1", 50); count(osOps, "live_1", 50);

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

    /** Counts diverge on the live index (exact count query, no cap) → COUNT_DRIFT. */
    @Test
    public void countDrift_onLive() {
        final Map<String, IndexStats> esStats = Map.of("working_1", present(), "live_1", present());
        final Map<String, IndexStats> osStats = Map.of("working_1.os", present(), "live_1.os", present());
        when(es.getIndicesStats()).thenReturn(esStats);
        when(os.getIndicesStats()).thenReturn(osStats);
        count(esOps, "working_1", 100); count(osOps, "working_1", 100);
        count(esOps, "live_1", 50);     count(osOps, "live_1", 40);

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
        final Map<String, IndexStats> esStats = Map.of("live_1", present());
        final Map<String, IndexStats> osStats = Map.of("live_1.os", present());
        when(es.getIndicesStats()).thenReturn(esStats);
        when(os.getIndicesStats()).thenReturn(osStats);
        count(esOps, "live_1", 50); count(osOps, "live_1", 50);

        final List<MirrorStatus> statuses = reconciler(indicies(null, PREFIX + "live_1")).statuses();

        assertEquals(1, statuses.size());
        assertEquals(IndexKind.CONTENT_LIVE, statuses.get(0).kind());
    }

    /**
     * The count is read live, not from the stats snapshot: the stats counter only advances on shard
     * refresh, so reading it would report a just-published document as missing for seconds — which a
     * support technician reads as a lost write (issue #36983). The stats entries here carry a poison
     * count, so this passes only if the reported numbers came from the count query.
     */
    @Test
    public void docCount_comesFromTheLiveCountQuery_notFromStats() {
        // Build the maps first: present() calls when(), which cannot run inside another when().
        final Map<String, IndexStats> esStats = Map.of("working_1", present());
        final Map<String, IndexStats> osStats = Map.of("working_1.os", present());
        when(es.getIndicesStats()).thenReturn(esStats);
        when(os.getIndicesStats()).thenReturn(osStats);
        count(esOps, "working_1", 683); count(osOps, "working_1", 15);

        final MirrorStatus working = reconciler(indicies(PREFIX + "working_1", null)).statuses().get(0);

        assertEquals(683, working.es().docCount());
        assertEquals(15, working.os().docCount());
        verify(esOps).getIndexDocumentCount("cluster_x.working_1");
        verify(osOps).getIndexDocumentCount("cluster_x.working_1.os");
    }

    /**
     * Coverage is each engine measured against the DATABASE, not against the other engine — the only
     * completeness signal that survives into Phase 3, where there is no second engine to diff against
     * (issue #36983). The scenario is the one observed live: the content mirror was never rebuilt.
     */
    @Test
    public void coverage_isMeasuredAgainstTheDatabase() {
        final Map<String, IndexStats> esStats = Map.of("working_1", present());
        final Map<String, IndexStats> osStats = Map.of("working_1.os", present());
        when(es.getIndicesStats()).thenReturn(esStats);
        when(os.getIndicesStats()).thenReturn(osStats);
        count(esOps, "working_1", 686); count(osOps, "working_1", 21);

        final MirrorStatus working = reconciler(indicies(PREFIX + "working_1", null),
                new ExpectedCounts(686L, 685L)).statuses().get(0);

        assertEquals(Long.valueOf(686), working.expectedDocCount());
        assertEquals(100.0, working.esCoveragePercent(), 0.001);
        assertEquals(3.06, working.osCoveragePercent(), 0.001);
        // The incomplete copy is named in the recommendation, with the fallout spelled out.
        assertTrue(working.recommendation().contains("OpenSearch copy holds 21 of the 686"));
        assertTrue(working.recommendation().contains("Site Search crawl"));
        assertFalse("the complete copy must not be flagged",
                working.recommendation().contains("Elasticsearch copy holds"));
    }

    /** No denominator (the query failed, or this is a Site Search row) → the fields are simply absent. */
    @Test
    public void coverage_absentWithoutADatabaseDenominator() {
        final Map<String, IndexStats> esStats = Map.of("working_1", present());
        final Map<String, IndexStats> osStats = Map.of("working_1.os", present());
        when(es.getIndicesStats()).thenReturn(esStats);
        when(os.getIndicesStats()).thenReturn(osStats);
        count(esOps, "working_1", 686); count(osOps, "working_1", 21);

        final MirrorStatus working = reconciler(indicies(PREFIX + "working_1", null)).statuses().get(0);

        assertNull(working.expectedDocCount());
        assertNull(working.esCoveragePercent());
        assertNull(working.osCoveragePercent());
        assertFalse(working.recommendation().contains("NOTE"));
    }

    /**
     * A complete mirror is not annotated, and coverage does not touch the verdict: the verdict states
     * the ES↔OS relationship, coverage states completeness against the database. Two separate facts.
     */
    @Test
    public void coverage_completeMirror_isNotFlagged() {
        final Map<String, IndexStats> esStats = Map.of("working_1", present());
        final Map<String, IndexStats> osStats = Map.of("working_1.os", present());
        when(es.getIndicesStats()).thenReturn(esStats);
        when(os.getIndicesStats()).thenReturn(osStats);
        count(esOps, "working_1", 686); count(osOps, "working_1", 686);

        final MirrorStatus working = reconciler(indicies(PREFIX + "working_1", null),
                new ExpectedCounts(686L, 685L)).statuses().get(0);

        assertEquals(100.0, working.osCoveragePercent(), 0.001);
        assertEquals(Verdict.IN_SYNC, working.verdict());
        assertFalse(working.recommendation().contains("NOTE"));
    }

    /**
     * A failing count query is reported as {@code -1} (the unmeasurable marker) instead of propagating:
     * an "unknown" answer for one engine still leaves a usable report, and -1 compares unequal so the
     * verdict degrades to out-of-sync rather than to a false green.
     */
    @Test
    public void countQueryFailure_isReportedAsUnmeasurable() {
        final Map<String, IndexStats> esStats = Map.of("working_1", present());
        final Map<String, IndexStats> osStats = Map.of("working_1.os", present());
        when(es.getIndicesStats()).thenReturn(esStats);
        when(os.getIndicesStats()).thenReturn(osStats);
        count(esOps, "working_1", 683);
        when(osOps.getIndexDocumentCount("cluster_x.working_1.os"))
                .thenThrow(new DotRuntimeException("OS unreachable"));

        final MirrorStatus working = reconciler(indicies(PREFIX + "working_1", null)).statuses().get(0);

        assertEquals(683, working.es().docCount());
        assertEquals(-1, working.os().docCount());
        assertEquals(Verdict.COUNT_DRIFT, working.verdict());
        assertNull("an unmeasurable count has no drift percentage", working.driftPercent());
    }
}
