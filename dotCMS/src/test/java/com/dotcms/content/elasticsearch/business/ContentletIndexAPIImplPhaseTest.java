package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.content.index.ContentletIndexOperations;
import com.dotcms.content.index.IndexAPI;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.VersionedIndicesAPI;
import com.dotcms.content.index.VersionedIndicesImpl;
import com.dotcms.content.index.domain.ClusterIndexHealth;
import com.dotcms.content.index.domain.ClusterStats;
import com.dotcms.content.index.domain.CreateIndexStatus;
import com.dotcms.content.index.domain.IndexBulkListener;
import com.dotcms.content.index.domain.IndexBulkProcessor;
import com.dotcms.content.index.domain.IndexBulkRequest;
import com.dotcms.content.index.domain.IndexStats;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import java.io.IOException;
import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for {@link ContentletIndexAPIImpl} phase-aware behavior
 * in the realistic "two different index names" scenario:
 *
 * <ul>
 *   <li>dotCMS started in Phase 0 and created ES index {@code working_T0} / {@code live_T0}</li>
 *   <li>Migration was started and OS index {@code working_T1} / {@code live_T1} was created</li>
 *   <li>Both indices co-exist; callers may supply either name</li>
 * </ul>
 *
 * <p>These tests document the <em>current, observable</em> behavior of each API method
 * across migration phases without requiring a running Elasticsearch or OpenSearch cluster.
 * All infrastructure is replaced by in-memory fakes injected via the package-private
 * testing constructor.</p>
 */
public class ContentletIndexAPIImplPhaseTest {

    // ── Logical index names ───────────────────────────────────────────────────
    /** ES index timestamp suffix (created first, in Phase 0). */
    private static final String ES_WORKING = "working_T0";
    private static final String ES_LIVE    = "live_T0";

    /** OS index timestamp suffix (created during migration catchup). */
    private static final String OS_WORKING = "working_T1";
    private static final String OS_LIVE    = "live_T1";

    /** Cluster prefix prepended by both providers' {@code toPhysicalName()}. */
    private static final String CLUSTER_PREFIX = "cluster_test.";

    // ── Test teardown ─────────────────────────────────────────────────────────

    @After
    public void clearPhase() {
        Config.setProperty(FLAG_KEY, null);
    }

    // =========================================================================
    // isDotCMSIndexName — purely syntactic (prefix check, no provider query)
    // =========================================================================

    /**
     * Given Scenario: any migration phase.
     * The method is purely syntactic: it checks whether the name starts with
     * {@code "working_"} or {@code "live_"}.
     *
     * When : isDotCMSIndexName() is called with the ES logical name.
     * Then : returns true — the ES name is a valid dotCMS index name.
     */
    @Test
    public void test_isDotCMSIndexName_esLogicalName_isTrue() {
        final ContentletIndexAPIImpl api = buildApi(
                new FakeIndexAPI(List.of(ES_WORKING, ES_LIVE)),
                new FakeIndiciesAPI(),
                new FakeVersionedIndicesAPI());

        assertTrue("ES working name must be recognised as a dotCMS index",
                api.isDotCMSIndexName(ES_WORKING));
        assertTrue("ES live name must be recognised as a dotCMS index",
                api.isDotCMSIndexName(ES_LIVE));
    }

    /**
     * Given Scenario: any migration phase.
     * When : isDotCMSIndexName() is called with the OS logical name.
     * Then : returns true — even though only OS has this index, the name itself
     *        starts with "working_" so it passes the syntactic check.
     *        The method does NOT query which provider actually holds the index.
     */
    @Test
    public void test_isDotCMSIndexName_osLogicalName_isTrue() {
        final ContentletIndexAPIImpl api = buildApi(
                new FakeIndexAPI(List.of()),
                new FakeIndiciesAPI(),
                new FakeVersionedIndicesAPI());

        assertTrue("OS working name must be recognised as a dotCMS index (syntactic check only)",
                api.isDotCMSIndexName(OS_WORKING));
        assertTrue("OS live name must be recognised as a dotCMS index",
                api.isDotCMSIndexName(OS_LIVE));
    }

    /**
     * Given Scenario: any phase.
     * When : isDotCMSIndexName() is called with a physical name (cluster prefix included).
     * Then : returns false — physical names do NOT start with "working_" or "live_".
     *        Callers must strip the cluster prefix before invoking this method.
     */
    @Test
    public void test_isDotCMSIndexName_physicalNameWithClusterPrefix_isFalse() {
        final ContentletIndexAPIImpl api = buildApi(
                new FakeIndexAPI(List.of()),
                new FakeIndiciesAPI(),
                new FakeVersionedIndicesAPI());

        assertFalse("Physical name with cluster prefix must NOT be recognised",
                api.isDotCMSIndexName(CLUSTER_PREFIX + ES_WORKING));
        assertFalse("Physical OS name with cluster prefix must NOT be recognised",
                api.isDotCMSIndexName(CLUSTER_PREFIX + OS_WORKING));
    }

    /**
     * Given Scenario: any phase.
     * When : isDotCMSIndexName() is called with a name that has no recognised dotCMS prefix.
     * Then : returns false.
     */
    @Test
    public void test_isDotCMSIndexName_unknownPrefix_isFalse() {
        final ContentletIndexAPIImpl api = buildApi(
                new FakeIndexAPI(List.of()),
                new FakeIndiciesAPI(),
                new FakeVersionedIndicesAPI());

        assertFalse("Unrecognised prefix must return false",
                api.isDotCMSIndexName("notadotcmsindex_T0"));
        assertFalse("Null must return false", api.isDotCMSIndexName(null));
    }

    // =========================================================================
    // listDotCMSIndices — pure delegation to IndexAPI.getIndices(true, false)
    // =========================================================================

    /**
     * Given Scenario: Phase 0 (ES only).
     * The fake IndexAPI is pre-loaded with ES indices only.
     * When : listDotCMSIndices() is called.
     * Then : returns exactly the ES indices — OS is not consulted in Phase 0.
     *        The phase-awareness is encapsulated inside IndexAPIImpl (the real
     *        implementation); ContentletIndexAPIImpl simply delegates.
     */
    @Test
    public void test_listDotCMSIndices_phase0_esIndicesOnly() {
        setPhase(0);
        final ContentletIndexAPIImpl api = buildApi(
                new FakeIndexAPI(List.of(ES_WORKING, ES_LIVE)),
                new FakeIndiciesAPI(),
                new FakeVersionedIndicesAPI());

        final List<String> indices = api.listDotCMSIndices();

        assertEquals(2, indices.size());
        assertTrue(indices.contains(ES_WORKING));
        assertTrue(indices.contains(ES_LIVE));
        assertFalse("OS index must not appear in Phase 0", indices.contains(OS_WORKING));
    }

    /**
     * Given Scenario: Phase 1 or 2 (dual-write). The fake IndexAPI returns a merged
     * list of ES and OS indices, simulating IndexAPIImpl's merge behavior.
     * When : listDotCMSIndices() is called.
     * Then : returns indices from both providers.
     *        Each provider contributes its own timestamped names — T0 from ES, T1 from OS.
     */
    @Test
    public void test_listDotCMSIndices_dualWrite_returnsBothProviders() {
        setPhase(1);
        final List<String> merged = List.of(ES_WORKING, ES_LIVE, OS_WORKING, OS_LIVE);
        final ContentletIndexAPIImpl api = buildApi(
                new FakeIndexAPI(merged),
                new FakeIndiciesAPI(),
                new FakeVersionedIndicesAPI());

        final List<String> indices = api.listDotCMSIndices();

        assertEquals(4, indices.size());
        assertTrue(indices.contains(ES_WORKING));
        assertTrue(indices.contains(OS_WORKING));
    }

    /**
     * Given Scenario: Phase 3 (OS only). The fake IndexAPI returns OS indices only.
     * When : listDotCMSIndices() is called.
     * Then : returns only OS indices — ES is decommissioned.
     */
    @Test
    public void test_listDotCMSIndices_phase3_osIndicesOnly() {
        setPhase(3);
        final ContentletIndexAPIImpl api = buildApi(
                new FakeIndexAPI(List.of(OS_WORKING, OS_LIVE)),
                new FakeIndiciesAPI(),
                new FakeVersionedIndicesAPI());

        final List<String> indices = api.listDotCMSIndices();

        assertEquals(2, indices.size());
        assertTrue(indices.contains(OS_WORKING));
        assertFalse("ES index must not appear in Phase 3", indices.contains(ES_WORKING));
    }

    // =========================================================================
    // activateIndex — writes to index pointer stores (IndiciesAPI / VersionedIndicesAPI)
    // =========================================================================

    /**
     * Given Scenario: Phase 0 (ES only). ES store is empty.
     * When : activateIndex("working_T0") is called (ES name).
     * Then : ES store (legacyIndiciesAPI) is updated with the physical working name.
     *        OS store (versionedIndicesAPI) is NOT touched — migration has not started.
     */
    @Test
    public void test_activateIndex_phase0_esName_updatesEsStoreOnly() throws DotDataException {
        setPhase(0);
        final FakeIndiciesAPI fakeIndicie = new FakeIndiciesAPI();
        final FakeVersionedIndicesAPI fakeVersioned = new FakeVersionedIndicesAPI();
        final ContentletIndexAPIImpl api = buildApi(
                new FakeIndexAPI(List.of(ES_WORKING, ES_LIVE)),
                fakeIndicie,
                fakeVersioned);

        api.activateIndex(ES_WORKING);

        assertEquals("ES store must record the physical working name",
                CLUSTER_PREFIX + ES_WORKING, fakeIndicie.loadIndicies().getWorking());
        assertNull("OS store must not be touched in Phase 0",
                fakeVersioned.stored);
    }

    /**
     * Given Scenario: Phase 0 (ES only). Caller passes the OS index name (working_T1),
     * even though OS is not active in Phase 0.
     * When : activateIndex("working_T1") is called.
     * Then : ES store is updated with the physical T1 name — activateIndex does NOT
     *        validate that the index physically exists; it only writes to the pointer store.
     *        OS store is NOT touched.
     *
     * <p><strong>Observation:</strong> in Phase 0 the ES store can be pointed at an index
     * name that does not physically exist in the ES cluster, because activateIndex is
     * a pure pointer-store update with no existence check.</p>
     */
    @Test
    public void test_activateIndex_phase0_osName_goesToEsStore_noOsWrite() throws DotDataException {
        setPhase(0);
        final FakeIndiciesAPI fakeIndicie = new FakeIndiciesAPI();
        final FakeVersionedIndicesAPI fakeVersioned = new FakeVersionedIndicesAPI();
        final ContentletIndexAPIImpl api = buildApi(
                new FakeIndexAPI(List.of(ES_WORKING, ES_LIVE)),
                fakeIndicie,
                fakeVersioned);

        api.activateIndex(OS_WORKING); // OS name passed in Phase 0

        assertEquals("ES store must record the OS physical name even in Phase 0",
                CLUSTER_PREFIX + OS_WORKING, fakeIndicie.loadIndicies().getWorking());
        assertNull("OS store must not be touched in Phase 0", fakeVersioned.stored);
    }

    /**
     * Given Scenario: Phase 2 (dual-write, OS reads). Caller uses the ES index name T0.
     * When : activateIndex("working_T0") is called.
     * Then : ES store is updated with T0 (physical).
     *        OS store mirror is also updated — but it receives the SAME logical name T0,
     *        even though OS physically has T1. The mirror writes the name as-is, without
     *        checking which index actually exists in the OS cluster.
     *
     * <p><strong>Key observation:</strong> after this call, the OS pointer store records
     * "working_T0" as the active working index — a mismatch with the physical OS index.
     * This is expected behavior during the catch-up window.</p>
     */
    @Test
    public void test_activateIndex_phase2_esName_mirroredToOsStore() throws DotDataException {
        setPhase(2);
        final FakeIndiciesAPI fakeIndicie = new FakeIndiciesAPI();
        final FakeVersionedIndicesAPI fakeVersioned = new FakeVersionedIndicesAPI();
        final ContentletIndexAPIImpl api = buildApi(
                new FakeIndexAPI(List.of()),
                fakeIndicie,
                fakeVersioned);

        api.activateIndex(ES_WORKING);

        // ES pointer store is updated ✓
        assertEquals("ES store must record the physical working name",
                CLUSTER_PREFIX + ES_WORKING, fakeIndicie.loadIndicies().getWorking());

        // OS store receives a mirror of the same logical name
        assertTrue("OS store must be populated in Phase 2",
                fakeVersioned.stored != null);
        assertEquals("OS store must contain the mirrored working name (even if OS has a different physical index)",
                CLUSTER_PREFIX + ES_WORKING,
                fakeVersioned.stored.working().orElse(null));
    }

    /**
     * Given Scenario: Phase 2 (dual-write, OS reads). Caller uses the OS index name T1.
     * When : activateIndex("working_T1") is called.
     * Then : ES store is updated with T1 (even though ES physically has T0).
     *        OS store is also updated with T1 — in this case both stores are correct
     *        because the caller used the OS-native name.
     */
    @Test
    public void test_activateIndex_phase2_osName_updatesBothStores() throws DotDataException {
        setPhase(2);
        final FakeIndiciesAPI fakeIndicie = new FakeIndiciesAPI();
        final FakeVersionedIndicesAPI fakeVersioned = new FakeVersionedIndicesAPI();
        final ContentletIndexAPIImpl api = buildApi(
                new FakeIndexAPI(List.of()),
                fakeIndicie,
                fakeVersioned);

        api.activateIndex(OS_WORKING);

        assertEquals("ES store must record the OS physical name",
                CLUSTER_PREFIX + OS_WORKING, fakeIndicie.loadIndicies().getWorking());
        assertEquals("OS store must record its own physical name",
                CLUSTER_PREFIX + OS_WORKING,
                fakeVersioned.stored.working().orElse(null));
    }

    /**
     * Given Scenario: Phase 3 (OS only).
     * When : activateIndex("working_T1") is called.
     * Then : only the OS store (versionedIndicesAPI) is updated.
     *        ES store (legacyIndiciesAPI) is NOT touched — ES is decommissioned.
     */
    @Test
    public void test_activateIndex_phase3_onlyOsStoreUpdated() throws DotDataException {
        setPhase(3);
        final FakeIndiciesAPI fakeIndicie = new FakeIndiciesAPI();
        final FakeVersionedIndicesAPI fakeVersioned = new FakeVersionedIndicesAPI();
        final ContentletIndexAPIImpl api = buildApi(
                new FakeIndexAPI(List.of()),
                fakeIndicie,
                fakeVersioned);

        api.activateIndex(OS_WORKING);

        assertNull("ES store must NOT be touched in Phase 3 (ES is decommissioned)",
                fakeIndicie.loadIndicies().getWorking());
        assertEquals("OS store must record the working name",
                CLUSTER_PREFIX + OS_WORKING,
                fakeVersioned.stored.working().orElse(null));
    }

    // =========================================================================
    // deactivateIndex — clears a slot from the pointer stores by index type
    // =========================================================================

    /**
     * Given Scenario: Phase 0 (ES only). ES store has working=T0, live=T0-live.
     * When : deactivateIndex("working_T0") is called.
     * Then : ES store working slot is cleared (null); live slot is preserved.
     *        OS store is NOT touched.
     */
    @Test
    public void test_deactivateIndex_phase0_clearsEsWorkingSlotOnly()
            throws DotDataException, IOException {
        setPhase(0);
        final FakeIndiciesAPI fakeIndicie = new FakeIndiciesAPI();
        fakeIndicie.setWorking(CLUSTER_PREFIX + ES_WORKING);
        fakeIndicie.setLive(CLUSTER_PREFIX + ES_LIVE);
        final FakeVersionedIndicesAPI fakeVersioned = new FakeVersionedIndicesAPI();

        final ContentletIndexAPIImpl api = buildApi(
                new FakeIndexAPI(List.of()), fakeIndicie, fakeVersioned);

        api.deactivateIndex(ES_WORKING);

        assertNull("Working slot must be cleared after deactivation",
                fakeIndicie.loadIndicies().getWorking());
        assertEquals("Live slot must be preserved",
                CLUSTER_PREFIX + ES_LIVE, fakeIndicie.loadIndicies().getLive());
        assertNull("OS store must not be touched in Phase 0", fakeVersioned.stored);
    }

    /**
     * Given Scenario: Phase 2 (dual-write). Both ES and OS stores have their respective
     * working indices recorded (T0 in ES, T1 in OS).
     * When : deactivateIndex("working_T0") is called.
     * Then : ES store working slot is cleared.
     *        OS store working slot is ALSO cleared — the deactivation clears the slot
     *        by INDEX TYPE (WORKING), not by the specific index name (T0 vs T1).
     *        The OS live slot is preserved.
     *
     * <p><strong>Key observation:</strong> deactivateIndex identifies which slot to clear
     * via the index-type prefix ("working_" / "live_"), not via the exact name.
     * Passing "working_T0" in Phase 2 will clear the OS working slot even though OS
     * physically records "working_T1" there.</p>
     */
    @Test
    public void test_deactivateIndex_phase2_clearsBothStoresByIndexType()
            throws DotDataException, IOException {
        setPhase(2);
        final FakeIndiciesAPI fakeIndicie = new FakeIndiciesAPI();
        fakeIndicie.setWorking(CLUSTER_PREFIX + ES_WORKING);
        fakeIndicie.setLive(CLUSTER_PREFIX + ES_LIVE);

        final FakeVersionedIndicesAPI fakeVersioned = new FakeVersionedIndicesAPI();
        // Pre-populate OS store: OS working = T1, OS live = T1-live
        fakeVersioned.stored = VersionedIndicesImpl.builder()
                .working(CLUSTER_PREFIX + OS_WORKING)
                .live(CLUSTER_PREFIX + OS_LIVE)
                .build();

        final ContentletIndexAPIImpl api = buildApi(
                new FakeIndexAPI(List.of()), fakeIndicie, fakeVersioned);

        // Deactivate using the ES index name — but the SLOT (WORKING) is cleared in both stores
        api.deactivateIndex(ES_WORKING);

        assertNull("ES working slot must be cleared",
                fakeIndicie.loadIndicies().getWorking());
        assertEquals("ES live slot must be preserved",
                CLUSTER_PREFIX + ES_LIVE, fakeIndicie.loadIndicies().getLive());

        // OS mirror: working slot cleared (by index type), live preserved
        assertNull("OS working slot must also be cleared (deactivation is by index type, not name)",
                fakeVersioned.stored.working().orElse(null));
        assertEquals("OS live slot must be preserved",
                CLUSTER_PREFIX + OS_LIVE, fakeVersioned.stored.live().orElse(null));
    }

    // =========================================================================
    // Factory and helpers
    // =========================================================================

    private static ContentletIndexAPIImpl buildApi(
            final FakeIndexAPI fakeIndex,
            final FakeIndiciesAPI fakeIndicie,
            final FakeVersionedIndicesAPI fakeVersioned) {
        return new ContentletIndexAPIImpl(
                new FakeContentletIndexOperations(),
                new FakeContentletIndexOperations(),
                fakeIndex,
                fakeIndicie,
                fakeVersioned);
    }

    private static void setPhase(final int ordinal) {
        Config.setProperty(FLAG_KEY, String.valueOf(ordinal));
    }

    // =========================================================================
    // Fake implementations — in-memory stubs with no vendor dependencies
    // =========================================================================

    /**
     * In-memory {@link IndexAPI} stub.
     * Only the three methods used by the target methods are implemented;
     * all others throw {@link UnsupportedOperationException}.
     */
    static class FakeIndexAPI implements IndexAPI {

        private final List<String> openIndices;

        FakeIndexAPI(final List<String> openIndices) {
            this.openIndices = new ArrayList<>(openIndices);
        }

        @Override
        public List<String> getIndices(final boolean expandOpen, final boolean expandClosed) {
            return Collections.unmodifiableList(openIndices);
        }

        @Override
        public List<String> getClosedIndexes() {
            return List.of();
        }

        @Override
        public String getNameWithClusterIDPrefix(final String name) {
            return name.startsWith(CLUSTER_PREFIX) ? name : CLUSTER_PREFIX + name;
        }

        @Override
        public String removeClusterIdFromName(final String name) {
            if (name == null) return "";
            return name.startsWith(CLUSTER_PREFIX) ? name.substring(CLUSTER_PREFIX.length()) : name;
        }

        // ── unneeded methods ─────────────────────────────────────────────────

        @Override public Map<String, IndexStats> getIndicesStats() { throw new UnsupportedOperationException(); }
        @Override public Map<String, Integer> flushCaches(List<String> n) { throw new UnsupportedOperationException(); }
        @Override public boolean optimize(List<String> n) { throw new UnsupportedOperationException(); }
        @Override public boolean delete(String n) { throw new UnsupportedOperationException(); }
        @Override public boolean deleteMultiple(String... n) { throw new UnsupportedOperationException(); }
        @Override public void deleteInactiveLiveWorkingIndices(int n) { throw new UnsupportedOperationException(); }
        @Override public Set<String> listIndices() { throw new UnsupportedOperationException(); }
        @Override public boolean isIndexClosed(String n) { throw new UnsupportedOperationException(); }
        @Override public boolean indexExists(String n) { throw new UnsupportedOperationException(); }
        @Override public void createIndex(String n) { throw new UnsupportedOperationException(); }
        @Override public CreateIndexStatus createIndex(String n, int s) { throw new UnsupportedOperationException(); }
        @Override public void clearIndex(String n) { throw new UnsupportedOperationException(); }
        @Override public CreateIndexStatus createIndex(String n, String s, int sh) { throw new UnsupportedOperationException(); }
        @Override public String getDefaultIndexSettings() { throw new UnsupportedOperationException(); }
        @Override public Map<String, ClusterIndexHealth> getClusterHealth() { throw new UnsupportedOperationException(); }
        @Override public void updateReplicas(String n, int r) { throw new UnsupportedOperationException(); }
        @Override public void createAlias(String n, String a) { throw new UnsupportedOperationException(); }
        @Override public Map<String, String> getIndexAlias(List<String> n) { throw new UnsupportedOperationException(); }
        @Override public Map<String, String> getIndexAlias(String[] n) { throw new UnsupportedOperationException(); }
        @Override public String getIndexAlias(String n) { throw new UnsupportedOperationException(); }
        @Override public Map<String, String> getAliasToIndexMap(List<String> n) { throw new UnsupportedOperationException(); }
        @Override public void closeIndex(String n) { throw new UnsupportedOperationException(); }
        @Override public void openIndex(String n) { throw new UnsupportedOperationException(); }
        @Override public List<String> getLiveWorkingIndicesSortedByCreationDateDesc() { throw new UnsupportedOperationException(); }
        @Override public Status getIndexStatus(String n) { throw new UnsupportedOperationException(); }
        @Override public boolean waitUtilIndexReady() { throw new UnsupportedOperationException(); }
        @Override public ClusterStats getClusterStats() { throw new UnsupportedOperationException(); }
    }

    /**
     * In-memory {@link IndiciesAPI} stub that stores the current index pointers as a
     * mutable {@link IndiciesInfo}.
     */
    @SuppressWarnings("deprecation")
    static class FakeIndiciesAPI implements IndiciesAPI {

        private IndiciesInfo current = new IndiciesInfo.Builder().build();

        void setWorking(final String working) {
            current = new IndiciesInfo.Builder()
                    .setWorking(working)
                    .setLive(current.getLive())
                    .build();
        }

        void setLive(final String live) {
            current = new IndiciesInfo.Builder()
                    .setWorking(current.getWorking())
                    .setLive(live)
                    .build();
        }

        @Override
        public IndiciesInfo loadIndicies() {
            return current;
        }

        @Override
        public IndiciesInfo loadIndicies(final Connection conn) {
            return current;
        }

        @Override
        public void point(final IndiciesInfo newInfo) {
            this.current = newInfo;
        }
    }

    /**
     * In-memory {@link VersionedIndicesAPI} stub that stores a single
     * {@link VersionedIndices} record (the "default" OS record).
     */
    static class FakeVersionedIndicesAPI implements VersionedIndicesAPI {

        VersionedIndices stored = null;

        @Override
        public Optional<VersionedIndices> loadDefaultVersionedIndices() {
            return Optional.ofNullable(stored);
        }

        @Override
        public void saveIndices(final VersionedIndices info) {
            this.stored = info;
        }

        @Override
        public Optional<VersionedIndices> loadNonVersionedIndices() {
            return Optional.empty();
        }

        // ── unneeded methods ─────────────────────────────────────────────────

        @Override public Optional<VersionedIndices> loadIndices(String v) { throw new UnsupportedOperationException(); }
        @Override public List<VersionedIndices> loadAllIndices() { throw new UnsupportedOperationException(); }
        @Override public void removeVersion(String v) { throw new UnsupportedOperationException(); }
        @Override public boolean versionExists(String v) { throw new UnsupportedOperationException(); }
        @Override public int getIndicesCount(String v) { throw new UnsupportedOperationException(); }
        @Override public Instant extractTimestamp(String n) { throw new UnsupportedOperationException(); }
        @Override public void clearCache() { /* no-op */ }
    }

    /**
     * Minimal {@link ContentletIndexOperations} stub used only as a constructor argument.
     *
     * <p>Only {@link #toPhysicalName} is implemented (the default interface method is
     * overridden to avoid calling a real {@link IndexAPI}). All bulk and lifecycle
     * operations throw {@link UnsupportedOperationException} since none of the
     * tested methods invoke them.</p>
     */
    static class FakeContentletIndexOperations implements ContentletIndexOperations {

        @Override
        public String toPhysicalName(final String indexName) {
            return indexName.startsWith(CLUSTER_PREFIX) ? indexName : CLUSTER_PREFIX + indexName;
        }

        @Override
        public IndexAPI indexAPI() {
            throw new UnsupportedOperationException("indexAPI() not used by tests");
        }

        // ── unneeded methods ─────────────────────────────────────────────────

        @Override public IndexBulkRequest createBulkRequest() { throw new UnsupportedOperationException(); }
        @Override public void addIndexOp(IndexBulkRequest r, String i, String d, String j) { throw new UnsupportedOperationException(); }
        @Override public void addDeleteOp(IndexBulkRequest r, String i, String d) { throw new UnsupportedOperationException(); }
        @Override public void setRefreshPolicy(IndexBulkRequest r, IndexBulkRequest.RefreshPolicy p) { throw new UnsupportedOperationException(); }
        @Override public void putToIndex(IndexBulkRequest r) { throw new UnsupportedOperationException(); }
        @Override public IndexBulkProcessor createBulkProcessor(IndexBulkListener l) { throw new UnsupportedOperationException(); }
        @Override public void addIndexOpToProcessor(IndexBulkProcessor p, String i, String d, String j) { throw new UnsupportedOperationException(); }
        @Override public void addDeleteOpToProcessor(IndexBulkProcessor p, String i, String d) { throw new UnsupportedOperationException(); }
        @Override public boolean createContentIndex(String n, int s) { throw new UnsupportedOperationException(); }
        @Override public void removeContentFromIndexByContentType(ContentType t) { throw new UnsupportedOperationException(); }
        @Override public long getIndexDocumentCount(String n) { throw new UnsupportedOperationException(); }
    }
}
