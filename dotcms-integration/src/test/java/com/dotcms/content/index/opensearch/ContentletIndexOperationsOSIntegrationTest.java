package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.content.index.ContentletIndexOperations;
import com.dotcms.content.index.domain.IndexBulkProcessor;
import com.dotcms.content.index.domain.IndexBulkRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.content.index.domain.IndexBulkItemResult;
import com.dotcms.content.index.domain.IndexBulkListener;
import com.dotmarketing.util.Logger;
import java.util.List;
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
 * Integration tests for {@link ContentletIndexOperationsOS} that exercise all methods of the
 * {@link ContentletIndexOperations} contract against a live OpenSearch 3.x container.
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
public class ContentletIndexOperationsOSIntegrationTest extends IntegrationTestBase {

    /**
     * Unique suffix appended to every OS index name created by this suite.
     * Prevents cross-run pollution in a shared OpenSearch node.
     */
    private static final String RUN_ID =
            UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    // ── bare index names — OSIndexAPIImpl adds the cluster prefix internally ─
    private static final String IDX_LIVE    = "live_ops_"    + RUN_ID;
    private static final String IDX_WORKING = "working_ops_" + RUN_ID;

    /** Minimal JSON document used for indexing tests. */
    private static final String TEST_DOC_JSON =
            "{\"identifier\":\"test-id-" + RUN_ID + "\","
            + "\"title\":\"ContentletIndexOperationsOS Integration Test\","
            + "\"language_id\":1,"
            + "\"contenttype\":\"testtype\"}";

    private static final String TEST_DOC_ID = "test-id-" + RUN_ID + "_1_default";

    // ── CDI-injected beans ────────────────────────────────────────────────────
    // OSTestClientProvider (@Alternative @Priority(1)) is on the test classpath and will be
    // used to configure ContentletIndexOperationsOS for testing.
    @Inject
    private ContentletIndexOperationsOS opsOS;

    /** Helper for creating / deleting / checking test indices. */
    @Inject
    private OSIndexAPIImpl osIndexAPI;

    /** Used directly for refresh-after-write operations in tests. */
    @Inject
    private OSClientProvider clientProvider;

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
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
    // Tests – synchronous batch write path
    // =========================================================================

    /**
     * Given scenario: A fresh {@link IndexBulkRequest} is created via
     *                 {@link ContentletIndexOperationsOS#createBulkRequest()}.
     * Expected: The returned handle is non-null and reports size zero.
     */
    @Test
    public void test_createBulkRequest_shouldReturnEmptyRequest() {
        final IndexBulkRequest req = opsOS.createBulkRequest();

        assertNotNull("createBulkRequest must never return null", req);
        assertEquals("A fresh bulk request must have zero operations", 0, req.size());
        assertTrue("isEmpty() must return true on a fresh request", req.isEmpty());
        Logger.info(this, "✅ test_createBulkRequest_shouldReturnEmptyRequest passed");
    }

    /**
     * Given scenario: An index operation is appended to an empty bulk request via
     *                 {@link ContentletIndexOperationsOS#addIndexOp}.
     * Expected: The request size increases to 1.
     */
    @Test
    public void test_addIndexOp_shouldIncreaseBulkRequestSize() {
        final IndexBulkRequest req = opsOS.createBulkRequest();
        assertEquals("Pre-condition: request must be empty", 0, req.size());

        opsOS.addIndexOp(req, IDX_LIVE, TEST_DOC_ID, TEST_DOC_JSON);

        assertEquals("Bulk request must contain exactly one operation after addIndexOp", 1, req.size());
        Logger.info(this, "✅ test_addIndexOp_shouldIncreaseBulkRequestSize passed");
    }

    /**
     * Given scenario: A delete operation is appended to an empty bulk request via
     *                 {@link ContentletIndexOperationsOS#addDeleteOp}.
     * Expected: The request size increases to 1.
     */
    @Test
    public void test_addDeleteOp_shouldIncreaseBulkRequestSize() {
        final IndexBulkRequest req = opsOS.createBulkRequest();
        assertEquals("Pre-condition: request must be empty", 0, req.size());

        opsOS.addDeleteOp(req, IDX_LIVE, TEST_DOC_ID);

        assertEquals("Bulk request must contain exactly one operation after addDeleteOp", 1, req.size());
        Logger.info(this, "✅ test_addDeleteOp_shouldIncreaseBulkRequestSize passed");
    }

    /**
     * Given scenario: A document is indexed via {@link ContentletIndexOperationsOS#putToIndex}
     *                 into a freshly created OS index.
     * Expected: {@link ContentletIndexOperationsOS#getIndexDocumentCount} returns 1 after indexing.
     */
    @Test
    public void test_putToIndex_shouldIndexDocument() throws Exception {
        osIndexAPI.createIndex(IDX_LIVE, 1);
        final String fullName = osIndexAPI.getNameWithClusterIDPrefix(IDX_LIVE);
        assertTrue("Pre-condition: index must exist", osIndexAPI.indexExists(IDX_LIVE));
        assertEquals("Pre-condition: index must be empty", 0L, opsOS.getIndexDocumentCount(fullName));

        final IndexBulkRequest req = opsOS.createBulkRequest();
        opsOS.addIndexOp(req, fullName, TEST_DOC_ID, TEST_DOC_JSON);
        opsOS.putToIndex(req);
        refreshTestIndex(fullName);

        assertEquals("Document count must be 1 after putToIndex", 1L,
                opsOS.getIndexDocumentCount(fullName));
        Logger.info(this, "✅ test_putToIndex_shouldIndexDocument passed");
    }

    /**
     * Given scenario: A document is indexed and then deleted via a second
     *                 {@link ContentletIndexOperationsOS#putToIndex} call containing a delete op.
     * Expected: {@link ContentletIndexOperationsOS#getIndexDocumentCount} returns 0 after the delete.
     */
    @Test
    public void test_putToIndex_withDeleteOp_shouldRemoveDocument() throws Exception {
        osIndexAPI.createIndex(IDX_LIVE, 1);
        final String fullName = osIndexAPI.getNameWithClusterIDPrefix(IDX_LIVE);

        // Index the document first
        final IndexBulkRequest indexReq = opsOS.createBulkRequest();
        opsOS.addIndexOp(indexReq, fullName, TEST_DOC_ID, TEST_DOC_JSON);
        opsOS.putToIndex(indexReq);
        refreshTestIndex(fullName);
        assertEquals("Pre-condition: document must be indexed", 1L,
                opsOS.getIndexDocumentCount(fullName));

        // Delete the document
        final IndexBulkRequest deleteReq = opsOS.createBulkRequest();
        opsOS.addDeleteOp(deleteReq, fullName, TEST_DOC_ID);
        opsOS.putToIndex(deleteReq);
        refreshTestIndex(fullName);

        assertEquals("Document count must be 0 after delete putToIndex", 0L,
                opsOS.getIndexDocumentCount(fullName));
        Logger.info(this, "✅ test_putToIndex_withDeleteOp_shouldRemoveDocument passed");
    }

    /**
     * Given scenario: {@link ContentletIndexOperationsOS#setRefreshPolicy} is called with each
     *                 known policy string ({@code "NONE"}, {@code "WAIT_FOR"}, {@code "IMMEDIATE"}).
     * Expected: No exception is thrown for any policy value.
     */
    @Test
    public void test_setRefreshPolicy_shouldNotThrow() {
        final IndexBulkRequest req = opsOS.createBulkRequest();

        for (final IndexBulkRequest.RefreshPolicy policy : IndexBulkRequest.RefreshPolicy.values()) {
            opsOS.setRefreshPolicy(req, policy);
        }

        Logger.info(this, "✅ test_setRefreshPolicy_shouldNotThrow passed");
    }

    /**
     * Given scenario: An empty bulk request is submitted via
     *                 {@link ContentletIndexOperationsOS#putToIndex}.
     * Expected: No exception is thrown (empty batch is a no-op).
     */
    @Test
    public void test_putToIndex_withEmptyRequest_shouldBeNoOp() {
        final IndexBulkRequest req = opsOS.createBulkRequest();
        assertTrue("Pre-condition: request is empty", req.isEmpty());

        opsOS.putToIndex(req); // must not throw

        Logger.info(this, "✅ test_putToIndex_withEmptyRequest_shouldBeNoOp passed");
    }

    // =========================================================================
    // Tests – async bulk-processor write path
    // =========================================================================

    /**
     * Given scenario: A bulk processor is created via
     *                 {@link ContentletIndexOperationsOS#createBulkProcessor}.
     * Expected: The returned handle is non-null and implements {@link IndexBulkProcessor}.
     */
    @Test
    public void test_createBulkProcessor_shouldReturnNonNullProcessor() {
        final IndexBulkListener listener = noOpListener();
        final IndexBulkProcessor proc = opsOS.createBulkProcessor(listener);

        assertNotNull("createBulkProcessor must never return null", proc);
        Logger.info(this, "✅ test_createBulkProcessor_shouldReturnNonNullProcessor passed");
    }

    /**
     * Given scenario: An index operation is added to the bulk processor via
     *                 {@link ContentletIndexOperationsOS#addIndexOpToProcessor}, then the processor
     *                 is closed (which triggers a flush).
     * Expected: The document is visible in the index after close.
     */
    @Test
    public void test_addIndexOpToProcessor_andClose_shouldFlushDocument() throws Exception {
        osIndexAPI.createIndex(IDX_WORKING, 1);
        final String fullName = osIndexAPI.getNameWithClusterIDPrefix(IDX_WORKING);
        assertTrue("Pre-condition: index must exist", osIndexAPI.indexExists(IDX_WORKING));
        assertEquals("Pre-condition: index must be empty", 0L,
                opsOS.getIndexDocumentCount(fullName));

        final IndexBulkListener listener = noOpListener();
        final IndexBulkProcessor proc = opsOS.createBulkProcessor(listener);
        opsOS.addIndexOpToProcessor(proc, fullName, TEST_DOC_ID, TEST_DOC_JSON);
        proc.close(); // flushes remaining operations
        refreshTestIndex(fullName);

        assertEquals("Document count must be 1 after processor flush", 1L,
                opsOS.getIndexDocumentCount(fullName));
        Logger.info(this, "✅ test_addIndexOpToProcessor_andClose_shouldFlushDocument passed");
    }

    /**
     * Given scenario: A document is indexed and then a delete operation is added to the processor
     *                 via {@link ContentletIndexOperationsOS#addDeleteOpToProcessor}, then closed.
     * Expected: The document count drops to 0 after the processor flush.
     */
    @Test
    public void test_addDeleteOpToProcessor_andClose_shouldFlushDeletion() throws Exception {
        osIndexAPI.createIndex(IDX_WORKING, 1);
        final String fullName = osIndexAPI.getNameWithClusterIDPrefix(IDX_WORKING);

        // Index the document directly
        final IndexBulkRequest indexReq = opsOS.createBulkRequest();
        opsOS.addIndexOp(indexReq, fullName, TEST_DOC_ID, TEST_DOC_JSON);
        opsOS.putToIndex(indexReq);
        refreshTestIndex(fullName);
        assertEquals("Pre-condition: document must be indexed", 1L,
                opsOS.getIndexDocumentCount(fullName));

        // Delete via the async processor
        final IndexBulkListener listener = noOpListener();
        final IndexBulkProcessor proc = opsOS.createBulkProcessor(listener);
        opsOS.addDeleteOpToProcessor(proc, fullName, TEST_DOC_ID);
        proc.close(); // flushes the pending delete
        refreshTestIndex(fullName);

        assertEquals("Document count must be 0 after delete processor flush", 0L,
                opsOS.getIndexDocumentCount(fullName));
        Logger.info(this, "✅ test_addDeleteOpToProcessor_andClose_shouldFlushDeletion passed");
    }

    // =========================================================================
    // Tests – getIndexDocumentCount
    // =========================================================================

    /**
     * Given scenario: A fresh index is created with no documents.
     * Expected: {@link ContentletIndexOperationsOS#getIndexDocumentCount} returns 0.
     */
    @Test
    public void test_getIndexDocumentCount_shouldReturnZeroForEmptyIndex() throws Exception {
        osIndexAPI.createIndex(IDX_LIVE, 1);
        final String fullName = osIndexAPI.getNameWithClusterIDPrefix(IDX_LIVE);
        assertTrue("Pre-condition: index must exist", osIndexAPI.indexExists(IDX_LIVE));

        final long count = opsOS.getIndexDocumentCount(fullName);

        assertEquals("Empty index must have document count 0", 0L, count);
        Logger.info(this, "✅ test_getIndexDocumentCount_shouldReturnZeroForEmptyIndex passed");
    }

    /**
     * Given scenario: Multiple documents are indexed into the same index.
     * Expected: {@link ContentletIndexOperationsOS#getIndexDocumentCount} returns the exact count.
     */
    @Test
    public void test_getIndexDocumentCount_shouldReflectIndexedDocuments() throws Exception {
        osIndexAPI.createIndex(IDX_LIVE, 1);
        final String fullName = osIndexAPI.getNameWithClusterIDPrefix(IDX_LIVE);

        // Index 3 documents with distinct IDs
        final IndexBulkRequest req = opsOS.createBulkRequest();
        for (int i = 1; i <= 3; i++) {
            final String docId = "test-doc-" + RUN_ID + "-" + i;
            final String json  = "{\"identifier\":\"" + docId + "\","
                    + "\"title\":\"Doc " + i + "\","
                    + "\"language_id\":1,"
                    + "\"contenttype\":\"testtype\"}";
            opsOS.addIndexOp(req, fullName, docId, json);
        }
        opsOS.putToIndex(req);
        refreshTestIndex(fullName);

        assertEquals("Document count must equal the number of indexed documents", 3L,
                opsOS.getIndexDocumentCount(fullName));
        Logger.info(this, "✅ test_getIndexDocumentCount_shouldReflectIndexedDocuments passed");
    }

    // =========================================================================
    // Tests – mirrored from ContentletIndexOperationsESTest
    // =========================================================================

    /**
     * Given scenario: Two index operations with the same doc ID are added — first an upsert,
     *                 then another upsert that overwrites it.
     * Expected: The index contains exactly 1 document (the second write is an upsert, not an
     *           insert), mirroring ES behaviour for the same pattern.
     */
    @Test
    public void test_putToIndex_upsertSemantics_shouldNotDuplicateDocument() throws Exception {
        osIndexAPI.createIndex(IDX_LIVE, 1);
        final String fullName = osIndexAPI.getNameWithClusterIDPrefix(IDX_LIVE);

        final String json1 = "{\"identifier\":\"" + TEST_DOC_ID + "\",\"title\":\"First\","
                + "\"language_id\":1,\"contenttype\":\"testtype\"}";
        final String json2 = "{\"identifier\":\"" + TEST_DOC_ID + "\",\"title\":\"Second\","
                + "\"language_id\":1,\"contenttype\":\"testtype\"}";

        // First write
        final IndexBulkRequest req1 = opsOS.createBulkRequest();
        opsOS.addIndexOp(req1, fullName, TEST_DOC_ID, json1);
        opsOS.putToIndex(req1);
        refreshTestIndex(fullName);
        assertEquals("After first write: count must be 1", 1L,
                opsOS.getIndexDocumentCount(fullName));

        // Second write with the same doc ID — must be an upsert
        final IndexBulkRequest req2 = opsOS.createBulkRequest();
        opsOS.addIndexOp(req2, fullName, TEST_DOC_ID, json2);
        opsOS.putToIndex(req2);
        refreshTestIndex(fullName);

        assertEquals("After upsert: count must still be 1 (no duplicate)", 1L,
                opsOS.getIndexDocumentCount(fullName));
        Logger.info(this, "✅ test_putToIndex_upsertSemantics_shouldNotDuplicateDocument passed");
    }

    /**
     * Given scenario: A mixed bulk request containing both index and delete operations is
     *                 submitted via {@link ContentletIndexOperationsOS#putToIndex}.
     * Expected: The net result matches expectations — indexed docs appear, deleted doc is absent.
     */
    @Test
    public void test_putToIndex_mixedOps_shouldApplyAll() throws Exception {
        osIndexAPI.createIndex(IDX_LIVE, 1);
        final String fullName = osIndexAPI.getNameWithClusterIDPrefix(IDX_LIVE);

        // Pre-index a document that will be deleted in the mixed batch
        final String deleteDocId  = "delete-me-" + RUN_ID;
        final String deleteDocJson = "{\"identifier\":\"" + deleteDocId + "\","
                + "\"title\":\"ToDelete\",\"language_id\":1,\"contenttype\":\"testtype\"}";
        final IndexBulkRequest preReq = opsOS.createBulkRequest();
        opsOS.addIndexOp(preReq, fullName, deleteDocId, deleteDocJson);
        opsOS.putToIndex(preReq);
        refreshTestIndex(fullName);
        assertEquals("Pre-condition: 1 document before mixed batch", 1L,
                opsOS.getIndexDocumentCount(fullName));

        // Mixed batch: index a new doc + delete the pre-indexed doc
        final String newDocId  = "keep-me-" + RUN_ID;
        final String newDocJson = "{\"identifier\":\"" + newDocId + "\","
                + "\"title\":\"ToKeep\",\"language_id\":1,\"contenttype\":\"testtype\"}";
        final IndexBulkRequest mixedReq = opsOS.createBulkRequest();
        opsOS.addIndexOp(mixedReq, fullName, newDocId, newDocJson);
        opsOS.addDeleteOp(mixedReq, fullName, deleteDocId);
        opsOS.putToIndex(mixedReq);
        refreshTestIndex(fullName);

        assertEquals("After mixed batch: net count must be 1 (1 added, 1 removed)", 1L,
                opsOS.getIndexDocumentCount(fullName));
        Logger.info(this, "✅ test_putToIndex_mixedOps_shouldApplyAll passed");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Returns a no-op {@link IndexBulkListener} suitable for tests that only verify the
     * bulk-processor mechanics (flush/close) and do not assert on listener callbacks.
     */
    private static IndexBulkListener noOpListener() {
        return new IndexBulkListener() {
            @Override public void beforeBulk(final long executionId, final int actionCount) { /* no-op */ }
            @Override public void afterBulk(final long executionId, final List<IndexBulkItemResult> results) { /* no-op */ }
            @Override public void afterBulk(final long executionId, final Throwable failure) { /* no-op */ }
        };
    }

    /**
     * Forces an index refresh so documents written by {@code putToIndex} or
     * {@code close()} become immediately visible to count queries.
     *
     * <p>OpenSearch does not expose a direct {@code refreshIndex} helper on
     * {@link OSIndexAPIImpl}, so this test-only helper calls the low-level client.</p>
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
     * Deletes every test-scoped index that actually exists in OpenSearch.
     * Skipping the delete when the index is absent avoids noisy error logs between tests.
     */
    private synchronized void cleanupTestIndices() {
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
}
