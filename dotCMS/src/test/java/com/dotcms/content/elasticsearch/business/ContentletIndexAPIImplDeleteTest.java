package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeContentletIndexOperations;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeIndexAPI;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeIndiciesAPI;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseTest.FakeVersionedIndicesAPI;
import com.dotcms.content.index.IndexAPI;
import com.dotcms.content.index.IndexTag;
import com.dotmarketing.util.Config;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for {@link ContentletIndexAPIImpl#delete(String)} with divergent index names
 * across the two providers (the normal state after a migration catchup deployment).
 *
 * <p>Covers the fix for <a href="https://github.com/dotCMS/core/issues/36423">#36423</a>:</p>
 * <ul>
 *   <li>An explicitly {@code .os}-tagged name is tag-dispatched to the OS provider only,
 *       never fanned out to ES (where the physical {@code .os} name can never exist).</li>
 *   <li>For bare names, the shadow leg skips names its engine does not hold instead of
 *       attempting a delete that is guaranteed to miss.</li>
 *   <li>Real shadow failures remain fire-and-forget.</li>
 * </ul>
 */
public class ContentletIndexAPIImplDeleteTest {

    private static final String CLUSTER_PREFIX = "cluster_test.";
    private static final String BARE_NAME = "working_T0";
    private static final String ES_PHYSICAL = CLUSTER_PREFIX + BARE_NAME;
    private static final String OS_PHYSICAL = CLUSTER_PREFIX + BARE_NAME + ".os";

    @After
    public void clearPhase() {
        Config.setProperty(FLAG_KEY, null);
    }

    private static void setPhase(final int ordinal) {
        Config.setProperty(FLAG_KEY, String.valueOf(ordinal));
    }

    // =========================================================================
    // Tag-dispatch: .os names go to the OS provider only
    // =========================================================================

    /**
     * Given Scenario: Phase 1 (dual-write). Caller deletes an explicitly {@code .os}-tagged
     * name (e.g. an orphaned OS index left behind by a divergent catchup).
     * When : delete("working_T0.os") is called.
     * Then : only the OS provider's delete is invoked; ES is never asked to delete a
     *        physical ".os" name it can never hold.
     */
    @Test
    public void test_delete_osTaggedName_tagDispatchedToOsOnly() {
        setPhase(1);
        final Rig rig = new Rig(Set.of(ES_PHYSICAL), Set.of(OS_PHYSICAL));

        final boolean result = rig.api.delete(IndexTag.OS.tag(BARE_NAME));

        assertTrue("OS delete must succeed", result);
        assertEquals("ES provider must not be called for a .os-tagged name",
                List.of(), rig.esIndexAPI.deleted);
        assertEquals(List.of(OS_PHYSICAL), rig.osIndexAPI.deleted);
    }

    /**
     * Given Scenario: Phase 0 (migration not started). A leftover {@code .os} index is
     * being cleaned up by name.
     * When : delete("working_T0.os") is called.
     * Then : the delete is tag-dispatched to OS regardless of phase (per the IndexTag
     *        contract, the tag carries routing intent and skips phase logic).
     */
    @Test
    public void test_delete_osTaggedName_phase0_stillGoesToOs() {
        setPhase(0);
        final Rig rig = new Rig(Set.of(ES_PHYSICAL), Set.of(OS_PHYSICAL));

        final boolean result = rig.api.delete(IndexTag.OS.tag(BARE_NAME));

        assertTrue(result);
        assertEquals(List.of(), rig.esIndexAPI.deleted);
        assertEquals(List.of(OS_PHYSICAL), rig.osIndexAPI.deleted);
    }

    /**
     * Given Scenario: Phase 1. The .os-tagged name does not exist in OS either
     * (delete throws index-not-found).
     * When : delete("working_T0.os") is called.
     * Then : returns false; no exception propagates.
     */
    @Test
    public void test_delete_osTaggedName_missingInOs_returnsFalse() {
        setPhase(1);
        final Rig rig = new Rig(Set.of(ES_PHYSICAL), Set.of());

        assertFalse(rig.api.delete(IndexTag.OS.tag(BARE_NAME)));
        assertEquals(List.of(), rig.esIndexAPI.deleted);
    }

    // =========================================================================
    // Bare-name fan-out with divergent names
    // =========================================================================

    /**
     * Given Scenario: Phase 1. Both siblings exist (the normal paired case).
     * When : delete("working_T0") is called.
     * Then : both physical indices are deleted; primary (ES) result is returned.
     */
    @Test
    public void test_delete_bareName_pairedCase_deletesBothSiblings() {
        setPhase(1);
        final Rig rig = new Rig(Set.of(ES_PHYSICAL), Set.of(OS_PHYSICAL));

        assertTrue(rig.api.delete(BARE_NAME));
        assertEquals(List.of(ES_PHYSICAL), rig.esIndexAPI.deleted);
        assertEquals(List.of(OS_PHYSICAL), rig.osIndexAPI.deleted);
    }

    /**
     * Given Scenario: Phase 1. The bare name exists only in ES — no {@code .os} sibling
     * (divergent names after catchup).
     * When : delete("working_T0") is called.
     * Then : ES delete succeeds and the shadow (OS) delete is skipped entirely —
     *        no delete attempt is made against an index OS does not hold.
     */
    @Test
    public void test_delete_bareName_onlyInEs_skipsShadowDelete() {
        setPhase(1);
        final Rig rig = new Rig(Set.of(ES_PHYSICAL), Set.of());

        assertTrue(rig.api.delete(BARE_NAME));
        assertEquals(List.of(ES_PHYSICAL), rig.esIndexAPI.deleted);
        assertEquals("Shadow delete must be skipped when the index does not exist",
                List.of(), rig.osIndexAPI.deleted);
    }

    /**
     * Given Scenario: Phase 1. Shadow index exists but its delete blows up
     * (e.g. transient engine failure).
     * When : delete("working_T0") is called.
     * Then : shadow failure is swallowed (fire-and-forget); primary result is returned.
     */
    @Test
    public void test_delete_bareName_shadowDeleteFails_isSwallowed() {
        setPhase(1);
        final Rig rig = new Rig(Set.of(ES_PHYSICAL), Set.of(OS_PHYSICAL));
        rig.osIndexAPI.failDeletes = true;

        assertTrue("Primary result must survive a shadow failure", rig.api.delete(BARE_NAME));
        assertEquals(List.of(ES_PHYSICAL), rig.esIndexAPI.deleted);
    }

    /**
     * Given Scenario: Phase 0 (ES only).
     * When : delete("working_T0") is called.
     * Then : only ES is touched; OS provider is never consulted (not even exists-checked).
     */
    @Test
    public void test_delete_bareName_phase0_esOnly() {
        setPhase(0);
        final Rig rig = new Rig(Set.of(ES_PHYSICAL), Set.of(OS_PHYSICAL));

        assertTrue(rig.api.delete(BARE_NAME));
        assertEquals(List.of(ES_PHYSICAL), rig.esIndexAPI.deleted);
        assertEquals(List.of(), rig.osIndexAPI.deleted);
        assertEquals("OS must not even be exists-checked in Phase 0",
                0, rig.osIndexAPI.existsChecks);
    }

    // =========================================================================
    // Test rig
    // =========================================================================

    /** Wires two recording providers into the full-injection testing constructor. */
    private static final class Rig {
        final RecordingIndexAPI esIndexAPI;
        final RecordingIndexAPI osIndexAPI;
        final ContentletIndexAPIImpl api;

        Rig(final Set<String> esIndices, final Set<String> osIndices) {
            this.esIndexAPI = new RecordingIndexAPI(esIndices);
            this.osIndexAPI = new RecordingIndexAPI(osIndices);
            this.api = new ContentletIndexAPIImpl(
                    new RecordingOps(esIndexAPI, IndexTag.ES),
                    new RecordingOps(osIndexAPI, IndexTag.OS),
                    new FakeIndexAPI(List.of()),
                    new FakeIndiciesAPI(),
                    new FakeVersionedIndicesAPI());
        }
    }

    /**
     * {@link IndexAPI} fake holding a set of physical index names. {@code delete} throws on
     * a missing index, mirroring both real engines (index_not_found_exception).
     */
    static final class RecordingIndexAPI extends FakeIndexAPI {

        private final Set<String> existing;
        final List<String> deleted = new ArrayList<>();
        int existsChecks = 0;
        boolean failDeletes = false;

        RecordingIndexAPI(final Set<String> existing) {
            super(List.of());
            this.existing = new HashSet<>(existing);
        }

        @Override
        public boolean indexExists(final String indexName) {
            existsChecks++;
            return existing.contains(indexName);
        }

        @Override
        public boolean delete(final String indexName) {
            if (failDeletes) {
                throw new RuntimeException("simulated engine failure deleting " + indexName);
            }
            if (!existing.remove(indexName)) {
                throw new RuntimeException("index_not_found_exception: no such index [" + indexName + "]");
            }
            deleted.add(indexName);
            return true;
        }
    }

    /** Provider ops whose physical name applies the vendor's {@link IndexTag} marker. */
    static final class RecordingOps extends FakeContentletIndexOperations {

        private final IndexAPI indexAPI;
        private final IndexTag vendor;

        RecordingOps(final IndexAPI indexAPI, final IndexTag vendor) {
            this.indexAPI = indexAPI;
            this.vendor = vendor;
        }

        @Override
        public String toPhysicalName(final String indexName) {
            return vendor.tag(super.toPhysicalName(indexName));
        }

        @Override
        public IndexAPI indexAPI() {
            return indexAPI;
        }
    }
}
