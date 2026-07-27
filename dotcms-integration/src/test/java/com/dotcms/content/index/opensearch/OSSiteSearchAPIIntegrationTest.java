package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.content.index.IndexTag;
import com.dotcms.content.index.domain.Aggregation;
import com.dotcms.content.index.domain.AggregationBucket;
import com.dotcms.content.index.domain.SearchHit;
import com.dotcms.enterprise.publishing.sitesearch.OSSiteSearchAPI;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchResult;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchResults;
import com.dotcms.LicenseTestUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONObject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Integration tests for {@link OSSiteSearchAPI} exercised against a live OpenSearch 3.x container.
 *
 * <p>Validates the OpenSearch implementation of the Site Search API in isolation: index lifecycle
 * (create / list / delete), the document round-trip ({@code putToIndex} → {@code getFromIndex} →
 * {@code search} → {@code deleteFromIndex}), aggregations, and default-index activation through
 * {@link com.dotcms.content.index.VersionedIndicesAPI}.</p>
 *
 * <p>The {@code @Inject}-ed {@link OSSiteSearchAPI} resolves its OpenSearch client through
 * {@link OSClientProvider}; the {@code @Alternative} {@link OSTestClientProvider} on the test
 * classpath points it at the {@code opensearch-upgrade} container ({@code http://localhost:9201}).
 * Index names are scoped with a per-run suffix so concurrent runs never collide; the {@code .os}
 * tag is intentionally not used for site search (see {@link OSSiteSearchAPI}).</p>
 *
 * <p>Registered in {@link com.dotcms.OpenSearchUpgradeSuite}. Run with:
 * <pre>
 *   ./mvnw verify -pl :dotcms-integration \
 *       -Dcoreit.test.skip=false \
 *       -Dopensearch.upgrade.test=true
 * </pre>
 * </p>
 *
 * @author Fabrizio Araya
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class OSSiteSearchAPIIntegrationTest extends IntegrationTestBase {

    private static final String RUN_ID =
            UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    /** Numeric suffix so names match the {@code sitesearch_<timestamp>} convention. */
    private static final String SUFFIX = String.valueOf(Math.abs((long) RUN_ID.hashCode()));

    private static final String IDX_ONE = "sitesearch_" + SUFFIX;
    private static final String IDX_TWO = "sitesearch_" + (Long.parseLong(SUFFIX) + 1);

    // The physical OpenSearch index is .os-tagged (issue #36672); the OSSiteSearchAPI surface uses the
    // logical (bare) names above, so raw osIndexAPI assertions must query the tagged physical name.
    private static final String OS_IDX_ONE = IndexTag.OS.tag(IDX_ONE);
    private static final String OS_IDX_TWO = IndexTag.OS.tag(IDX_TWO);

    private static final String DOC_ID = "os-ss-it-" + RUN_ID;

    @Inject
    private OSSiteSearchAPI osSiteSearchAPI;

    @Inject
    private OSIndexAPIImpl osIndexAPI;

    // =======================================================================
    // Lifecycle
    // =======================================================================

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
    }

    @Before
    public void setUp() {
        cleanupTestData();
    }

    @After
    public void tearDown() {
        cleanupTestData();
    }

    // =======================================================================
    // Section 1 — Core index lifecycle
    // =======================================================================

    /**
     * Given scenario: a fresh site-search index name that does not yet exist in OpenSearch.
     * Expected: createSiteSearchIndex creates it, indexExists reports it, and it shows up in
     * listIndices.
     */
    @Test
    public void test_createSiteSearchIndex_shouldExistAndBeListed() throws Exception {
        assertFalse("Pre-condition: index must not exist yet", osIndexAPI.indexExists(OS_IDX_ONE));

        final boolean created = osSiteSearchAPI.createSiteSearchIndex(IDX_ONE, null, 1);

        assertTrue("createSiteSearchIndex must return true", created);
        assertTrue("The physical OpenSearch index must be .os-tagged after creation (issue #36672)",
                osIndexAPI.indexExists(OS_IDX_ONE));
        assertTrue("listIndices must return the LOGICAL (bare) name",
                osSiteSearchAPI.listIndices().contains(IDX_ONE));
        assertFalse("listIndices must NOT leak the .os-tagged name",
                osSiteSearchAPI.listIndices().contains(OS_IDX_ONE));

        Logger.info(this, "✅ test_createSiteSearchIndex_shouldExistAndBeListed passed – index: " + IDX_ONE);
    }

    /**
     * Given scenario: an existing site-search index.
     * Expected: deleting it through the OpenSearch index API removes it from the cluster.
     */
    @Test
    public void test_deleteSiteSearchIndex_shouldRemoveIt() throws Exception {
        osSiteSearchAPI.createSiteSearchIndex(IDX_ONE, null, 1);
        assertTrue("Pre-condition: index must exist", osIndexAPI.indexExists(OS_IDX_ONE));

        osIndexAPI.delete(OS_IDX_ONE);

        assertFalse("Index must be gone after deletion", osIndexAPI.indexExists(OS_IDX_ONE));
        Logger.info(this, "✅ test_deleteSiteSearchIndex_shouldRemoveIt passed");
    }

    /**
     * Given scenario: a site-search index name that does not exist on this OpenSearch engine — the
     * situation the build-time index switch hits during migration when the old default lived on only
     * one engine (a Phase-0 ES-only index has no OpenSearch twin).
     * Expected: {@code deleteIndex} is a no-op, NOT a thrown {@code index_not_found}. Before the fix
     * the raw delete threw, and because that delete ran inside the Site Search build's index switch,
     * it aborted the whole build after the surviving copy had already been destroyed (issue #36360,
     * I-7).
     */
    @Test
    public void test_deleteIndex_absentOnThisEngine_isIdempotent() throws Exception {
        assertFalse("Pre-condition: index must not exist", osIndexAPI.indexExists(OS_IDX_ONE));

        // Must not throw even though the index is absent on this engine.
        osSiteSearchAPI.deleteIndex(IDX_ONE);

        assertFalse("Index must still be absent", osIndexAPI.indexExists(OS_IDX_ONE));
        Logger.info(this, "✅ test_deleteIndex_absentOnThisEngine_isIdempotent passed");
    }

    /**
     * Given scenario: an alias lookup whose batch mixes an existing aliased index with a name that
     * does NOT exist on this engine — exactly what the Site Search portlet does during migration,
     * when it passes the MERGED ES∪OS index list to a single-engine {@code getIndexAlias}.
     * Expected: the alias of the existing index is still returned; one missing index in the batch
     * must not throw {@code index_not_found} and wipe every alias (issue #36360, I-3).
     */
    @Test
    public void test_getIndexAlias_toleratesMissingIndexInBatch() throws Exception {
        final String alias = "ss_alias_" + RUN_ID;
        osSiteSearchAPI.createSiteSearchIndex(IDX_ONE, alias, 1);
        assertFalse("Pre-condition: sibling index must not exist", osIndexAPI.indexExists(OS_IDX_TWO));

        // OS_IDX_TWO is absent on this engine; before the fix its presence in the batch threw
        // index_not_found and returned an empty map. Query the .os-tagged physical names (issue #36672).
        final Map<String, String> aliases = osIndexAPI.getIndexAlias(List.of(OS_IDX_ONE, OS_IDX_TWO));

        assertEquals("the resolvable alias must survive a missing sibling in the same batch",
                alias, aliases.get(OS_IDX_ONE));
        Logger.info(this, "✅ test_getIndexAlias_toleratesMissingIndexInBatch passed");
    }

    // =======================================================================
    // Section 2 — Document round-trip (put / get / search / delete)
    // =======================================================================

    /**
     * Given scenario: an empty site-search index.
     * Expected: a document put to the index is retrievable by id, discoverable by search, and gone
     * after deleteFromIndex.
     */
    @Test
    public void test_putGetSearchDelete_documentRoundTrip() throws Exception {
        osSiteSearchAPI.createSiteSearchIndex(IDX_ONE, null, 1);
        assertNull("Pre-condition: document must not exist yet",
                osSiteSearchAPI.getFromIndex(IDX_ONE, DOC_ID));

        final SiteSearchResult doc = new SiteSearchResult();
        doc.setId(DOC_ID);
        doc.setUrl("/os-site-search-it/" + RUN_ID);
        doc.setTitle("OpenSearch Site Search IT " + RUN_ID);
        doc.setMimeType("text/html");
        doc.setContent("dotcms opensearch site search integration roundtrip " + RUN_ID);
        doc.setContentLength(doc.getContent().length());

        osSiteSearchAPI.putToIndex(IDX_ONE, doc, "content");

        final SiteSearchResult fetched = osSiteSearchAPI.getFromIndex(IDX_ONE, DOC_ID);
        assertNotNull("Document must be retrievable after put", fetched);
        assertEquals("Fetched document id must match", DOC_ID, fetched.getId());

        final SiteSearchResults results = osSiteSearchAPI.search(IDX_ONE, "roundtrip", 0, 10);
        assertNull("Search must not return an error: " + results.getError(), results.getError());
        assertTrue("Search must find the indexed document", results.getTotalResults() >= 1);

        osSiteSearchAPI.deleteFromIndex(IDX_ONE, DOC_ID);
        assertNull("Document must be gone after deleteFromIndex",
                osSiteSearchAPI.getFromIndex(IDX_ONE, DOC_ID));

        Logger.info(this, "✅ test_putGetSearchDelete_documentRoundTrip passed – hits: "
                + results.getTotalResults());
    }

    /**
     * Given scenario: an index holding 3 html + 2 pdf documents.
     * Expected: a terms aggregation on {@code mimeType} maps through the OpenSearch
     * {@code fromOS(StringTermsBucket)} factory to a neutral {@link Aggregation} with one bucket per
     * mimeType — correct keys, doc counts, {@code getKeyAsString} mirroring {@code getKey}, a null
     * numeric key for the non-numeric mimeType, and no top-hits — so the OS path produces the same
     * neutral shape the ES path does (not merely a non-null map).
     */
    @Test
    public void test_getAggregations_termsBucketsHaveCorrectKeysAndCounts() throws Exception {
        osSiteSearchAPI.createSiteSearchIndex(IDX_ONE, null, 1);

        final int htmlDocs = 3;
        final int pdfDocs = 2;
        for (int i = 0; i < htmlDocs + pdfDocs; i++) {
            final SiteSearchResult doc = new SiteSearchResult();
            doc.setId(DOC_ID + "-" + i);
            doc.setUrl("/agg/" + RUN_ID + "/" + i);
            doc.setTitle("Aggregation doc " + i);
            doc.setMimeType(i < htmlDocs ? "text/html" : "application/pdf");
            doc.setContent("aggregation bucket sample " + RUN_ID);
            doc.setContentLength(doc.getContent().length());
            osSiteSearchAPI.putToIndex(IDX_ONE, doc, "content");
        }

        final String aggQuery = new JSONObject()
                .put("size", 0)
                .put("aggs", new JSONObject().put("by_mime",
                        new JSONObject().put("terms",
                                new JSONObject().put("field", "mimeType").put("size", 10)))).toString();

        final Map<String, Aggregation> aggregations =
                osSiteSearchAPI.getAggregations(IDX_ONE, aggQuery);

        assertNotNull("Aggregations map must not be null", aggregations);
        final Aggregation byMime = aggregations.get("by_mime");
        assertNotNull("Aggregation 'by_mime' must be present", byMime);
        assertEquals("aggregation name must round-trip", "by_mime", byMime.getName());
        assertNull("a terms aggregation carries no top-hits", byMime.getHits());
        assertEquals("there must be one bucket per mimeType", 2, byMime.getBuckets().size());

        final Set<String> expectedMimes = Set.of("text/html", "application/pdf");
        long htmlCount = -1;
        long pdfCount = -1;
        for (final AggregationBucket bucket : byMime.getBuckets()) {
            assertTrue("bucket key must be a known mimeType", expectedMimes.contains(bucket.getKey()));
            assertEquals("getKeyAsString must mirror getKey", bucket.getKey(), bucket.getKeyAsString());
            assertNull("a non-numeric key must yield a null number", bucket.getKeyAsNumber());
            assertTrue("each bucket must carry documents", bucket.getDocCount() > 0);
            if ("text/html".equals(bucket.getKey())) {
                htmlCount = bucket.getDocCount();
            } else if ("application/pdf".equals(bucket.getKey())) {
                pdfCount = bucket.getDocCount();
            }
        }
        assertEquals("html bucket must count the html docs", htmlDocs, htmlCount);
        assertEquals("pdf bucket must count the pdf docs", pdfDocs, pdfCount);

        Logger.info(this, "✅ test_getAggregations_termsBucketsHaveCorrectKeysAndCounts passed");
    }

    /**
     * Given scenario: documents carrying a {@code modified} date, aggregated by a
     * {@code date_histogram} on that field.
     * Expected: the aggregation maps through the OpenSearch {@code fromSingleOS} date-histogram
     * branch to a neutral {@link Aggregation} typed {@code "date_histogram"} whose buckets expose
     * epoch-millis keys. Before the fix that branch did not exist, so {@code fromSingleOS} returned
     * {@code null} and the whole {@code by_month} aggregation was silently dropped under OpenSearch
     * reads — the ES path always returned it (issue #36360, I-6).
     */
    @Test
    public void test_getAggregations_dateHistogramMapsOnOpenSearchPath() throws Exception {
        osSiteSearchAPI.createSiteSearchIndex(IDX_ONE, null, 1);

        final java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(2024, java.util.Calendar.JANUARY, 15, 0, 0, 0);
        for (int i = 0; i < 3; i++) {
            final SiteSearchResult doc = new SiteSearchResult();
            doc.setId(DOC_ID + "-dh-" + i);
            doc.setUrl("/agg-dh/" + RUN_ID + "/" + i);
            doc.setTitle("Date histogram doc " + i);
            doc.setMimeType("text/html");
            doc.setContent("date histogram sample " + RUN_ID);
            doc.setContentLength(doc.getContent().length());
            doc.setModified(cal.getTime());
            osSiteSearchAPI.putToIndex(IDX_ONE, doc, "content");
        }

        final String aggQuery = new JSONObject()
                .put("size", 0)
                .put("aggs", new JSONObject().put("by_month", new JSONObject()
                        .put("date_histogram", new JSONObject()
                                .put("field", "modified")
                                .put("calendar_interval", "month")))).toString();

        final Map<String, Aggregation> aggregations =
                osSiteSearchAPI.getAggregations(IDX_ONE, aggQuery);

        final Aggregation byMonth = aggregations.get("by_month");
        assertNotNull("date_histogram 'by_month' must be mapped, not dropped", byMonth);
        assertEquals("type must match the ES path for parity", "date_histogram", byMonth.getType());
        assertFalse("date_histogram must carry at least one bucket", byMonth.getBuckets().isEmpty());

        final AggregationBucket bucket = byMonth.getBuckets().get(0);
        assertTrue("bucket must count the indexed docs", bucket.getDocCount() >= 1);
        assertNotNull("a date-histogram key must expose epoch-millis as a number",
                bucket.getKeyAsNumber());

        Logger.info(this, "✅ test_getAggregations_dateHistogramMapsOnOpenSearchPath passed");
    }

    /**
     * Given scenario: a terms aggregation with a nested {@code top_hits} sub-aggregation.
     * Expected: the OpenSearch path preserves the nested {@code top_docs} as a neutral
     * {@link Aggregation} carrying {@link SearchHit}s (each with an id and a non-empty source),
     * reachable via {@code bucket.getAggregations()} — exercising
     * {@code AggregationBucket.fromOS} sub-aggregation nesting and {@code SearchHits.from(OS hits)},
     * which the terms-only test does not reach.
     */
    @Test
    public void test_getAggregations_nestedTopHits_preservedOnOpenSearchPath() throws Exception {
        osSiteSearchAPI.createSiteSearchIndex(IDX_ONE, null, 1);

        for (int i = 0; i < 3; i++) {
            final SiteSearchResult doc = new SiteSearchResult();
            doc.setId(DOC_ID + "-th-" + i);
            doc.setUrl("/agg-th/" + RUN_ID + "/" + i);
            doc.setTitle("Top hits doc " + i);
            doc.setMimeType("text/html");
            doc.setContent("top hits nested sample " + RUN_ID);
            doc.setContentLength(doc.getContent().length());
            osSiteSearchAPI.putToIndex(IDX_ONE, doc, "content");
        }

        final String aggQuery = new JSONObject()
                .put("size", 0)
                .put("aggs", new JSONObject().put("by_mime", new JSONObject()
                        .put("terms", new JSONObject().put("field", "mimeType").put("size", 10))
                        .put("aggs", new JSONObject().put("top_docs",
                                new JSONObject().put("top_hits",
                                        new JSONObject().put("size", 2)))))).toString();

        final Map<String, Aggregation> aggregations =
                osSiteSearchAPI.getAggregations(IDX_ONE, aggQuery);

        final Aggregation byMime = aggregations.get("by_mime");
        assertNotNull("'by_mime' aggregation must be present", byMime);
        assertFalse("'by_mime' must have buckets", byMime.getBuckets().isEmpty());

        final AggregationBucket firstBucket = byMime.getBuckets().get(0);
        final Aggregation topDocs = firstBucket.getAggregations().get("top_docs");
        assertNotNull("nested top_hits sub-aggregation must be preserved on the OS path", topDocs);
        assertNotNull("top_hits must carry a SearchHits container", topDocs.getHits());

        final List<SearchHit> hits = topDocs.getHits().getHits();
        assertFalse("top_hits must carry at least one hit", hits.isEmpty());
        final SearchHit hit = hits.get(0);
        assertNotNull("each top-hit must expose an id", hit.getId());
        assertFalse("each top-hit must expose its source document", hit.getSourceAsMap().isEmpty());

        Logger.info(this, "✅ test_getAggregations_nestedTopHits_preservedOnOpenSearchPath passed – "
                + "hits: " + hits.size());
    }

    /**
     * Given scenario: a document write targeting an index name carrying characters OpenSearch
     * forbids.
     * Expected: putToIndex fails fast with an IllegalArgumentException (the malformed name never
     * reaches the cluster as a cryptic HTTP 400).
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_putToIndex_invalidIndexName_throwsFast() {
        final SiteSearchResult doc = new SiteSearchResult();
        doc.setId(DOC_ID);
        doc.setContent("x");
        doc.setContentLength(1);
        osSiteSearchAPI.putToIndex("Invalid Name/With*Chars", doc, "content");
    }

    /**
     * Given scenario: a delete targeting a blank index name.
     * Expected: deleteFromIndex fails fast with an IllegalArgumentException rather than NPE-ing on
     * the null/blank name.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_deleteFromIndex_blankIndexName_throwsFast() {
        osSiteSearchAPI.deleteFromIndex("   ", DOC_ID);
    }

    // =======================================================================
    // Section 3 — Default index activation (VersionedIndicesAPI)
    // =======================================================================

    /**
     * Given scenario: a created site-search index that is not yet the default.
     * Expected: activateIndex makes isDefaultIndex true and orders it first in listIndices;
     * deactivateIndex clears the default.
     */
    @Test
    public void test_activateDeactivate_shouldToggleDefault() throws Exception {
        osSiteSearchAPI.createSiteSearchIndex(IDX_ONE, null, 1);
        assertFalse("Pre-condition: index must not be default yet",
                osSiteSearchAPI.isDefaultIndex(IDX_ONE));

        osSiteSearchAPI.activateIndex(IDX_ONE);
        assertTrue("Index must be the default after activation",
                osSiteSearchAPI.isDefaultIndex(IDX_ONE));

        osSiteSearchAPI.deactivateIndex(IDX_ONE);
        assertFalse("Index must no longer be the default after deactivation",
                osSiteSearchAPI.isDefaultIndex(IDX_ONE));

        Logger.info(this, "✅ test_activateDeactivate_shouldToggleDefault passed");
    }

    /**
     * Given scenario: two created site-search indices with the second activated as default.
     * Expected: listIndices returns both and places the active (default) index first.
     */
    @Test
    public void test_listIndices_shouldPlaceDefaultFirst() throws Exception {
        osSiteSearchAPI.createSiteSearchIndex(IDX_ONE, null, 1);
        osSiteSearchAPI.createSiteSearchIndex(IDX_TWO, null, 1);

        osSiteSearchAPI.activateIndex(IDX_TWO);

        final List<String> indices = osSiteSearchAPI.listIndices();
        assertTrue("Both indices must be listed",
                indices.contains(IDX_ONE) && indices.contains(IDX_TWO));
        assertEquals("The default index must be first", IDX_TWO, indices.get(0));

        Logger.info(this, "✅ test_listIndices_shouldPlaceDefaultFirst passed – order: " + indices);
    }

    // =======================================================================
    // Section 4 — Additional interface methods
    // =======================================================================

    /**
     * Given scenario: no closed site-search indices for this run.
     * Expected: listClosedIndices returns a non-null list without raising.
     */
    @Test
    public void test_listClosedIndices_shouldNotFail() {
        final List<String> closed = osSiteSearchAPI.listClosedIndices();
        assertNotNull("listClosedIndices must never return null", closed);
        Logger.info(this, "✅ test_listClosedIndices_shouldNotFail passed – count: " + closed.size());
    }

    // =======================================================================
    // Cleanup helpers
    // =======================================================================

    private synchronized void cleanupTestData() {
        for (final String name : List.of(OS_IDX_ONE, OS_IDX_TWO)) {
            try {
                if (osIndexAPI.indexExists(name)) {
                    osIndexAPI.delete(name);
                }
            } catch (final Exception e) {
                Logger.warn(this, "Cleanup: error removing OS index '" + name + "': " + e.getMessage());
            }
        }
        cleanupVersionedRows();
    }

    private void cleanupVersionedRows() {
        try {
            new DotConnect()
                    .setSQL("DELETE FROM indicies WHERE index_name LIKE ?")
                    .addParam("%" + SUFFIX + "%")
                    .loadResult();
            APILocator.getVersionedIndicesAPI().clearCache();
        } catch (final Exception e) {
            Logger.warn(this, "Cleanup: error removing versioned DB rows: " + e.getMessage());
        }
    }
}
