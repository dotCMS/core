package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.VersionedIndicesAPI;
import com.dotcms.content.index.VersionedIndicesImpl;
import com.dotcms.content.index.domain.Aggregation;
import com.dotcms.content.index.domain.AggregationBucket;
import com.dotcms.content.index.domain.ContentSearchResponse;
import com.dotcms.content.index.domain.ContentSearchResults;
import com.dotcms.content.index.domain.IndexBulkRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.RefreshRequest;

/**
 * Integration tests for {@link OSSearchAPIImpl} exercised against a live OpenSearch 3.x container.
 *
 * <p>Each test creates minimal live/working indices in OpenSearch, registers them in
 * {@link VersionedIndicesAPI} so {@link OSSearchAPIImpl#resolveIndex} can find them, then
 * verifies that the search path returns well-formed {@link ContentSearchResponse} /
 * {@link ContentSearchResults} objects (structure and non-null invariants).  The tests search
 * against empty indices — no content is indexed — so the result sets are always empty, but the
 * full execution path (permissions, pagination, JSON deserialisation) is exercised.</p>
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
public class OSSearchAPIImplIntegrationTest extends IntegrationTestBase {

    private static final String RUN_ID =
            UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    private static final String IDX_LIVE    = "live_search_"    + RUN_ID;
    private static final String IDX_WORKING = "working_search_" + RUN_ID;

    /** Unique identifier and inode for the document indexed in _source rewrite tests. */
    private static final String TEST_DOC_IDENTIFIER = "test-identifier-" + RUN_ID;
    private static final String TEST_DOC_INODE       = "test-inode-"       + RUN_ID;
    private static final String TEST_DOC_ID          = TEST_DOC_IDENTIFIER + "_1_default";
    private static final String TEST_DOC_JSON =
            "{\"identifier\":\"" + TEST_DOC_IDENTIFIER + "\","
            + "\"inode\":\""     + TEST_DOC_INODE       + "\","
            + "\"title\":\"OSSearchAPIImpl source-rewrite test\","
            + "\"language_id\":1,"
            + "\"contenttype\":\"testtype\"}";

    /**
     * The version constant used by {@link VersionedIndicesAPI#loadDefaultVersionedIndices()}.
     * Test indices must be registered under this version for {@code OSSearchAPIImpl.resolveIndex()}
     * to find them.
     */
    private static final String DEFAULT_OS_VERSION = VersionedIndices.OPENSEARCH_3X;

    // ── CDI-injected beans ──────────────────────────────────────────────────
    @Inject
    private OSSearchAPIImpl osSearchAPI;

    @Inject
    private OSIndexAPIImpl osIndexAPI;

    @Inject
    private ContentletIndexOperationsOS opsOS;

    @Inject
    private OSClientProvider clientProvider;

    @Inject
    private VersionedIndicesAPI versionedIndicesAPI;

    private User systemUser;

    /** Cluster-prefixed (and tag-suffixed) physical names; resolved once per test in {@link #setUp()}. */
    private String physicalLive;
    private String physicalWorking;

    // =======================================================================
    // Lifecycle
    // =======================================================================

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void setUp() throws Exception {
        physicalLive    = opsOS.toPhysicalName(IDX_LIVE);
        physicalWorking = opsOS.toPhysicalName(IDX_WORKING);

        cleanupTestOsIndices();
        cleanupVersionedRows();

        systemUser = APILocator.getUserAPI().getSystemUser();

        // Create OS indices and register them under the default OPENSEARCH_3X version
        // so resolveIndex() (which calls loadDefaultVersionedIndices()) finds them.
        // Physical names must match across OS, the DB, and the search path — store the
        // same value here as the one resolveIndex() will look up later.
        osIndexAPI.createIndex(physicalLive, 1);
        osIndexAPI.createIndex(physicalWorking, 1);

        versionedIndicesAPI.saveIndices(
                VersionedIndicesImpl.builder()
                        .version(DEFAULT_OS_VERSION)
                        .live(physicalLive)
                        .working(physicalWorking)
                        .build());

        // Ensure resolveIndex() reads the freshly saved rows, not a cached prior state.
        versionedIndicesAPI.clearCache();
    }

    @After
    public void tearDown() {
        cleanupTestOsIndices();
        cleanupVersionedRows();
        // Drop the cached pointer to the now-deleted test indices. setUp() registers these indices
        // as the default versioned indices and clears the cache; tearDown must clear it again, or the
        // stale cache (pointing at deleted `*_search_*` indices) leaks into the next suite member and
        // breaks its index resolution. Mirrors the clearCache() in setUp.
        versionedIndicesAPI.clearCache();
    }

    // =======================================================================
    // Tests – searchRaw
    // =======================================================================

    /**
     * Given scenario: A valid match-all JSON query against an empty working index.
     * Expected: {@link OSSearchAPIImpl#searchRaw} returns a non-null {@link ContentSearchResponse}
     *           with a non-null {@link com.dotcms.content.index.domain.SearchHits} and zero total hits.
     */
    @Test
    public void test_searchRaw_matchAll_shouldReturnEmptyResultsWithNonNullStructure()
            throws Exception {

        final String matchAll = "{\"query\":{\"match_all\":{}}}";

        final ContentSearchResponse response =
                osSearchAPI.searchRaw(matchAll, false, systemUser, false);

        assertNotNull("searchRaw must return a non-null ContentSearchResponse", response);
        assertNotNull("hits must not be null on an empty-index response", response.hits());
        assertNotNull("aggregations map must not be null", response.aggregations());
        Logger.info(this,
                "✅ test_searchRaw_matchAll_shouldReturnEmptyResultsWithNonNullStructure passed"
                        + " – totalHits=" + response.hits().getTotalHits().value());
    }

    /**
     * Given scenario: A terms aggregation query against an empty working index.
     * Expected: {@link ContentSearchResponse#aggregations()} contains the key {@code "entries"}
     *           (from the JSON {@code "aggs": {"entries": {"terms": ...}}}) and that bucket list
     *           is empty (no documents).
     */
    @Test
    public void test_searchRaw_withTermsAgg_shouldReturnAggregationKey() throws Exception {
        final String aggQuery =
                "{\"size\":0,\"aggs\":{\"entries\":{\"terms\":{\"field\":\"contenttype_dotraw\",\"size\":100}}}}";

        final ContentSearchResponse response =
                osSearchAPI.searchRaw(aggQuery, false, systemUser, false);

        assertNotNull("searchRaw must return a non-null response", response);
        assertTrue(
                "aggregations map must contain the 'entries' key even when result is empty",
                response.aggregations().containsKey("entries"));
        assertTrue("entries bucket must be empty for an empty index",
                response.aggregations().get("entries").isEmpty());
        Logger.info(this,
                "✅ test_searchRaw_withTermsAgg_shouldReturnAggregationKey passed");
    }

    /**
     * Given scenario: Two live documents share the same {@code contenttype}, queried with a
     * {@code terms} aggregation that carries a <b>nested {@code top_hits}</b> sub-aggregation —
     * exactly the shape the customer template in #36026 walks
     * ({@code aggregations.content_types.buckets[].top_content.hits}).
     *
     * <p>Expected (correct behaviour): the first-level {@code content_types} terms bucket is
     * returned <i>and</i> its nested {@code top_content} ({@code top_hits}) sub-aggregation is
     * preserved on the bucket, carrying the matching hits.</p>
     *
     * <p><b>Reproduction for the OpenSearch read-path bug:</b> this test currently <b>FAILS</b> at
     * the nested-aggregation assertion. {@code OSSearchAPIImpl} parses the query body through
     * {@code SearchRequest._DESERIALIZER} and re-applies only {@code bodyTemplate.aggregations()};
     * the opensearch-java request model ({@code Aggregation} tagged-union) does not carry the
     * sibling {@code "aggs"} key, so the nested {@code top_hits} is dropped from the request and
     * never computed by OpenSearch. The first-level bucket (key/docCount) survives, but
     * {@code bucket.getAggregations()} comes back empty. The {@code Aggregation.fromOS} /
     * {@code AggregationBucket.fromOS} mappers are <i>not</i> at fault — they faithfully map an
     * already-empty sub-aggregation map.</p>
     */
    @Test
    public void test_searchRaw_nestedTopHits_shouldBePreservedOnBuckets() throws Exception {
        final String fullLive = osIndexAPI.getNameWithClusterIDPrefix(IDX_LIVE);

        // Two live docs sharing the same contenttype -> one terms bucket with docCount 2.
        indexDocument(fullLive, "nested-a-" + RUN_ID,
                "{\"identifier\":\"nested-a-" + RUN_ID + "\",\"inode\":\"nested-a-inode-" + RUN_ID
                        + "\",\"contenttype\":\"blogtype\",\"live\":true}");
        indexDocument(fullLive, "nested-b-" + RUN_ID,
                "{\"identifier\":\"nested-b-" + RUN_ID + "\",\"inode\":\"nested-b-inode-" + RUN_ID
                        + "\",\"contenttype\":\"blogtype\",\"live\":true}");

        // terms(content_types) -> top_hits(top_content): aggregate on the keyword sub-field so the
        // dynamically-mapped string field is aggregatable regardless of explicit mappings.
        final String nestedAggQuery =
                "{\"size\":0,\"query\":{\"match_all\":{}},"
                        + "\"aggs\":{\"content_types\":{\"terms\":{\"field\":\"contenttype.keyword\",\"size\":5},"
                        + "\"aggs\":{\"top_content\":{\"top_hits\":{\"size\":3}}}}}}";

        final ContentSearchResponse response =
                osSearchAPI.searchRaw(nestedAggQuery, true, systemUser, false);

        // Sanity: first-level terms aggregation works (this part is NOT affected by the bug).
        final Map<String, Aggregation> tree = response.aggregationTree();
        assertTrue("aggregation tree must contain content_types", tree.containsKey("content_types"));
        final Aggregation contentTypes = tree.get("content_types");
        assertFalse("content_types must have at least one bucket", contentTypes.getBuckets().isEmpty());
        final AggregationBucket bucket = contentTypes.getBuckets().get(0);
        assertEquals("bucket docCount must reflect the two indexed docs", 2L, bucket.getDocCount());

        Logger.info(this, "nested-agg bucket key=" + bucket.getKeyAsString()
                + " docCount=" + bucket.getDocCount()
                + " subAggKeys=" + bucket.getAggregations().keySet());

        // THE BUG: the nested top_hits sub-aggregation must be preserved on the bucket.
        final Aggregation topContent = bucket.getAggregations().get("top_content");
        assertNotNull("nested top_hits sub-aggregation 'top_content' must be preserved on the bucket"
                + " (dropped today by the OpenSearch request round-trip)", topContent);
        assertNotNull("nested top_hits must carry a SearchHits", topContent.getHits());
        assertFalse("nested top_hits must carry at least one hit",
                topContent.getHits().getHits().isEmpty());

        Logger.info(this, "✅ test_searchRaw_nestedTopHits_shouldBePreservedOnBuckets passed");
    }

    /**
     * Given scenario: A match-all query against the live index.
     * Expected: No exception is thrown and a valid response is returned.
     */
    @Test
    public void test_searchRaw_liveIndex_shouldNotThrow() throws Exception {
        final String matchAll = "{\"query\":{\"match_all\":{}}}";

        final ContentSearchResponse response =
                osSearchAPI.searchRaw(matchAll, true, systemUser, false);

        assertNotNull("searchRaw against live index must return non-null response", response);
        assertNotNull("hits must be non-null for live index query", response.hits());
        Logger.info(this, "✅ test_searchRaw_liveIndex_shouldNotThrow passed");
    }

    /**
     * Given scenario: A null query string is passed to {@link OSSearchAPIImpl#searchRaw}.
     * Expected: A {@link com.dotmarketing.business.DotStateException} is thrown.
     */
    @Test(expected = com.dotmarketing.business.DotStateException.class)
    public void test_searchRaw_nullQuery_shouldThrowDotStateException() throws Exception {
        osSearchAPI.searchRaw(null, false, systemUser, false);
    }

    /**
     * Given scenario: An empty string query is passed to {@link OSSearchAPIImpl#searchRaw}.
     * Expected: A {@link com.dotmarketing.business.DotStateException} is thrown.
     */
    @Test(expected = com.dotmarketing.business.DotStateException.class)
    public void test_searchRaw_emptyQuery_shouldThrowDotStateException() throws Exception {
        osSearchAPI.searchRaw("", false, systemUser, false);
    }

    /**
     * Given scenario: {@code user} is {@code null} and {@code respectFrontendRoles} is false.
     * Expected: A {@link com.dotmarketing.exception.DotSecurityException} is thrown.
     */
    @Test(expected = com.dotmarketing.exception.DotSecurityException.class)
    public void test_searchRaw_nullUserNoFrontendRoles_shouldThrowDotSecurityException()
            throws Exception {
        osSearchAPI.searchRaw("{\"query\":{\"match_all\":{}}}", false, null, false);
    }

    // =======================================================================
    // Tests – search (full contentlet population)
    // =======================================================================

    /**
     * Given scenario: A match-all JSON query with no documents in the index.
     * Expected: {@link OSSearchAPIImpl#search} returns a non-null {@link ContentSearchResults}
     *           that is empty and reports zero total hits.
     */
    @Test
    public void test_search_matchAll_emptyIndex_shouldReturnEmptyContentSearchResults()
            throws Exception {

        final String matchAll = "{\"query\":{\"match_all\":{}}}";

        final ContentSearchResults results =
                osSearchAPI.search(matchAll, false, systemUser, false);

        assertNotNull("search must return non-null ContentSearchResults", results);
        assertTrue("ContentSearchResults must be empty when index has no documents",
                results.isEmpty());
        assertNotNull("getResponse must return non-null ContentSearchResponse",
                results.getResponse());
        Logger.info(this,
                "✅ test_search_matchAll_emptyIndex_shouldReturnEmptyContentSearchResults passed");
    }

    /**
     * Given scenario: A match-all query is run with {@code respectFrontendRoles=true} and a null
     *                 user — the anonymous-user path.
     * Expected: No security exception; an empty but valid result is returned.
     */
    @Test
    public void test_search_respectFrontendRoles_nullUser_shouldNotThrow() throws Exception {
        final String matchAll = "{\"query\":{\"match_all\":{}}}";

        final ContentSearchResults results =
                osSearchAPI.search(matchAll, false, null, true);

        assertNotNull("search with respectFrontendRoles=true must return non-null results", results);
        Logger.info(this,
                "✅ test_search_respectFrontendRoles_nullUser_shouldNotThrow passed");
    }

    // =======================================================================
    // Tests – pagination
    // =======================================================================

    /**
     * Given scenario: A search with explicit limit and offset against an empty index.
     * Expected: No exception; result is empty and response structure is valid.
     */
    @Test
    public void test_searchRelated_withLimitAndOffset_emptyIndex_shouldReturnValidStructure()
            throws Exception {

        final String matchAll = "{\"query\":{\"match_all\":{}}}";
        // Use a fake identifier/inode so the method constructs the query and fires against OS
        // (it will find 0 results because the index is empty)
        final com.dotmarketing.portlets.contentlet.model.Contentlet fakeContent =
                new com.dotmarketing.portlets.contentlet.model.Contentlet();
        fakeContent.setIdentifier("fake-id-" + RUN_ID);
        fakeContent.setInode("fake-inode-" + RUN_ID);

        final ContentSearchResponse response = osSearchAPI.searchRelated(
                fakeContent, "fakeRelationship", false, false, systemUser, false, 10, 0, null);

        assertNotNull("searchRelated must return non-null response", response);
        assertNotNull("hits must be non-null", response.hits());
        Logger.info(this,
                "✅ test_searchRelated_withLimitAndOffset_emptyIndex_shouldReturnValidStructure passed");
    }

    // =======================================================================
    // Tests – _source rewrite (identifier + inode always present)
    // =======================================================================

    /**
     * Given scenario: A document with both {@code identifier} and {@code inode} fields is indexed.
     *                 A plain match-all query (no {@code _source} clause) is executed via
     *                 {@link OSSearchAPIImpl#searchRaw}.
     * Expected: {@link OSSearchAPIImpl} rewrites the query to {@code "_source":[identifier,inode]}
     *           and both fields are returned in the hit's {@code sourceAsMap}.
     */
    @Test
    public void test_searchRaw_withIndexedDocument_sourceRewrite_shouldIncludeIdentifierAndInode()
            throws Exception {

        indexTestDocument(physicalWorking);

        final String matchAll = "{\"query\":{\"match_all\":{}}}";
        final ContentSearchResponse response =
                osSearchAPI.searchRaw(matchAll, false, systemUser, false);

        assertNotNull("searchRaw must return a non-null response", response);
        assertEquals("Exactly one hit expected", 1, response.hits().getTotalHits().value());

        final com.dotcms.content.index.domain.SearchHit hit =
                response.hits().iterator().next();
        assertEquals("identifier must be present in sourceAsMap",
                TEST_DOC_IDENTIFIER, hit.getSourceAsMap().get("identifier"));
        assertEquals("inode must be present in sourceAsMap — _source rewrite must include it",
                TEST_DOC_INODE, hit.getSourceAsMap().get("inode"));

        Logger.info(this, "✅ test_searchRaw_withIndexedDocument_sourceRewrite_shouldIncludeIdentifierAndInode passed");
    }

    /**
     * Given scenario: A document with both {@code identifier} and {@code inode} fields is indexed.
     *                 A query that explicitly restricts {@code _source} to only {@code ["identifier"]}
     *                 is executed via {@link OSSearchAPIImpl#searchRaw}.
     * Expected: The impl overwrites the caller's {@code _source} with {@code [identifier, inode]},
     *           so {@code inode} is still present in the hit's {@code sourceAsMap} despite not
     *           being requested by the caller.
     */
    @Test
    public void test_searchRaw_userDefinedSource_isOverwrittenToIncludeInode()
            throws Exception {

        indexTestDocument(physicalWorking);

        // Caller requests only identifier — inode intentionally omitted.
        final String queryWithSource =
                "{\"query\":{\"match_all\":{}},\"_source\":[\"identifier\"]}";
        final ContentSearchResponse response =
                osSearchAPI.searchRaw(queryWithSource, false, systemUser, false);

        assertNotNull(response);
        assertTrue("Response must have at least one hit", response.hits().getTotalHits().value() > 0);

        final com.dotcms.content.index.domain.SearchHit hit =
                response.hits().iterator().next();
        assertNotNull("inode must still be present after _source overwrite",
                hit.getSourceAsMap().get("inode"));

        Logger.info(this, "✅ test_searchRaw_userDefinedSource_isOverwrittenToIncludeInode passed");
    }

    // =======================================================================
    // Helpers
    // =======================================================================

    /**
     * Indexes {@link #TEST_DOC_JSON} into the given index and refreshes it so the
     * document is immediately visible to searches.
     */
    private void indexTestDocument(final String fullIndexName) throws Exception {
        indexDocument(fullIndexName, TEST_DOC_ID, TEST_DOC_JSON);
    }

    /**
     * Indexes an arbitrary {@code docJson} under {@code docId} into the given index and refreshes
     * it so the document is immediately visible to searches.
     */
    private void indexDocument(final String fullIndexName, final String docId, final String docJson)
            throws Exception {
        final IndexBulkRequest req = opsOS.createBulkRequest();
        opsOS.addIndexOp(req, fullIndexName, docId, docJson);
        opsOS.putToIndex(req);
        refreshTestIndex(fullIndexName);
    }

    private void refreshTestIndex(final String fullIndexName) {
        try {
            final OpenSearchClient client = clientProvider.getClient();
            client.indices().refresh(RefreshRequest.of(r -> r.index(fullIndexName)));
        } catch (final Exception e) {
            Logger.warn(this, "refreshTestIndex: error refreshing '" + fullIndexName
                    + "': " + e.getMessage());
        }
    }

    private synchronized void cleanupTestOsIndices() {
        for (final String idx : List.of(physicalLive, physicalWorking)) {
            try {
                if (osIndexAPI.indexExists(idx)) {
                    osIndexAPI.delete(idx);
                }
            } catch (Exception e) {
                Logger.warn(this, "Cleanup: error removing OS index '" + idx + "': " + e.getMessage());
            }
        }
    }

    private void cleanupVersionedRows() {
        try {
            // Remove only the test-scoped index entries by their RUN_ID-tagged index names.
            // Deleting by index_name avoids removing the entire OPENSEARCH_3X version row,
            // which would break other tests running in parallel.
            new DotConnect()
                    .setSQL("DELETE FROM indicies WHERE index_name LIKE ?")
                    .addParam("%" + RUN_ID + "%")
                    .loadResult();
        } catch (Exception e) {
            Logger.warn(this, "Cleanup: error removing versioned DB rows: " + e.getMessage());
        }
    }

}
