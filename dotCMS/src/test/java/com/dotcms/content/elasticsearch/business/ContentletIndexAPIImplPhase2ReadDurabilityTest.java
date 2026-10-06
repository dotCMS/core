package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImpl.DualIndexBulkRequest;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeContentletIndexOperations;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeIndexAPI;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeIndiciesAPI;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeVersionedIndicesAPI;
import com.dotcms.content.index.domain.IndexBulkRequest;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import java.util.List;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for the durability of the OpenSearch write leg once OpenSearch serves reads.
 *
 * <p>Covers the Phase 2 gap found while reviewing
 * <a href="https://github.com/dotCMS/core/pull/37320">#37320</a> for
 * <a href="https://github.com/dotCMS/core/issues/37276">#37276</a>.</p>
 *
 * <h2>The gap</h2>
 * <p>ADR-0009 says a failed write to the shadow store is logged and must not impact operations.
 * That is right for <b>Phase 1</b>, where nothing reads from OpenSearch. From <b>Phase 2</b>
 * onwards {@code PhaseRouter#readProvider} serves reads from OpenSearch while writes still
 * fan out ES-primary / OS-shadow — so a removal lost on the OS leg left an orphaned document in
 * the very index being queried. That is the original defect, in the phase the migration spends
 * the longest in.</p>
 *
 * <p>The fix is scoped by <em>who serves reads</em>, not by dual-write: OS stays fire-and-forget
 * while it is invisible, and becomes durable the moment it is readable. ADR-0009's intent is
 * preserved; only its Phase 2 assumption is corrected.</p>
 */
public class ContentletIndexAPIImplPhase2ReadDurabilityTest {

    @After
    public void clearPhase() {
        Config.setProperty(FLAG_KEY, null);
    }

    private static void setPhase(final int ordinal) {
        Config.setProperty(FLAG_KEY, String.valueOf(ordinal));
    }

    /** A bulk request handle with no behaviour — only identity matters here. */
    private static final class StubBulkRequest implements IndexBulkRequest {
        @Override
        public int size() {
            return 1;
        }
    }

    /** Provider whose {@code putToIndex} either fails or records the call. */
    private static final class RecordingOperations extends FakeContentletIndexOperations {

        private final boolean fail;
        int putCalls = 0;

        RecordingOperations(final boolean fail) {
            this.fail = fail;
        }

        @Override
        public void putToIndex(final IndexBulkRequest req) {
            putCalls++;
            if (fail) {
                throw new DotRuntimeException("bulk write rejected");
            }
        }
    }

    private static ContentletIndexAPIImpl buildApi(final RecordingOperations es,
            final RecordingOperations os) {
        return new ContentletIndexAPIImpl(es, os,
                new FakeIndexAPI(List.of()), new FakeIndiciesAPI(), new FakeVersionedIndicesAPI());
    }

    /**
     * Given Scenario: Phase 1 — ES is primary and nothing reads from OpenSearch. The OS leg fails.
     * When : putToIndex fans the batch out to both providers.
     * Then : the caller is not told. ADR-0009's fire-and-forget shadow policy is preserved where
     *        it belongs, and this test is what stops the Phase 2 fix from over-reaching into it.
     */
    @Test
    public void test_phase1_shadowFailure_isStillSwallowed() {
        setPhase(1);
        final RecordingOperations es = new RecordingOperations(false);
        final RecordingOperations os = new RecordingOperations(true);

        buildApi(es, os).putToIndex(
                new DualIndexBulkRequest(new StubBulkRequest(), new StubBulkRequest()));

        assertEquals("Both legs must still be attempted", 1, es.putCalls);
        assertEquals(1, os.putCalls);
    }

    /**
     * Given Scenario: Phase 2 — OpenSearch serves reads while writes are still dual. The OS leg
     *                 fails.
     * When : putToIndex fans the batch out to both providers.
     * Then : the caller IS told, because the failure left the index that answers queries out of
     *        sync with the database. Swallowing it here is what reproduced #37276 in Phase 2.
     */
    @Test
    public void test_phase2_osFailure_reachesCaller_becauseOsServesReads() {
        setPhase(2);
        final RecordingOperations es = new RecordingOperations(false);
        final RecordingOperations os = new RecordingOperations(true);
        final ContentletIndexAPIImpl api = buildApi(es, os);

        assertThrows(RuntimeException.class, () -> api.putToIndex(
                new DualIndexBulkRequest(new StubBulkRequest(), new StubBulkRequest())));

        assertEquals("ES must still have been written before the OS failure surfaces",
                1, es.putCalls);
    }

    /**
     * Given Scenario: Phase 2, and BOTH legs fail.
     * When : putToIndex runs.
     * Then : the ES exception is the one raised. ES stays authoritative for the caller's error;
     *        demoting it would change behaviour beyond the gap being closed here.
     */
    @Test
    public void test_phase2_bothFail_esExceptionWins() {
        setPhase(2);
        final RecordingOperations es = new RecordingOperations(true);
        final RecordingOperations os = new RecordingOperations(true);
        final ContentletIndexAPIImpl api = buildApi(es, os);

        assertThrows(RuntimeException.class, () -> api.putToIndex(
                new DualIndexBulkRequest(new StubBulkRequest(), new StubBulkRequest())));

        assertEquals("The OS leg is always attempted, even when ES already failed",
                1, os.putCalls);
    }
}
