package com.dotcms.content.index.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.content.index.IndexConfigHelper;
import com.dotcms.content.index.migration.MirrorStatus.IndexKind;
import com.dotcms.content.index.migration.MirrorStatus.Verdict;
import com.dotmarketing.util.Config;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link MigrationReadinessService} — the phase-aware verdict composed from the two
 * mirror reconcilers (issue #36360). Both reconcilers are mocked, so no live cluster is needed; the
 * phase is driven through {@code Config}.
 */
public class MigrationReadinessServiceTest extends UnitTestBase {

    private static final int PHASE_0 = 0;
    private static final int PHASE_1 = 1;
    private static final int PHASE_2 = 2;
    private static final int PHASE_3 = 3;

    private String previousPhase;
    private SiteSearchMirrorReconciler siteSearch;
    private ContentIndexMirrorReconciler content;
    private MigrationReadinessService service;

    @Before
    public void setUp() {
        previousPhase = Config.getStringProperty(IndexConfigHelper.MigrationPhase.FLAG_KEY, null);
        siteSearch = mock(SiteSearchMirrorReconciler.class);
        content = mock(ContentIndexMirrorReconciler.class);
        // Default to a healthy WORKING/LIVE pair so the mandatory-content precondition is satisfied;
        // tests that exercise missing content override this explicitly.
        when(content.statuses()).thenReturn(healthyContentPair());
        service = new MigrationReadinessService(siteSearch, content, () -> "cluster_x");
    }

    @After
    public void tearDown() {
        Config.setProperty(IndexConfigHelper.MigrationPhase.FLAG_KEY, previousPhase);
    }

    private static void setPhase(final int ordinal) {
        Config.setProperty(IndexConfigHelper.MigrationPhase.FLAG_KEY, String.valueOf(ordinal));
    }

    private static MirrorStatus ss(final String name, final boolean esExists, final long esCount,
            final boolean osExists, final long osCount) {
        final Verdict verdict = MirrorStatus.verdictFor(esExists, osExists, esCount, osCount);
        return new MirrorStatus(name, IndexKind.SITE_SEARCH,
                new MirrorStatus.EngineCopy(esExists, esCount, "cluster_x." + name),
                new MirrorStatus.EngineCopy(osExists, osCount, "cluster_x." + name + ".os"),
                verdict, "advice");
    }

    /** Dual-write phase with every mirror in sync → safe to advance, nothing out of sync. */
    @Test
    public void dualWrite_allInSync_safeToAdvance() {
        setPhase(PHASE_1);
        when(siteSearch.statuses()).thenReturn(List.of(ss("a", true, 100, true, 100)));

        final MigrationReadiness r = service.evaluate();

        assertEquals("cluster_x", r.clusterId());
        assertTrue(r.phase().dualWrite());
        assertEquals("Elasticsearch", r.phase().readEngine());
        assertTrue(r.verdict().safeToAdvance());
        assertEquals(0, r.verdict().outOfSyncCount());
        assertTrue(r.verdict().blockers().isEmpty());
    }

    /** Dual-write with a missing counterpart → NOT safe to advance, one blocker, count reported. */
    @Test
    public void dualWrite_missingCounterpart_blocksAdvance() {
        setPhase(PHASE_2);
        when(siteSearch.statuses()).thenReturn(List.of(
                ss("a", true, 100, true, 100),
                ss("b", true, 50, false, 0))); // OS counterpart missing

        final MigrationReadiness r = service.evaluate();

        assertEquals("OpenSearch", r.phase().readEngine());
        assertFalse(r.verdict().safeToAdvance());
        assertEquals(1, r.verdict().outOfSyncCount());
        assertEquals(1, r.verdict().blockers().size());
        assertTrue(r.verdict().blockers().get(0).contains("'b'"));
    }

    /** Count drift above 10k is caught (the reconciler feeds exact counts) → blocks advance. */
    @Test
    public void dualWrite_countDriftAbove10k_blocksAdvance() {
        setPhase(PHASE_2);
        when(siteSearch.statuses()).thenReturn(List.of(ss("big", true, 15_000, true, 12_000)));

        final MigrationReadiness r = service.evaluate();

        assertFalse(r.verdict().safeToAdvance());
        assertEquals(1, r.verdict().outOfSyncCount());
    }

    /** OpenSearch ahead of Elasticsearch → a downgrade would lose that delta → not safe to rollback. */
    @Test
    public void osAheadOfEs_notSafeToRollback() {
        setPhase(PHASE_2);
        when(siteSearch.statuses()).thenReturn(List.of(ss("a", true, 80, true, 100)));

        final MigrationReadiness r = service.evaluate();

        assertFalse(r.verdict().safeToRollback());
    }

    /** Mirrors even → safe to rollback. */
    @Test
    public void mirrorsEven_safeToRollback() {
        setPhase(PHASE_2);
        when(siteSearch.statuses()).thenReturn(List.of(ss("a", true, 100, true, 100)));

        final MigrationReadiness r = service.evaluate();

        assertTrue(r.verdict().safeToRollback());
    }

    /** Phase 0: not a dual-write phase, but advancing to dual-write is safe. */
    @Test
    public void phase0_notDualWrite_safeToAdvance() {
        setPhase(PHASE_0);
        when(siteSearch.statuses()).thenReturn(List.of());

        final MigrationReadiness r = service.evaluate();

        assertFalse(r.phase().dualWrite());
        assertEquals(List.of("Elasticsearch"), r.phase().writeEngines());
        assertTrue(r.verdict().safeToAdvance());
    }

    /**
     * Phase 0: OpenSearch counterparts do not exist yet by design, so they must NOT inflate
     * outOfSyncCount — a non-zero count next to a "nothing to reconcile yet" summary reads as a
     * contradiction to the technician.
     */
    @Test
    public void phase0_missingOsCounterparts_notCountedAsOutOfSync() {
        setPhase(PHASE_0);
        when(content.statuses()).thenReturn(List.of(
                cc(IndexKind.CONTENT_WORKING, "working_1", true, 10, false, 0),
                cc(IndexKind.CONTENT_LIVE, "live_1", true, 5, false, 0)));
        when(siteSearch.statuses()).thenReturn(List.of(ss("a", true, 100, false, 0)));

        final MigrationReadiness r = service.evaluate();

        assertTrue(r.verdict().safeToAdvance());
        assertTrue(r.verdict().blockers().isEmpty());
        assertEquals(0, r.verdict().outOfSyncCount());
    }

    /**
     * Phase 0 does not blanket-silence the count: an OpenSearch copy that exists while Elasticsearch's
     * is gone is unexpected even before the migration starts, and still counts.
     */
    @Test
    public void phase0_unexpectedMismatch_stillCountedAsOutOfSync() {
        setPhase(PHASE_0);
        when(siteSearch.statuses()).thenReturn(List.of(
                ss("orphan", false, 0, true, 40), // OS copy with no ES source
                ss("drifted", true, 100, true, 90))); // both present, counts differ

        final MigrationReadiness r = service.evaluate();

        assertEquals(2, r.verdict().outOfSyncCount());
    }

    /**
     * An unmeasurable OpenSearch count (-1) must not read as "ES is ahead": {@code 100 < -1} is false, so
     * a naive comparison would return a false green while OpenSearch may hold more documents.
     */
    @Test
    public void unknownOsCount_notSafeToRollback() {
        setPhase(PHASE_2);
        when(siteSearch.statuses()).thenReturn(List.of(ss("a", true, 100, true, -1)));

        final MigrationReadiness r = service.evaluate();

        assertFalse(r.verdict().safeToRollback());
    }

    /** An unmeasurable Elasticsearch count is equally unsafe to roll back to. */
    @Test
    public void unknownEsCount_notSafeToRollback() {
        setPhase(PHASE_2);
        when(siteSearch.statuses()).thenReturn(List.of(ss("a", true, -1, true, -1)));

        final MigrationReadiness r = service.evaluate();

        assertFalse(r.verdict().safeToRollback());
    }

    /** Phase 3: not a dual-write phase; write engine is OpenSearch only. */
    @Test
    public void phase3_notDualWrite_openSearchOnly() {
        setPhase(PHASE_3);
        when(siteSearch.statuses()).thenReturn(List.of(ss("a", true, 100, true, 100)));

        final MigrationReadiness r = service.evaluate();

        assertFalse(r.phase().dualWrite());
        assertEquals("OpenSearch", r.phase().readEngine());
        assertEquals(List.of("OpenSearch"), r.phase().writeEngines());
    }

    /** Content is keyed by slot (WORKING/LIVE); Site Search stays an ordered list. */
    @Test
    public void content_keyedBySlot_siteSearchAsList() {
        setPhase(PHASE_2);
        when(content.statuses()).thenReturn(List.of(
                cc(IndexKind.CONTENT_WORKING, "working_1", 10),
                cc(IndexKind.CONTENT_LIVE, "live_1", 5)));
        when(siteSearch.statuses()).thenReturn(List.of(ss("sitesearch_a", true, 3, true, 3)));

        final MigrationReadiness r = service.evaluate();

        assertTrue(r.content().containsKey("WORKING"));
        assertTrue(r.content().containsKey("LIVE"));
        assertEquals("working_1", r.content().get("WORKING").indexName());
        assertEquals(1, r.siteSearch().size());
        assertEquals("sitesearch_a", r.siteSearch().get(0).indexName());
    }

    /** No active content indices at all → must NOT pass vacuously; both mandatory slots are blockers. */
    @Test
    public void dualWrite_noContentIndices_blocksAdvance() {
        setPhase(PHASE_1);
        when(content.statuses()).thenReturn(List.of());
        when(siteSearch.statuses()).thenReturn(List.of(ss("a", true, 100, true, 100)));

        final MigrationReadiness r = service.evaluate();

        assertFalse(r.verdict().safeToAdvance());
        assertEquals(2, r.verdict().blockers().size());
        assertTrue(r.verdict().blockers().stream().anyMatch(b -> b.contains("WORKING")));
        assertTrue(r.verdict().blockers().stream().anyMatch(b -> b.contains("LIVE")));
    }

    /** Phase 0 with no source content indices → cannot even start the migration. */
    @Test
    public void phase0_noContentIndices_blocksAdvance() {
        setPhase(PHASE_0);
        when(content.statuses()).thenReturn(List.of());
        when(siteSearch.statuses()).thenReturn(List.of());

        final MigrationReadiness r = service.evaluate();

        assertFalse(r.verdict().safeToAdvance());
        assertEquals(2, r.verdict().blockers().size());
    }

    /** One content slot present, the other missing → single blocker for the missing slot. */
    @Test
    public void dualWrite_oneContentSlotMissing_blocksAdvance() {
        setPhase(PHASE_2);
        when(content.statuses()).thenReturn(List.of(cc(IndexKind.CONTENT_WORKING, "working_1", 10)));
        when(siteSearch.statuses()).thenReturn(List.of());

        final MigrationReadiness r = service.evaluate();

        assertFalse(r.verdict().safeToAdvance());
        assertEquals(1, r.verdict().blockers().size());
        assertTrue(r.verdict().blockers().get(0).contains("LIVE"));
    }

    /** A content slot whose ES copy is gone is one blocker, not double-reported as MISSING_COUNTERPART. */
    @Test
    public void dualWrite_contentEsCopyMissing_singleBlockerNoDuplicate() {
        setPhase(PHASE_2);
        when(content.statuses()).thenReturn(List.of(
                cc(IndexKind.CONTENT_WORKING, "working_1", true, 10, true, 10),
                cc(IndexKind.CONTENT_LIVE, "live_1", false, 0, true, 5))); // ES copy gone
        when(siteSearch.statuses()).thenReturn(List.of());

        final MigrationReadiness r = service.evaluate();

        assertFalse(r.verdict().safeToAdvance());
        assertEquals(1, r.verdict().blockers().size());
        assertTrue(r.verdict().blockers().get(0).contains("no Elasticsearch copy"));
    }

    private static List<MirrorStatus> healthyContentPair() {
        return List.of(cc(IndexKind.CONTENT_WORKING, "working_1", 10),
                cc(IndexKind.CONTENT_LIVE, "live_1", 5));
    }

    private static MirrorStatus cc(final IndexKind kind, final String name, final long count) {
        return cc(kind, name, true, count, true, count);
    }

    private static MirrorStatus cc(final IndexKind kind, final String name, final boolean esExists,
            final long esCount, final boolean osExists, final long osCount) {
        return new MirrorStatus(name, kind,
                new MirrorStatus.EngineCopy(esExists, esCount, "cluster_x." + name),
                new MirrorStatus.EngineCopy(osExists, osCount, "cluster_x." + name + ".os"),
                MirrorStatus.verdictFor(esExists, osExists, esCount, osCount), "advice");
    }
}
