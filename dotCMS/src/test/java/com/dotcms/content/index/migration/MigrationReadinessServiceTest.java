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
        when(content.statuses()).thenReturn(List.of());
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
        assertTrue(r.phase().evaluable());
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

    /** Phase 0: not evaluable for a forward comparison, but advancing to dual-write is safe. */
    @Test
    public void phase0_notEvaluable_safeToAdvance() {
        setPhase(PHASE_0);
        when(siteSearch.statuses()).thenReturn(List.of());

        final MigrationReadiness r = service.evaluate();

        assertFalse(r.phase().evaluable());
        assertEquals(List.of("Elasticsearch"), r.phase().writeEngines());
        assertTrue(r.verdict().safeToAdvance());
    }

    /** Phase 3: not evaluable; write engine is OpenSearch only. */
    @Test
    public void phase3_notEvaluable_openSearchOnly() {
        setPhase(PHASE_3);
        when(siteSearch.statuses()).thenReturn(List.of(ss("a", true, 100, true, 100)));

        final MigrationReadiness r = service.evaluate();

        assertFalse(r.phase().evaluable());
        assertEquals("OpenSearch", r.phase().readEngine());
        assertEquals(List.of("OpenSearch"), r.phase().writeEngines());
    }
}
