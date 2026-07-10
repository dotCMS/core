package com.dotcms.enterprise.publishing.sitesearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotmarketing.business.DotStateException;
import com.dotcms.content.index.IndexAPIImpl;
import com.dotcms.content.index.IndexConfigHelper;
import com.dotcms.content.index.opensearch.OSIndexAPIImpl;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Integration tests that exercise Site Search through the phase-aware {@link SiteSearchAPIImpl}
 * router in a <strong>dual-write</strong> phase, where every write fans out to both the
 * Elasticsearch ({@link ESSiteSearchAPI}) and OpenSearch ({@link OSSiteSearchAPI}) leaves.
 *
 * <p>These tests guard two regressions that only reproduce through the router fan-out — the
 * isolated {@link com.dotcms.content.index.opensearch.OSSiteSearchAPIIntegrationTest} (which calls
 * the OS leaf directly) cannot catch them:</p>
 *
 * <ol>
 *   <li><strong>Shared mutable result across the fan-out.</strong> {@code putToIndex} mutates the
 *       {@link SiteSearchResult} map in place — notably {@link SiteSearchResult#setKeywords(String)}
 *       rewrites the {@code keywords} entry from a {@code String} to a {@code List}. With a single
 *       shared instance, the first leaf (ES) corrupted the input the second leaf (OS) then read,
 *       producing {@code ClassCastException: EmptyList cannot be cast to String} on the OS write —
 *       silently dropping <em>every</em> document from OpenSearch. The router now hands each
 *       provider its own copy. This test asserts the document actually lands in OpenSearch.</li>
 *   <li><strong>Mapping fan-out leak.</strong> {@code createSiteSearchIndex} on the ES leaf applied
 *       its mapping through the phase-dispatched {@code ESMappingAPIImpl.putMapping}, which fanned
 *       out a second time to OpenSearch using a {@code .os}-tagged physical name that site-search OS
 *       indices never use → HTTP 404. The create path is now ES-pinned; this test asserts a
 *       router-driven create yields a fully functional, queryable OS index.</li>
 * </ol>
 *
 * <p>Runs only when ES and OS are separate clusters (dual-write requires two endpoints); skipped
 * via {@link org.junit.Assume#assumeFalse} on the single-cluster {@code opensearch-upgrade}
 * profile. Registered in {@link com.dotcms.OpenSearchUpgradeSuite}. Run with:
 * <pre>
 *   ./mvnw verify -pl :dotcms-integration \
 *       -Dcoreit.test.skip=false \
 *       -Dopensearch.upgrade.test=true
 * </pre>
 *
 * @author Fabrizio Araya
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class SiteSearchDualWriteRouterIT extends IntegrationTestBase {

    /** Phase 1 — dual-write, ES reads. Writes fan out to [ES, OS]; reads come from ES. */
    private static final int PHASE_DUAL_WRITE_ES_READS = 1;

    private static final String RUN_ID =
            UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    /** Numeric suffix so the name matches the {@code sitesearch_<timestamp>} convention. */
    private static final String SUFFIX = String.valueOf(Math.abs((long) RUN_ID.hashCode()));

    private static final String IDX = "sitesearch_" + SUFFIX;
    private static final String DOC_ID = "ss-dualwrite-it-" + RUN_ID;

    @Inject
    private OSSiteSearchAPI osSiteSearchAPI;

    @Inject
    private OSIndexAPIImpl osIndexAPI;

    /** The phase-aware fan-out router under test. */
    private SiteSearchAPI router;

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
        // Dual-write fans out to both clusters; a single-cluster profile would collide on the
        // shared untagged site-search name (and cannot host both leaves), so skip there.
        assumeFalse("Requires separate ES and OS clusters for dual-write", esSameAsOs());
        router = APILocator.getSiteSearchAPI();
        cleanupTestData();
        setPhase(PHASE_DUAL_WRITE_ES_READS);
    }

    @After
    public void tearDown() {
        setPhase(null);
        cleanupTestData();
    }

    // =======================================================================
    // Tests
    // =======================================================================

    /**
     * Given scenario: Phase 1 (dual-write). An index and a single document with {@code keywords}
     * set are written through the router, fanning out to ES then OS on the same result instance.
     * Expected: the document reaches OpenSearch (no {@code ClassCastException} on the OS leaf) and
     * is searchable through the router's ES read path — proving the dual-write completed on both
     * backends. {@code keywords} round-trips as a {@code List}.
     */
    @Test
    public void test_dualWritePutToIndex_documentReachesBothBackends() throws Exception {
        router.createSiteSearchIndex(IDX, null, 1);

        final SiteSearchResult doc = new SiteSearchResult();
        doc.setId(DOC_ID);
        doc.setUrl("/ss-dualwrite-it/" + RUN_ID);
        doc.setTitle("Dual-write Site Search IT " + RUN_ID);
        doc.setMimeType("text/html");
        doc.setContent("dotcms dual write roundtrip " + RUN_ID);
        doc.setContentLength(doc.getContent().length());
        // The exact Bug 1 trigger: keywords enters the map as a raw String. The first leaf in the
        // fan-out rewrites it to a List; the second leaf must not see that mutation.
        doc.getMap().put("keywords", "alpha, beta");

        router.putToIndex(IDX, doc, "content");

        // Bug 1 — OpenSearch must have received the document (unpatched: ClassCastException → null).
        final SiteSearchResult fromOs = osSiteSearchAPI.getFromIndex(IDX, DOC_ID);
        assertNotNull("Document must be retrievable from OpenSearch after dual-write", fromOs);
        assertEquals("Document id must match in OpenSearch", DOC_ID, fromOs.getId());
        assertEquals("keywords must round-trip as a trimmed list",
                List.of("alpha", "beta"), fromOs.getKeywords());

        // The dual-write also reached ES: in Phase 1 the router reads from ES.
        final SiteSearchResults esRead = router.search(IDX, "roundtrip", 0, 10);
        assertNull("ES read must not error: " + esRead.getError(), esRead.getError());
        assertTrue("Document must be searchable via the router's ES read path",
                esRead.getTotalResults() >= 1);

        Logger.info(this, "✅ test_dualWritePutToIndex_documentReachesBothBackends passed");
    }

    /**
     * Given scenario: Phase 1 (dual-write). A batch of documents is written through the
     * {@code putToIndex(String, List, String)} router overload. This exercises the list fan-out
     * path, where each provider must receive its own copy of every result.
     * Expected: every document lands in OpenSearch.
     */
    @Test
    public void test_dualWriteBatchPutToIndex_allDocumentsReachOpenSearch() throws Exception {
        router.createSiteSearchIndex(IDX, null, 1);

        final List<SiteSearchResult> docs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final SiteSearchResult doc = new SiteSearchResult();
            doc.setId(DOC_ID + "-" + i);
            doc.setUrl("/ss-dualwrite-batch/" + RUN_ID + "/" + i);
            doc.setTitle("Batch doc " + i);
            doc.setMimeType("text/html");
            doc.setContent("dotcms dual write batch sample " + RUN_ID);
            doc.setContentLength(doc.getContent().length());
            doc.getMap().put("keywords", "kw" + i + ", shared");
            docs.add(doc);
        }

        router.putToIndex(IDX, docs, "content");

        for (int i = 0; i < 3; i++) {
            final String id = DOC_ID + "-" + i;
            assertNotNull("Batch document '" + id + "' must reach OpenSearch",
                    osSiteSearchAPI.getFromIndex(IDX, id));
        }

        Logger.info(this, "✅ test_dualWriteBatchPutToIndex_allDocumentsReachOpenSearch passed");
    }

    /**
     * Given Scenario: dual-write phase; a site-search index created via the router exists in both
     *                 backends.
     * When : {@code router.deleteIndex(IDX)} is called.
     * Then : the index is removed from BOTH the ES and the OS cluster — a site-search delete mirrors
     *        to both engines instead of being mistreated as a content index (issue #35640).
     */
    @Test
    public void test_deleteIndex_removesFromBothBackends() throws Exception {
        router.createSiteSearchIndex(IDX, null, 1);
        final ESIndexAPI esIndex = ((IndexAPIImpl) APILocator.getESIndexAPI()).esImpl();
        assertTrue("Pre: must exist in ES", esIndex.indexExists(IDX));
        assertTrue("Pre: must exist in OS", osIndexAPI.indexExists(IDX));

        router.deleteIndex(IDX);

        assertFalse("Site-search index must be gone from ES", esIndex.indexExists(IDX));
        assertFalse("Site-search index must be gone from OS", osIndexAPI.indexExists(IDX));

        Logger.info(this, "✅ test_deleteIndex_removesFromBothBackends passed");
    }

    /**
     * Given Scenario: dual-write phase; a site-search index is created and activated (it becomes the
     *                 default/active site-search index).
     * When : {@code router.deleteIndex(IDX)} is called on the active index.
     * Then : it is rejected with {@link DotStateException} and the index survives — the active
     *        site-search index cannot be deleted (deactivate first), mirroring the content
     *        active-index guard (issue #35640).
     */
    @Test
    public void test_deleteIndex_activeIndex_isRejected() throws Exception {
        router.createSiteSearchIndex(IDX, null, 1);
        router.activateIndex(IDX);
        final ESIndexAPI esIndex = ((IndexAPIImpl) APILocator.getESIndexAPI()).esImpl();
        try {
            assertThrows(DotStateException.class, () -> router.deleteIndex(IDX));
            // The active index must survive in BOTH engines — the guard blocks before any delete.
            assertTrue("Active site-search index must survive in ES", esIndex.indexExists(IDX));
            assertTrue("Active site-search index must survive in OS", osIndexAPI.indexExists(IDX));
        } finally {
            router.deactivateIndex(IDX);
        }

        Logger.info(this, "✅ test_deleteIndex_activeIndex_isRejected passed");
    }

    // =======================================================================
    // Helpers
    // =======================================================================

    /**
     * True when the ES and OS clients are configured against the same cluster endpoint (the
     * single-cluster {@code opensearch-upgrade} profile). Mirrors the gate used by the core
     * migration ITs.
     */
    private static boolean esSameAsOs() {
        final String esEndpoint = Config.getStringProperty("DOT_ES_ENDPOINTS",
                "http://localhost:9207");
        final String osEndpoint = Config.getStringProperty("OS_ENDPOINTS",
                "http://localhost:9201");
        return esEndpoint.trim().equalsIgnoreCase(osEndpoint.trim());
    }

    private static void setPhase(final Integer ordinal) {
        Config.setProperty(IndexConfigHelper.MigrationPhase.FLAG_KEY,
                ordinal == null ? null : String.valueOf(ordinal));
    }

    private synchronized void cleanupTestData() {
        try {
            if (osIndexAPI.indexExists(IDX)) {
                osIndexAPI.delete(IDX);
            }
        } catch (final Exception e) {
            Logger.warn(this, "Cleanup: error removing OS index '" + IDX + "': " + e.getMessage());
        }
        // The dual-write create also lands an ES index; remove it directly on the ES cluster.
        try {
            final ESIndexAPI esIndex = ((IndexAPIImpl) APILocator.getESIndexAPI()).esImpl();
            if (esIndex.indexExists(IDX)) {
                esIndex.delete(IDX);
            }
        } catch (final Exception e) {
            Logger.warn(this, "Cleanup: error removing ES index '" + IDX + "': " + e.getMessage());
        }
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
