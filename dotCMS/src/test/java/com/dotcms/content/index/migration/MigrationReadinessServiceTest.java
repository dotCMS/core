package com.dotcms.content.index.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.content.index.IndexConfigHelper;
import com.dotcms.content.index.migration.ContentIndexMirrorReconciler.ContentMirrors;
import com.dotcms.content.index.migration.MirrorStatus.IndexKind;
import com.dotcms.content.index.migration.MirrorStatus.Verdict;
import com.dotmarketing.util.Config;
import java.util.List;
import java.util.Optional;
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
        stubContent(healthyContentPair());
        service = new MigrationReadinessService(siteSearch, content, () -> "cluster_x");
    }

    @After
    public void tearDown() {
        Config.setProperty(IndexConfigHelper.MigrationPhase.FLAG_KEY, previousPhase);
    }

    /**
     * Stubs the content reconciler's contract. The service reads {@code mirrors()}, not
     * {@code statuses()}, because an empty list alone cannot say whether the store was read and held
     * nothing or could not be read at all — see {@link ContentMirrors}.
     */
    private void stubContent(final List<MirrorStatus> statuses) {
        when(content.mirrors()).thenReturn(new ContentMirrors(statuses, Optional.empty()));
    }

    /** Stubs the reconciler reporting that the index store could not be read at all. */
    private void stubContentUnreadable(final String reason) {
        when(content.mirrors()).thenReturn(new ContentMirrors(List.of(), Optional.of(reason)));
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
        stubContent(List.of(
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

    /**
     * Phase 3 with nothing measured must not read as healthy. After a Phase 3 full reindex the legacy
     * Elasticsearch pointers are deleted, and a reconciler that cannot resolve the active content pair
     * emits no rows at all — on which every field of the verdict would otherwise assert a clean bill of
     * health: {@code outOfSyncCount} reads 0 because nothing was counted, and both booleans read safe
     * because no row could contradict them (issue #37635). The rollback verdict is the dangerous one:
     * a downgrade routes reads back to the engine whose pointers are gone.
     */
    @Test
    public void phase3_nothingMeasured_isNotReportedAsHealthy() {
        setPhase(PHASE_3);
        stubContent(List.of());
        when(siteSearch.statuses()).thenReturn(List.of());

        final MigrationReadiness r = service.evaluate();

        assertTrue(r.content().isEmpty());
        assertFalse(r.verdict().safeToAdvance());
        assertFalse(r.verdict().safeToRollback());
        assertEquals(2, r.verdict().blockers().size()); // one per unresolved slot
        // outOfSyncCount is still 0 here — it counts drift among existing rows, of which there are
        // none. What must not happen is that 0 reading as an all-clear on its own, so the summary has
        // to say outright that nothing was measured.
        assertEquals(0, r.verdict().outOfSyncCount());
        assertTrue(r.verdict().summary().contains("measured nothing"));
    }

    /**
     * Phase 3 measures the mandatory pair against OpenSearch, not Elasticsearch: past the final phase
     * Elasticsearch is decommissioned, so a missing Elasticsearch copy is the expected state and must
     * not be reported as a blocker — while a missing OpenSearch copy is now the fatal one.
     */
    @Test
    public void phase3_openSearchIsTheMandatoryEngine() {
        setPhase(PHASE_3);
        stubContent(List.of(
                cc(IndexKind.CONTENT_WORKING, "working_1", false, 0, true, 10),
                cc(IndexKind.CONTENT_LIVE, "live_1", false, 0, true, 5)));
        when(siteSearch.statuses()).thenReturn(List.of());

        final MigrationReadiness r = service.evaluate();

        assertTrue("an absent Elasticsearch copy is expected in Phase 3",
                r.verdict().blockers().isEmpty());
        assertTrue(r.verdict().safeToAdvance());
        // ...and it is not counted either: a decommissioned Elasticsearch is the steady state here,
        // so counting it would pin a healthy install at outOfSyncCount = 2 forever and make the
        // field useless as a health gauge. The absent copy is still reported, by the two fields that
        // own that fact — the rollback verdict and the summary's downgrade warning.
        assertEquals("a healthy Phase 3 install has nothing out of sync",
                0, r.verdict().outOfSyncCount());
        assertFalse("a downgrade is still unsafe: Elasticsearch has no copy to read from",
                r.verdict().safeToRollback());
        assertTrue(r.verdict().summary().contains("WARNING"));
    }

    /**
     * The common Phase 3 state: the switchover keeps the Elasticsearch pointers, so while that cluster
     * is reachable its copy is measured — frozen at cutover, and behind OpenSearch as soon as anything
     * is edited. That lag is expected and must not be counted as out of sync; it is still what makes a
     * downgrade unsafe, so the rollback verdict and the summary warning keep reporting it.
     */
    @Test
    public void phase3_frozenElasticsearchCopyBehindOpenSearch_isNotCountedAsOutOfSync() {
        setPhase(PHASE_3);
        stubContent(List.of(
                cc(IndexKind.CONTENT_WORKING, "working_1", true, 8, true, 10),
                cc(IndexKind.CONTENT_LIVE, "live_1", true, 4, true, 5)));
        when(siteSearch.statuses()).thenReturn(List.of());

        final MigrationReadiness r = service.evaluate();

        assertTrue(r.verdict().blockers().isEmpty());
        assertTrue(r.verdict().safeToAdvance());
        assertEquals("a frozen Elasticsearch copy is the Phase 3 steady state",
                0, r.verdict().outOfSyncCount());
        assertFalse("a downgrade would hide what was written since cutover",
                r.verdict().safeToRollback());
        assertTrue(r.verdict().summary().contains("WARNING"));
    }

    /**
     * The other direction is just as expected: content deleted or unpublished after cutover leaves the
     * frozen Elasticsearch copy holding MORE documents than OpenSearch. Comparing the two engines cannot
     * tell that apart from OpenSearch losing documents, so in Phase 3 neither direction is counted — the
     * database comparison below is what catches an incomplete OpenSearch copy.
     */
    @Test
    public void phase3_frozenElasticsearchCopyAheadOfOpenSearch_isNotCountedAsOutOfSync() {
        setPhase(PHASE_3);
        stubContent(List.of(
                cc(IndexKind.CONTENT_WORKING, "working_1", true, 10, true, 8),
                cc(IndexKind.CONTENT_LIVE, "live_1", true, 6, true, 5)));
        when(siteSearch.statuses()).thenReturn(List.of());

        final MigrationReadiness r = service.evaluate();

        assertEquals("deletions after cutover are the Phase 3 steady state",
                0, r.verdict().outOfSyncCount());
    }

    /** An unmeasurable count on either engine is an unknown, not a lag, and stays counted. */
    @Test
    public void phase3_unmeasurableCount_isStillCountedAsOutOfSync() {
        setPhase(PHASE_3);
        stubContent(List.of(
                cc(IndexKind.CONTENT_WORKING, "working_1", true, -1, true, 8),
                cc(IndexKind.CONTENT_LIVE, "live_1", true, 5, true, -1)));
        when(siteSearch.statuses()).thenReturn(List.of());

        final MigrationReadiness r = service.evaluate();

        assertEquals(2, r.verdict().outOfSyncCount());
    }

    /**
     * With Elasticsearch frozen, the database is what OpenSearch is measured against in Phase 3: a copy
     * holding well under what the database says it should (a reindex that never finished) is counted —
     * even when the frozen Elasticsearch copy happens to match it, so the two engines alone read in sync.
     */
    @Test
    public void phase3_openSearchIncompleteAgainstDatabase_isCountedAsOutOfSync() {
        setPhase(PHASE_3);
        stubContent(List.of(
                cc(IndexKind.CONTENT_WORKING, "working_1", true, 50, true, 50, 100L),
                cc(IndexKind.CONTENT_LIVE, "live_1", false, 0, true, 40, 100L)));
        when(siteSearch.statuses()).thenReturn(List.of());

        final MigrationReadiness r = service.evaluate();

        assertEquals(2, r.verdict().outOfSyncCount());
        assertTrue("the summary says what the count means and what to do",
                r.verdict().summary().contains("run a full reindex"));
    }

    /**
     * The database comparison uses the same tolerance as the report's "incomplete" note, so a copy that
     * holds nearly everything — a handful of documents short while indexing catches up — is not counted.
     */
    @Test
    public void phase3_openSearchNearlyCompleteAgainstDatabase_isNotCounted() {
        setPhase(PHASE_3);
        stubContent(List.of(
                cc(IndexKind.CONTENT_WORKING, "working_1", true, 80, true, 99, 100L),
                cc(IndexKind.CONTENT_LIVE, "live_1", true, 80, true, 100, 100L)));
        when(siteSearch.statuses()).thenReturn(List.of());

        final MigrationReadiness r = service.evaluate();

        assertEquals(0, r.verdict().outOfSyncCount());
    }

    /** The same state with the OpenSearch copy gone — that one blocks. */
    @Test
    public void phase3_missingOpenSearchCopy_blocks() {
        setPhase(PHASE_3);
        stubContent(List.of(
                cc(IndexKind.CONTENT_WORKING, "working_1", false, 0, false, 0),
                cc(IndexKind.CONTENT_LIVE, "live_1", false, 0, true, 5)));
        when(siteSearch.statuses()).thenReturn(List.of());

        final MigrationReadiness r = service.evaluate();

        assertFalse(r.verdict().safeToAdvance());
        assertEquals(1, r.verdict().blockers().size());
        assertTrue(r.verdict().blockers().get(0).contains("no OpenSearch copy"));
    }

    /**
     * A store that could not be read is not a store with no indices. Both leave the report with no
     * rows, but the operator action is the opposite — fix the read versus reindex — so the blocker
     * must name the read failure and must not prescribe a reindex on evidence it does not have.
     */
    @Test
    public void unreadableStore_saysSo_ratherThanPrescribingAReindex() {
        setPhase(PHASE_2);
        stubContentUnreadable("the Elasticsearch index store could not be read: connection refused");
        when(siteSearch.statuses()).thenReturn(List.of());

        final MigrationReadiness r = service.evaluate();

        assertFalse(r.verdict().safeToAdvance());
        assertFalse(r.verdict().safeToRollback());
        assertEquals("one blocker for the read failure, not one per unresolved slot",
                1, r.verdict().blockers().size());
        final String blocker = r.verdict().blockers().get(0);
        assertTrue("names the underlying failure", blocker.contains("connection refused"));
        assertTrue("warns against acting on it", blocker.contains("do NOT reindex"));
    }

    /** The same shape in the final phase: still one read-failure blocker, still not green. */
    @Test
    public void phase3_unreadableStore_isNotGreen() {
        setPhase(PHASE_3);
        stubContentUnreadable("the OpenSearch index store could not be read: timeout");
        when(siteSearch.statuses()).thenReturn(List.of());

        final MigrationReadiness r = service.evaluate();

        assertFalse(r.verdict().safeToAdvance());
        assertFalse(r.verdict().safeToRollback());
        assertEquals(1, r.verdict().blockers().size());
        assertTrue(r.verdict().blockers().get(0).contains("timeout"));
    }

    /** Content is keyed by slot (WORKING/LIVE); Site Search stays an ordered list. */
    @Test
    public void content_keyedBySlot_siteSearchAsList() {
        setPhase(PHASE_2);
        stubContent(List.of(
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
        stubContent(List.of());
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
        stubContent(List.of());
        when(siteSearch.statuses()).thenReturn(List.of());

        final MigrationReadiness r = service.evaluate();

        assertFalse(r.verdict().safeToAdvance());
        assertEquals(2, r.verdict().blockers().size());
    }

    /** One content slot present, the other missing → single blocker for the missing slot. */
    @Test
    public void dualWrite_oneContentSlotMissing_blocksAdvance() {
        setPhase(PHASE_2);
        stubContent(List.of(cc(IndexKind.CONTENT_WORKING, "working_1", 10)));
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
        stubContent(List.of(
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
        return cc(kind, name, esExists, esCount, osExists, osCount, null);
    }

    private static MirrorStatus cc(final IndexKind kind, final String name, final boolean esExists,
            final long esCount, final boolean osExists, final long osCount,
            final Long databaseDocCount) {
        return new MirrorStatus(name, kind,
                new MirrorStatus.EngineCopy(esExists, esCount, "cluster_x." + name),
                new MirrorStatus.EngineCopy(osExists, osCount, "cluster_x." + name + ".os"),
                MirrorStatus.verdictFor(esExists, osExists, esCount, osCount), "advice",
                databaseDocCount);
    }
}
