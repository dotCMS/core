package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.content.index.domain.Aggregation;
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
        assertFalse("Pre-condition: index must not exist yet", osIndexAPI.indexExists(IDX_ONE));

        final boolean created = osSiteSearchAPI.createSiteSearchIndex(IDX_ONE, null, 1);

        assertTrue("createSiteSearchIndex must return true", created);
        assertTrue("Index must exist in OpenSearch after creation", osIndexAPI.indexExists(IDX_ONE));
        assertTrue("Index must be returned by listIndices",
                osSiteSearchAPI.listIndices().contains(IDX_ONE));

        Logger.info(this, "✅ test_createSiteSearchIndex_shouldExistAndBeListed passed – index: " + IDX_ONE);
    }

    /**
     * Given scenario: an existing site-search index.
     * Expected: deleting it through the OpenSearch index API removes it from the cluster.
     */
    @Test
    public void test_deleteSiteSearchIndex_shouldRemoveIt() throws Exception {
        osSiteSearchAPI.createSiteSearchIndex(IDX_ONE, null, 1);
        assertTrue("Pre-condition: index must exist", osIndexAPI.indexExists(IDX_ONE));

        osIndexAPI.delete(IDX_ONE);

        assertFalse("Index must be gone after deletion", osIndexAPI.indexExists(IDX_ONE));
        Logger.info(this, "✅ test_deleteSiteSearchIndex_shouldRemoveIt passed");
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
     * Given scenario: an index holding a few documents that share a common term.
     * Expected: a terms aggregation query returns a non-null aggregation tree keyed by the
     * aggregation name.
     */
    @Test
    public void test_getAggregations_shouldReturnBuckets() throws Exception {
        osSiteSearchAPI.createSiteSearchIndex(IDX_ONE, null, 1);

        for (int i = 0; i < 3; i++) {
            final SiteSearchResult doc = new SiteSearchResult();
            doc.setId(DOC_ID + "-" + i);
            doc.setUrl("/agg/" + RUN_ID + "/" + i);
            doc.setTitle("Aggregation doc " + i);
            doc.setMimeType("text/html");
            doc.setContent("aggregation bucket sample " + RUN_ID);
            doc.setContentLength(doc.getContent().length());
            osSiteSearchAPI.putToIndex(IDX_ONE, doc, "content");
        }

        final String aggQuery = new JSONObject()
                .put("size", 0)
                .put("aggs", new JSONObject().put("by_mime",
                        new JSONObject().put("terms",
                                new JSONObject().put("field", "mimeType")))).toString();

        final Map<String, Aggregation> aggregations =
                osSiteSearchAPI.getAggregations(IDX_ONE, aggQuery);

        assertNotNull("Aggregations map must not be null", aggregations);
        assertTrue("Aggregation 'by_mime' must be present", aggregations.containsKey("by_mime"));

        Logger.info(this, "✅ test_getAggregations_shouldReturnBuckets passed – keys: "
                + aggregations.keySet());
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
        for (final String name : List.of(IDX_ONE, IDX_TWO)) {
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
