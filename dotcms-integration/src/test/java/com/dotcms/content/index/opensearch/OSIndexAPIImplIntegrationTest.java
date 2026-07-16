package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.index.IndexTag;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.VersionedIndicesAPI;
import com.dotcms.content.index.VersionedIndicesImpl;
import com.dotcms.content.index.domain.CreateIndexStatus;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
     * Version tag written to the {@code indices} table for all rows created by this suite.
     * The {@code os-it-} prefix is used by {@link #cleanupVersionedRows()} for safe cleanup.
     */
    private static final String TEST_DB_VERSION = "os-it-" + RUN_ID;

    // ── index names reused across several tests ───────────────────────────────
    private static final String IDX_LIVE    = IndexTag.OS.tag("live_"    + RUN_ID);
    private static final String IDX_WORKING = IndexTag.OS.tag("working_" + RUN_ID);

    private static final String OS_WORKING_TAGGED = IndexTag.OS.tag(IDX_WORKING);
    private static final String OS_LIVE_TAGGED     = IndexTag.OS.tag(IDX_LIVE);

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
        assertFalse("Pre-condition: index must not exist yet", osIndexAPI.indexExists(OS_LIVE_TAGGED));

        final CreateIndexStatus status = osIndexAPI.createIndex(OS_LIVE_TAGGED, 1);

        assertTrue("CreateIndexStatus must be acknowledged", status.acknowledged());
        assertTrue("Index must exist in OpenSearch after creation", osIndexAPI.indexExists(OS_LIVE_TAGGED));
        Logger.info(this, "✅ test_createIndex_shouldExistAfterCreation passed");
    }

    /**
     * Given scenario: An existing index is removed via {@link OSIndexAPIImpl#delete(String)}
     * Expected: {@link OSIndexAPIImpl#indexExists(String)} returns {@code false} afterwards
     */
    @Test
    public void test_deleteIndex_shouldNotExistAfterDeletion() throws Exception {
        osIndexAPI.createIndex(OS_LIVE_TAGGED, 1);
        assertTrue("Pre-condition: index must exist before deletion", osIndexAPI.indexExists(OS_LIVE_TAGGED));

        final boolean ack = osIndexAPI.delete(OS_LIVE_TAGGED);

        assertTrue("Delete must be acknowledged by OpenSearch", ack);
        assertFalse("Index must not exist in OpenSearch after deletion", osIndexAPI.indexExists(OS_LIVE_TAGGED));
        Logger.info(this, "✅ test_deleteIndex_shouldNotExistAfterDeletion passed");
    }

    /**
     * Given scenario: Two indices are created
     * Expected: {@link OSIndexAPIImpl#listIndices()} contains both names without cluster prefix
     */
    @Test
    public void test_listIndices_shouldIncludeBothCreatedIndices() throws Exception {
        osIndexAPI.createIndex(OS_LIVE_TAGGED, 1);
        osIndexAPI.createIndex(OS_WORKING_TAGGED, 1);

        final Set<String> indices = osIndexAPI.listIndices();

        assertNotNull("listIndices must never return null", indices);
        assertTrue("listIndices must contain the live index",    indices.contains(OS_LIVE_TAGGED));
        assertTrue("listIndices must contain the working index", indices.contains(OS_WORKING_TAGGED));
        Logger.info(this, "✅ test_listIndices_shouldIncludeBothCreatedIndices passed – " + indices);
    }

    /**
     * Given scenario: Two indices exist and {@link OSIndexAPIImpl#deleteMultiple} is called
     * Expected: Both indices are absent from OpenSearch afterwards
     */
    @Test
    public void test_deleteMultiple_shouldDeleteAllRequestedIndices() throws Exception {
        osIndexAPI.createIndex(OS_LIVE_TAGGED, 1);
        osIndexAPI.createIndex(OS_WORKING_TAGGED, 1);

        final boolean allAck = osIndexAPI.deleteMultiple(OS_LIVE_TAGGED, OS_WORKING_TAGGED);

        assertTrue("deleteMultiple must be fully acknowledged", allAck);
        assertFalse("Live index must be gone after deleteMultiple",    osIndexAPI.indexExists(OS_LIVE_TAGGED));
        assertFalse("Working index must be gone after deleteMultiple", osIndexAPI.indexExists(OS_WORKING_TAGGED));
        Logger.info(this, "✅ test_deleteMultiple_shouldDeleteAllRequestedIndices passed");
    }

    /**
     * Given scenario: An open index is closed then reopened
     * Expected: After closing the index no longer appears in {@link OSIndexAPIImpl#listIndices()};
     *           after reopening it reappears
     */
    @Test
    public void test_closeAndOpenIndex_shouldTransitionCorrectly() throws Exception {
        osIndexAPI.createIndex(OS_LIVE_TAGGED, 1);
        assertTrue("Pre-condition: newly created index must be open",
                osIndexAPI.listIndices().contains(OS_LIVE_TAGGED));

        osIndexAPI.closeIndex(OS_LIVE_TAGGED);
        assertFalse("Closed index must not appear in the open-index listing",
                osIndexAPI.listIndices().contains(OS_LIVE_TAGGED));

        osIndexAPI.openIndex(OS_LIVE_TAGGED);
        assertTrue("Reopened index must appear in the open-index listing",
                osIndexAPI.listIndices().contains(OS_LIVE_TAGGED));
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
        final String withPrefix = osIndexAPI.getNameWithClusterIDPrefix(OS_LIVE_TAGGED);

        assertTrue("Prefixed name must end with the original bare name",
                withPrefix.endsWith(OS_LIVE_TAGGED));
        assertTrue("Prefixed name must contain a dot separator (cluster_<id>.<name>)",
                withPrefix.contains("."));

        assertEquals("Applying the prefix a second time must be a no-op",
                withPrefix, osIndexAPI.getNameWithClusterIDPrefix(withPrefix));

        assertEquals("Stripping the prefix must restore the original bare name",
                OS_LIVE_TAGGED, osIndexAPI.removeClusterIdFromName(withPrefix));
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
        osIndexAPI.createIndex(OS_LIVE_TAGGED, 1);
        osIndexAPI.createIndex(OS_WORKING_TAGGED, 1);
        assertTrue("Pre-condition: live index must exist in OS",    osIndexAPI.indexExists(OS_LIVE_TAGGED));
        assertTrue("Pre-condition: working index must exist in OS", osIndexAPI.indexExists(OS_WORKING_TAGGED));

        // 2. Persist the names in the versioned registry
        versionedIndicesAPI.saveIndices(
                VersionedIndicesImpl.builder()
                        .version(TEST_DB_VERSION)
                        .live(OS_LIVE_TAGGED)
                        .working(OS_WORKING_TAGGED)
                        .build());

        assertTrue("Version must be registered in DB after save",
                versionedIndicesAPI.versionExists(TEST_DB_VERSION));

        // 3. Reload from DB
        final Optional<VersionedIndices> loaded = versionedIndicesAPI.loadIndices(TEST_DB_VERSION);
        assertTrue("Loaded VersionedIndices must be present", loaded.isPresent());
        final VersionedIndices reloaded = loaded.get();

        // 4. Names in DB must match what was written
        assertEquals("Live index name must survive the DB round-trip",
                OS_LIVE_TAGGED, reloaded.live().orElse(null));
        assertEquals("Working index name must survive the DB round-trip",
                OS_WORKING_TAGGED, reloaded.working().orElse(null));
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
        osIndexAPI.createIndex(OS_LIVE_TAGGED, 1);
        osIndexAPI.createIndex(OS_WORKING_TAGGED, 1);

        versionedIndicesAPI.saveIndices(
                VersionedIndicesImpl.builder()
                        .version(TEST_DB_VERSION)
                        .live(OS_LIVE_TAGGED)
                        .working(OS_WORKING_TAGGED)
                        .build());

        // Delete only the live index from OpenSearch – registry is NOT updated
        osIndexAPI.delete(OS_LIVE_TAGGED);
        assertFalse("Live index must be gone from OS after deletion",
                osIndexAPI.indexExists(OS_LIVE_TAGGED));

        // Registry still has the live entry
        final Optional<VersionedIndices> loaded = versionedIndicesAPI.loadIndices(TEST_DB_VERSION);
        assertTrue("Registry entry must still be present in DB", loaded.isPresent());

        final String registeredLive = loaded.get().live().orElseThrow();
        assertFalse("Divergence: OS reports no index for the name stored in registry – " + registeredLive,
                osIndexAPI.indexExists(registeredLive));

        // Working index is consistent: present in both
        assertTrue("Working index must still exist in OS", osIndexAPI.indexExists(OS_WORKING_TAGGED));
        assertTrue("Working entry must still be present in registry",
                loaded.get().working().isPresent());

        Logger.info(this, "✅ test_osAndVersionedRegistry_divergence_shouldBeDetectable passed");
    }

    // =========================================================================
    // Tests – mirrored from ESIndexAPITest
    // =========================================================================

    /**
     * Given scenario: A new index is created via {@link OSIndexAPIImpl#createIndex(String, int)}
     * Expected: The OpenSearch cluster stores {@code auto_expand_replicas=0-1} on that index,
     *           consistent with the behaviour verified in {@code ESIndexAPITest}.
     */
    @Test
    public void test_createIndex_shouldHaveAutoExpandReplicasSetting() throws Exception {
        osIndexAPI.createIndex(OS_LIVE_TAGGED, 1);

        final OpenSearchClient client = CDIUtils.getBeanThrows(OSClientProvider.class).getClient();
        final String fullName = osIndexAPI.getNameWithClusterIDPrefix(OS_LIVE_TAGGED);
        final GetIndexResponse response = client.indices().get(b -> b.index(fullName));

        assertNotNull("Index state must be present in GET response", response.result().get(fullName));
        final String autoExpand = response.result().get(fullName).settings().index().autoExpandReplicas();
        assertEquals("auto_expand_replicas must be 0-1 on a freshly created index", "0-1", autoExpand);
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
        final String clustered = osIndexAPI.getNameWithClusterIDPrefix(OS_LIVE_TAGGED);

        assertFalse("Pre: bare name must not exist",      osIndexAPI.indexExists(OS_LIVE_TAGGED));
        assertFalse("Pre: clustered name must not exist", osIndexAPI.indexExists(clustered));

        osIndexAPI.createIndex(OS_LIVE_TAGGED, 1);

        assertTrue("After creation: bare name must resolve",      osIndexAPI.indexExists(OS_LIVE_TAGGED));
        assertTrue("After creation: clustered name must resolve", osIndexAPI.indexExists(clustered));

        osIndexAPI.delete(OS_LIVE_TAGGED);

        assertFalse("After deletion: bare name must be gone",      osIndexAPI.indexExists(OS_LIVE_TAGGED));
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
                    CDIUtils.getBeanThrows(OSClientProvider.class)) {
                @Override
                public String getClusterPrefix() {
                    return CLUSTER_PREFIX + clusterName + ".";
                }
            };

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
        osIndexAPI.createIndex(OS_LIVE_TAGGED, 1);
        osIndexAPI.createIndex(OS_WORKING_TAGGED, 1);

        final List<String> sorted = osIndexAPI.getLiveWorkingIndicesSortedByCreationDateDesc();

        assertNotNull("Sorted list must never be null", sorted);
        assertTrue("Sorted list must contain the live index",    sorted.contains(OS_LIVE_TAGGED));
        assertTrue("Sorted list must contain the working index", sorted.contains(OS_WORKING_TAGGED));
        Logger.info(this, "✅ test_getLiveWorkingIndicesSortedByCreationDateDesc_shouldContainBothIndices passed");
    }

    /**
     * Given scenario: An existing index is cleared via {@link OSIndexAPIImpl#clearIndex(String)}.
     * Expected: The index still exists after the operation (it is recreated, not permanently deleted).
     */
    @Test
    public void test_clearIndex_shouldLeaveIndexExisting() throws Exception {
        osIndexAPI.createIndex(OS_LIVE_TAGGED, 1);
        assertTrue("Pre-condition: index must exist before clearIndex", osIndexAPI.indexExists(OS_LIVE_TAGGED));

        osIndexAPI.clearIndex(OS_LIVE_TAGGED);

        assertTrue("Index must still exist after clearIndex (it is recreated)", osIndexAPI.indexExists(OS_LIVE_TAGGED));
        Logger.info(this, "✅ test_clearIndex_shouldLeaveIndexExisting passed");
    }

    /**
     * Given scenario: Two open indices exist.
     * Expected: {@link OSIndexAPIImpl#getIndices(boolean, boolean)} with {@code (true, false)}
     *           returns both; with {@code (false, false)} returns none.
     */
    @Test
    public void test_getIndices_shouldRespectOpenFlag() throws Exception {
        osIndexAPI.createIndex(OS_LIVE_TAGGED, 1);
        osIndexAPI.createIndex(OS_WORKING_TAGGED, 1);

        final List<String> open = osIndexAPI.getIndices(true, false);
        assertTrue("open-only list must contain live index",    open.contains(OS_LIVE_TAGGED));
        assertTrue("open-only list must contain working index", open.contains(OS_WORKING_TAGGED));

        final List<String> none = osIndexAPI.getIndices(false, false);
        assertFalse("empty-flag list must not contain live index",    none.contains(OS_LIVE_TAGGED));
        assertFalse("empty-flag list must not contain working index", none.contains(OS_WORKING_TAGGED));
        Logger.info(this, "✅ test_getIndices_shouldRespectOpenFlag passed");
    }

    /**
     * Regression test for issue #35304.
     *
     * <p>Given scenario: An open index is closed via {@link OSIndexAPIImpl#closeIndex(String)}.
     * Expected:
     * <ul>
     *   <li>{@link OSIndexAPIImpl#getClosedIndexes()} returns the index after it is closed</li>
     *   <li>{@link OSIndexAPIImpl#isIndexClosed(String)} returns {@code true} for the closed index</li>
     *   <li>{@link OSIndexAPIImpl#getIndices(boolean, boolean)} with {@code (false, true)} includes
     *       the closed index</li>
     *   <li>All three revert to their pre-close state after {@link OSIndexAPIImpl#openIndex(String)}</li>
     * </ul>
     */
    @Test
    public void test_getClosedIndexes_shouldReflectActualClosedState() throws Exception {
        osIndexAPI.createIndex(OS_LIVE_TAGGED, 1);

        // Pre-condition: no closed indices
        assertFalse("Pre: getClosedIndexes must not contain a freshly created index",
                osIndexAPI.getClosedIndexes().contains(OS_LIVE_TAGGED));
        assertFalse("Pre: isIndexClosed must return false for an open index",
                osIndexAPI.isIndexClosed(OS_LIVE_TAGGED));

        // Close the index
        osIndexAPI.closeIndex(OS_LIVE_TAGGED);

        // Post-close assertions
        assertTrue("getClosedIndexes must contain the index after closeIndex",
                osIndexAPI.getClosedIndexes().contains(OS_LIVE_TAGGED));
        assertTrue("isIndexClosed must return true after closeIndex",
                osIndexAPI.isIndexClosed(OS_LIVE_TAGGED));
        assertFalse("listIndices (open only) must NOT contain a closed index",
                osIndexAPI.listIndices().contains(OS_LIVE_TAGGED));

        final List<String> closedOnly = osIndexAPI.getIndices(false, true);
        assertTrue("getIndices(false, true) must include the closed index",
                closedOnly.contains(OS_LIVE_TAGGED));

        // Re-open and verify the state reverts
        osIndexAPI.openIndex(OS_LIVE_TAGGED);

        assertFalse("getClosedIndexes must be empty again after openIndex",
                osIndexAPI.getClosedIndexes().contains(OS_LIVE_TAGGED));
        assertFalse("isIndexClosed must return false after openIndex",
                osIndexAPI.isIndexClosed(OS_LIVE_TAGGED));
        assertTrue("listIndices must contain the index once it is reopened",
                osIndexAPI.listIndices().contains(OS_LIVE_TAGGED));
        Logger.info(this, "✅ test_getClosedIndexes_shouldReflectActualClosedState passed");
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
     * Given scenario: A live index exists in OpenSearch.
     * Expected: {@link OSIndexAPIImpl#getClusterHealth()} returns an entry for that index with
     *           a non-null status string and non-negative shard/replica counts.
     */
    @Test
    public void test_getClusterHealth_shouldReturnHealthForCreatedIndex() throws Exception {
        osIndexAPI.createIndex(OS_LIVE_TAGGED, 1);

        final java.util.Map<String, com.dotcms.content.index.domain.ClusterIndexHealth> health =
                osIndexAPI.getClusterHealth();

        assertNotNull("getClusterHealth must never return null", health);
        assertTrue("Health map must contain the live index", health.containsKey(OS_LIVE_TAGGED));

        final com.dotcms.content.index.domain.ClusterIndexHealth liveHealth = health.get(OS_LIVE_TAGGED);
        assertNotNull("Health status must not be null",      liveHealth.status());
        assertFalse("Health status must not be empty",       liveHealth.status().isBlank());
        assertTrue("numberOfShards must be positive",        liveHealth.numberOfShards()   > 0);
        assertTrue("numberOfReplicas must be non-negative",  liveHealth.numberOfReplicas() >= 0);
        Logger.info(this, "✅ test_getClusterHealth_shouldReturnHealthForCreatedIndex passed"
                + " – status: " + liveHealth.status()
                + ", shards: "  + liveHealth.numberOfShards()
                + ", replicas: " + liveHealth.numberOfReplicas());
    }

    /**
     * Given scenario: Live and working indices exist in OpenSearch.
     * Expected: {@link OSIndexAPIImpl#getIndicesStats()} returns an entry for each with a
     *           non-negative document count, non-negative raw size, and non-empty size string.
     */
    @Test
    public void test_getIndicesStats_shouldReturnStatsForCreatedIndices() throws Exception {
        osIndexAPI.createIndex(OS_LIVE_TAGGED, 1);
        osIndexAPI.createIndex(OS_WORKING_TAGGED, 1);

        final java.util.Map<String, com.dotcms.content.index.domain.IndexStats> stats =
                osIndexAPI.getIndicesStats();

        assertNotNull("getIndicesStats must never return null", stats);
        assertTrue("Stats map must contain the live index",    stats.containsKey(OS_LIVE_TAGGED));
        assertTrue("Stats map must contain the working index", stats.containsKey(OS_WORKING_TAGGED));

        final com.dotcms.content.index.domain.IndexStats liveStats = stats.get(OS_LIVE_TAGGED);
        assertTrue("Document count must be non-negative", liveStats.documentCount() >= 0);
        assertTrue("Raw size must be non-negative",       liveStats.sizeRaw()       >= 0);
        assertNotNull("Human-readable size must not be null",  liveStats.size());
        assertFalse("Human-readable size must not be empty",   liveStats.size().isBlank());
        Logger.info(this, "✅ test_getIndicesStats_shouldReturnStatsForCreatedIndices passed"
                + " – live: " + liveStats.documentCount() + " docs / " + liveStats.size());
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
    // Tests – performance operations (flushCaches / optimize / updateReplicas)
    // =========================================================================

    /**
     * Given scenario: cache flush is requested for a freshly created index.
     * Expected: the response map carries both shard counters and a non-negative
     * {@code successfulShards}; an empty input list short-circuits to the zero map.
     */
    @Test
    public void test_flushCaches_shouldReportShardsForCreatedIndex() throws Exception {
        osIndexAPI.createIndex(OS_WORKING_TAGGED, 1);

        final Map<String, Integer> result = osIndexAPI.flushCaches(List.of(OS_WORKING_TAGGED));

        assertNotNull("flushCaches must return a non-null map", result);
        assertTrue("map must contain successfulShards", result.containsKey("successfulShards"));
        assertTrue("map must contain failedShards", result.containsKey("failedShards"));
        assertTrue("successfulShards must be non-negative", result.get("successfulShards") >= 0);

        final Map<String, Integer> empty = osIndexAPI.flushCaches(List.of());
        assertEquals("empty input must yield zero successfulShards",
                Integer.valueOf(0), empty.get("successfulShards"));
        Logger.info(this, "✅ test_flushCaches_shouldReportShardsForCreatedIndex passed");
    }

    /**
     * Given scenario: optimize (force-merge) is requested for a freshly created index.
     * Expected: the call completes and returns {@code true}.
     */
    @Test
    public void test_optimize_shouldReturnTrueForCreatedIndex() throws Exception {
        osIndexAPI.createIndex(OS_WORKING_TAGGED, 1);

        assertTrue("optimize must return true for an existing index",
                osIndexAPI.optimize(List.of(OS_WORKING_TAGGED)));
        Logger.info(this, "✅ test_optimize_shouldReturnTrueForCreatedIndex passed");
    }

    /**
     * Given scenario: replicas are updated on a freshly created index.
     * Expected: the call is gated on the same Enterprise-license + numeric-replicas config that
     * ES enforces. Depending on the test environment's license/config the gate may be open or
     * closed, so the test asserts the <em>contract</em>: the call either completes, or fails with
     * a {@link DotDataException} — it must never leak any other exception type out of the layer.
     */
    @Test
    public void test_updateReplicas_shouldApplyOrThrowDotDataException() throws Exception {
        osIndexAPI.createIndex(OS_WORKING_TAGGED, 1);

        try {
            osIndexAPI.updateReplicas(OS_WORKING_TAGGED, 0);
            Logger.info(this, "✅ test_updateReplicas: applied (gate open)");
        } catch (DotDataException expected) {
            Logger.info(this, "✅ test_updateReplicas: rejected with DotDataException (gate closed)");
        }
    }

    // =========================================================================
    // Tests – alias management
    // =========================================================================

    /**
     * Given scenario: an alias is created for an index, then read back through every getter.
     * Expected: {@code getIndexAlias} (list + single) returns the alias keyed by index, and
     * {@code getAliasToIndexMap} returns the reverse mapping. All names are reported as logical
     * (cluster prefix stripped, {@code .os} tag preserved).
     */
    @Test
    public void test_alias_createAndResolve_shouldRoundTrip() throws Exception {
        osIndexAPI.createIndex(OS_WORKING_TAGGED, 1);
        final String aliasName = "alias_" + RUN_ID;

        osIndexAPI.createAlias(OS_WORKING_TAGGED, aliasName);

        final Map<String, String> indexToAlias = osIndexAPI.getIndexAlias(List.of(OS_WORKING_TAGGED));
        assertEquals("getIndexAlias(list) must map the index to its alias",
                aliasName, indexToAlias.get(OS_WORKING_TAGGED));

        assertEquals("getIndexAlias(single) must return the alias",
                aliasName, osIndexAPI.getIndexAlias(OS_WORKING_TAGGED));

        final Map<String, String> aliasToIndex = osIndexAPI.getAliasToIndexMap(List.of(OS_WORKING_TAGGED));
        assertEquals("getAliasToIndexMap must reverse the mapping",
                OS_WORKING_TAGGED, aliasToIndex.get(aliasName));

        Logger.info(this, "✅ test_alias_createAndResolve_shouldRoundTrip passed");
    }

    /**
     * Given scenario: an index has no alias attached.
     * Expected: {@code getIndexAlias} returns an empty map (no entry for that index) and never
     * throws, and an empty/blank input list short-circuits to an empty map.
     */
    @Test
    public void test_getIndexAlias_withoutAlias_shouldReturnEmpty() throws Exception {
        osIndexAPI.createIndex(OS_WORKING_TAGGED, 1);

        final Map<String, String> aliases = osIndexAPI.getIndexAlias(List.of(OS_WORKING_TAGGED));
        assertFalse("an index with no alias must not appear in the result",
                aliases.containsKey(OS_WORKING_TAGGED));

        assertTrue("empty input must yield an empty map",
                osIndexAPI.getIndexAlias(List.of()).isEmpty());
        Logger.info(this, "✅ test_getIndexAlias_withoutAlias_shouldReturnEmpty passed");
    }

    // =========================================================================
    // Tests – inactive live/working set cleanup
    // =========================================================================

    /**
     * Given scenario: two live/working indices with different embedded timestamps exist.
     * Expected: {@link OSIndexAPIImpl#getLiveWorkingIndicesSortedByCreationDateDesc()} orders them
     * newest-first by the embedded timestamp (not by raw string), with the {@code .os} tag preserved.
     */
    @Test
    public void test_getLiveWorkingIndicesSorted_shouldOrderByTimestampDesc() throws Exception {
        final String older = IndexTag.OS.tag("working_20200101000001");
        final String newer = IndexTag.OS.tag("working_20210101000001");
        try {
            osIndexAPI.createIndex(older, 1);
            osIndexAPI.createIndex(newer, 1);

            final List<String> sorted = osIndexAPI.getLiveWorkingIndicesSortedByCreationDateDesc();
            final int idxNewer = sorted.indexOf(newer);
            final int idxOlder = sorted.indexOf(older);

            assertTrue("newer index must be present in the sorted list", idxNewer >= 0);
            assertTrue("older index must be present in the sorted list", idxOlder >= 0);
            assertTrue("newer index must come before the older one (descending)",
                    idxNewer < idxOlder);
            Logger.info(this, "✅ test_getLiveWorkingIndicesSorted_shouldOrderByTimestampDesc passed");
        } finally {
            safeDelete(older, newer);
        }
    }

    /**
     * Given scenario: an inactive (not registered in the OS default versioned set) live/working
     * set exists, alongside whatever the cluster considers active.
     * Expected: {@code deleteInactiveLiveWorkingIndices(0)} deletes the inactive set, while any
     * genuinely-present active index registered in {@link VersionedIndicesAPI#loadDefaultVersionedIndices()}
     * is preserved.
     *
     * <p>Preservation is asserted only for active names that actually resolve via
     * {@link OSIndexAPIImpl#indexExists} <em>before</em> the cleanup. The ambient default can point
     * at a name that does not belong to the current cluster — e.g. a foreign cluster prefix
     * ({@code cluster_default.*} from a different/bootstrap cluster id) or a site-search index
     * ({@code live_search_*}). Such names neither round-trip through {@code removeClusterIdFromName}
     * (so the production preserve logic cannot protect them — tracked separately as a latent
     * hardening item) nor resolve through {@code indexExists}, so asserting on them made this test
     * non-deterministic across environments. Scoping the assertion to verifiably-present names keeps
     * it deterministic while still catching a real regression when a current-cluster active index
     * is wrongly deleted.</p>
     */
    @Test
    public void test_deleteInactiveLiveWorkingIndices_shouldDeleteInactiveAndPreserveActive()
            throws Exception {
        final String oldLive    = IndexTag.OS.tag("live_19990101000001");
        final String oldWorking = IndexTag.OS.tag("working_19990101000001");
        try {
            osIndexAPI.createIndex(oldLive, 1);
            osIndexAPI.createIndex(oldWorking, 1);
            assertTrue(osIndexAPI.indexExists(oldLive));
            assertTrue(osIndexAPI.indexExists(oldWorking));

            // Snapshot the active names that genuinely exist now — only these can be meaningfully
            // asserted as "preserved" after the cleanup (see method javadoc).
            final Optional<VersionedIndices> active = versionedIndicesAPI.loadDefaultVersionedIndices();
            final List<String> protectable = new ArrayList<>();
            active.ifPresent(versioned -> {
                versioned.live().filter(osIndexAPI::indexExists).ifPresent(protectable::add);
                versioned.working().filter(osIndexAPI::indexExists).ifPresent(protectable::add);
            });

            // keep 0 inactive sets -> every set except the active default becomes eligible.
            osIndexAPI.deleteInactiveLiveWorkingIndices(0);

            assertFalse("inactive live index must be deleted", osIndexAPI.indexExists(oldLive));
            assertFalse("inactive working index must be deleted", osIndexAPI.indexExists(oldWorking));

            // A current-cluster active index that existed before the cleanup must survive it.
            for (final String activeName : protectable) {
                assertTrue("active index present before cleanup must be preserved: " + activeName,
                        osIndexAPI.indexExists(activeName));
            }
            Logger.info(this, "✅ test_deleteInactiveLiveWorkingIndices_shouldDeleteInactiveAndPreserveActive passed"
                    + " (asserted preservation of " + protectable.size() + " active index/indices)");
        } finally {
            safeDelete(oldLive, oldWorking);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Deletes the given indices, ignoring any that are already gone. */
    private void safeDelete(final String... indexNames) {
        for (final String idx : indexNames) {
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
     * Deletes every test-scoped index that actually exists in OpenSearch.
     * Skipping delete when the index is absent avoids noisy error logs between tests.
     */
    private synchronized void cleanupTestOsIndices() {
        for (final String idx : List.of(OS_LIVE_TAGGED, OS_WORKING_TAGGED)) {
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
