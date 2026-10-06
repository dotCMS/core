package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
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
 * <p>Covers the fix for <a href="https://github.com/dotCMS/core/issues/36423">#36423</a>,
 * reconciled onto the transparent-mirror routing from
 * <a href="https://github.com/dotCMS/core/issues/35640">#35640</a>:</p>
 * <ul>
 *   <li>The shadow leg skips names its engine does not hold (exists-check) instead of
 *       attempting a delete that is guaranteed to miss and logging an ERROR stack trace —
 *       the expected divergent-name miss is logged through the shadow-write policy.</li>
 *   <li>Real shadow failures remain fire-and-forget (swallowed).</li>
 *   <li>A name is deleted through the transparent mirror: whether the caller passes the ES
 *       (bare) or the OS ({@code .os}) name, the tag is stripped to the logical name and
 *       broadcast to every provider, so both siblings are removed.</li>
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
    // Transparent mirror: deleting by the .os name strips the tag and cascades
    // =========================================================================

    /**
     * Given Scenario: Phase 1 (dual-write). Both siblings exist and the caller deletes by the
     * explicitly {@code .os}-tagged name (as the QA/preview UI shows the OS index).
     * When : delete("working_T0.os") is called.
     * Then : the tag is stripped to the logical name and broadcast to every provider, so BOTH
     *        siblings are removed (bidirectional transparent mirror, #35640); the primary (ES)
     *        result is returned.
     */
    @Test
    public void test_delete_osTaggedName_phase1_cascadesToBothSiblings() {
        setPhase(1);
        final Rig rig = new Rig(Set.of(ES_PHYSICAL), Set.of(OS_PHYSICAL));

        assertTrue("Primary (ES) delete must succeed", rig.api.delete(IndexTag.OS.tag(BARE_NAME)));
        assertEquals("ES sibling must be removed via the transparent mirror",
                List.of(ES_PHYSICAL), rig.esIndexAPI.deleted);
        assertEquals(List.of(OS_PHYSICAL), rig.osIndexAPI.deleted);
    }

    /**
     * Given Scenario: Phase 1. The caller passes the {@code .os} name but only the ES (bare)
     * sibling actually exists — the {@code .os} index is absent (divergent names after catchup).
     * When : delete("working_T0.os") is called.
     * Then : the tag is stripped and broadcast; the primary (ES) sibling is removed and the
     *        shadow (OS) leg is skipped (no delete attempt against an index OS does not hold),
     *        so no ERROR-level index_not_found noise is produced (#36423).
     */
    @Test
    public void test_delete_osTaggedName_shadowMissing_skipsShadowDelete() {
        setPhase(1);
        final Rig rig = new Rig(Set.of(ES_PHYSICAL), Set.of());

        assertTrue("Primary result must survive the missing .os sibling",
                rig.api.delete(IndexTag.OS.tag(BARE_NAME)));
        assertEquals(List.of(ES_PHYSICAL), rig.esIndexAPI.deleted);
        assertEquals("Shadow delete must be skipped when the index does not exist",
                List.of(), rig.osIndexAPI.deleted);
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
