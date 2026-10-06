package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeContentletIndexOperations;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeIndexAPI;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeIndiciesAPI;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeVersionedIndicesAPI;
import com.dotcms.content.index.IndexAPI;
import com.dotmarketing.util.Config;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for the error-propagation contract of
 * {@link ContentletIndexAPIImpl#delete(String)}.
 *
 * <p>Covers the fix for <a href="https://github.com/dotCMS/core/issues/36430">#36430</a>:
 * a failure in the <em>primary</em> provider must surface to the caller (re-thrown after all
 * providers were called, matching {@code PhaseRouter.writeBoolean}); a failure in the
 * <em>shadow</em> provider stays fire-and-forget. Pre-migration, primary failures propagated
 * as exceptions — the dual-write fan-out must not silently swallow them.</p>
 */
public class ContentletIndexAPIImplDeletePropagationTest {

    private static final String BARE_NAME = "working_T0";

    @After
    public void clearPhase() {
        Config.setProperty(FLAG_KEY, null);
    }

    private static void setPhase(final int ordinal) {
        Config.setProperty(FLAG_KEY, String.valueOf(ordinal));
    }

    /**
     * Given Scenario: Phase 1 (dual-write). The primary (ES) delete throws.
     * When : delete("working_T0") is called.
     * Then : the exception is re-thrown to the caller — but only AFTER the shadow provider
     *        was also called (the shadow delete must not be skipped by a primary failure).
     */
    @Test
    public void test_delete_primaryFails_exceptionPropagates_shadowStillCalled() {
        setPhase(1);
        final ThrowingIndexAPI es = new ThrowingIndexAPI(true, true);
        final ThrowingIndexAPI os = new ThrowingIndexAPI(false, true);
        final ContentletIndexAPIImpl api = buildApi(es, os);

        assertThrows(RuntimeException.class, () -> api.delete(BARE_NAME));
        assertEquals("Shadow provider must still be called when the primary fails",
                1, os.deleteCalls.size());
    }

    /**
     * Given Scenario: Phase 1. The shadow (OS) delete throws; primary succeeds.
     * When : delete("working_T0") is called.
     * Then : shadow failure is swallowed (fire-and-forget) and the primary's result returns.
     */
    @Test
    public void test_delete_shadowFails_isSwallowed_primaryResultReturned() {
        setPhase(1);
        final ThrowingIndexAPI es = new ThrowingIndexAPI(false, true);
        final ThrowingIndexAPI os = new ThrowingIndexAPI(true, true);
        final ContentletIndexAPIImpl api = buildApi(es, os);

        assertTrue(api.delete(BARE_NAME));
        assertEquals(1, es.deleteCalls.size());
    }

    /**
     * Given Scenario: Phase 1. Neither provider throws, but the primary delete is not
     * acknowledged (returns false).
     * When : delete("working_T0") is called.
     * Then : returns false — no exception, but the caller can see the delete failed.
     */
    @Test
    public void test_delete_primaryUnacknowledged_returnsFalse() {
        setPhase(1);
        final ThrowingIndexAPI es = new ThrowingIndexAPI(false, false);
        final ThrowingIndexAPI os = new ThrowingIndexAPI(false, true);
        final ContentletIndexAPIImpl api = buildApi(es, os);

        assertFalse(api.delete(BARE_NAME));
    }

    /**
     * Given Scenario: Phase 0 (ES only). The delete throws.
     * When : delete("working_T0") is called.
     * Then : the exception propagates — same contract as pre-migration behavior.
     */
    @Test
    public void test_delete_phase0_failurePropagates() {
        setPhase(0);
        final ThrowingIndexAPI es = new ThrowingIndexAPI(true, true);
        final ThrowingIndexAPI os = new ThrowingIndexAPI(false, true);
        final ContentletIndexAPIImpl api = buildApi(es, os);

        assertThrows(RuntimeException.class, () -> api.delete(BARE_NAME));
        assertEquals("OS must not be called in Phase 0", 0, os.deleteCalls.size());
    }

    /**
     * Given Scenario: Phase 1. Both providers succeed.
     * When : delete("working_T0") is called.
     * Then : returns true and both providers were called.
     */
    @Test
    public void test_delete_bothSucceed_returnsTrue() {
        setPhase(1);
        final ThrowingIndexAPI es = new ThrowingIndexAPI(false, true);
        final ThrowingIndexAPI os = new ThrowingIndexAPI(false, true);
        final ContentletIndexAPIImpl api = buildApi(es, os);

        assertTrue(api.delete(BARE_NAME));
        assertEquals(1, es.deleteCalls.size());
        assertEquals(1, os.deleteCalls.size());
    }

    // =========================================================================
    // Test rig
    // =========================================================================

    private static ContentletIndexAPIImpl buildApi(final IndexAPI esIndexAPI,
            final IndexAPI osIndexAPI) {
        return new ContentletIndexAPIImpl(
                new OpsWithIndexAPI(esIndexAPI),
                new OpsWithIndexAPI(osIndexAPI),
                new FakeIndexAPI(List.of()),
                new FakeIndiciesAPI(),
                new FakeVersionedIndicesAPI());
    }

    /** {@link IndexAPI} fake with configurable delete outcome. */
    static final class ThrowingIndexAPI extends FakeIndexAPI {

        private final boolean throwOnDelete;
        private final boolean acknowledged;
        final List<String> deleteCalls = new ArrayList<>();

        ThrowingIndexAPI(final boolean throwOnDelete, final boolean acknowledged) {
            super(List.of());
            this.throwOnDelete = throwOnDelete;
            this.acknowledged = acknowledged;
        }

        @Override
        public boolean indexExists(final String indexName) {
            return true;
        }

        @Override
        public boolean delete(final String indexName) {
            deleteCalls.add(indexName);
            if (throwOnDelete) {
                throw new RuntimeException("simulated delete failure for " + indexName);
            }
            return acknowledged;
        }
    }

    /** Provider ops wired to a specific {@link IndexAPI} fake. */
    static final class OpsWithIndexAPI extends FakeContentletIndexOperations {

        private final IndexAPI indexAPI;

        OpsWithIndexAPI(final IndexAPI indexAPI) {
            this.indexAPI = indexAPI;
        }

        @Override
        public IndexAPI indexAPI() {
            return indexAPI;
        }
    }
}
