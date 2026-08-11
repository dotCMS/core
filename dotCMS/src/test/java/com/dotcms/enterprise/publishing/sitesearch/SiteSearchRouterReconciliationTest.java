package com.dotcms.enterprise.publishing.sitesearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.content.index.IndexConfigHelper;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.Config;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the mirror-reconciliation routing invariants of {@link SiteSearchAPIImpl} — the
 * phase-aware fan-out router — with both engine leaves mocked, so no live ES/OS cluster is needed
 * (issue #36360). Two behaviours are pinned here:
 *
 * <ol>
 *   <li>{@link SiteSearchAPIImpl#existsOnAllWriteEngines(String)} must return {@code true} only when
 *       <em>every</em> current write engine holds the index — the gate that demotes an incremental
 *       crawl to a full rebuild when a mirror is missing.</li>
 *   <li>{@link SiteSearchAPIImpl#deleteIndex(String)} must sweep <em>both</em> engines (not only the
 *       current phase's write providers), so a twin left on a non-write engine by a phase rollback is
 *       not orphaned; the primary (read-provider) delete is authoritative and its failure propagates,
 *       while the other engine is best-effort and its failure is swallowed.</li>
 * </ol>
 *
 * <p>The migration phase is driven through {@code Config} the same way the integration router tests
 * drive it; each test restores the flag afterwards.</p>
 */
public class SiteSearchRouterReconciliationTest extends UnitTestBase {

    private static final int PHASE_0_ES_ONLY = 0;
    private static final int PHASE_1_DUAL_WRITE_ES_READS = 1;
    private static final String IDX = "sitesearch_20260101000000";

    private String previousPhase;
    private SiteSearchAPI esImpl;
    private SiteSearchAPI osImpl;
    private SiteSearchAPIImpl router;

    @Before
    public void setUp() {
        previousPhase = Config.getStringProperty(IndexConfigHelper.MigrationPhase.FLAG_KEY, null);
        esImpl = mock(SiteSearchAPI.class);
        osImpl = mock(SiteSearchAPI.class);
        router = new SiteSearchAPIImpl(esImpl, osImpl);
    }

    @After
    public void tearDown() {
        Config.setProperty(IndexConfigHelper.MigrationPhase.FLAG_KEY, previousPhase);
    }

    private static void setPhase(final int ordinal) {
        Config.setProperty(IndexConfigHelper.MigrationPhase.FLAG_KEY, String.valueOf(ordinal));
    }

    // =======================================================================
    // existsOnAllWriteEngines — the incremental-crawl gate
    // =======================================================================

    /** Dual-write phase, both engines hold the index → true; both leaves are consulted. */
    @Test
    public void existsOnAllWriteEngines_dualWrite_bothPresent_true() {
        setPhase(PHASE_1_DUAL_WRITE_ES_READS);
        when(esImpl.existsOnAllWriteEngines(IDX)).thenReturn(true);
        when(osImpl.existsOnAllWriteEngines(IDX)).thenReturn(true);

        assertTrue(router.existsOnAllWriteEngines(IDX));
        verify(esImpl).existsOnAllWriteEngines(IDX);
        verify(osImpl).existsOnAllWriteEngines(IDX);
    }

    /** Dual-write phase, the OpenSearch twin is missing → false (the gate forces a full rebuild). */
    @Test
    public void existsOnAllWriteEngines_dualWrite_osTwinMissing_false() {
        setPhase(PHASE_1_DUAL_WRITE_ES_READS);
        when(esImpl.existsOnAllWriteEngines(IDX)).thenReturn(true);
        when(osImpl.existsOnAllWriteEngines(IDX)).thenReturn(false);

        assertFalse(router.existsOnAllWriteEngines(IDX));
    }

    /** Phase 0 writes to ES only, so OpenSearch must NOT be consulted — no phase-0 OS dependency. */
    @Test
    public void existsOnAllWriteEngines_phase0_onlyConsultsEs() {
        setPhase(PHASE_0_ES_ONLY);
        when(esImpl.existsOnAllWriteEngines(IDX)).thenReturn(true);

        assertTrue(router.existsOnAllWriteEngines(IDX));
        verify(esImpl).existsOnAllWriteEngines(IDX);
        verify(osImpl, never()).existsOnAllWriteEngines(IDX);
    }

    // =======================================================================
    // writeMirrorsInSync — existence + document-count parity (content-drift gate)
    // =======================================================================

    /** Dual-write, both twins present with equal document counts → in sync. */
    @Test
    public void writeMirrorsInSync_dualWrite_equalCounts_true() {
        setPhase(PHASE_1_DUAL_WRITE_ES_READS);
        when(esImpl.existsOnAllWriteEngines(IDX)).thenReturn(true);
        when(osImpl.existsOnAllWriteEngines(IDX)).thenReturn(true);
        when(esImpl.documentCount(IDX)).thenReturn(100L);
        when(osImpl.documentCount(IDX)).thenReturn(100L);

        assertTrue(router.writeMirrorsInSync(IDX));
    }

    /** Dual-write, both present but the OpenSearch twin holds fewer docs (drift) → NOT in sync. */
    @Test
    public void writeMirrorsInSync_dualWrite_countDrift_false() {
        setPhase(PHASE_1_DUAL_WRITE_ES_READS);
        when(esImpl.existsOnAllWriteEngines(IDX)).thenReturn(true);
        when(osImpl.existsOnAllWriteEngines(IDX)).thenReturn(true);
        when(esImpl.documentCount(IDX)).thenReturn(100L);
        when(osImpl.documentCount(IDX)).thenReturn(60L);

        assertFalse(router.writeMirrorsInSync(IDX));
    }

    /**
     * Drift above the default 10k hit-count cap: the parity gate uses the exact {@code documentCount}
     * (not a capped search total), so ES 15,000 vs OS 12,000 is caught as drift (issue #36360).
     */
    @Test
    public void writeMirrorsInSync_dualWrite_driftAboveTenThousand_false() {
        setPhase(PHASE_1_DUAL_WRITE_ES_READS);
        when(esImpl.existsOnAllWriteEngines(IDX)).thenReturn(true);
        when(osImpl.existsOnAllWriteEngines(IDX)).thenReturn(true);
        when(esImpl.documentCount(IDX)).thenReturn(15_000L);
        when(osImpl.documentCount(IDX)).thenReturn(12_000L);

        assertFalse(router.writeMirrorsInSync(IDX));
    }

    /**
     * A failed count on any engine ({@code -1}) is fail-safe: treated as out of sync so the caller
     * rebuilds, rather than a both-failed {@code 0 == 0} being mistaken for "in sync" (issue #36360).
     */
    @Test
    public void writeMirrorsInSync_dualWrite_failedCount_false() {
        setPhase(PHASE_1_DUAL_WRITE_ES_READS);
        when(esImpl.existsOnAllWriteEngines(IDX)).thenReturn(true);
        when(osImpl.existsOnAllWriteEngines(IDX)).thenReturn(true);
        when(esImpl.documentCount(IDX)).thenReturn(-1L); // count query errored on ES
        when(osImpl.documentCount(IDX)).thenReturn(-1L); // ...and on OS too

        assertFalse(router.writeMirrorsInSync(IDX));
    }

    /** Dual-write, the OpenSearch twin is missing → NOT in sync (existence short-circuits counts). */
    @Test
    public void writeMirrorsInSync_dualWrite_twinMissing_false() {
        setPhase(PHASE_1_DUAL_WRITE_ES_READS);
        when(esImpl.existsOnAllWriteEngines(IDX)).thenReturn(true);
        when(osImpl.existsOnAllWriteEngines(IDX)).thenReturn(false);

        assertFalse(router.writeMirrorsInSync(IDX));
        // counts are never consulted once a twin is known to be missing
        verify(esImpl, never()).documentCount(IDX);
    }

    /** Phase 0 writes to a single engine — nothing to reconcile, so trivially in sync. */
    @Test
    public void writeMirrorsInSync_phase0_true() {
        setPhase(PHASE_0_ES_ONLY);

        assertTrue(router.writeMirrorsInSync(IDX));
        verify(osImpl, never()).existsOnAllWriteEngines(IDX);
    }

    // =======================================================================
    // deleteIndex — sweep both engines, primary authoritative, secondary best-effort
    // =======================================================================

    /**
     * Phase 0 (ES is the sole write provider), but a delete must still sweep the OpenSearch twin so a
     * leftover from a phase rollback is not orphaned. Both leaves receive the delete.
     */
    @Test
    public void deleteIndex_phase0_sweepsBothEngines() throws Exception {
        setPhase(PHASE_0_ES_ONLY);
        when(esImpl.isDefaultIndex(IDX)).thenReturn(false);

        router.deleteIndex(IDX);

        verify(esImpl).deleteIndex(IDX); // primary (read provider in phase 0)
        verify(osImpl).deleteIndex(IDX); // best-effort sweep of the other engine
    }

    /** A failure on the best-effort (non-primary) engine must NOT fail the delete. */
    @Test
    public void deleteIndex_secondaryFailure_isSwallowed() throws Exception {
        setPhase(PHASE_0_ES_ONLY);
        when(esImpl.isDefaultIndex(IDX)).thenReturn(false);
        doThrow(new RuntimeException("OS unreachable")).when(osImpl).deleteIndex(IDX);

        router.deleteIndex(IDX); // must not throw

        verify(esImpl).deleteIndex(IDX);
        verify(osImpl).deleteIndex(IDX);
    }

    /** A failure on the primary (authoritative) engine must propagate. */
    @Test
    public void deleteIndex_primaryFailure_propagates() throws Exception {
        setPhase(PHASE_0_ES_ONLY);
        when(esImpl.isDefaultIndex(IDX)).thenReturn(false);
        doThrow(new DotDataException("ES delete failed")).when(esImpl).deleteIndex(IDX);

        assertThrows(DotDataException.class, () -> router.deleteIndex(IDX));
    }

    /** The active (default) index cannot be deleted — the guard blocks before any engine is touched. */
    @Test
    public void deleteIndex_activeIndex_isRejectedBeforeAnyDelete() throws Exception {
        setPhase(PHASE_0_ES_ONLY);
        when(esImpl.isDefaultIndex(IDX)).thenReturn(true);

        assertThrows(DotStateException.class, () -> router.deleteIndex(IDX));
        verify(esImpl, never()).deleteIndex(IDX);
        verify(osImpl, never()).deleteIndex(IDX);
    }

    // =======================================================================
    // getAliasToIndexMapAllEngines — the management/display alias view (#36983)
    // =======================================================================

    private static final String OS_ONLY_IDX = "sitesearch_20260811155758";

    /**
     * The defect: in Phase 1 reads come from Elasticsearch, so an index that lives only on OpenSearch
     * (created by a crawl in Phase 3, still listed after a downgrade) had no resolvable alias and the
     * portlet rendered it blank. The management view must see both engines.
     */
    @Test
    public void aliasMapAllEngines_dualWrite_includesTheEngineThePhaseDoesNotReadFrom() {
        setPhase(PHASE_1_DUAL_WRITE_ES_READS);
        when(esImpl.getAliasToIndexMap()).thenReturn(Map.of("es-alias", IDX));
        when(osImpl.getAliasToIndexMap()).thenReturn(Map.of("os-alias", OS_ONLY_IDX));

        final Map<String, String> merged = router.getAliasToIndexMapAllEngines();

        assertEquals(2, merged.size());
        assertEquals(IDX, merged.get("es-alias"));
        assertEquals(OS_ONLY_IDX, merged.get("os-alias"));
    }

    /**
     * Mirror desync — one alias resolving to different indices on each engine. The read provider wins,
     * so the management view never contradicts what a search would actually hit.
     */
    @Test
    public void aliasMapAllEngines_conflictingAlias_readProviderWins() {
        setPhase(PHASE_1_DUAL_WRITE_ES_READS); // reads = ES
        when(esImpl.getAliasToIndexMap()).thenReturn(Map.of("shared", IDX));
        when(osImpl.getAliasToIndexMap()).thenReturn(Map.of("shared", OS_ONLY_IDX));

        assertEquals(IDX, router.getAliasToIndexMapAllEngines().get("shared"));
    }

    /** Single-provider phase: nothing to merge, and the idle engine must not be consulted. */
    @Test
    public void aliasMapAllEngines_phase0_onlyConsultsEs() {
        setPhase(PHASE_0_ES_ONLY);
        when(esImpl.getAliasToIndexMap()).thenReturn(Map.of("es-alias", IDX));

        assertEquals(Map.of("es-alias", IDX), router.getAliasToIndexMapAllEngines());
        verify(osImpl, never()).getAliasToIndexMap();
    }

    /**
     * The single-engine view stays single-engine: searches must resolve an alias against the engine
     * that will serve the query, so widening this one would be wrong.
     */
    @Test
    public void aliasMap_singleEngine_staysOnTheReadProvider() {
        setPhase(PHASE_1_DUAL_WRITE_ES_READS); // reads = ES
        when(esImpl.getAliasToIndexMap()).thenReturn(Map.of("es-alias", IDX));

        assertEquals(Map.of("es-alias", IDX), router.getAliasToIndexMap());
        verify(osImpl, never()).getAliasToIndexMap();
    }
}
