package com.dotcms.content.index.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.content.elasticsearch.business.IndiciesInfo;
import com.dotcms.content.index.ContentletIndexOperations;
import com.dotcms.content.index.migration.ContentIndexMirrorReconciler.ContentMirrors;
import com.dotcms.content.index.migration.ContentIndexMirrorReconciler.DatabaseCounts;
import com.dotcms.content.index.IndexAPI;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotcms.content.index.domain.IndexStats;
import com.dotcms.content.index.IndexConfigHelper;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.VersionedIndicesImpl;
import com.dotcms.content.index.migration.MirrorStatus.IndexKind;
import com.dotcms.content.index.migration.MirrorStatus.Verdict;
import com.dotmarketing.util.Config;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ContentIndexMirrorReconciler} — the content (working/live) half of the
 * migration-readiness report (issue #36360). Both engine leaves are mocked and the index names are
 * fed through an injected index store, so no live cluster is needed. The mocked ES leaf strips a fixed
 * {@code cluster_x.} prefix, matching {@code removeClusterIdFromName}.
 *
 * <p>Which store supplies those names is phase-dependent, so every case pins the phase: the bulk run in
 * Phase 0 against {@code IndiciesInfo}; the {@code phase3_*} cases run against the OpenSearch store,
 * which is all that is left once the Phase 3 switchover purges the legacy pointers (issue #37635).</p>
 */
public class ContentIndexMirrorReconcilerTest extends UnitTestBase {

    private static final String PREFIX = "cluster_x.";

    private IndexAPI es;
    private IndexAPI os;
    private ContentletIndexOperations esOps;
    private ContentletIndexOperations osOps;

    private String previousPhase;

    @Before
    public void setUp() {
        // Pin the phase: which store owns the active pointers is phase-dependent, and these cases all
        // assert the pre-Phase-3 source. Without this the suite inherits whatever phase ran last in the
        // shared JVM.
        previousPhase = Config.getStringProperty(IndexConfigHelper.MigrationPhase.FLAG_KEY, null);
        Config.setProperty(IndexConfigHelper.MigrationPhase.FLAG_KEY, "0");
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

    @After
    public void tearDown() {
        Config.setProperty(IndexConfigHelper.MigrationPhase.FLAG_KEY, previousPhase);
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

    /**
     * The OpenSearch index store as Phase 3 holds it: cluster-prefixed names carrying the .os tag.
     * Unset slots are passed as {@code Optional.empty()} — the builder rejects a null String.
     */
    private static Optional<VersionedIndices> osStore(final String working, final String live) {
        return Optional.of(VersionedIndicesImpl.builder()
                .version("3.X")
                .working(Optional.ofNullable(working))
                .live(Optional.ofNullable(live))
                .build());
    }

    private ContentIndexMirrorReconciler reconciler(final IndiciesInfo info) {
        return reconciler(info, (DatabaseCounts) null);
    }

    /** Phase 3 shape: the legacy pointers are gone and the names come from the OpenSearch store. */
    private ContentIndexMirrorReconciler reconciler(final IndiciesInfo info,
            final Optional<VersionedIndices> osStore) {
        return new ContentIndexMirrorReconciler(es, os, esOps, osOps, () -> info, () -> osStore, () -> null);
    }

    /** @param expected the database denominator behind the coverage percentages, or null when absent */
    private ContentIndexMirrorReconciler reconciler(final IndiciesInfo info,
            final DatabaseCounts expected) {
        // These cases all run in a pre-Phase-3 phase, where IndiciesInfo owns the active pointers, so
        // the OpenSearch store is never consulted — the phase3_* cases cover that source.
        return new ContentIndexMirrorReconciler(es, os, esOps, osOps, () -> info,
                Optional::empty, () -> expected);
    }

    /** Both content indices present on both engines with equal counts → two IN_SYNC rows. */
    @Test
    public void workingAndLive_inSync() {
        // Build the stats maps first: nesting stats() (a when()) inside a when().thenReturn(...) would
        // trip Mockito's UnfinishedStubbingException.
        final Map<String, IndexStats> esStats = Map.of("working_1", present(), "live_1", present());
        final Map<String, IndexStats> osStats = Map.of("working_1.os", present(), "live_1.os", present());
        when(es.getIndicesStatsOrThrow()).thenReturn(esStats);
        when(os.getIndicesStatsOrThrow()).thenReturn(osStats);
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
        when(es.getIndicesStatsOrThrow()).thenReturn(esStats);
        when(os.getIndicesStatsOrThrow()).thenReturn(osStats);
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
        when(es.getIndicesStatsOrThrow()).thenReturn(esStats);
        when(os.getIndicesStatsOrThrow()).thenReturn(osStats);
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

    /**
     * Phase 3 on an installation whose Elasticsearch pointers are gone — the state left by a build
     * that still purged them at switchover. The OpenSearch store is then the only surviving source,
     * and the rows must still come back from it: resolving through {@code IndiciesInfo} alone would
     * skip every slot and produce an empty report, which downstream reads as "nothing is out of sync"
     * (issue #37635). The absent Elasticsearch copy is reported as absent, which at that point is all
     * that can honestly be said — its name is unrecoverable.
     */
    @Test
    public void phase3_sourcesTheActiveNamesFromTheOpenSearchStore() {
        Config.setProperty(IndexConfigHelper.MigrationPhase.FLAG_KEY, "3");
        final Map<String, IndexStats> osStats =
                Map.of("working_1.os", present(), "live_1.os", present());
        when(es.getIndicesStatsOrThrow()).thenReturn(Map.of());
        when(os.getIndicesStatsOrThrow()).thenReturn(osStats);
        count(osOps, "working_1", 683); count(osOps, "live_1", 682);

        final List<MirrorStatus> statuses = reconciler(null, osStore(
                PREFIX + "working_1.os", PREFIX + "live_1.os")).statuses();

        assertEquals(2, statuses.size());
        final MirrorStatus working = statuses.get(0);
        // The .os tag is stripped back off, so the row is keyed by the same logical name both engines
        // share — and the Elasticsearch side is reported as the missing copy it now is.
        assertEquals("working_1", working.indexName());
        assertEquals(IndexKind.CONTENT_WORKING, working.kind());
        assertTrue(working.os().exists());
        assertEquals(683, working.os().docCount());
        assertFalse(working.es().exists());
        assertEquals(IndexKind.CONTENT_LIVE, statuses.get(1).kind());
        assertEquals(682, statuses.get(1).os().docCount());
    }

    /**
     * The case this whole split exists for: after a Phase 3 reindex the two engines are on DIFFERENT
     * generations — OpenSearch advanced to the newly built pair, Elasticsearch still names the index
     * it held at cutover, which is still on the cluster with content in it.
     *
     * <p>Both copies must be reported, each counted on its own index. Deriving the Elasticsearch name
     * from the OpenSearch one would look for a generation Elasticsearch never had and report the copy
     * as absent — technically true of that name, and badly misleading about the engine (issue #37635).
     * The Elasticsearch stats entry below deliberately holds ONLY the old generation, so this passes
     * only if the old name was really used to look it up.</p>
     */
    @Test
    public void phase3_divergedGenerations_reportsEachEngineOnItsOwnIndex() {
        Config.setProperty(IndexConfigHelper.MigrationPhase.FLAG_KEY, "3");
        final Map<String, IndexStats> esStats = Map.of("working_OLD", present());
        final Map<String, IndexStats> osStats = Map.of("working_NEW.os", present());
        when(es.getIndicesStatsOrThrow()).thenReturn(esStats);
        when(os.getIndicesStatsOrThrow()).thenReturn(osStats);
        when(esOps.getIndexDocumentCount(PREFIX + "working_OLD")).thenReturn(600L);
        when(osOps.getIndexDocumentCount(PREFIX + "working_NEW.os")).thenReturn(683L);

        final List<MirrorStatus> statuses = new ContentIndexMirrorReconciler(es, os, esOps, osOps,
                () -> indicies(PREFIX + "working_OLD", null),
                () -> osStore(PREFIX + "working_NEW.os", null),
                () -> null).statuses();

        assertEquals(1, statuses.size());
        final MirrorStatus working = statuses.get(0);
        assertTrue("the Elasticsearch index is still there and must be seen", working.es().exists());
        assertEquals(600, working.es().docCount());
        assertTrue(working.os().exists());
        assertEquals(683, working.os().docCount());
        // Each engine's own physical name is carried through, so the split is visible in the report.
        assertEquals(PREFIX + "working_OLD", working.es().physicalName());
        assertEquals(PREFIX + "working_NEW.os", working.os().physicalName());
        // The row is named after the engine that owns the content in this phase.
        assertEquals("working_NEW", working.indexName());
        // 600 vs 683 is a real, reportable difference — exactly what a rollback would lose.
        assertEquals(Verdict.COUNT_DRIFT, working.verdict());
    }

    /** Phase 3 with an empty OpenSearch store: no rows, rather than a throw — and no failure reported. */
    @Test
    public void phase3_emptyOsStore_emptyListWithNoFailure() {
        Config.setProperty(IndexConfigHelper.MigrationPhase.FLAG_KEY, "3");

        final ContentMirrors mirrors = reconciler(null, Optional.<VersionedIndices>empty()).mirrors();

        assertTrue(mirrors.statuses().isEmpty());
        assertTrue("read fine, nothing registered — not a read failure",
                mirrors.unreadableReason().isEmpty());
    }

    /**
     * A store read that throws is a different fact from a store that holds nothing: both yield no
     * rows, but only one of them means the report knows nothing at all. Reported separately so the
     * verdict can tell the operator to fix the read instead of to reindex (issue #37635).
     */
    @Test
    public void storeReadFailure_isReportedSeparatelyFromAnEmptyStore() {
        Config.setProperty(IndexConfigHelper.MigrationPhase.FLAG_KEY, "3");
        final Supplier<Optional<VersionedIndices>> throwing = () -> {
            throw new DotRuntimeException("connection refused");
        };

        final ContentMirrors mirrors = new ContentIndexMirrorReconciler(
                es, os, esOps, osOps, () -> null, throwing, () -> null).mirrors();

        assertTrue(mirrors.statuses().isEmpty());
        assertTrue(mirrors.unreadableReason().isPresent());
        assertTrue(mirrors.unreadableReason().get().contains("connection refused"));
    }

    /** Same distinction on the pre-Phase-3 side, where the Elasticsearch store owns the pointers. */
    @Test
    public void storeReadFailure_beforePhase3_isAlsoReported() {
        Config.setProperty(IndexConfigHelper.MigrationPhase.FLAG_KEY, "1");
        final Supplier<IndiciesInfo> throwing = () -> {
            throw new DotRuntimeException("database unavailable");
        };

        final ContentMirrors mirrors = new ContentIndexMirrorReconciler(
                es, os, esOps, osOps, throwing, Optional::empty, () -> null).mirrors();

        assertTrue(mirrors.statuses().isEmpty());
        assertTrue(mirrors.unreadableReason().get().contains("database unavailable"));
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
        when(es.getIndicesStatsOrThrow()).thenReturn(esStats);
        when(os.getIndicesStatsOrThrow()).thenReturn(osStats);
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
        when(es.getIndicesStatsOrThrow()).thenReturn(esStats);
        when(os.getIndicesStatsOrThrow()).thenReturn(osStats);
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
        when(es.getIndicesStatsOrThrow()).thenReturn(esStats);
        when(os.getIndicesStatsOrThrow()).thenReturn(osStats);
        count(esOps, "working_1", 686); count(osOps, "working_1", 21);

        final MirrorStatus working = reconciler(indicies(PREFIX + "working_1", null),
                new DatabaseCounts(686L, 685L)).statuses().get(0);

        assertEquals(Long.valueOf(686), working.databaseDocCount());
        assertEquals(100.0, working.esIndexedPercent(), 0.001);
        assertEquals(3.06, working.osIndexedPercent(), 0.001);
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
        when(es.getIndicesStatsOrThrow()).thenReturn(esStats);
        when(os.getIndicesStatsOrThrow()).thenReturn(osStats);
        count(esOps, "working_1", 686); count(osOps, "working_1", 21);

        final MirrorStatus working = reconciler(indicies(PREFIX + "working_1", null)).statuses().get(0);

        assertNull(working.databaseDocCount());
        assertNull(working.esIndexedPercent());
        assertNull(working.osIndexedPercent());
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
        when(es.getIndicesStatsOrThrow()).thenReturn(esStats);
        when(os.getIndicesStatsOrThrow()).thenReturn(osStats);
        count(esOps, "working_1", 686); count(osOps, "working_1", 686);

        final MirrorStatus working = reconciler(indicies(PREFIX + "working_1", null),
                new DatabaseCounts(686L, 685L)).statuses().get(0);

        assertEquals(100.0, working.osIndexedPercent(), 0.001);
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
        when(es.getIndicesStatsOrThrow()).thenReturn(esStats);
        when(os.getIndicesStatsOrThrow()).thenReturn(osStats);
        count(esOps, "working_1", 683);
        when(osOps.getIndexDocumentCount("cluster_x.working_1.os"))
                .thenThrow(new DotRuntimeException("OS unreachable"));

        final MirrorStatus working = reconciler(indicies(PREFIX + "working_1", null)).statuses().get(0);

        assertEquals(683, working.es().docCount());
        assertEquals(-1, working.os().docCount());
        assertEquals(Verdict.COUNT_DRIFT, working.verdict());
        assertNull("an unmeasurable count has no drift percentage", working.driftPercent());
    }

    /**
     * The dangerous case: BOTH count queries fail. Both sides read {@code -1}, and left to plain
     * equality {@code -1 == -1} would look like a perfect match — the report would answer "in sync,
     * safe to advance" about a mirror it never measured. Existence still resolves here (the stats call
     * succeeds), which is exactly how it happens in the field: a search user granted
     * {@code indices:monitor/stats} but not {@code indices:data/read/count}, or a cluster too loaded to
     * answer counts.
     */
    @Test
    public void bothCountQueriesFailing_isNeverInSync() {
        final Map<String, IndexStats> esStats = Map.of("working_1", present());
        final Map<String, IndexStats> osStats = Map.of("working_1.os", present());
        when(es.getIndicesStatsOrThrow()).thenReturn(esStats);
        when(os.getIndicesStatsOrThrow()).thenReturn(osStats);
        when(esOps.getIndexDocumentCount("cluster_x.working_1"))
                .thenThrow(new DotRuntimeException("ES unreachable"));
        when(osOps.getIndexDocumentCount("cluster_x.working_1.os"))
                .thenThrow(new DotRuntimeException("OS unreachable"));

        final MirrorStatus working = reconciler(indicies(PREFIX + "working_1", null)).statuses().get(0);

        assertEquals(-1, working.es().docCount());
        assertEquals(-1, working.os().docCount());
        assertEquals("two unmeasurable counts must not read as a match",
                Verdict.COUNT_DRIFT, working.verdict());
        assertTrue("an unmeasured mirror needs attention", working.needsAttention());
    }

    /**
     * An engine that has no copy of the index reports no indexed percentage at all — not {@code 0.0}.
     * Zero would say "this engine lost all its content"; the truth is "there is no such index here",
     * which `exists` and the MISSING_COUNTERPART verdict already state. It matters most in Phase 3,
     * where every Elasticsearch copy is legitimately gone.
     */
    @Test
    public void absentCopy_reportsNoIndexedPercentage() {
        final Map<String, IndexStats> osStats = Map.of("working_1.os", present());
        when(es.getIndicesStatsOrThrow()).thenReturn(Map.of());
        when(os.getIndicesStatsOrThrow()).thenReturn(osStats);
        count(osOps, "working_1", 686);

        final MirrorStatus working = reconciler(indicies(PREFIX + "working_1", null),
                new DatabaseCounts(686L, 685L)).statuses().get(0);

        assertFalse(working.es().exists());
        assertNull("an absent copy has no completeness to report", working.esIndexedPercent());
        assertEquals(100.0, working.osIndexedPercent(), 0.001);
    }

    /**
     * Elasticsearch cannot be reached — the runbook's "retire the old cluster" step, or any outage.
     * The report must still answer for OpenSearch: before issue #37636 the stats call threw straight
     * out of the reconciler and the endpoint returned nothing but the connection error. The
     * Elasticsearch side is marked unavailable with the reason — not "missing", which would prescribe
     * a reindex over an unknown — and no count query is sent to an engine already known to be down.
     */
    @Test
    public void elasticsearchUnreachable_reportsOpenSearchSide_andMarksElasticsearchUnavailable() {
        final Map<String, IndexStats> osStats = Map.of("working_1.os", present(), "live_1.os", present());
        when(es.getIndicesStatsOrThrow()).thenThrow(
                new DotRuntimeException("elasticsearch: Name or service not known"));
        when(os.getIndicesStatsOrThrow()).thenReturn(osStats);
        count(osOps, "working_1", 100);
        count(osOps, "live_1", 50);

        final ContentMirrors mirrors =
                reconciler(indicies(PREFIX + "working_1", PREFIX + "live_1")).mirrors();

        assertTrue(mirrors.unreadableReason().isEmpty());
        assertEquals(List.of(MirrorStatus.ELASTICSEARCH),
                List.copyOf(mirrors.unreachableEngines().keySet()));
        assertTrue(mirrors.unreachableEngines().get(MirrorStatus.ELASTICSEARCH)
                .contains("Name or service not known"));
        assertEquals(2, mirrors.statuses().size());
        final MirrorStatus working = mirrors.statuses().get(0);
        assertEquals("working_1", working.indexName());
        assertEquals(Verdict.UNMEASURED, working.verdict());
        assertFalse(working.es().wasRead());
        assertTrue(working.es().unavailableReason().contains("Name or service not known"));
        assertFalse(working.es().exists());
        assertEquals(-1, working.es().docCount());
        assertEquals("cluster_x.working_1", working.es().physicalName());
        assertTrue(working.os().wasRead());
        assertTrue(working.os().exists());
        assertEquals(100, working.os().docCount());
        assertEquals(50, mirrors.statuses().get(1).os().docCount());
        assertFalse("an unknown copy must not be reported as missing",
                working.recommendation().contains("is missing"));
        verify(esOps, never()).getIndexDocumentCount(anyString());
    }

    /** The same, the other way round: OpenSearch down, Elasticsearch still reported. */
    @Test
    public void openSearchUnreachable_reportsElasticsearchSide_andMarksOpenSearchUnavailable() {
        final Map<String, IndexStats> esStats = Map.of("working_1", present(), "live_1", present());
        when(es.getIndicesStatsOrThrow()).thenReturn(esStats);
        when(os.getIndicesStatsOrThrow()).thenThrow(new DotRuntimeException("Connection refused"));
        count(esOps, "working_1", 100);
        count(esOps, "live_1", 50);

        final ContentMirrors mirrors =
                reconciler(indicies(PREFIX + "working_1", PREFIX + "live_1")).mirrors();

        assertEquals(List.of(MirrorStatus.OPENSEARCH),
                List.copyOf(mirrors.unreachableEngines().keySet()));
        final MirrorStatus working = mirrors.statuses().get(0);
        assertEquals(Verdict.UNMEASURED, working.verdict());
        assertEquals(100, working.es().docCount());
        assertFalse(working.os().wasRead());
        assertEquals(-1, working.os().docCount());
        assertEquals("cluster_x.working_1.os", working.os().physicalName());
        verify(osOps, never()).getIndexDocumentCount(anyString());
    }

    /** Both engines down: the rows are still there, both sides unavailable, nothing thrown. */
    @Test
    public void bothEnginesUnreachable_rowsStillReported() {
        when(es.getIndicesStatsOrThrow()).thenThrow(new DotRuntimeException("es down"));
        when(os.getIndicesStatsOrThrow()).thenThrow(new DotRuntimeException("os down"));

        final ContentMirrors mirrors =
                reconciler(indicies(PREFIX + "working_1", PREFIX + "live_1")).mirrors();

        assertEquals(2, mirrors.unreachableEngines().size());
        assertEquals(2, mirrors.statuses().size());
        assertEquals(Verdict.UNMEASURED, mirrors.statuses().get(0).verdict());
        assertFalse(mirrors.statuses().get(0).es().wasRead());
        assertFalse(mirrors.statuses().get(0).os().wasRead());
    }
}
