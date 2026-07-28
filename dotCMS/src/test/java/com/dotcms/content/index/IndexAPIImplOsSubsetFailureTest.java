package com.dotcms.content.index;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.content.index.opensearch.OSIndexAPIImpl;
import com.dotmarketing.util.Config;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for the OS-failure isolation of the two <em>tag-dispatched</em> operations in
 * {@link IndexAPIImpl} — {@link IndexAPIImpl#optimize(List)} and
 * {@link IndexAPIImpl#flushCaches(List)} (issue #36222 follow-up).
 *
 * <h2>What regressed</h2>
 * <p>Unlike every other index-admin operation, these two do not go through {@link PhaseRouter}: they
 * split the incoming name list by vendor tag and call each provider with the names it owns, which
 * means they never inherited the router's shadow-failure handling. Any exception from the OS half
 * propagated to the caller, so an OS-only problem — the motivating one being a role scoped to
 * {@code cluster_<customer>*} rejecting names built from a different {@code DOT_DOTCMS_CLUSTER_ID}
 * with HTTP 403 — turned a maintenance action into a 500 even though the ES half had succeeded.</p>
 *
 * <h2>Contract under test</h2>
 * <ul>
 *   <li>Dual-write phase: the ES half runs, the OS failure is absorbed, and the caller sees the ES
 *       outcome (for {@code flushCaches}, shard counts covering the providers actually contacted).</li>
 *   <li>Phase 3: OS is the only store, so the failure propagates.</li>
 * </ul>
 *
 * @author Fabrizzio Araya
 */
public class IndexAPIImplOsSubsetFailureTest {

    private static final String ES_INDEX = "working_20260101000000";
    private static final String OS_INDEX = "working_20260101000000.os";

    @After
    public void clearPhase() {
        Config.setProperty(FLAG_KEY, null);
    }

    private static void setPhase(final MigrationPhase phase) {
        Config.setProperty(FLAG_KEY, String.valueOf(phase.ordinal()));
    }

    /** The 403 an index-scoped OS role raises for names outside its pattern. */
    private static RuntimeException forbidden(final String operation) {
        return new RuntimeException("Failed to " + operation + " OpenSearch indices",
                new RuntimeException("OpenSearch exception [type=security_exception,"
                        + " reason=no permissions for [indices:admin/forcemerge] and User"
                        + " [name=dotcms-es-user, roles=[dotcms-role]]] status: 403"));
    }

    /**
     * Given : Phase 1, an optimize over one ES and one OS index, and an OS provider that rejects
     *         the force-merge with 403.
     * When  : optimize() runs.
     * Then  : the ES index is still optimized and the call reports the ES outcome instead of
     *         failing — the OS rejection is logged, not propagated.
     */
    @Test
    public void optimize_phase1_osForbidden_keepsEsResult() {
        setPhase(MigrationPhase.PHASE_1_DUAL_WRITE_ES_READS);

        final ESIndexAPI esImpl = mock(ESIndexAPI.class);
        final OSIndexAPIImpl osImpl = mock(OSIndexAPIImpl.class);
        when(esImpl.optimize(anyList())).thenReturn(true);
        when(osImpl.optimize(anyList())).thenThrow(forbidden("optimize"));

        final boolean result = new IndexAPIImpl(esImpl, osImpl)
                .optimize(List.of(ES_INDEX, OS_INDEX));

        assertTrue("An OS force-merge rejection must not fail an optimize whose ES half succeeded",
                result);
        verify(esImpl).optimize(List.of(ES_INDEX));
        verify(osImpl).optimize(List.of(OS_INDEX));
    }

    /**
     * Given : Phase 3 (OS only) and an OS provider that rejects the force-merge with 403.
     * When  : optimize() runs.
     * Then  : the failure propagates — there is no ES half to fall back to.
     */
    @Test
    public void optimize_phase3_osForbidden_propagates() {
        setPhase(MigrationPhase.PHASE_3_OPENSEARCH_ONLY);

        final OSIndexAPIImpl osImpl = mock(OSIndexAPIImpl.class);
        when(osImpl.optimize(anyList())).thenThrow(forbidden("optimize"));

        try {
            new IndexAPIImpl(mock(ESIndexAPI.class), osImpl).optimize(List.of(OS_INDEX));
            fail("In Phase 3 OS is the only store — the failure must propagate");
        } catch (final RuntimeException expected) {
            assertTrue("Expected the OS failure, got: " + expected.getMessage(),
                    expected.getMessage().contains("OpenSearch indices"));
        }
    }

    /**
     * Given : Phase 2, a cache flush over one ES and one OS index, and an OS provider that rejects
     *         the flush with 403.
     * When  : flushCaches() runs.
     * Then  : the ES shard counts are still returned (the OS indices are simply not represented),
     *         instead of the whole flush failing.
     */
    @Test
    public void flushCaches_phase2_osForbidden_returnsEsShardCounts() {
        setPhase(MigrationPhase.PHASE_2_DUAL_WRITE_OS_READS);

        final ESIndexAPI esImpl = mock(ESIndexAPI.class);
        final OSIndexAPIImpl osImpl = mock(OSIndexAPIImpl.class);
        when(esImpl.flushCaches(List.of(ES_INDEX)))
                .thenReturn(Map.of("successfulShards", 4, "failedShards", 0));
        when(osImpl.flushCaches(anyList())).thenThrow(forbidden("flush"));

        final Map<String, Integer> result = new IndexAPIImpl(esImpl, osImpl)
                .flushCaches(List.of(ES_INDEX, OS_INDEX));

        assertEquals("Only the ES shards were flushed, and they must still be reported",
                Integer.valueOf(4), result.get("successfulShards"));
        assertEquals(Integer.valueOf(0), result.get("failedShards"));
        verify(osImpl).flushCaches(List.of(OS_INDEX));
    }

    /**
     * Given : Phase 3 (OS only) and an OS provider that rejects the flush with 403.
     * When  : flushCaches() runs.
     * Then  : the failure propagates.
     */
    @Test
    public void flushCaches_phase3_osForbidden_propagates() {
        setPhase(MigrationPhase.PHASE_3_OPENSEARCH_ONLY);

        final OSIndexAPIImpl osImpl = mock(OSIndexAPIImpl.class);
        when(osImpl.flushCaches(anyList())).thenThrow(forbidden("flush"));

        try {
            new IndexAPIImpl(mock(ESIndexAPI.class), osImpl).flushCaches(List.of(OS_INDEX));
            fail("In Phase 3 OS is the only store — the failure must propagate");
        } catch (final RuntimeException expected) {
            assertTrue("Expected the OS failure, got: " + expected.getMessage(),
                    expected.getMessage().contains("OpenSearch indices"));
        }
    }
}
