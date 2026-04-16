package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.content.index.IndexContentletScroll;
import com.dotcms.content.index.domain.IndexBulkRequest;
import com.dotcms.content.index.domain.SearchHits;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.CountRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.indices.RefreshRequest;

/**
 * Integration tests for {@link ContentFactoryIndexOperationsOS} that exercise all public methods of
 * the {@link com.dotcms.content.index.ContentFactoryIndexOperations} contract against a live
 * OpenSearch 3.x container.
 *
 * <p>Requires the {@code opensearch-upgrade} Docker container running on
 * {@code http://localhost:9201} with security disabled. Registered in
 * {@link com.dotcms.OpenSearchUpgradeSuite}.</p>
 *
 * <p>Run with:
 * <pre>
 *   ./mvnw verify -pl :dotcms-integration \
 *       -Dcoreit.test.skip=false \
 *       -Dopensearch.upgrade.test=true
 * </pre>
 * </p>
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentFactoryIndexOperationsOSIntegrationTest extends IntegrationTestBase {

    /**
     * Unique suffix appended to every OS index name created by this suite. Prevents cross-run
     * pollution in a shared OpenSearch node.
     */
    private static final String RUN_ID =
            UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    // ── bare index names — OSIndexAPIImpl adds the cluster prefix internally ─
    private static final String IDX_LIVE = "live_ops_" + RUN_ID;
    private static final String IDX_WORKING = "working_ops_" + RUN_ID;

    /**
     * Content-type name unique to this run — used as the Lucene query discriminator.
     */
    private static final String CONTENT_TYPE = "ops_content_type_" + RUN_ID;

    /**
     * Inode value embedded in the test document — needed because
     * {@link ContentFactoryIndexOperationsOS#search} reads the {@code inode} field from results.
     */
    private static final String TEST_DOC_INODE = "test-ops-inode-" + RUN_ID;

    /**
     * OS document ID — conventional dotCMS format: {@code inode_languageid_variant}.
     */
    private static final String TEST_DOC_ID = TEST_DOC_INODE + "_1_default";

    /**
     * Minimal JSON document used for indexing tests. Includes {@code live:true} so queries with
     * {@code +live:true} match, and {@code moddate} so the default sort field is present.
     */
    private static final String TEST_DOC_JSON =
            "{\"identifier\":\"test-cfops-id-" + RUN_ID + "\","
                    + "\"inode\":\"" + TEST_DOC_INODE + "\","
                    + "\"title\":\"ContentFactoryIndexOperationsOS Integration Test\","
                    + "\"language_id\":1,"
                    + "\"live\":true,"
                    + "\"moddate\":1000000,"
                    + "\"contenttype\":\"" + CONTENT_TYPE + "\"}";

    // ── OS reachability — evaluated once per JVM, shared by @ClassRule and @BeforeClass ──
    private static final boolean OS_REACHABLE = isOSReachable();

    // ── CDI-injected beans ────────────────────────────────────────────────────
    // OSTestClientProvider (@Alternative @Priority(1)) is on the test classpath and will be
    // used by ContentFactoryIndexOperationsOS (via CDIUtils.getBeanThrows) for testing.

    /**
     * Helper for creating / deleting / checking test indices.
     */
    @Inject
    private OSIndexAPIImpl osIndexAPI;

    /**
     * Used directly for low-level refresh operations and for ControllableOps construction.
     */
    @Inject
    private OSClientProvider clientProvider;

    /**
     * Used to write test documents into the OS index so the read-path tests have data.
     * {@link ContentletIndexOperationsOS} is the tested write path; it is stable and used here only
     * as a test-data helper.
     */
    @Inject
    private ContentletIndexOperationsOS contentletOps;

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Returns {@code true} when the OpenSearch test endpoint responds with an HTTP status below
     * 500. The endpoint defaults to {@code http://localhost:9201} and can be overridden via the
     * {@code opensearch.test.endpoint} system property.
     */
    private static boolean isOSReachable() {
        final String endpoint =
                System.getProperty("opensearch.test.endpoint", "http://localhost:9201");
        try {
            final java.net.HttpURLConnection conn =
                    (java.net.HttpURLConnection) new java.net.URL(endpoint).openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("HEAD");
            return conn.getResponseCode() < 500;
        } catch (final Exception e) {
            return false;
        }
    }

    @Before
    public void setUp() {
        cleanupTestIndices();
    }

    @After
    public void tearDown() {
        cleanupTestIndices();
    }

    // =========================================================================
    // Inner helper: ControllableOps
    // =========================================================================

    /**
     * A test-local subclass that overrides {@link ContentFactoryIndexOperationsOS#inferIndexToHit}
     * to return known, pre-created test-index full names.
     *
     * <p>This decouples all other method tests from the live {@code VersionedIndicesAPI}
     * state, letting them run against controlled data without requiring OS indices to be registered
     * in the dotCMS database.</p>
     */
    private class ControllableOps extends ContentFactoryIndexOperationsOS {

        private final String liveIndex;
        private final String workingIndex;

        ControllableOps(final String liveIndex, final String workingIndex) {
            // Use the testable 2-arg constructor so the injected OSTestClientProvider is used.
            super(CacheLocator.getOSQueryCache(), clientProvider);
            this.liveIndex = liveIndex;
            this.workingIndex = workingIndex;
        }

        /**
         * Returns the pre-wired test index instead of consulting the live VersionedIndicesAPI,
         * mirroring the routing logic of the real implementation: {@code +live:true} (and no
         * {@code +deleted:true}) → live index; everything else → working.
         */
        @Override
        public String inferIndexToHit(final String query) {
            return (query.contains("+live:true") && !query.contains("+deleted:true"))
                    ? liveIndex : workingIndex;
        }

        /**
         * Overrides scroll creation to pass {@code this} as the {@code indexOps} argument so that
         * {@link OSContentletScrollImpl} uses our overridden {@link #inferIndexToHit} instead of
         * creating its own {@code ContentFactoryIndexOperationsOS} with the real database-backed
         * implementation.
         */
        @Override
        public IndexContentletScroll createScrollQuery(final String luceneQuery,
                final com.liferay.portal.model.User user,
                final boolean respectFrontendRoles, final int batchSize, final String sortBy) {
            return new OSContentletScrollImpl(
                    luceneQuery, user, respectFrontendRoles, batchSize, sortBy,
                    clientProvider, this);
        }
    }

    // =========================================================================
    // Tests – setTrackHits  (no live OS needed — pure builder configuration)
    // =========================================================================

    /**
     * Given scenario: {@code OS_TRACK_TOTAL_HITS} is set to an explicit integer value. Expected:
     * {@link ContentFactoryIndexOperationsOS#setTrackHits} configures the SearchRequest.Builder so
     * the built request reflects that exact count.
     */
    @Test
    public void test_setTrackHits_withConfiguredValue_shouldApplyLimit() {
        final ContentFactoryIndexOperationsOS ops = new ContentFactoryIndexOperationsOS();
        final SearchRequest.Builder builder = new SearchRequest.Builder();
        final int limit = new Random().nextInt(100) + 1;

        final String savedValue = Config.getStringProperty("OS_TRACK_TOTAL_HITS",
                String.valueOf(ContentFactoryIndexOperationsOS.OS_TRACK_TOTAL_HITS_DEFAULT));
        try {
            Config.setProperty("OS_TRACK_TOTAL_HITS", String.valueOf(limit));
            ops.setTrackHits(builder);

            final SearchRequest built = builder.build();
            assertNotNull("trackTotalHits must not be null after setTrackHits",
                    built.trackTotalHits());
            assertTrue("trackTotalHits must be configured as a count",
                    built.trackTotalHits().isCount());
            assertEquals("trackTotalHits count must match the configured value",
                    limit, built.trackTotalHits().count().intValue());
        } finally {
            Config.setProperty("OS_TRACK_TOTAL_HITS", savedValue);
        }
    }

    /**
     * Given scenario: {@code OS_TRACK_TOTAL_HITS} is absent from config (null). Expected:
     * {@link ContentFactoryIndexOperationsOS#setTrackHits} falls back to
     * {@link ContentFactoryIndexOperationsOS#OS_TRACK_TOTAL_HITS_DEFAULT}.
     */
    @Test
    public void test_setTrackHits_withNoConfig_shouldApplyDefault() {
        final ContentFactoryIndexOperationsOS ops = new ContentFactoryIndexOperationsOS();
        final SearchRequest.Builder builder = new SearchRequest.Builder();

        final String savedValue = Config.getStringProperty("OS_TRACK_TOTAL_HITS",
                String.valueOf(ContentFactoryIndexOperationsOS.OS_TRACK_TOTAL_HITS_DEFAULT));
        try {
            Config.setProperty("OS_TRACK_TOTAL_HITS", null);
            ops.setTrackHits(builder);

            final SearchRequest built = builder.build();
            assertNotNull("trackTotalHits must not be null after setTrackHits",
                    built.trackTotalHits());
            assertTrue("trackTotalHits must be configured as a count",
                    built.trackTotalHits().isCount());
            assertEquals("trackTotalHits count must equal the default",
                    ContentFactoryIndexOperationsOS.OS_TRACK_TOTAL_HITS_DEFAULT,
                    built.trackTotalHits().count().intValue());
        } finally {
            Config.setProperty("OS_TRACK_TOTAL_HITS", savedValue);
        }
    }

    // =========================================================================
    // Tests – searchHits
    // =========================================================================

    /**
     * Given scenario: One document is indexed into the working test index. Expected:
     * {@code searchHits} returns exactly 1 hit with totalHits=1.
     */
    @Test
    public void test_searchHits_shouldReturnMatchingDocuments() throws Exception {
        osIndexAPI.createIndex(IDX_WORKING, 1);
        final String fullWorking = osIndexAPI.getNameWithClusterIDPrefix(IDX_WORKING);
        indexSingleDoc(fullWorking);

        final ControllableOps ops = new ControllableOps(fullWorking, fullWorking);
        final SearchHits hits = ops.searchHits("+contenttype:" + CONTENT_TYPE, 10, 0, null);

        assertNotNull("searchHits must never return null", hits);
        assertFalse("searchHits must return at least one result", hits.hits().isEmpty());
        assertEquals("totalHits must be 1", 1L, hits.totalHits().value());
    }

    /**
     * Given scenario: Three documents are indexed; searchHits is called with limit=1, offset=0.
     * Expected: Exactly 1 hit in the response; totalHits still reflects all 3.
     */
    @Test
    public void test_searchHits_withPagination_shouldRespectLimitAndOffset() throws Exception {
        osIndexAPI.createIndex(IDX_WORKING, 1);
        final String fullWorking = osIndexAPI.getNameWithClusterIDPrefix(IDX_WORKING);
        indexThreeDocs(fullWorking);

        final ControllableOps ops = new ControllableOps(fullWorking, fullWorking);
        final SearchHits hits = ops.searchHits("+contenttype:" + CONTENT_TYPE, 1, 0, null);

        assertNotNull("searchHits must not be null", hits);
        assertEquals("Only 1 hit must be returned when limit=1", 1, hits.hits().size());
        assertEquals("totalHits must reflect all 3 indexed documents", 3L, hits.totalHits().value());
    }

    /**
     * Given scenario: {@code searchHits} is called with the invalid bare sort values {@code "asc"}
     * and {@code "desc"} (no field name), mirroring the equivalent ES test in
     * {@code ESContentFactoryImplTest}. Expected: No exception is thrown for any value; a non-null
     * result is returned.
     */
    @Test
    public void test_searchHits_withBadSorts_shouldNotThrow() throws Exception {
        osIndexAPI.createIndex(IDX_WORKING, 1);
        final String fullWorking = osIndexAPI.getNameWithClusterIDPrefix(IDX_WORKING);
        indexSingleDoc(fullWorking);

        final ControllableOps ops = new ControllableOps(fullWorking, fullWorking);
        for (final String sort : List.of("asc", "desc", "ASC", "DESC")) {
            final SearchHits result = ops.searchHits("+contenttype:" + CONTENT_TYPE, 10, 0, sort);
            assertNotNull("searchHits with sort='" + sort + "' must not return null", result);
        }
    }

    /**
     * Given scenario: No documents exist in the working test index. Expected: {@code searchHits}
     * returns an empty (but non-null) result.
     */
    @Test
    public void test_searchHits_onEmptyIndex_shouldReturnEmpty() throws Exception {
        osIndexAPI.createIndex(IDX_WORKING, 1);
        final String fullWorking = osIndexAPI.getNameWithClusterIDPrefix(IDX_WORKING);

        final ControllableOps ops = new ControllableOps(fullWorking, fullWorking);
        final SearchHits hits = ops.searchHits("+contenttype:" + CONTENT_TYPE, 10, 0, null);

        assertNotNull("searchHits must not return null for an empty index", hits);
        assertTrue("Empty index must produce no hits", hits.hits().isEmpty());
    }

    /**
     * Given scenario: A document is indexed into the live index only; the working index is empty.
     * Expected: a {@code +live:true} query routes to the live index and finds the document, while
     * a query without {@code +live:true} routes to the working index and finds nothing — proving
     * that the two routing branches are actually exercised independently.
     */
    @Test
    public void test_searchHits_withLiveQuery_shouldRouteToLiveIndex() throws Exception {
        osIndexAPI.createIndex(IDX_LIVE, 1);
        osIndexAPI.createIndex(IDX_WORKING, 1);
        final String fullLive = osIndexAPI.getNameWithClusterIDPrefix(IDX_LIVE);
        final String fullWorking = osIndexAPI.getNameWithClusterIDPrefix(IDX_WORKING);

        // Document exists only in the live index
        indexSingleDoc(fullLive);

        final ControllableOps ops = new ControllableOps(fullLive, fullWorking);

        // +live:true → routed to live index → document found
        final SearchHits liveHits = ops.searchHits(
                "+live:true +contenttype:" + CONTENT_TYPE, 10, 0, null);
        assertNotNull("searchHits must not return null for live query", liveHits);
        assertFalse("Live query must find the document in the live index", liveHits.hits().isEmpty());

        // No +live:true → routed to working index (empty) → no results
        final SearchHits workingHits = ops.searchHits(
                "+contenttype:" + CONTENT_TYPE, 10, 0, null);
        assertNotNull("searchHits must not return null for working query", workingHits);
        assertTrue("Working query must return no results — document is only in the live index",
                workingHits.hits().isEmpty());
    }

    // =========================================================================
    // Tests – search (returns list of inodes)
    // =========================================================================

    /**
     * Given scenario: A document (with an {@code inode} field) is indexed into the working index.
     * Expected: {@code search} returns a list containing the document's inode.
     */
    @Test
    public void test_search_shouldReturnInodes() throws Exception {
        osIndexAPI.createIndex(IDX_WORKING, 1);
        final String fullWorking = osIndexAPI.getNameWithClusterIDPrefix(IDX_WORKING);
        indexSingleDoc(fullWorking);

        final ControllableOps ops = new ControllableOps(fullWorking, fullWorking);
        final List<String> inodes = ops.search("+contenttype:" + CONTENT_TYPE, 10, 0);

        assertNotNull("search must never return null", inodes);
        assertFalse("search must return at least one inode", inodes.isEmpty());
        assertTrue("Returned inode must match the indexed document", inodes.contains(TEST_DOC_INODE));
    }

    /**
     * Given scenario: No documents exist in the working test index. Expected: {@code search}
     * returns an empty list (not null).
     */
    @Test
    public void test_search_onEmptyIndex_shouldReturnEmpty() throws Exception {
        osIndexAPI.createIndex(IDX_WORKING, 1);
        final String fullWorking = osIndexAPI.getNameWithClusterIDPrefix(IDX_WORKING);

        final ControllableOps ops = new ControllableOps(fullWorking, fullWorking);
        final List<String> inodes = ops.search("+contenttype:" + CONTENT_TYPE, 10, 0);

        assertNotNull("search must not return null for empty index", inodes);
        assertTrue("Empty index must yield an empty inode list", inodes.isEmpty());
    }

    // =========================================================================
    // Tests – indexCount
    // =========================================================================

    /**
     * Given scenario: One document is indexed; {@code indexCount} is called with a matching query.
     * Expected: Returns 1.
     */
    @Test
    public void test_indexCount_shouldReturnCorrectCount() throws Exception {
        osIndexAPI.createIndex(IDX_WORKING, 1);
        final String fullWorking = osIndexAPI.getNameWithClusterIDPrefix(IDX_WORKING);
        indexSingleDoc(fullWorking);

        final ControllableOps ops = new ControllableOps(fullWorking, fullWorking);
        final long count = ops.indexCount("+contenttype:" + CONTENT_TYPE);

        assertEquals("indexCount must return 1 after indexing one document", 1L, count);
    }

    /**
     * Given scenario: Empty working index; {@code indexCount} is called with a matching query.
     * Expected: Returns 0.
     */
    @Test
    public void test_indexCount_onEmptyIndex_shouldReturnZero() throws Exception {
        osIndexAPI.createIndex(IDX_WORKING, 1);
        final String fullWorking = osIndexAPI.getNameWithClusterIDPrefix(IDX_WORKING);

        final ControllableOps ops = new ControllableOps(fullWorking, fullWorking);
        final long count = ops.indexCount("+contenttype:" + CONTENT_TYPE);

        assertEquals("Empty index must have count 0", 0L, count);
    }

    /**
     * Given scenario: Three documents are indexed; {@code indexCount} should accurately count all
     * of them regardless of the {@code OS_TRACK_TOTAL_HITS} setting — mirroring the equivalent ES
     * assertion in {@code ESContentFactoryImplTest}. Expected: Returns 3 even when
     * {@code OS_TRACK_TOTAL_HITS} is set to a value below 3.
     */
    @Test
    public void test_indexCount_shouldBeAccurateRegardlessOfTrackHits() throws Exception {
        osIndexAPI.createIndex(IDX_WORKING, 1);
        final String fullWorking = osIndexAPI.getNameWithClusterIDPrefix(IDX_WORKING);
        indexThreeDocs(fullWorking);

        final ControllableOps ops = new ControllableOps(fullWorking, fullWorking);

        final String savedValue = Config.getStringProperty("OS_TRACK_TOTAL_HITS",
                String.valueOf(ContentFactoryIndexOperationsOS.OS_TRACK_TOTAL_HITS_DEFAULT));
        try {
            // Set track-hits lower than the actual document count
            Config.setProperty("OS_TRACK_TOTAL_HITS", "1");
            CacheLocator.getOSQueryCache().clearCache();

            final long count = ops.indexCount("+contenttype:" + CONTENT_TYPE);
            assertEquals("indexCount must always return the exact count (3), "
                    + "independent of OS_TRACK_TOTAL_HITS", 3L, count);
        } finally {
            Config.setProperty("OS_TRACK_TOTAL_HITS", savedValue);
        }
    }

    // =========================================================================
    // Tests – cachedIndexCount (direct CountRequest path)
    // =========================================================================

    /**
     * Given scenario: {@code cachedIndexCount} is called with a CountRequest pointing directly at
     * the full index name (bypassing {@code inferIndexToHit}). Expected: Returns the correct
     * document count for that index.
     */
    @Test
    public void test_cachedIndexCount_shouldCountDirectly() throws Exception {
        osIndexAPI.createIndex(IDX_WORKING, 1);
        final String fullWorking = osIndexAPI.getNameWithClusterIDPrefix(IDX_WORKING);
        indexSingleDoc(fullWorking);

        final ContentFactoryIndexOperationsOS ops = new ContentFactoryIndexOperationsOS();
        final CountRequest countReq = CountRequest.of(cr -> cr.index(fullWorking));
        final long count = ops.cachedIndexCount(countReq);

        assertEquals("cachedIndexCount must return 1 for an index with one document", 1L, count);
    }

    // =========================================================================
    // Tests – indexSearchScroll
    // =========================================================================

    /**
     * Given scenario: Three documents are indexed into the working test index. Expected:
     * {@code indexSearchScroll} returns a list whose total equals 3.
     */
    @Test
    public void test_indexSearchScroll_shouldReturnAllDocuments() throws Exception {
        osIndexAPI.createIndex(IDX_WORKING, 1);
        final String fullWorking = osIndexAPI.getNameWithClusterIDPrefix(IDX_WORKING);
        indexThreeDocs(fullWorking);

        final ControllableOps ops = new ControllableOps(fullWorking, fullWorking);
        final PaginatedArrayList<ContentletSearch> results =
                ops.indexSearchScroll("+contenttype:" + CONTENT_TYPE, "title asc", 10);

        assertNotNull("indexSearchScroll must not return null", results);
        assertEquals("indexSearchScroll must return all 3 indexed documents",
                3L, results.getTotalResults());
    }

    /**
     * Given scenario: No documents exist in the working test index. Expected:
     * {@code indexSearchScroll} returns an empty list (not null, no exception).
     */
    @Test
    public void test_indexSearchScroll_onEmptyIndex_shouldReturnEmpty() throws Exception {
        osIndexAPI.createIndex(IDX_WORKING, 1);
        final String fullWorking = osIndexAPI.getNameWithClusterIDPrefix(IDX_WORKING);

        final ControllableOps ops = new ControllableOps(fullWorking, fullWorking);
        final PaginatedArrayList<ContentletSearch> results =
                ops.indexSearchScroll("+contenttype:" + CONTENT_TYPE, null, 10);

        assertNotNull("indexSearchScroll must not return null for an empty index", results);
        assertTrue("Empty index must yield an empty scroll result", results.isEmpty());
    }

    // =========================================================================
    // Tests – createScrollQuery
    // =========================================================================

    /**
     * Given scenario: {@code createScrollQuery} is called with valid parameters. Expected: Returns
     * a non-null {@link IndexContentletScroll} that can be closed without error.
     */
    @Test
    public void test_createScrollQuery_shouldReturnNonNull() throws Exception {
        osIndexAPI.createIndex(IDX_WORKING, 1);
        final String fullWorking = osIndexAPI.getNameWithClusterIDPrefix(IDX_WORKING);

        final ControllableOps ops = new ControllableOps(fullWorking, fullWorking);
        try (IndexContentletScroll scroll = ops.createScrollQuery(
                "+contenttype:" + CONTENT_TYPE, APILocator.systemUser(), false, 10,
                "title asc")) {
            assertNotNull("createScrollQuery must not return null", scroll);
        }
    }

    /**
     * Given scenario: The convenience overload of {@code createScrollQuery} (no sortBy) is called.
     * Expected: Returns a non-null scroll handle using the default sort order.
     */
    @Test
    public void test_createScrollQuery_defaultSortOverload_shouldReturnNonNull() throws Exception {
        osIndexAPI.createIndex(IDX_WORKING, 1);
        final String fullWorking = osIndexAPI.getNameWithClusterIDPrefix(IDX_WORKING);

        final ControllableOps ops = new ControllableOps(fullWorking, fullWorking);
        try (IndexContentletScroll scroll = ops.createScrollQuery(
                "+contenttype:" + CONTENT_TYPE, APILocator.systemUser(), false, 10)) {
            assertNotNull("createScrollQuery (default-sort overload) must not return null", scroll);
        }
    }

    // =========================================================================
    // Tests – cache behaviour
    // =========================================================================

    /**
     * Given scenario: {@code searchHits} is called on an empty index (result cached as 0 hits). A
     * document is then indexed and the index refreshed. The same query is repeated <em>without</em>
     * clearing the cache. Expected: The second call returns the stale cached result (0 hits),
     * proving the cache is active. After {@link OSQueryCache#clearCache()} the third call reaches
     * OS and returns the now-visible document (1 hit).
     */
    @Test
    public void test_searchCache_shouldServeStaleCachedResult() throws Exception {
        osIndexAPI.createIndex(IDX_WORKING, 1);
        final String fullWorking = osIndexAPI.getNameWithClusterIDPrefix(IDX_WORKING);

        final ControllableOps ops = new ControllableOps(fullWorking, fullWorking);
        final String query = "+contenttype:" + CONTENT_TYPE;

        // Phase 1: empty index → 0 hits, result now cached
        final SearchHits hitsEmpty = ops.searchHits(query, 10, 0, null);
        assertEquals("Empty index must return 0 hits", 0L, hitsEmpty.totalHits().value());

        // Phase 2: index a doc + refresh (OS now has 1 visible doc)
        indexSingleDoc(fullWorking);

        // Phase 3: cache still valid — must return the stale 0
        final SearchHits hitsCached = ops.searchHits(query, 10, 0, null);
        assertEquals("Cache must return stale 0-hit result before clear",
                0L, hitsCached.totalHits().value());

        // Phase 4: clear cache → fresh request → 1 doc
        CacheLocator.getOSQueryCache().clearCache();
        final SearchHits hitsFresh = ops.searchHits(query, 10, 0, null);
        assertEquals("After cache clear, fresh query must return the indexed document",
                1L, hitsFresh.totalHits().value());
    }

    /**
     * Given scenario: {@code indexCount} is called on an empty index (count cached as 0). A
     * document is indexed + refreshed. The count is requested again without clearing the cache.
     * Expected: The second count returns the cached 0. After clearing the cache the third request
     * returns the correct count of 1.
     */
    @Test
    public void test_countCache_shouldServeStaleCachedCount() throws Exception {
        osIndexAPI.createIndex(IDX_WORKING, 1);
        final String fullWorking = osIndexAPI.getNameWithClusterIDPrefix(IDX_WORKING);

        final ControllableOps ops = new ControllableOps(fullWorking, fullWorking);
        final String query = "+contenttype:" + CONTENT_TYPE;

        // Phase 1: empty index → count 0, cached
        assertEquals("Empty index count must be 0", 0L, ops.indexCount(query));

        // Phase 2: index 1 doc + refresh
        indexSingleDoc(fullWorking);

        // Phase 3: cache hit → still 0
        assertEquals("Cached count must still be 0 before clear", 0L, ops.indexCount(query));

        // Phase 4: clear cache → fresh count → 1
        CacheLocator.getOSQueryCache().clearCache();
        assertEquals("Fresh count after cache clear must be 1", 1L, ops.indexCount(query));
    }

    /**
     * Given scenario: {@code searchHits} is called multiple times with varying {@code limit} and
     * {@code offset} values against an index with 3 documents. Expected: Different parameter
     * combinations produce independent cache entries. Repeating an identical call returns exactly
     * the same result (cache hit).
     */
    @Test
    public void test_searchCache_differentParams_shouldUseSeparateCacheEntries() throws Exception {
        CacheLocator.getOSQueryCache().clearCache();

        osIndexAPI.createIndex(IDX_WORKING, 1);
        final String fullWorking = osIndexAPI.getNameWithClusterIDPrefix(IDX_WORKING);
        indexThreeDocs(fullWorking);

        final ControllableOps ops = new ControllableOps(fullWorking, fullWorking);
        final String query = "+contenttype:" + CONTENT_TYPE;

        // Different limit values → independent cache entries
        final SearchHits limit2 = ops.searchHits(query, 2, 0, null);
        final SearchHits limit3 = ops.searchHits(query, 3, 0, null);
        assertEquals("limit=2 must return 2 hits", 2, limit2.hits().size());
        assertEquals("limit=3 must return 3 hits", 3, limit3.hits().size());
        assertEquals("totalHits must reflect all 3 docs regardless of limit",
                3L, limit2.totalHits().value());

        // Repeat limit=2 → cache hit, same result
        final SearchHits limit2Again = ops.searchHits(query, 2, 0, null);
        assertEquals("Repeated limit=2 call must return same hit count (cache hit)",
                limit2.hits().size(), limit2Again.hits().size());
        assertEquals("Repeated limit=2 call must return same totalHits (cache hit)",
                limit2.totalHits().value(), limit2Again.totalHits().value());

        // Different offset → independent cache entry, different page
        final SearchHits page2 = ops.searchHits(query, 2, 1, null);
        assertEquals("limit=2,offset=1 must return 2 hits (page 2)", 2, page2.hits().size());
        assertNotEquals("Page 2 must start at a different document than page 1",
                page2.hits().get(0).id(), limit2.hits().get(0).id());
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Indexes {@link #TEST_DOC_JSON} into the given full index name and refreshes the index so the
     * document is immediately visible to search queries.
     */
    private void indexSingleDoc(final String fullIndexName) {
        final IndexBulkRequest req = contentletOps.createBulkRequest();
        contentletOps.addIndexOp(req, fullIndexName, TEST_DOC_ID, TEST_DOC_JSON);
        contentletOps.putToIndex(req);
        refreshTestIndex(fullIndexName);
    }

    /**
     * Indexes 3 documents with distinct IDs and content types matching {@link #CONTENT_TYPE} into
     * the given full index name, then refreshes so they are visible to queries.
     */
    private void indexThreeDocs(final String fullIndexName) {
        final IndexBulkRequest req = contentletOps.createBulkRequest();
        for (int i = 1; i <= 3; i++) {
            final String inode = "test-cfops-inode-" + RUN_ID + "-" + i;
            final String docId = inode + "_1_default";
            final String json = "{\"identifier\":\"test-cfops-id-" + RUN_ID + "-" + i + "\","
                    + "\"inode\":\"" + inode + "\","
                    + "\"title\":\"CFOps Doc " + i + "\","
                    + "\"language_id\":1,"
                    + "\"live\":true,"
                    + "\"moddate\":" + (1000000 + i) + ","
                    + "\"contenttype\":\"" + CONTENT_TYPE + "\"}";
            contentletOps.addIndexOp(req, fullIndexName, docId, json);
        }
        contentletOps.putToIndex(req);
        refreshTestIndex(fullIndexName);
    }

    /**
     * Forces an index refresh so documents written by {@code putToIndex} become immediately visible
     * to count and search queries.
     */
    private void refreshTestIndex(final String fullIndexName) {
        try {
            final OpenSearchClient client = clientProvider.getClient();
            client.indices().refresh(RefreshRequest.of(r -> r.index(fullIndexName)));
        } catch (Exception e) {
            Logger.warn(this, "refreshTestIndex: error refreshing '" + fullIndexName
                    + "': " + e.getMessage());
        }
    }

    /**
     * Deletes every test-scoped index that actually exists in OpenSearch. Skipping the delete when
     * the index is absent avoids noisy error logs between tests.
     */
    private synchronized void cleanupTestIndices() {
        CacheLocator.getOSQueryCache().clearCache();

        for (final String idx : List.of(IDX_LIVE, IDX_WORKING)) {
            try {
                if (osIndexAPI.indexExists(idx)) {
                    osIndexAPI.delete(idx);
                }
            } catch (Exception e) {
                Logger.warn(this, "Cleanup: error removing OS index '" + idx
                        + "': " + e.getMessage());
            }
        }
    }
}
