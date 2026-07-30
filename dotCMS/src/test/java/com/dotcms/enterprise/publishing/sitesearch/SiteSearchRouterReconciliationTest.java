package com.dotcms.enterprise.publishing.sitesearch;

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

    private static SiteSearchResults resultsWithTotal(final long total) {
        final SiteSearchResults results = new SiteSearchResults();
        results.setTotalResults(total);
        return results;
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
        when(esImpl.search(IDX, "*", 0, 0)).thenReturn(resultsWithTotal(100));
        when(osImpl.search(IDX, "*", 0, 0)).thenReturn(resultsWithTotal(100));

        assertTrue(router.writeMirrorsInSync(IDX));
    }

    /** Dual-write, both present but the OpenSearch twin holds fewer docs (drift) → NOT in sync. */
    @Test
    public void writeMirrorsInSync_dualWrite_countDrift_false() {
        setPhase(PHASE_1_DUAL_WRITE_ES_READS);
        when(esImpl.existsOnAllWriteEngines(IDX)).thenReturn(true);
        when(osImpl.existsOnAllWriteEngines(IDX)).thenReturn(true);
        when(esImpl.search(IDX, "*", 0, 0)).thenReturn(resultsWithTotal(100));
        when(osImpl.search(IDX, "*", 0, 0)).thenReturn(resultsWithTotal(60));

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
        verify(esImpl, never()).search(IDX, "*", 0, 0);
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
}
