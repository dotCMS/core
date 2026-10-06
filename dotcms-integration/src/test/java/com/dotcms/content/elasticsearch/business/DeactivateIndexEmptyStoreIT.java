package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.VersionedIndicesAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.MigrationPhaseStoreBootstrap;
import com.dotcms.util.MigrationPhaseStoreBootstrap.OpenSearchPhaseStore;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Regression test for issue #36501: {@link ContentletIndexAPIImpl#deactivateIndex(String)} must
 * handle the case where the deactivated slot was the <em>last</em> populated one in the OpenSearch
 * {@link VersionedIndices} store.
 *
 * <h2>The bug</h2>
 * <p>When the last populated slot is removed, the rebuilt {@code VersionedIndices} is empty, and
 * {@code VersionedIndicesAPI.saveIndices} rejects it by contract ("At least one index must be
 * specified"). Before the fix that meant:</p>
 * <ul>
 *   <li><strong>Phase 3</strong> (OS primary, no try/catch): the exception propagated and
 *       {@code deactivateIndex} failed hard.</li>
 *   <li><strong>Phases 1/2</strong> (best-effort OS mirror): the exception was swallowed with a
 *       warning, leaving a <em>stale/dangling</em> store row that {@code initOSCatchup} would treat
 *       as authoritative on the next restart.</li>
 * </ul>
 *
 * <p>The fix removes the version row instead of saving an empty record (parity with
 * {@code clearOsStorePointer}, issue #35640).</p>
 *
 * <h2>Environment</h2>
 * <p>Runs in the single-cluster {@code opensearch-upgrade} profile. {@code deactivateIndex} only
 * touches the DB-backed index stores (IndiciesInfo + VersionedIndices) and OS index admin — no ES
 * content bulk writes — so it is unaffected by the single-cluster ES-against-OS-3.x limitation.</p>
 *
 * <pre>
 *   ./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false \
 *       -Dopensearch.upgrade.test=true -Dit.test=DeactivateIndexEmptyStoreIT
 * </pre>
 *
 * @author Fabrizzio Araya
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class DeactivateIndexEmptyStoreIT extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * PHASE 3: deactivating the last populated slot must NOT throw (before the fix it did) and must
     * leave no dangling working/live pointer in the OS store.
     */
    @Test
    public void deactivateLastIndex_phase3_doesNotThrow_andClearsStore() throws Exception {
        assertDeactivateLastIndexHandlesEmptyStore(MigrationPhase.PHASE_3_OPENSEARCH_ONLY);
    }

    /**
     * PHASE 1: deactivating the last populated slot must leave no stale OS store row (before the fix
     * the swallowed exception left the row pointing at the just-deactivated index).
     */
    @Test
    public void deactivateLastIndex_phase1_leavesNoStaleStoreRow() throws Exception {
        assertDeactivateLastIndexHandlesEmptyStore(MigrationPhase.PHASE_1_DUAL_WRITE_ES_READS);
    }

    private void assertDeactivateLastIndexHandlesEmptyStore(final MigrationPhase phase)
            throws Exception {

        final OpenSearchPhaseStore store = MigrationPhaseStoreBootstrap.openForPhase(phase);
        final ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();
        final VersionedIndicesAPI versionedIndicesAPI = APILocator.getVersionedIndicesAPI();

        final String timeStamp = String.valueOf(System.currentTimeMillis());
        final String workingIndex = IndexType.WORKING.getPrefix() + "_" + timeStamp;
        final String liveIndex = IndexType.LIVE.getPrefix() + "_" + timeStamp;

        try {
            // Create + activate a working and live index so the OS store points only at them.
            assertTrue(indexAPI.createContentIndex(workingIndex));
            indexAPI.activateIndex(workingIndex);
            assertTrue(indexAPI.createContentIndex(liveIndex));
            indexAPI.activateIndex(liveIndex);

            // Deactivate working: the store still has live, so it is non-empty — no throw.
            indexAPI.deactivateIndex(workingIndex);

            // Deactivate live: this removes the LAST populated slot. Before the fix this threw
            // (phase 3) or left a stale row (phase 1). After the fix the version row is removed.
            indexAPI.deactivateIndex(liveIndex);

            // The OS versioned store must not carry a dangling working/live pointer. When the fix
            // removed the version row, loadDefaultVersionedIndices() is empty and the assertions
            // below are vacuously satisfied.
            final Optional<VersionedIndices> after =
                    versionedIndicesAPI.loadDefaultVersionedIndices();
            after.ifPresent(vi -> {
                assertFalse("working slot must be cleared after deactivating the last index",
                        vi.working().isPresent());
                assertFalse("live slot must be cleared after deactivating the last index",
                        vi.live().isPresent());
            });

            Logger.info(this, "✅ deactivate-last-index handled cleanly under " + phase);
        } finally {
            Try.run(() -> indexAPI.delete(workingIndex));
            Try.run(() -> indexAPI.delete(liveIndex));
            store.close();
        }
    }
}
