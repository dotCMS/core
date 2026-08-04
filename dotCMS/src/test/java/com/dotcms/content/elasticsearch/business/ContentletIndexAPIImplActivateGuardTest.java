package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertThrows;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplDeleteTest.RecordingIndexAPI;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplDeleteTest.RecordingOps;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeIndexAPI;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeIndiciesAPI;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeVersionedIndicesAPI;
import com.dotcms.content.index.IndexTag;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Config;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for the {@link ContentletIndexAPIImpl#activateIndex(String)} migration guard (issue
 * #36360): during the OpenSearch migration it refuses to activate an index whose OpenSearch
 * counterpart does not exist — repointing to a missing {@code .os} index would silently diverge ES
 * from OS and break reads at Phase 3. A rollback to a migration-era index that DOES have its {@code
 * .os} copy still works, and the guard does not apply in Phase 0 or when the override flag is set.
 *
 * <p>Both engine leaves are set-backed fakes ({@link RecordingOps}/{@link RecordingIndexAPI}) so the
 * OpenSearch counterpart's existence is controlled per test; no live cluster is needed.</p>
 */
public class ContentletIndexAPIImplActivateGuardTest {

    private static final String CLUSTER_PREFIX = "cluster_test.";
    private static final String BARE = "working_T0";
    private static final String ES_PHYSICAL = CLUSTER_PREFIX + BARE;
    private static final String OS_PHYSICAL = CLUSTER_PREFIX + BARE + ".os";

    @After
    public void clear() {
        Config.setProperty(FLAG_KEY, null);
        Config.setProperty(ContentletIndexAPIImpl.ALLOW_ACTIVATE_INDEX_WITHOUT_OS_MIRROR, null);
    }

    private static void setPhase(final int ordinal) {
        Config.setProperty(FLAG_KEY, String.valueOf(ordinal));
    }

    /** ES always holds the index; OS holds it only when {@code osIndices} contains its {@code .os} name. */
    private static ContentletIndexAPIImpl apiWith(final Set<String> osIndices) {
        return new ContentletIndexAPIImpl(
                new RecordingOps(new RecordingIndexAPI(Set.of(ES_PHYSICAL)), IndexTag.ES),
                new RecordingOps(new RecordingIndexAPI(osIndices), IndexTag.OS),
                new FakeIndexAPI(List.of()),
                new FakeIndiciesAPI(),
                new FakeVersionedIndicesAPI());
    }

    // =========================================================================
    // Blocked: OS counterpart missing during the migration
    // =========================================================================

    @Test
    public void phase1_missingOsCounterpart_blocks() {
        setPhase(1);
        assertThrows(DotStateException.class, () -> apiWith(Set.of()).activateIndex(BARE));
    }

    @Test
    public void phase2_missingOsCounterpart_blocks() {
        setPhase(2);
        assertThrows(DotStateException.class, () -> apiWith(Set.of()).activateIndex(BARE));
    }

    @Test
    public void phase3_missingOsCounterpart_blocks() {
        setPhase(3);
        assertThrows(DotStateException.class, () -> apiWith(Set.of()).activateIndex(BARE));
    }

    // =========================================================================
    // Allowed: guard passes / does not apply
    // =========================================================================

    /** OS counterpart exists → a migration-era rollback proceeds normally. */
    @Test
    public void phase1_osCounterpartPresent_allowed() throws Exception {
        setPhase(1);
        apiWith(Set.of(OS_PHYSICAL)).activateIndex(BARE); // must not throw
    }

    /** Phase 0 has no OpenSearch store, so the guard does not apply. */
    @Test
    public void phase0_notGuarded() throws Exception {
        setPhase(0);
        apiWith(Set.of()).activateIndex(BARE); // must not throw
    }

    /**
     * No migration phase configured (flag removed/absent) → {@code MigrationPhase.current()} defaults
     * to Phase 0 → the guard does not apply, so a rollback works once migration is turned off. This is
     * the intended escape: set the phase to 0 (or remove the flag) to re-enable unrestricted rollback.
     */
    @Test
    public void noPhaseConfigured_notGuarded() throws Exception {
        // deliberately do NOT set FLAG_KEY — it is cleared by @After, so it is absent here
        apiWith(Set.of()).activateIndex(BARE); // must not throw
    }

    /** The override flag forces the activation even when the OS counterpart is missing. */
    @Test
    public void overrideFlag_bypassesGuard() throws Exception {
        setPhase(1);
        Config.setProperty(ContentletIndexAPIImpl.ALLOW_ACTIVATE_INDEX_WITHOUT_OS_MIRROR, "true");
        apiWith(Set.of()).activateIndex(BARE); // must not throw
    }
}
