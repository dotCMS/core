package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.content.index.IndexAPIImpl;
import com.dotcms.content.index.IndexTag;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.VersionedIndicesImpl;
import com.dotmarketing.business.DotStateException;
import com.dotcms.content.elasticsearch.util.MappingHelper;
import com.dotcms.content.index.opensearch.ContentletIndexOperationsOS;
import com.dotcms.content.index.opensearch.MappingOperationsOS;
import com.dotcms.content.index.opensearch.OSClientProvider;
import com.dotcms.content.index.opensearch.OSIndexAPIImpl;
import com.dotcms.util.IntegrationTestInitService;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.GetIndexResponse;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Integration tests for {@link ContentletIndexAPIImpl} that exercise the phase-aware routing
 * behavior against live search clusters.
 *
 * <h2>Scenario</h2>
 * <p>The tests simulate the real catch-up situation that arises during the ES→OS migration:
 * Elasticsearch (or an ES-compatible cluster) was the original search backend and holds index
 * {@code working_T0} / {@code live_T0}. OpenSearch was brought up later and received index
 * {@code working_T1} / {@code live_T1}. Both clusters are live simultaneously; the migration
 * phase flag controls which provider is treated as primary.</p>
 *
 * <h2>Two-cluster vs single-cluster profiles</h2>
 * <p>Some tests require <strong>separate</strong> ES and OS clusters to observe cluster-isolation
 * behavior (e.g. "Phase 0 shows only ES indices"). When both clients point to the same cluster
 * — as happens in the {@code opensearch-upgrade} Maven profile where
 * {@code DOT_ES_ENDPOINTS == OS_ENDPOINTS} — those tests are automatically skipped via
 * {@link org.junit.Assume}. Tests that only verify routing logic or DB pointer state work
 * correctly in both single- and two-cluster setups.</p>
 *
 * <h2>Run command</h2>
 * <pre>
 *   ./mvnw verify -pl :dotcms-integration \
 *       -Dcoreit.test.skip=false \
 *       -Dopensearch.upgrade.test=true \
 *       -Dit.test=ContentletIndexAPIImplMigrationIntegrationTest
 * </pre>
 *
 * @author Fabrizzio Araya
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentletIndexAPIImplMigrationIntegrationTest extends IntegrationTestBase {

    // ── Unique suffix prevents cross-run index name collisions ────────────────
    private static final String RUN_ID =
            UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    /**
     * ES index name — represents the index created during Phase 0 (before OS existed).
     */
    private static final String ES_WORKING = "working_t0_" + RUN_ID;
    private static final String ES_LIVE    = "live_t0_"    + RUN_ID;

    /**
     * OS index name — represents the index created during migration catch-up.
     * Different timestamp suffix → different from the ES name.
     */
    private static final String OS_WORKING = "working_t1_" + RUN_ID;
    private static final String OS_LIVE    = "live_t1_"    + RUN_ID;

    /**
     * Name used for the dual-write fan-out test — a single logical name that
     * {@code createContentIndex()} sends to both providers simultaneously.
     */
    private static final String DUAL_WORKING = "working_dual_" + RUN_ID;

    /**
     * A name that both ES and OS reject at the cluster level: spaces are not allowed in index names.
     * Used to verify that cluster-level validation errors always propagate — they are NOT
     * swallowed by the fire-and-forget mechanism (which only applies to shadow {@code index_not_found}).
     */
    private static final String INVALID_INDEX_NAME = "invalid name with spaces " + RUN_ID;

    /**
     * A name that matches the {@code working} prefix (so {@link IndexType#WORKING} recognises it)
     * but was <strong>never created</strong> in any cluster.
     * Used to verify that {@code deactivateIndex} is a DB-pointer-only operation: it clears
     * the slot based on the name pattern, without checking cluster existence.
     */
    private static final String GHOST_WORKING = "working_ghost_" + RUN_ID;

    // ── CDI-injected direct OS handle (bypasses the phase router) ────────────
    @Inject
    private OSIndexAPIImpl osIndexAPI;

    // ── Used to resolve the OS physical name for indices created through the phase router ──
    @Inject
    private ContentletIndexOperationsOS opsOS;

    // ── Used by the orphan-repair regression test to read back mapping + analysis settings ──
    @Inject
    private MappingOperationsOS mappingOps;

    @Inject
    private OSClientProvider clientProvider;

    /**
     * Physical (cluster-prefixed, tag-suffixed) form of {@link #DUAL_WORKING}; resolved once
     * per test in {@link #setUp()}. Only {@code DUAL_WORKING} needs a physical-name field —
     * it is created through {@code contentletIndexAPI().createContentIndex(...)}, which goes
     * through {@code ContentletIndexOperationsOS.toPhysicalName} on the OS side. The other OS
     * indices ({@code OS_WORKING}, {@code OS_LIVE}, {@code GHOST_WORKING}) enter via
     * {@link OSIndexAPIImpl#createIndex(String,int)} directly and stay symmetric on the bare name.
     */
    private String physicalDualWorking;

    // ── Saved DB state — restored in @After to avoid polluting the running app ──
    @SuppressWarnings("deprecation")
    private IndiciesInfo savedEsInfo;
    private Optional<VersionedIndices> savedOsIndices;

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void setUp() throws Exception {
        physicalDualWorking = opsOS.toPhysicalName(DUAL_WORKING);

        savedEsInfo    = APILocator.getIndiciesAPI().loadIndicies();
        savedOsIndices = APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices();

        cleanupTestIndices();

        // Create ES indices directly against the ES cluster (bypasses phase routing)
        esImpl().createIndex(ES_WORKING, 1);
        esImpl().createIndex(ES_LIVE,    1);

        // Create OS indices directly against the OS cluster (bypasses phase routing)
        osIndexAPI.createIndex(OS_WORKING, 1);
        osIndexAPI.createIndex(OS_LIVE,    1);
    }

    @After
    public void tearDown() throws Exception {
        Config.setProperty(FLAG_KEY, null);
        cleanupTestIndices();

        Try.run(() -> APILocator.getIndiciesAPI().point(savedEsInfo))
           .onFailure(e -> Logger.warn(this, "tearDown: failed to restore ES indices info: " + e.getMessage()));
        savedOsIndices.ifPresent(v ->
                Try.run(() -> APILocator.getVersionedIndicesAPI().saveIndices(v))
                   .onFailure(e -> Logger.warn(this, "tearDown: failed to restore OS versioned indices: " + e.getMessage())));
    }

    // =========================================================================
    // listDotCMSIndices — real cluster visibility by phase
    //
    // NOTE: Phase 0 and Phase 3 isolation tests require two *separate* clusters
    // (ES on one endpoint, OS on another). They are skipped automatically when
    // the profile routes both clients to the same cluster.
    // =========================================================================

    /**
     * Given Scenario: Phase 0 (ES only). ES has T0; OS has T1.
     * When : listDotCMSIndices() is called.
     * Then : Only the ES indices are returned — OS cluster not queried.
     * Skip   : when ES and OS endpoints are the same cluster (single-cluster profile).
     */
    @Test
    public void test_listDotCMSIndices_phase0_returnsEsIndicesOnly() {
        assumeFalse("Requires separate ES and OS clusters (ES and OS point to same endpoint)",
                esSameAsOs());
        setPhase(0);

        final List<String> indices = contentletIndexAPI().listDotCMSIndices();

        assertTrue("ES working must appear in Phase 0", indices.contains(ES_WORKING));
        assertTrue("ES live must appear in Phase 0",    indices.contains(ES_LIVE));
        assertFalse("OS working must NOT appear in Phase 0 (OS not queried)",
                indices.contains(OS_WORKING));

        Logger.info(this, "✅ listDotCMSIndices Phase 0 — ES only: " + indices);
    }

    /**
     * Given Scenario: Phase 1 (dual-write). Both clusters are live.
     * When : listDotCMSIndices() is called.
     * Then : Both ES (T0) and OS (T1) indices appear — different names from different clusters.
     *        Works in single-cluster profile too: both T0 and T1 exist in the shared cluster.
     */
    @Test
    public void test_listDotCMSIndices_phase1_returnsBothIndexNames() {
        setPhase(1);

        final List<String> indices = contentletIndexAPI().listDotCMSIndices();

        assertTrue("ES working (T0) must appear in Phase 1",  indices.contains(ES_WORKING));
        assertTrue("ES live (T0) must appear in Phase 1",     indices.contains(ES_LIVE));
        assertTrue("OS working (T1) must appear in Phase 1",  indices.contains(OS_WORKING));
        assertTrue("OS live (T1) must appear in Phase 1",     indices.contains(OS_LIVE));

        Logger.info(this, "✅ listDotCMSIndices Phase 1 — both index names: " + indices);
    }

    // =========================================================================
    // getIndicesStats — dual-write aggregation
    // =========================================================================

    /**
     * Given Scenario: Phase 1 (dual-write). ES has T0 indices; OS has T1 indices.
     * When : getIndicesStats() is called via the phase-routing IndexAPIImpl.
     * Then : The map contains entries for both ES (T0) and OS (T1) indices, each with
     *        non-negative document count and raw size.
     *        Before this fix the OS entries were missing — IndexAPIImpl routed stats
     *        to the read provider (ES) only and OSIndexAPIImpl was a stub.
     */
    @Test
    public void test_getIndicesStats_phase1_mergesBothProviders() {
        setPhase(1);
        final IndexAPIImpl indexAPI = (IndexAPIImpl) APILocator.getESIndexAPI();

        final java.util.Map<String, com.dotcms.content.index.domain.IndexStats> stats =
                indexAPI.getIndicesStats();

        assertNotNull("getIndicesStats must not return null", stats);
        assertTrue("ES working (T0) must appear in Phase 1 stats", stats.containsKey(ES_WORKING));
        assertTrue("ES live (T0) must appear in Phase 1 stats",    stats.containsKey(ES_LIVE));
        assertTrue("OS working (T1) must appear in Phase 1 stats", stats.containsKey(OS_WORKING));
        assertTrue("OS live (T1) must appear in Phase 1 stats",    stats.containsKey(OS_LIVE));

        final com.dotcms.content.index.domain.IndexStats osStats = stats.get(OS_WORKING);
        assertTrue("OS working doc count must be non-negative", osStats.documentCount() >= 0);
        assertTrue("OS working raw size must be non-negative",  osStats.sizeRaw()       >= 0);
        assertNotNull("OS working size string must not be null", osStats.size());

        Logger.info(this, "✅ getIndicesStats Phase 1 — ES+OS merged: " + stats.keySet());
    }

    /**
     * Given Scenario: Phase 0 (ES only). ES has T0; OS has T1 on a separate cluster.
     * When : getIndicesStats() is called.
     * Then : Only ES indices appear — OS cluster not queried.
     * Skip   : when ES and OS endpoints are the same cluster (single-cluster profile).
     */
    @Test
    public void test_getIndicesStats_phase0_esOnly() {
        assumeFalse("Requires separate ES and OS clusters", esSameAsOs());
        setPhase(0);
        final IndexAPIImpl indexAPI = (IndexAPIImpl) APILocator.getESIndexAPI();

        final java.util.Map<String, com.dotcms.content.index.domain.IndexStats> stats =
                indexAPI.getIndicesStats();

        assertTrue("ES working (T0) must appear in Phase 0 stats", stats.containsKey(ES_WORKING));
        assertTrue("ES live (T0) must appear in Phase 0 stats",    stats.containsKey(ES_LIVE));
        assertFalse("OS working must NOT appear in Phase 0 stats",  stats.containsKey(OS_WORKING));
        assertFalse("OS live must NOT appear in Phase 0 stats",     stats.containsKey(OS_LIVE));

        Logger.info(this, "✅ getIndicesStats Phase 0 — ES only: " + stats.keySet());
    }

    // =========================================================================
    // getClusterHealth — dual-write aggregation
    // =========================================================================

    /**
     * Given Scenario: Phase 1 (dual-write). ES has T0; OS has T1.
     * When : getClusterHealth() is called via the phase-routing IndexAPIImpl.
     * Then : Health entries exist for both ES (T0) and OS (T1) indices, each with
     *        a non-null status and positive shard count.
     *        Before this fix the OS entries were missing because OSIndexAPIImpl was a stub.
     */
    @Test
    public void test_getClusterHealth_phase1_mergesBothProviders() {
        setPhase(1);
        final IndexAPIImpl indexAPI = (IndexAPIImpl) APILocator.getESIndexAPI();

        final java.util.Map<String, com.dotcms.content.index.domain.ClusterIndexHealth> health =
                indexAPI.getClusterHealth();

        assertNotNull("getClusterHealth must not return null", health);
        assertTrue("ES working (T0) must appear in Phase 1 health", health.containsKey(ES_WORKING));
        assertTrue("ES live (T0) must appear in Phase 1 health",    health.containsKey(ES_LIVE));
        assertTrue("OS working (T1) must appear in Phase 1 health", health.containsKey(OS_WORKING));
        assertTrue("OS live (T1) must appear in Phase 1 health",    health.containsKey(OS_LIVE));

        final com.dotcms.content.index.domain.ClusterIndexHealth osHealth = health.get(OS_WORKING);
        assertNotNull("OS health status must not be null",     osHealth.status());
        assertFalse("OS health status must not be empty",      osHealth.status().isBlank());
        assertTrue("OS working shard count must be positive",  osHealth.numberOfShards() > 0);
        assertTrue("OS replicas must be non-negative",         osHealth.numberOfReplicas() >= 0);

        Logger.info(this, "✅ getClusterHealth Phase 1 — ES+OS merged: " + health.keySet());
    }

    /**
     * Given Scenario: Phase 3 (OS only). OS has T1.
     * When : listDotCMSIndices() is called.
     * Then : Only OS indices appear — ES decommissioned.
     * Skip   : when ES and OS endpoints are the same cluster.
     */
    @Test
    public void test_listDotCMSIndices_phase3_returnsOsIndicesOnly() {
        assumeFalse("Requires separate ES and OS clusters (ES and OS point to same endpoint)",
                esSameAsOs());
        setPhase(3);

        final List<String> indices = contentletIndexAPI().listDotCMSIndices();

        assertTrue("OS working must appear in Phase 3",  indices.contains(OS_WORKING));
        assertFalse("ES working must NOT appear in Phase 3 (ES decommissioned)",
                indices.contains(ES_WORKING));

        Logger.info(this, "✅ listDotCMSIndices Phase 3 — OS only: " + indices);
    }

    // =========================================================================
    // createContentIndex — real dual-write fan-out
    // =========================================================================

    /**
     * Given Scenario: Phase 0. A new content index is requested.
     * When : createContentIndex() is called.
     * Then : Index created ONLY in ES. OS cluster unchanged.
     * Skip   : when ES and OS point to the same cluster.
     */
    @Test
    public void test_createContentIndex_phase0_createsOnlyInEs() throws IOException, DotIndexException {
        assumeFalse("Requires separate ES and OS clusters", esSameAsOs());
        setPhase(0);

        assertFalse("Pre: not in ES yet", esImpl().indexExists(DUAL_WORKING));
        assertFalse("Pre: not in OS yet", osIndexAPI.indexExists(physicalDualWorking));

        contentletIndexAPI().createContentIndex(DUAL_WORKING, 1);

        assertTrue("Must exist in ES after Phase 0 createContentIndex",
                esImpl().indexExists(DUAL_WORKING));
        assertFalse("Must NOT exist in OS in Phase 0",
                osIndexAPI.indexExists(physicalDualWorking));

        Logger.info(this, "✅ createContentIndex Phase 0 — ES only: " + DUAL_WORKING);
    }

    /**
     * Given Scenario: Phase 1 (dual-write). A new content index is requested.
     * When : createContentIndex() is called.
     * Then : Index created in BOTH clusters simultaneously — the core dual-write guarantee.
     *        Works in single-cluster profile too (same cluster receives both writes).
     */
    @Test
    public void test_createContentIndex_phase1_createsInBothClusters() throws IOException, DotIndexException {
        setPhase(1);

        assertFalse("Pre: not in ES yet", esImpl().indexExists(DUAL_WORKING));
        assertFalse("Pre: not in OS yet", osIndexAPI.indexExists(physicalDualWorking));

        contentletIndexAPI().createContentIndex(DUAL_WORKING, 1);

        assertTrue("Must exist in ES after Phase 1 (fan-out)",
                esImpl().indexExists(DUAL_WORKING));
        assertTrue("Must exist in OS after Phase 1 (fan-out)",
                osIndexAPI.indexExists(physicalDualWorking));

        Logger.info(this, "✅ createContentIndex Phase 1 — both: " + DUAL_WORKING);
    }

    /**
     * Given Scenario: Phase 3. A new content index is requested.
     * When : createContentIndex() is called.
     * Then : Index created ONLY in OS.
     * Skip   : when ES and OS point to the same cluster.
     */
    @Test
    public void test_createContentIndex_phase3_createsOnlyInOs() throws IOException, DotIndexException {
        assumeFalse("Requires separate ES and OS clusters", esSameAsOs());
        setPhase(3);

        assertFalse("Pre: not in ES yet", esImpl().indexExists(DUAL_WORKING));
        assertFalse("Pre: not in OS yet", osIndexAPI.indexExists(physicalDualWorking));

        contentletIndexAPI().createContentIndex(DUAL_WORKING, 1);

        assertFalse("Must NOT exist in ES in Phase 3 (ES decommissioned)",
                esImpl().indexExists(DUAL_WORKING));
        assertTrue("Must exist in OS after Phase 3",
                osIndexAPI.indexExists(physicalDualWorking));

        Logger.info(this, "✅ createContentIndex Phase 3 — OS only: " + DUAL_WORKING);
    }

    /**
     * Given Scenario: Phase 1 (dual-write). {@code DUAL_WORKING} is created in both clusters via
     *                 the fan-out (ES bare, OS {@code .os}).
     * When : {@code contentletIndexAPI().delete(DUAL_WORKING)} is called with the BARE logical name
     *        — exactly what {@code ESIndexResource}/the router hand in.
     * Then : both the ES index AND the OS {@code .os} index are removed. Regression for the orphan
     *        bug where delete routed the bare name straight to the OS provider, missed the real
     *        {@code .os} index, and left it behind. Works in single-cluster mode because the OS
     *        physical name carries the {@code .os} tag, distinct from the ES bare name.
     */
    @Test
    public void test_delete_phase1_removesFromBothClusters() throws IOException, DotIndexException {
        setPhase(1);

        contentletIndexAPI().createContentIndex(DUAL_WORKING, 1);
        assertTrue("Pre: must exist in ES", esImpl().indexExists(DUAL_WORKING));
        assertTrue("Pre: must exist in OS", osIndexAPI.indexExists(physicalDualWorking));

        contentletIndexAPI().delete(DUAL_WORKING);

        assertFalse("ES index must be gone after delete", esImpl().indexExists(DUAL_WORKING));
        assertFalse("OS .os index must be gone after delete (must not be orphaned)",
                osIndexAPI.indexExists(physicalDualWorking));

        Logger.info(this, "✅ delete Phase 1 — both removed: " + DUAL_WORKING);
    }

    /**
     * Given Scenario: Phase 1 (dual-write). The supplied name exists in NEITHER engine
     *                 ({@code GHOST_WORKING} was never created anywhere) — e.g. cleaning up a
     *                 dangling DB pointer whose cluster index is already gone.
     * When : {@code delete(GHOST_WORKING)} is called.
     * Then : the delete is a benign, idempotent no-op — a missing index means the delete goal
     *        (index absent) is already met, so the primary provider's {@code index_not_found} is
     *        NOT propagated as a failure. This reconciles #36430's primary-failure propagation
     *        with the #35640 {@code lastOsSlot} cleanup path. GENUINE primary failures (engine
     *        down / auth) still propagate — covered by the unit test
     *        {@code ContentletIndexAPIImplDeletePropagationTest}.
     */
    @Test
    public void test_delete_phase1_nameInNeither_isBenignNoOp() {
        setPhase(1);

        assertFalse("Pre: ghost name must not exist in ES", esImpl().indexExists(GHOST_WORKING));
        assertFalse("Pre: ghost name must not exist in OS",
                osIndexAPI.indexExists(opsOS.toPhysicalName(GHOST_WORKING)));

        // A missing index is idempotent-success, not an error: no exception is thrown.
        final boolean result = contentletIndexAPI().delete(GHOST_WORKING);
        assertTrue("Deleting a name that exists in neither engine is a benign no-op", result);

        Logger.info(this, "✅ delete Phase 1 — name in neither engine is a benign no-op: " + GHOST_WORKING);
    }

    /**
     * Given Scenario: Phase 1 (dual-write). {@code DUAL_WORKING} exists in both clusters.
     * When : {@code delete()} is called with the {@code .os}-tagged name (as the QA/preview UI
     *        shows the OS index), with cascade on (default).
     * Then : BOTH twins are removed — the cascade is bidirectional, so deleting by the OS name
     *        also removes the ES twin (the tag is stripped to the logical name and broadcast to
     *        every provider). Regression for the one-directional-cascade bug (issue #35640).
     */
    @Test
    public void test_delete_phase1_byOsName_removesFromBothClusters()
            throws IOException, DotIndexException {
        setPhase(1);

        contentletIndexAPI().createContentIndex(DUAL_WORKING, 1);
        assertTrue("Pre: must exist in ES", esImpl().indexExists(DUAL_WORKING));
        assertTrue("Pre: must exist in OS", osIndexAPI.indexExists(physicalDualWorking));

        contentletIndexAPI().delete(IndexTag.OS.tag(DUAL_WORKING)); // delete by the .os name

        assertFalse("ES twin must be gone when deleting by the .os name (bidirectional cascade)",
                esImpl().indexExists(DUAL_WORKING));
        assertFalse("OS .os index must be gone", osIndexAPI.indexExists(physicalDualWorking));

        Logger.info(this, "✅ delete Phase 1 by .os name — both removed: " + DUAL_WORKING);
    }

    /**
     * Given Scenario: Phase 2 (OS serves reads). The ES store and OS store have <strong>diverged</strong>:
     *                 the ES store points at {@code working_es/live_es} while the OS store — the read
     *                 source in Phase 2 — points at the differently-named {@code working_div/live_div}.
     *                 This is swicken's swallowed-switchover divergence (issue #35640).
     * When : {@code delete()} is attempted on the OS-store active index by its {@code .os} name, and on
     *        the ES-store active index by its bare name.
     * Then : BOTH are rejected. The guard now unions the protected set from <strong>both</strong> stores
     *        in the dual-write phases, so the live OS index is protected even though its name is absent
     *        from the ES store. Before the fix the protected set was ES-store-only and the {@code .os}
     *        delete went through, wiping the index serving every read.
     */
    @Test
    public void test_delete_phase2_divergentOsName_isProtectedByGuard() throws Exception {
        setPhase(2);
        final String esWorking = "working_es_"  + RUN_ID;
        final String esLive    = "live_es_"     + RUN_ID;
        final String osWorking = "working_div_" + RUN_ID;
        final String osLive    = "live_div_"    + RUN_ID;

        // ES store points at the T0 names...
        APILocator.getIndiciesAPI().point(new IndiciesInfo.Builder()
                .setWorking(esImpl().getNameWithClusterIDPrefix(esWorking))
                .setLive(esImpl().getNameWithClusterIDPrefix(esLive))
                .build());
        // ...while the OS store (read source in Phase 2) points at DIFFERENT names.
        APILocator.getVersionedIndicesAPI().saveIndices(VersionedIndicesImpl.builder()
                .working(opsOS.toPhysicalName(osWorking))
                .live(opsOS.toPhysicalName(osLive))
                .build());

        // The OS-store active index must be protected even though the ES store does not know it.
        assertThrows("Active OS index (divergent name) must be protected from delete",
                DotStateException.class,
                () -> contentletIndexAPI().delete(IndexTag.OS.tag(osWorking)));
        // The ES-store active index must remain protected too.
        assertThrows("Active ES index must remain protected",
                DotStateException.class,
                () -> contentletIndexAPI().delete(esWorking));

        Logger.info(this, "✅ delete Phase 2 — divergent OS name protected by the union guard");
    }

    /**
     * Given Scenario: Phase 1 (dual-write). {@code DUAL_WORKING} exists in both engines and is the
     *                 active working index.
     * When : {@code clearIndex()} is called on it without the bypass flag.
     * Then : it is rejected ({@link DotStateException}) and BOTH twins survive — clear is
     *        delete + recreate-empty, so it is guarded exactly like delete (issue #35640, TC-018).
     *        With the bypass flag the clear goes through and both twins still exist (recreated).
     */
    @Test
    public void test_clear_phase1_activeIndex_isRejectedUnlessBypass() throws Exception {
        setPhase(1);
        contentletIndexAPI().createContentIndex(DUAL_WORKING, 1);
        // Make DUAL_WORKING the active working index in the ES store (read source in Phase 1).
        APILocator.getIndiciesAPI().point(new IndiciesInfo.Builder()
                .setWorking(esImpl().getNameWithClusterIDPrefix(DUAL_WORKING))
                .build());

        assertThrows("clear must be rejected on the active index",
                DotStateException.class,
                () -> APILocator.getESIndexAPI().clearIndex(DUAL_WORKING));
        assertTrue("ES twin must survive a rejected clear", esImpl().indexExists(DUAL_WORKING));
        assertTrue("OS twin must survive a rejected clear",  osIndexAPI.indexExists(physicalDualWorking));

        // Bypass: the same clear now goes through; both twins still exist (recreated empty).
        Config.setProperty(ContentletIndexAPIImpl.ALLOW_ACTIVE_INDEX_DELETE, true);
        try {
            APILocator.getESIndexAPI().clearIndex(DUAL_WORKING);
            assertTrue("ES twin exists after bypassed clear (recreated)", esImpl().indexExists(DUAL_WORKING));
            assertTrue("OS twin exists after bypassed clear (recreated)", osIndexAPI.indexExists(physicalDualWorking));
        } finally {
            Config.setProperty(ContentletIndexAPIImpl.ALLOW_ACTIVE_INDEX_DELETE, false);
        }

        Logger.info(this, "✅ clear Phase 1 — active index guarded, bypass works: " + DUAL_WORKING);
    }

    /**
     * Given Scenario: Phase 1. The OS store has exactly ONE populated slot
     *                 ({@code working = logicalA.os}) — the state left after cleaning up a stale
     *                 pair one index at a time.
     * When : that last index is deleted (bypass on, since any single populated slot is "active").
     * Then : the OS store ROW is removed, not left dangling. {@code saveIndices} contractually
     *        rejects an empty record, so {@code clearOsStorePointer} must fall back to
     *        {@code removeVersion} (issue #35640, swicken review). A surviving row would let
     *        {@code initOSCatchup} resurrect the deleted index empty on the next restart.
     */
    @Test
    public void test_delete_lastOsSlot_removesStoreRowInsteadOfDanglingPointer() throws Exception {
        setPhase(1);
        final String logicalA = "working_last_" + RUN_ID;
        APILocator.getVersionedIndicesAPI().saveIndices(VersionedIndicesImpl.builder()
                .working(opsOS.toPhysicalName(logicalA))
                .build());
        assertTrue("Pre: OS store row must exist with one slot",
                APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices().isPresent());

        Config.setProperty(ContentletIndexAPIImpl.ALLOW_ACTIVE_INDEX_DELETE, true);
        try {
            contentletIndexAPI().delete(IndexTag.OS.tag(logicalA));
        } finally {
            Config.setProperty(ContentletIndexAPIImpl.ALLOW_ACTIVE_INDEX_DELETE, false);
        }

        assertTrue("OS store row must be removed once its last slot was deleted (no dangling pointer)",
                APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices().isEmpty());

        Logger.info(this, "✅ delete last OS slot — store row removed, no dangling pointer");
    }

    /**
     * Given Scenario: Phase 1 (dual-write). {@code DUAL_WORKING} exists in both clusters, so a
     *                 real flush must reach the ES index (bare name) and the OS index ({@code .os}).
     * When : flushCaches() is called with the mixed list (ES bare + OS {@code .os}), exactly as the
     *        {@code /api/v1/esindex/cache} endpoint does via {@code listDotCMSIndices()}.
     * Then : the call succeeds with NO failed shards — each engine is flushed only with the names
     *        it owns, instead of every engine receiving the other's names and hitting
     *        {@code index_not_found_exception} (issue #35640).
     */
    @Test
    public void test_flushCaches_phase1_flushesBothEnginesWithoutCrossContamination()
            throws IOException, DotIndexException {
        setPhase(1);

        contentletIndexAPI().createContentIndex(DUAL_WORKING, 1);
        assertTrue("Pre: must exist in ES", esImpl().indexExists(DUAL_WORKING));
        assertTrue("Pre: must exist in OS", osIndexAPI.indexExists(physicalDualWorking));

        final List<String> mixed = List.of(DUAL_WORKING, IndexTag.OS.tag(DUAL_WORKING));
        final Map<String, Integer> result = APILocator.getESIndexAPI().flushCaches(mixed);

        assertNotNull(result);
        assertEquals("No failed shards — no engine received the other's index names",
                Integer.valueOf(0), result.get("failedShards"));
        assertTrue("Both twins must still exist after a cache flush (flush is not a delete)",
                esImpl().indexExists(DUAL_WORKING) && osIndexAPI.indexExists(physicalDualWorking));

        Logger.info(this, "✅ flushCaches Phase 1 — both engines flushed cleanly: " + result);
    }

    /**
     * Given Scenario: Phase 1 (dual-write). {@code DUAL_WORKING} exists in both clusters.
     * When : {@code closeIndex()} / {@code openIndex()} are called with the bare (ES) name.
     * Then : the lifecycle op is mirrored to BOTH engines — the OS twin is closed/opened too, not
     *        just ES. Under the transparent-mirror principle a user lifecycle op applies to the
     *        whole mirror; the OS leg receives the {@code .os} name so it targets the real OS index
     *        instead of a bare name it does not hold (issue #35640). Exercises the {@code providerName}
     *        routing shared by clear/open/close/updateReplicas.
     */
    @Test
    public void test_closeAndOpen_phase1_mirrorToBothEngines() throws Exception {
        setPhase(1);

        contentletIndexAPI().createContentIndex(DUAL_WORKING, 1);
        assertTrue("Pre: must exist in ES", esImpl().indexExists(DUAL_WORKING));
        assertTrue("Pre: must exist in OS", osIndexAPI.indexExists(physicalDualWorking));

        APILocator.getESIndexAPI().closeIndex(DUAL_WORKING); // close by the bare name
        assertTrue("ES twin must be closed", esImpl().isIndexClosed(DUAL_WORKING));
        assertTrue("OS twin must be closed too (mirror)",
                osIndexAPI.isIndexClosed(IndexTag.OS.tag(DUAL_WORKING)));

        APILocator.getESIndexAPI().openIndex(DUAL_WORKING); // reopen by the bare name
        assertFalse("ES twin must be open again", esImpl().isIndexClosed(DUAL_WORKING));
        assertFalse("OS twin must be open again (mirror)",
                osIndexAPI.isIndexClosed(IndexTag.OS.tag(DUAL_WORKING)));

        Logger.info(this, "✅ close/open Phase 1 — mirrored to both engines: " + DUAL_WORKING);
    }

    // =========================================================================
    // Orphan bootstrap repair — bare cluster index missing from the store (#36237)
    // =========================================================================

    /**
     * Given Scenario: Phase 1. A <b>bare</b> OS index physically exists in the cluster (default
     *                 settings, empty mapping — no custom {@code my_analyzer}, no dotCMS dynamic
     *                 templates) but is absent from the index store. This is the orphan left by a
     *                 prior bootstrap that created the index but never committed its store pointer,
     *                 reproduced bare exactly as QA did for #36237 TC-003.
     * When : the bootstrap seam {@code createContentIndex(name, shards, OS, ...)} runs.
     * Then : the orphan is deleted and recreated from scratch, so the index now carries the FULL
     *        settings (custom {@code my_analyzer}) and base mapping (dotCMS dynamic templates).
     *        This is the regression guard for TC-003 — reusing the bare orphan in place left it
     *        half-mapped and triggered {@code putMapping HTTP 400} (analyzer not found).
     */
    @Test
    public void test_bootstrap_bareOrphan_phase1_isRecreatedWithFullSettingsAndMapping()
            throws Exception {
        setPhase(1);

        // Pre: create a BARE orphan — default settings, no mapping (QA recreated it as PUT {}).
        osIndexAPI.createIndex(physicalDualWorking, 0);
        assertTrue("Pre: bare orphan must exist in OS",
                osIndexAPI.indexExists(physicalDualWorking));
        assertFalse("Pre: bare orphan must NOT yet carry the dotCMS dynamic templates",
                mappingOps.getMapping(DUAL_WORKING).contains("template_1"));
        assertFalse("Pre: bare orphan must NOT yet define the custom my_analyzer",
                analyzers().containsKey("my_analyzer"));

        // When: run the orphan-aware bootstrap seam against the real OS collaborators.
        final boolean result = ((ContentletIndexAPIImpl) contentletIndexAPI())
                .createContentIndex(DUAL_WORKING, 1, IndexTag.OS,
                        opsOS, osIndexAPI, MappingHelper.getInstance());

        // Then: the orphan was repaired — full settings + base mapping are present.
        assertTrue("Bootstrap must report the recreated index as available", result);
        assertTrue("Recreated index must exist in OS",
                osIndexAPI.indexExists(physicalDualWorking));
        assertTrue("Recreated index must carry the dotCMS dynamic templates (base mapping restored)",
                mappingOps.getMapping(DUAL_WORKING).contains("template_1"));
        assertTrue("Recreated index must define the custom my_analyzer (settings restored)",
                analyzers().containsKey("my_analyzer"));

        Logger.info(this, "✅ bootstrap bare-orphan Phase 1 — recreated with full settings + mapping");
    }

    /**
     * Given Scenario: Phase 0. ES store is currently pointing at the pre-existing app index.
     * When : activateIndex(ES_WORKING) is called.
     * Then : ES DB working slot is updated to T0.
     *        OS DB is unchanged compared to what it was before this call
     *        (Phase 0 → isMigrationStarted() = false → OS mirror block never executes).
     */
    @Test
    public void test_activateIndex_phase0_updatesEsDbOnly() throws DotDataException {
        setPhase(0);
        // Capture OS state immediately before the call — for before/after comparison
        final Optional<VersionedIndices> osBefore =
                APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices();

        contentletIndexAPI().activateIndex(ES_WORKING);

        final IndiciesInfo esInfo = APILocator.getIndiciesAPI().loadIndicies();
        assertTrue("ES DB working slot must hold the T0 physical name",
                esInfo.getWorking() != null && esInfo.getWorking().endsWith(ES_WORKING));

        // OS DB must be exactly as it was before — activateIndex in Phase 0 must not touch it
        final Optional<VersionedIndices> osAfter =
                APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices();
        assertEquals("OS DB working slot must be unchanged by Phase 0 activateIndex",
                osBefore.flatMap(VersionedIndices::working).orElse(null),
                osAfter.flatMap(VersionedIndices::working).orElse(null));

        Logger.info(this, "✅ activateIndex Phase 0 — ES DB: " + esInfo.getWorking());
    }

    /**
     * Given Scenario: Phase 2 (dual-write, OS reads). Caller passes the ES index name T0.
     * When : activateIndex(ES_WORKING) is called.
     * Then : ES DB updated with T0.
     *        OS DB mirror ALSO updated with T0 — even though OS physically holds T1.
     *        This documents the known mismatch: the OS DB pointer reflects the name
     *        passed in, regardless of which index the OS cluster actually holds.
     */
    @Test
    public void test_activateIndex_phase2_esName_mirroredToOsDb() throws DotDataException {
        setPhase(2);

        contentletIndexAPI().activateIndex(ES_WORKING);

        final IndiciesInfo esInfo = APILocator.getIndiciesAPI().loadIndicies();
        assertTrue("ES DB must hold T0 physical name",
                esInfo.getWorking() != null && esInfo.getWorking().endsWith(ES_WORKING));

        final Optional<VersionedIndices> osInfo =
                APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices();
        assertTrue("OS DB must be populated with the mirrored name",
                osInfo.isPresent()
                && osInfo.get().working()
                        .map(IndexTag.OS::untag)
                        .map(w -> w.endsWith(ES_WORKING))
                        .orElse(false));

        Logger.info(this, "✅ activateIndex Phase 2 (ES name) — ES DB: " + esInfo.getWorking()
                + ", OS DB: " + osInfo.map(v -> v.working().orElse("(empty)")).orElse("(absent)"));
    }

    /**
     * Given Scenario: Phase 2. Caller passes the OS index name T1.
     * When : activateIndex(OS_WORKING) is called.
     * Then : Both pointer stores consistently point to T1.
     */
    @Test
    public void test_activateIndex_phase2_osName_updatesBothDbsConsistently() throws DotDataException {
        setPhase(2);

        contentletIndexAPI().activateIndex(OS_WORKING);

        final IndiciesInfo esInfo = APILocator.getIndiciesAPI().loadIndicies();
        assertTrue("ES DB must point to T1",
                esInfo.getWorking() != null && esInfo.getWorking().endsWith(OS_WORKING));

        final Optional<VersionedIndices> osInfo =
                APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices();
        assertTrue("OS DB must also point to T1",
                osInfo.isPresent()
                && osInfo.get().working()
                        .map(IndexTag.OS::untag)
                        .map(w -> w.endsWith(OS_WORKING))
                        .orElse(false));

        Logger.info(this, "✅ activateIndex Phase 2 (OS name) — both DBs point to T1");
    }

    /**
     * Given Scenario: Phase 3.
     * When : activateIndex(OS_WORKING) is called.
     * Then : Only OS DB updated. Legacy ES DB unchanged.
     */
    @Test
    public void test_activateIndex_phase3_onlyOsDbUpdated() throws DotDataException {
        setPhase(3);
        final String esWorkingBefore = APILocator.getIndiciesAPI().loadIndicies().getWorking();

        contentletIndexAPI().activateIndex(OS_WORKING);

        assertEquals("ES DB must NOT be touched in Phase 3",
                esWorkingBefore, APILocator.getIndiciesAPI().loadIndicies().getWorking());
        final Optional<VersionedIndices> osInfo =
                APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices();
        assertTrue("OS DB must hold T1",
                osInfo.isPresent()
                && osInfo.get().working()
                        .map(IndexTag.OS::untag)
                        .map(w -> w.endsWith(OS_WORKING))
                        .orElse(false));

        Logger.info(this, "✅ activateIndex Phase 3 — only OS DB updated");
    }

    // =========================================================================
    // fire-and-forget — real index_not_found handling at cluster level
    // =========================================================================

    /**
     * Given Scenario: Phase 1 (dual-write, ES reads). ES has T0; OS has T1.
     * When : closeIndex("working_T0") is called — T0 exists in ES but NOT in OS.
     * Then : ES close succeeds; OS throws {@code index_not_found} for T0 (shadow in Phase 1).
     *        Exception is swallowed — the call returns normally.
     *        This is the exact bug scenario from #35302.
     *
     * <p>Skip: when ES and OS point to the same cluster — in that case T0 exists in
     * both client views so neither throws {@code index_not_found} and the
     * fire-and-forget path is not exercised.</p>
     */
    @Test
    public void test_closeIndex_phase1_osIndexNotFound_isSwallowed() {
        assumeFalse("Requires separate ES and OS clusters to get real index_not_found from OS",
                esSameAsOs());
        setPhase(1);

        try {
            APILocator.getESIndexAPI().closeIndex(ES_WORKING);
            Logger.info(this, "✅ closeIndex Phase 1 — OS index_not_found swallowed successfully");
        } catch (final RuntimeException e) {
            throw new AssertionError(
                    "closeIndex must NOT throw in Phase 1 when OS has a different index name. Got: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Given Scenario: Phase 1. ES has T0; OS has T1. Caller uses the OS name T1.
     * When : closeIndex("working_T1") is called — T1 exists in OS but NOT in ES.
     * Then : ES is primary in Phase 1 → ES throws {@code index_not_found} for T1.
     *        Primary failure PROPAGATES — callers must not use the OS-native name in Phase 1.
     *
     * <p>Skip: when ES and OS point to the same cluster — T1 exists in the ES view too.</p>
     */
    @Test(expected = RuntimeException.class)
    public void test_closeIndex_phase1_esIndexNotFound_propagates() {
        assumeFalse("Requires separate ES and OS clusters", esSameAsOs());
        setPhase(1);

        // OS_WORKING exists in OS but NOT in ES — ES is primary → must throw
        APILocator.getESIndexAPI().closeIndex(OS_WORKING);
    }

    /**
     * Given Scenario: Phase 3 (OS only). OS has T1; T0 does not exist in OS.
     * When : closeIndex("working_T0") is called.
     * Then : OS is the only provider and is primary → exception propagates.
     *
     * <p>Skip: when ES and OS point to the same cluster — T0 also exists in the OS view.</p>
     */
    @Test(expected = RuntimeException.class)
    public void test_closeIndex_phase3_osIndexNotFound_propagates() {
        assumeFalse("Requires separate ES and OS clusters", esSameAsOs());
        setPhase(3);

        APILocator.getESIndexAPI().closeIndex(ES_WORKING);
    }

    // =========================================================================
    // deactivateIndex — ghost index (name never created in any cluster)
    //
    // deactivateIndex is a pure pointer-store operation: it does NOT query the cluster
    // to verify the index exists. It uses IndexType.WORKING.is(name) (a startsWith check)
    // to decide which DB slot to clear, then writes the result back to the store.
    //
    // Calling it with a name that was never created must:
    //   - not throw (cluster absence is irrelevant)
    //   - clear the working slot in the primary pointer store
    // =========================================================================

    /**
     * Given Scenario: Phase 1 (dual-write, ES reads). {@code GHOST_WORKING} starts with
     *                 "working" but was never created in either cluster.
     * When : deactivateIndex(GHOST_WORKING) is called.
     * Then : No exception — {@code deactivateIndex} never validates cluster existence.
     *        The ES DB working slot is cleared (name matched {@link IndexType#WORKING}).
     *        The OS DB working slot is also cleared (mirrored in Phase 1).
     *
     * <p>This documents the fundamental contract: {@code deactivateIndex} is a
     * pointer-store update driven by the name pattern, not by cluster state.</p>
     */
    @Test
    @SuppressWarnings("deprecation")
    public void test_deactivateIndex_ghostIndex_phase1_clearsWorkingSlot()
            throws DotDataException, IOException {
        setPhase(1);

        // Verify the ghost index truly doesn't exist in either cluster
        assertFalse("Pre: ghost must not exist in ES", esImpl().indexExists(GHOST_WORKING));
        assertFalse("Pre: ghost must not exist in OS", osIndexAPI.indexExists(GHOST_WORKING));

        // Give BOTH working AND live slots a known value.
        // The live slot must survive the deactivate call so that the VersionedIndices
        // builder always has at least one field set (saveIndices rejects empty builders).
        contentletIndexAPI().activateIndex(ES_WORKING);
        contentletIndexAPI().activateIndex(ES_LIVE);
        assertNotNull("Pre: ES DB working slot must be non-null before deactivate",
                APILocator.getIndiciesAPI().loadIndicies().getWorking());

        // deactivateIndex with a ghost name must NOT throw
        contentletIndexAPI().deactivateIndex(GHOST_WORKING);

        // ES DB working slot must be null — GHOST_WORKING starts with "working",
        // so IndexType.WORKING.is(GHOST_WORKING) == true → builder.setWorking(null)
        final IndiciesInfo esInfo = APILocator.getIndiciesAPI().loadIndicies();
        assertNull("ES DB working slot must be cleared after deactivating a ghost index",
                esInfo.getWorking());

        // OS DB working slot must also be cleared (mirrored in Phase 1).
        // The live slot in OS must be preserved (builder kept it because IndexType.LIVE
        // did not match GHOST_WORKING, so the save is valid).
        final Optional<VersionedIndices> osInfo =
                APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices();
        assertTrue("OS DB working slot must be cleared (Phase 1 mirrors ES deactivation)",
                osInfo.map(v -> v.working().isEmpty()).orElse(true));
        assertTrue("OS DB live slot must be preserved after deactivating the working slot",
                osInfo.flatMap(VersionedIndices::live).isPresent());

        Logger.info(this,
                "✅ deactivateIndex Phase 1 cleared both DB working slots for ghost index: "
                + GHOST_WORKING);
    }

    /**
     * Given Scenario: Phase 3 (OS only). {@code GHOST_WORKING} was never created in OS.
     * When : deactivateIndex(GHOST_WORKING) is called.
     * Then : No exception — OS is the primary store but cluster existence is not validated.
     *        The OS DB working slot is cleared; the legacy ES DB is not touched.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void test_deactivateIndex_ghostIndex_phase3_clearsOsWorkingSlot()
            throws DotDataException, IOException {
        setPhase(3);

        assertFalse("Pre: ghost must not exist in OS", osIndexAPI.indexExists(GHOST_WORKING));

        // Give BOTH working AND live slots a known value.
        // The live slot must survive the deactivate call so that VersionedIndicesAPI
        // never sees an empty builder (saveIndices rejects empty builders).
        contentletIndexAPI().activateIndex(OS_WORKING);
        contentletIndexAPI().activateIndex(OS_LIVE);
        assertTrue("Pre: OS DB working slot must be set",
                APILocator.getVersionedIndicesAPI()
                        .loadDefaultVersionedIndices()
                        .flatMap(VersionedIndices::working)
                        .isPresent());

        // Capture ES DB state — Phase 3 must not touch it
        final String esWorkingBefore =
                APILocator.getIndiciesAPI().loadIndicies().getWorking();

        // Must not throw
        contentletIndexAPI().deactivateIndex(GHOST_WORKING);

        // OS DB working slot cleared; live slot preserved (builder had live → save valid)
        final Optional<VersionedIndices> osAfter =
                APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices();
        assertTrue("OS DB working slot must be cleared after deactivating a ghost index",
                osAfter.map(v -> v.working().isEmpty()).orElse(true));
        assertTrue("OS DB live slot must be preserved after deactivating the working slot",
                osAfter.flatMap(VersionedIndices::live).isPresent());

        // Legacy ES DB untouched in Phase 3
        final String esWorkingAfter =
                APILocator.getIndiciesAPI().loadIndicies().getWorking();
        assertEquals("ES DB must NOT be touched by deactivateIndex in Phase 3",
                esWorkingBefore, esWorkingAfter);

        Logger.info(this,
                "✅ deactivateIndex Phase 3 cleared OS DB working slot without touching ES DB");
    }

    // =========================================================================
    // Invalid index names — cluster rejection must always propagate
    //
    // The fire-and-forget mechanism silences shadow index_not_found errors only.
    // A cluster-level rejection of a syntactically invalid name is a primary failure
    // and must propagate regardless of phase.
    // =========================================================================

    /**
     * Given Scenario: Phase 1 (dual-write, ES reads). Index name contains spaces.
     * When : createContentIndex(invalidName) is called.
     * Then : Both providers reject the name at the cluster level.
     *        {@link ContentletIndexAPI#createContentIndex(String,int)}
     *        absorbs provider-level exceptions and converts them to a {@code false} return value —
     *        this is the documented soft-failure contract for that method.
     *        The return value {@code false} signals to the caller that no cluster acknowledged the request.
     */
    @Test
    public void test_createContentIndex_invalidName_phase1_returnsFalse()
            throws IOException, DotIndexException {
        setPhase(1);

        final boolean result = contentletIndexAPI().createContentIndex(INVALID_INDEX_NAME, 1);

        assertFalse(
                "createContentIndex must return false when clusters reject an invalid index name (Phase 1)",
                result);
        Logger.info(this, "✅ createContentIndex Phase 1 returned false for invalid name (as expected)");
    }

    /**
     * Given Scenario: Phase 3 (OS only). Index name contains spaces.
     * When : createContentIndex(invalidName) is called.
     * Then : OS is the sole provider and rejects the name. The soft-failure return value
     *        {@code false} surfaces — the same contract as Phase 1, with a single-provider path.
     */
    @Test
    public void test_createContentIndex_invalidName_phase3_returnsFalse()
            throws IOException, DotIndexException {
        setPhase(3);

        final boolean result = contentletIndexAPI().createContentIndex(INVALID_INDEX_NAME, 1);

        assertFalse(
                "createContentIndex must return false when the cluster rejects an invalid index name (Phase 3)",
                result);
        Logger.info(this, "✅ createContentIndex Phase 3 returned false for invalid name (as expected)");
    }

    // =========================================================================
    // Phase 3 reindex cleanup — orphan ES rows in the indicies table (#36077)
    // =========================================================================

    /**
     * Given Scenario: Phase 3 (OS-only). The {@code indicies} table holds the exact orphan
     *                 state reported in #36077 after a Phase-3 reindex: the legacy ES
     *                 {@code live}/{@code working} pair plus the transient
     *                 {@code reindex_live}/{@code reindex_working} pair (all NULL version),
     *                 alongside the promoted OS {@code .os} (version 3.X) {@code live}/{@code working}
     *                 pair — and an unrelated {@code site_search} pointer (also NULL version).
     * When : {@link VersionedIndicesAPI#removeLegacyIndices()} runs (the cleanup step the
     *        Phase-3 switchover now invokes).
     * Then : the four legacy ES content rows are gone; the {@code site_search} row is preserved
     *        (it is NOT part of the content-index migration); and the OS rows are preserved
     *        (they carry a non-NULL version). The expected end state is the two OS rows only.
     *
     * <p>Asserts against the specific seeded names rather than table totals so the result is
     * independent of whatever rows the running instance already holds. The blanket delete also
     * removes the live instance's real ES content rows; {@code @After} restores them via
     * {@code IndiciesAPI.point(savedEsInfo)}.</p>
     */
    @Test
    public void test_phase3_removeLegacyIndices_purgesEsRowsPreservesSiteSearchAndOs()
            throws DotDataException {
        setPhase(3);

        final String esWorking   = "cluster_test.working_20200101000000_"        + RUN_ID;
        final String esLive      = "cluster_test.live_20200101000000_"           + RUN_ID;
        final String esReindexWk = "cluster_test.working_20200102000000_"        + RUN_ID;
        final String esReindexLv = "cluster_test.live_20200102000000_"           + RUN_ID;
        final String esSiteSrch  = "cluster_test.sitesearch_20200101000000_"     + RUN_ID;
        final String osWorking   = "cluster_test.working_20260101000000_" + RUN_ID + ".os";
        final String osLive      = "cluster_test.live_20260101000000_"    + RUN_ID + ".os";
        final List<String> seeded = List.of(
                esWorking, esLive, esReindexWk, esReindexLv, esSiteSrch, osWorking, osLive);

        try {
            // Clear the content-index slots first so the seeded orphan state is deterministic.
            // Bootstrap already populated (working|live, NULL) and (working|live, 3.X) rows; the
            // OS seeds below reuse those (index_type, index_version) pairs and would otherwise hit
            // the uq_index_type_version UNIQUE constraint. The @After restores the saved ES/OS
            // indices state, so removing these rows mid-test is safe.
            clearContentIndiciesRows();

            insertIndiciesRow(esWorking,   "working",         null);
            insertIndiciesRow(esLive,      "live",            null);
            insertIndiciesRow(esReindexWk, "reindex_working", null);
            insertIndiciesRow(esReindexLv, "reindex_live",    null);
            insertIndiciesRow(esSiteSrch,  "site_search",     null);
            insertIndiciesRow(osWorking,   "working",         VersionedIndices.OPENSEARCH_3X);
            insertIndiciesRow(osLive,      "live",            VersionedIndices.OPENSEARCH_3X);

            final int removed = APILocator.getVersionedIndicesAPI().removeLegacyIndices();

            assertTrue("Cleanup must remove at least our 4 seeded ES content rows",
                    removed >= 4);
            assertEquals("Legacy ES content rows (NULL version) must be purged",
                    0L, countIndiciesRows(esWorking, esLive, esReindexWk, esReindexLv));
            assertEquals("site_search row must be preserved (not part of content migration)",
                    1L, countIndiciesRows(esSiteSrch));
            assertEquals("OS rows (version 3.X) must be preserved",
                    2L, countIndiciesRows(osWorking, osLive));

            Logger.info(this, "✅ Phase 3 cleanup purged orphan ES rows, kept site_search + OS");
        } finally {
            for (final String name : seeded) {
                Try.run(() -> new DotConnect()
                        .setSQL("DELETE FROM indicies WHERE index_name = ?")
                        .addParam(name).loadResult());
            }
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Removes every content-index pointer row (both legacy ES NULL-version rows and OS 3.X rows)
     * so a test can seed a controlled {@code indicies} state without colliding with the bootstrap
     * rows on the {@code uq_index_type_version} UNIQUE constraint. {@code site_search} is included
     * for completeness. The caller is responsible for restoring state (handled by {@code @After}).
     */
    private static void clearContentIndiciesRows() throws DotDataException {
        new DotConnect()
                .setSQL("DELETE FROM indicies WHERE index_type IN "
                        + "('working','live','reindex_working','reindex_live','site_search')")
                .loadResult();
    }

    private static void insertIndiciesRow(final String indexName, final String indexType,
            final String version) throws DotDataException {
        if (version == null) {
            // Legacy ES row: omit the version column so it stores NULL (matches IndiciesFactory.point).
            new DotConnect()
                    .setSQL("INSERT INTO indicies (index_name, index_type) VALUES (?, ?)")
                    .addParam(indexName).addParam(indexType)
                    .loadResult();
        } else {
            new DotConnect()
                    .setSQL("INSERT INTO indicies (index_name, index_type, index_version) VALUES (?, ?, ?)")
                    .addParam(indexName).addParam(indexType).addParam(version)
                    .loadResult();
        }
    }

    private static long countIndiciesRows(final String... indexNames) throws DotDataException {
        final String placeholders = String.join(",", java.util.Collections.nCopies(indexNames.length, "?"));
        final DotConnect dc = new DotConnect()
                .setSQL("SELECT COUNT(*) AS cnt FROM indicies WHERE index_name IN (" + placeholders + ")");
        for (final String name : indexNames) {
            dc.addParam(name);
        }
        final List<Map<String, Object>> rows = dc.loadResults();
        return Long.parseLong(rows.get(0).get("cnt").toString());
    }

    /**
     * Returns {@code true} when the ES client and OS client are configured to talk to
     * the same cluster endpoint. This happens in the {@code opensearch-upgrade} Maven profile,
     * where {@code DOT_ES_ENDPOINTS} is overridden to equal {@code OS_ENDPOINTS}.
     *
     * <p>Tests that require two physically separate clusters use this to skip themselves
     * via {@link org.junit.Assume#assumeFalse}.</p>
     */
    private static boolean esSameAsOs() {
        final String esEndpoint = Config.getStringProperty("DOT_ES_ENDPOINTS",
                "http://localhost:9207");
        final String osEndpoint = Config.getStringProperty("OS_ENDPOINTS",
                "http://localhost:9201");
        return esEndpoint.trim().equalsIgnoreCase(osEndpoint.trim());
    }

    private static void setPhase(final int ordinal) {
        Config.setProperty(FLAG_KEY, String.valueOf(ordinal));
    }

    /**
     * Reads back the analyzer map configured on the {@link #physicalDualWorking} OS index.
     * Returns an empty map when the index has no analysis settings (e.g. a bare orphan).
     */
    private Map<String, ?> analyzers() throws IOException {
        final OpenSearchClient client = clientProvider.getClient();
        final GetIndexResponse response = client.indices().get(b -> b.index(physicalDualWorking));
        final var indexState = response.result().get(physicalDualWorking);
        if (indexState == null || indexState.settings() == null
                || indexState.settings().index() == null
                || indexState.settings().index().analysis() == null) {
            return Map.of();
        }
        return indexState.settings().index().analysis().analyzer();
    }

    private static ContentletIndexAPI contentletIndexAPI() {
        return APILocator.getContentletIndexAPI();
    }

    private static ESIndexAPI esImpl() {
        return ((IndexAPIImpl) APILocator.getESIndexAPI()).esImpl();
    }

    private void cleanupTestIndices() {
        final ESIndexAPI esIndex = esImpl();
        for (final String name : List.of(ES_WORKING, ES_LIVE, DUAL_WORKING)) {
            Try.run(() -> { if (esIndex.indexExists(name)) esIndex.delete(name); })
               .onFailure(e -> Logger.warn(this,
                       "Cleanup: error removing ES index '" + name + "': " + e.getMessage()));
        }
        // OS_WORKING / OS_LIVE enter via osIndexAPI.createIndex directly (no tagging), so cleanup
        // with the bare name is symmetric. DUAL_WORKING enters via contentletIndexAPI().createContentIndex,
        // which routes through ContentletIndexOperationsOS.toPhysicalName — use the cached physical
        // name so cleanup deletes the same physical index that was created.
        for (final String name : List.of(OS_WORKING, OS_LIVE, physicalDualWorking)) {
            Try.run(() -> { if (osIndexAPI.indexExists(name)) osIndexAPI.delete(name); })
               .onFailure(e -> Logger.warn(this,
                       "Cleanup: error removing OS index '" + name + "': " + e.getMessage()));
        }
    }
}
