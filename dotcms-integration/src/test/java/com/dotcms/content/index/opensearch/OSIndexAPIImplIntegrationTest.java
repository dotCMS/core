package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.VersionedIndicesAPI;
import com.dotcms.content.index.VersionedIndicesImpl;
import com.dotcms.content.index.domain.CreateIndexStatus;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.GetIndexResponse;

import static com.dotcms.content.index.IndicesFactory.CLUSTER_PREFIX;

/**
 * Integration tests for {@link OSIndexAPIImpl} that exercise the index lifecycle against a live
 * OpenSearch 3.x container and verify that index names can be round-tripped through
 * {@link VersionedIndicesAPI} (the database layer).
 *
 * <p>Requires the {@code opensearch-upgrade} Docker container running on
 * {@code http://localhost:9201} with security disabled.
 * Registered in {@link com.dotcms.OpenSearchUpgradeSuite}.</p>
 *
 * <p>Run with:
 * <pre>
 *   ./mvnw verify -pl :dotcms-integration \
 *       -Dcoreit.test.skip=false \
 *       -Dopensearch.upgrade.test=true
 * </pre>
 * </p>
 *
 * @author Fabrizzio Araya
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class OSIndexAPIImplIntegrationTest extends IntegrationTestBase {

    /**
     * Unique suffix appended to every OS index name created by this suite.
     * Prevents cross-run pollution in a shared OpenSearch node.
     */
    private static final String RUN_ID =
            UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    /**
     * Version tag written to the {@code indicies} table for all rows created by this suite.
     * The {@code os-it-} prefix is used by {@link #cleanupVersionedRows()} for safe cleanup.
     */
    private static final String TEST_DB_VERSION = "os-it-" + RUN_ID;

    // ── index names reused across several tests ───────────────────────────────
    private static final String IDX_LIVE    = "live_"    + RUN_ID;
    private static final String IDX_WORKING = "working_" + RUN_ID;

    // ── CDI-injected beans ────────────────────────────────────────────────────
    // OSTestClientProvider (@Alternative @Priority(1)) is on the test classpath and will be used to configure our API for testing
    @Inject
    private OSIndexAPIImpl osIndexAPI;

    @Inject
    private VersionedIndicesAPI versionedIndicesAPI;

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void setUp() {
        cleanupTestOsIndices();
        cleanupVersionedRows();
    }

    @After
    public void tearDown() {
        cleanupTestOsIndices();
        cleanupVersionedRows();
    }

    // =========================================================================
    // Tests – OSIndexAPIImpl index lifecycle
    // =========================================================================

    /**
     * Given scenario: An index is created via {@link OSIndexAPIImpl#createIndex(String, int)}
     * Expected: {@link OSIndexAPIImpl#indexExists(String)} returns {@code true} immediately after
     */
    @Test
    public void test_createIndex_shouldExistAfterCreation() throws Exception {
        assertFalse("Pre-condition: index must not exist yet", osIndexAPI.indexExists(IDX_LIVE));

        final CreateIndexStatus status = osIndexAPI.createIndex(IDX_LIVE, 1);

        assertTrue("CreateIndexStatus must be acknowledged", status.acknowledged());
        assertTrue("Index must exist in OpenSearch after creation", osIndexAPI.indexExists(IDX_LIVE));
        Logger.info(this, "✅ test_createIndex_shouldExistAfterCreation passed");
    }

    /**
     * Given scenario: An existing index is removed via {@link OSIndexAPIImpl#delete(String)}
     * Expected: {@link OSIndexAPIImpl#indexExists(String)} returns {@code false} afterwards
     */
    @Test
    public void test_deleteIndex_shouldNotExistAfterDeletion() throws Exception {
        osIndexAPI.createIndex(IDX_LIVE, 1);
        assertTrue("Pre-condition: index must exist before deletion", osIndexAPI.indexExists(IDX_LIVE));

        final boolean ack = osIndexAPI.delete(IDX_LIVE);

        assertTrue("Delete must be acknowledged by OpenSearch", ack);
        assertFalse("Index must not exist in OpenSearch after deletion", osIndexAPI.indexExists(IDX_LIVE));
        Logger.info(this, "✅ test_deleteIndex_shouldNotExistAfterDeletion passed");
    }

    /**
     * Given scenario: Two indices are created
     * Expected: {@link OSIndexAPIImpl#listIndices()} contains both names without cluster prefix
     */
    @Test
    public void test_listIndices_shouldIncludeBothCreatedIndices() throws Exception {
        osIndexAPI.createIndex(IDX_LIVE, 1);
        osIndexAPI.createIndex(IDX_WORKING, 1);

        final Set<String> indices = osIndexAPI.listIndices();

        assertNotNull("listIndices must never return null", indices);
        assertTrue("listIndices must contain the live index",    indices.contains(IDX_LIVE));
        assertTrue("listIndices must contain the working index", indices.contains(IDX_WORKING));
        Logger.info(this, "✅ test_listIndices_shouldIncludeBothCreatedIndices passed – " + indices);
    }

    /**
     * Given scenario: Two indices exist and {@link OSIndexAPIImpl#deleteMultiple} is called
     * Expected: Both indices are absent from OpenSearch afterwards
     */
    @Test
    public void test_deleteMultiple_shouldDeleteAllRequestedIndices() throws Exception {
        osIndexAPI.createIndex(IDX_LIVE, 1);
        osIndexAPI.createIndex(IDX_WORKING, 1);

        final boolean allAck = osIndexAPI.deleteMultiple(IDX_LIVE, IDX_WORKING);

        assertTrue("deleteMultiple must be fully acknowledged", allAck);
        assertFalse("Live index must be gone after deleteMultiple",    osIndexAPI.indexExists(IDX_LIVE));
        assertFalse("Working index must be gone after deleteMultiple", osIndexAPI.indexExists(IDX_WORKING));
        Logger.info(this, "✅ test_deleteMultiple_shouldDeleteAllRequestedIndices passed");
    }

    /**
     * Given scenario: An open index is closed then reopened
     * Expected: After closing the index no longer appears in {@link OSIndexAPIImpl#listIndices()};
     *           after reopening it reappears
     */
    @Test
    public void test_closeAndOpenIndex_shouldTransitionCorrectly() throws Exception {
        osIndexAPI.createIndex(IDX_LIVE, 1);
        assertTrue("Pre-condition: newly created index must be open",
                osIndexAPI.listIndices().contains(IDX_LIVE));

        osIndexAPI.closeIndex(IDX_LIVE);
        assertFalse("Closed index must not appear in the open-index listing",
                osIndexAPI.listIndices().contains(IDX_LIVE));

        osIndexAPI.openIndex(IDX_LIVE);
        assertTrue("Reopened index must appear in the open-index listing",
                osIndexAPI.listIndices().contains(IDX_LIVE));
        Logger.info(this, "✅ test_closeAndOpenIndex_shouldTransitionCorrectly passed");
    }

    /**
     * Given scenario: A bare index name passes through
     *                 {@link OSIndexAPIImpl#getNameWithClusterIDPrefix} and back through
     *                 {@link OSIndexAPIImpl#removeClusterIdFromName}
     * Expected:
     * <ul>
     *   <li>The prefixed name contains the original name as suffix</li>
     *   <li>Applying the prefix a second time is a no-op (idempotent)</li>
     *   <li>Stripping the prefix restores the original bare name</li>
     * </ul>
     */
    @Test
    public void test_clusterPrefixRoundTrip_shouldBeIdempotentAndReversible() {
        final String withPrefix = osIndexAPI.getNameWithClusterIDPrefix(IDX_LIVE);

        assertTrue("Prefixed name must end with the original bare name",
                withPrefix.endsWith(IDX_LIVE));
        assertTrue("Prefixed name must contain a dot separator (cluster_<id>.<name>)",
                withPrefix.contains("."));

        assertEquals("Applying the prefix a second time must be a no-op",
                withPrefix, osIndexAPI.getNameWithClusterIDPrefix(withPrefix));

        assertEquals("Stripping the prefix must restore the original bare name",
                IDX_LIVE, osIndexAPI.removeClusterIdFromName(withPrefix));
        Logger.info(this, "✅ test_clusterPrefixRoundTrip_shouldBeIdempotentAndReversible passed"
                + " – prefixed form: " + withPrefix);
    }

    // =========================================================================
    // Tests – OSIndexAPIImpl + VersionedIndicesAPI integration
    // =========================================================================

    /**
     * Given scenario: Live and working indices are created in OpenSearch, their names are saved in
     *                 {@link VersionedIndicesAPI} under a test version, then reloaded from the DB.
     * Expected:
     * <ul>
     *   <li>The reloaded names exactly match what was stored</li>
     *   <li>Those names correspond to indices that actually exist in OpenSearch</li>
     * </ul>
     */
    @Test
    public void test_createIndicesInOS_andPersistInVersionedAPI_shouldRoundTrip()
            throws Exception {

        // 1. Create real indices in OpenSearch
        osIndexAPI.createIndex(IDX_LIVE, 1);
        osIndexAPI.createIndex(IDX_WORKING, 1);
        assertTrue("Pre-condition: live index must exist in OS",    osIndexAPI.indexExists(IDX_LIVE));
        assertTrue("Pre-condition: working index must exist in OS", osIndexAPI.indexExists(IDX_WORKING));

        // 2. Persist the names in the versioned registry
        versionedIndicesAPI.saveIndices(
                VersionedIndicesImpl.builder()
                        .version(TEST_DB_VERSION)
                        .live(IDX_LIVE)
                        .working(IDX_WORKING)
                        .build());

        assertTrue("Version must be registered in DB after save",
                versionedIndicesAPI.versionExists(TEST_DB_VERSION));

        // 3. Reload from DB
        final Optional<VersionedIndices> loaded = versionedIndicesAPI.loadIndices(TEST_DB_VERSION);
        assertTrue("Loaded VersionedIndices must be present", loaded.isPresent());
        final VersionedIndices reloaded = loaded.get();

        // 4. Names in DB must match what was written
        assertEquals("Live index name must survive the DB round-trip",
                IDX_LIVE, reloaded.live().orElse(null));
        assertEquals("Working index name must survive the DB round-trip",
                IDX_WORKING, reloaded.working().orElse(null));
        assertFalse("reindexLive must be absent (was not set)",
                reloaded.reindexLive().isPresent());

        // 5. Cross-check: names stored in DB point to real, existing OS indices
        assertTrue("Stored live name must resolve to a real OS index",
                osIndexAPI.indexExists(reloaded.live().orElseThrow()));
        assertTrue("Stored working name must resolve to a real OS index",
                osIndexAPI.indexExists(reloaded.working().orElseThrow()));

        Logger.info(this, "✅ test_createIndicesInOS_andPersistInVersionedAPI_shouldRoundTrip passed");
    }

    /**
     * Given scenario: Both indices are registered in {@link VersionedIndicesAPI}, then only the
     *                 live index is deleted from OpenSearch without updating the registry.
     * Expected: The registry still returns the live name, but {@link OSIndexAPIImpl#indexExists}
     *           reports it is gone — demonstrating that divergence between the two layers is
     *           detectable at runtime.
     */
    @Test
    public void test_osAndVersionedRegistry_divergence_shouldBeDetectable() throws Exception {
        osIndexAPI.createIndex(IDX_LIVE, 1);
        osIndexAPI.createIndex(IDX_WORKING, 1);

        versionedIndicesAPI.saveIndices(
                VersionedIndicesImpl.builder()
                        .version(TEST_DB_VERSION)
                        .live(IDX_LIVE)
                        .working(IDX_WORKING)
                        .build());

        // Delete only the live index from OpenSearch – registry is NOT updated
        osIndexAPI.delete(IDX_LIVE);
        assertFalse("Live index must be gone from OS after deletion",
                osIndexAPI.indexExists(IDX_LIVE));

        // Registry still has the live entry
        final Optional<VersionedIndices> loaded = versionedIndicesAPI.loadIndices(TEST_DB_VERSION);
        assertTrue("Registry entry must still be present in DB", loaded.isPresent());

        final String registeredLive = loaded.get().live().orElseThrow();
        assertFalse("Divergence: OS reports no index for the name stored in registry – " + registeredLive,
                osIndexAPI.indexExists(registeredLive));

        // Working index is consistent: present in both
        assertTrue("Working index must still exist in OS", osIndexAPI.indexExists(IDX_WORKING));
        assertTrue("Working entry must still be present in registry",
                loaded.get().working().isPresent());

        Logger.info(this, "✅ test_osAndVersionedRegistry_divergence_shouldBeDetectable passed");
    }

    // =========================================================================
    // Tests – mirrored from ESIndexAPITest
    // =========================================================================

    /**
     * Given scenario: A new index is created via {@link OSIndexAPIImpl#createIndex(String, int)}
     * Expected: The OpenSearch cluster stores {@code auto_expand_replicas=0-all} on that index,
     *           consistent with the behaviour verified in {@code ESIndexAPITest}.
     */
    @Test
    public void test_createIndex_shouldHaveAutoExpandReplicasSetting() throws Exception {
        osIndexAPI.createIndex(IDX_LIVE, 1);

        final OpenSearchClient client = CDIUtils.getBeanThrows(OSClientProvider.class).getClient();
        final String fullName = osIndexAPI.getNameWithClusterIDPrefix(IDX_LIVE);
        final GetIndexResponse response = client.indices().get(b -> b.index(fullName));

        assertNotNull("Index state must be present in GET response", response.result().get(fullName));
        final String autoExpand = response.result().get(fullName).settings().index().autoExpandReplicas();
        assertEquals("auto_expand_replicas must be 0-all on a freshly created index", "0-all", autoExpand);
        Logger.info(this, "✅ test_createIndex_shouldHaveAutoExpandReplicasSetting passed");
    }

    /**
     * Given scenario: An index is created using its bare name (no cluster prefix).
     * Expected: {@link OSIndexAPIImpl#indexExists} returns {@code true} for both the bare name
     *           and the cluster-prefixed name, and {@code false} for both after deletion —
     *           mirroring {@code ESIndexAPITest#index_exists_should_resolve_even_with_cluster_id}.
     */
    @Test
    public void test_indexExists_shouldResolveWithBothBareAndPrefixedName() throws Exception {
        final String clustered = osIndexAPI.getNameWithClusterIDPrefix(IDX_LIVE);

        assertFalse("Pre: bare name must not exist",      osIndexAPI.indexExists(IDX_LIVE));
        assertFalse("Pre: clustered name must not exist", osIndexAPI.indexExists(clustered));

        osIndexAPI.createIndex(IDX_LIVE, 1);

        assertTrue("After creation: bare name must resolve",      osIndexAPI.indexExists(IDX_LIVE));
        assertTrue("After creation: clustered name must resolve", osIndexAPI.indexExists(clustered));

        osIndexAPI.delete(IDX_LIVE);

        assertFalse("After deletion: bare name must be gone",      osIndexAPI.indexExists(IDX_LIVE));
        assertFalse("After deletion: clustered name must be gone", osIndexAPI.indexExists(clustered));
        Logger.info(this, "✅ test_indexExists_shouldResolveWithBothBareAndPrefixedName passed");
    }

    /**
     * Given scenario: Several realistic cluster-name formats are used to build a cluster prefix.
     * Expected: {@code getNameWithClusterIDPrefix} is idempotent, {@code hasClusterPrefix} and
     *           {@code removeClusterIdFromName} are consistent inverses for every format —
     *           mirroring {@code ESIndexAPITest#test_allowed_cluster_names_in_indexes}.
     */
    @Test
    public void test_clusterNameFormats_shouldAllBeHandledCorrectly() {
        final String[] clusterNames = {
                "testing_cluster_name", "testing.cluster-names",
                "cluster.123.ABC",      "12368689060",
                "__THIS_CLUSTER_"
        };
        final String indexName = "liveindex_20210322183037";

        for (final String clusterName : clusterNames) {
            final OSIndexAPIImpl api = new OSIndexAPIImpl(
                    CDIUtils.getBeanThrows(OSClientProvider.class),
                    Lazy.of(() -> CLUSTER_PREFIX + clusterName + ".")
            );

            final String withPrefix = api.getNameWithClusterIDPrefix(indexName);
            assertTrue("'" + clusterName + "': prefixed name must carry cluster prefix",
                    api.hasClusterPrefix(withPrefix));
            assertEquals("'" + clusterName + "': adding prefix twice must be a no-op",
                    withPrefix, api.getNameWithClusterIDPrefix(withPrefix));

            final String stripped = api.removeClusterIdFromName(withPrefix);
            assertFalse("'" + clusterName + "': stripped name must not have cluster prefix",
                    api.hasClusterPrefix(stripped));
            assertEquals("'" + clusterName + "': stripped name must equal the original",
                    indexName, stripped);
        }
        Logger.info(this, "✅ test_clusterNameFormats_shouldAllBeHandledCorrectly passed");
    }

    // =========================================================================
    // Tests – additional interface methods not yet covered
    // =========================================================================

    /**
     * Given scenario: Live and working indices exist.
     * Expected: {@link OSIndexAPIImpl#getLiveWorkingIndicesSortedByCreationDateDesc()} returns a
     *           non-null list that contains both indices.
     */
    @Test
    public void test_getLiveWorkingIndicesSortedByCreationDateDesc_shouldContainBothIndices()
            throws Exception {
        osIndexAPI.createIndex(IDX_LIVE, 1);
        osIndexAPI.createIndex(IDX_WORKING, 1);

        final List<String> sorted = osIndexAPI.getLiveWorkingIndicesSortedByCreationDateDesc();

        assertNotNull("Sorted list must never be null", sorted);
        assertTrue("Sorted list must contain the live index",    sorted.contains(IDX_LIVE));
        assertTrue("Sorted list must contain the working index", sorted.contains(IDX_WORKING));
        Logger.info(this, "✅ test_getLiveWorkingIndicesSortedByCreationDateDesc_shouldContainBothIndices passed");
    }

    /**
     * Given scenario: An existing index is cleared via {@link OSIndexAPIImpl#clearIndex(String)}.
     * Expected: The index still exists after the operation (it is recreated, not permanently deleted).
     */
    @Test
    public void test_clearIndex_shouldLeaveIndexExisting() throws Exception {
        osIndexAPI.createIndex(IDX_LIVE, 1);
        assertTrue("Pre-condition: index must exist before clearIndex", osIndexAPI.indexExists(IDX_LIVE));

        osIndexAPI.clearIndex(IDX_LIVE);

        assertTrue("Index must still exist after clearIndex (it is recreated)", osIndexAPI.indexExists(IDX_LIVE));
        Logger.info(this, "✅ test_clearIndex_shouldLeaveIndexExisting passed");
    }

    /**
     * Given scenario: Two open indices exist.
     * Expected: {@link OSIndexAPIImpl#getIndices(boolean, boolean)} with {@code (true, false)}
     *           returns both; with {@code (false, false)} returns none.
     */
    @Test
    public void test_getIndices_shouldRespectOpenFlag() throws Exception {
        osIndexAPI.createIndex(IDX_LIVE, 1);
        osIndexAPI.createIndex(IDX_WORKING, 1);

        final List<String> open = osIndexAPI.getIndices(true, false);
        assertTrue("open-only list must contain live index",    open.contains(IDX_LIVE));
        assertTrue("open-only list must contain working index", open.contains(IDX_WORKING));

        final List<String> none = osIndexAPI.getIndices(false, false);
        assertFalse("empty-flag list must not contain live index",    none.contains(IDX_LIVE));
        assertFalse("empty-flag list must not contain working index", none.contains(IDX_WORKING));
        Logger.info(this, "✅ test_getIndices_shouldRespectOpenFlag passed");
    }

    // =========================================================================
    // Tests – cluster stats & monitoring
    // =========================================================================

    /**
     * Given scenario: The cluster is reachable.
     * Expected: {@link OSIndexAPIImpl#getClusterStats()} returns a non-null {@link com.dotcms.content.index.domain.ClusterStats}
     *           with a non-blank {@code clusterName}.  This is a smoke test — once the method is
     *           fully implemented the assertion on {@code clusterName} will reflect the real value.
     */
    @Test
    public void test_getClusterStats_shouldReturnNonNullWithClusterName() {
        final com.dotcms.content.index.domain.ClusterStats stats = osIndexAPI.getClusterStats();

        assertNotNull("getClusterStats must never return null", stats);
        assertNotNull("clusterName must not be null", stats.clusterName());
        assertFalse("clusterName must not be blank", stats.clusterName().isBlank());
        Logger.info(this, "✅ test_getClusterStats_shouldReturnNonNullWithClusterName passed"
                + " – clusterName: " + stats.clusterName());
    }

    /**
     * Given scenario: The cluster is reachable.
     * Expected: {@link OSIndexAPIImpl#getClusterHealth()} returns a non-null map without throwing.
     *           Once the method is fully implemented the map will carry real health data per index.
     */
    @Test
    public void test_getClusterHealth_shouldReturnNonNull() {
        final java.util.Map<String, com.dotcms.content.index.domain.ClusterIndexHealth> health =
                osIndexAPI.getClusterHealth();

        assertNotNull("getClusterHealth must never return null", health);
        Logger.info(this, "✅ test_getClusterHealth_shouldReturnNonNull passed"
                + " – entries: " + health.size());
    }

    /**
     * Given scenario: At least one index exists.
     * Expected: {@link OSIndexAPIImpl#getIndicesStats()} returns a non-null map without throwing.
     *           Once the method is fully implemented the map will carry real per-index statistics.
     */
    @Test
    public void test_getIndicesStats_shouldReturnNonNull() throws Exception {
        osIndexAPI.createIndex(IDX_LIVE, 1);

        final java.util.Map<String, com.dotcms.content.index.domain.IndexStats> stats =
                osIndexAPI.getIndicesStats();

        assertNotNull("getIndicesStats must never return null", stats);
        Logger.info(this, "✅ test_getIndicesStats_shouldReturnNonNull passed"
                + " – entries: " + stats.size());
    }

    /**
     * Given scenario: The cluster is reachable.
     * Expected: {@link OSIndexAPIImpl#waitUtilIndexReady()} returns {@code true} without blocking
     *           or throwing.
     */
    @Test
    public void test_waitUtilIndexReady_shouldReturnTrueWhenClusterIsUp() {
        assertTrue("waitUtilIndexReady must return true when OpenSearch is reachable",
                osIndexAPI.waitUtilIndexReady());
        Logger.info(this, "✅ test_waitUtilIndexReady_shouldReturnTrueWhenClusterIsUp passed");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Deletes every test-scoped index that actually exists in OpenSearch.
     * Skipping the delete when the index is absent avoids noisy error logs between tests.
     */
    private synchronized void cleanupTestOsIndices() {
        for (final String idx : List.of(IDX_LIVE, IDX_WORKING)) {
            try {
                if (osIndexAPI.indexExists(idx)) {
                    osIndexAPI.delete(idx);
                }
            } catch (Exception e) {
                Logger.warn(this, "Cleanup: error removing OS index '" + idx + "': " + e.getMessage());
            }
        }
    }

    /**
     * Removes every row written by this test run from the {@code indicies} table.
     * The {@code os-it-%} pattern safely covers all versions created by any run of this suite.
     */
    private void cleanupVersionedRows() {
        try {
            new DotConnect()
                    .setSQL("DELETE FROM indicies WHERE index_version LIKE 'os-it-%'")
                    .loadResult();
        } catch (Exception e) {
            Logger.warn(this, "Cleanup: error removing versioned DB rows: " + e.getMessage());
        }
    }
}