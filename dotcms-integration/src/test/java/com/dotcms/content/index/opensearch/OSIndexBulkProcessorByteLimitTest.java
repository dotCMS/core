package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.content.index.domain.IndexBulkItemResult;
import com.dotcms.content.index.domain.IndexBulkListener;
import com.dotcms.content.index.domain.IndexBulkProcessor;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.util.ArrayList;
import java.util.Collections;
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
 * Integration test for byte-bounded batching of the OpenSearch reindex processor (#37905), run
 * against the OpenSearch 3.x container of the {@code OpenSearchUpgradeSuite}.
 *
 * <h2>What is at stake</h2>
 * <p>The processor used to close a batch only by document count, so large-but-legal documents
 * could make one request larger than OpenSearch accepts (HTTP 413) and exhaust the heap while the
 * client built it. This test proves, against a real server, that the processor splits the
 * documents into requests bounded by {@code OS_REINDEX_BULK_SIZE_MB} and that every document is
 * indexed. The 413 itself needs requests over 100 MB and is verified manually (see the feature's
 * quickstart), as signed off in the plan.</p>
 *
 * <pre>
 *   ./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dopensearch.upgrade.test=true \
 *       -Dit.test=OSIndexBulkProcessorByteLimitTest -Dmaven.build.cache.enabled=false
 * </pre>
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class OSIndexBulkProcessorByteLimitTest extends IntegrationTestBase {

    private static final String RUN_ID =
            UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    /** Bare index name; {@link OSIndexAPIImpl} adds the cluster prefix. */
    private static final String INDEX = "working_bytelimit_" + RUN_ID;

    private static final String LIMIT_KEY = "OS_REINDEX_BULK_SIZE_MB";

    /** Ten documents of this size: two fit under 1 MB, a third would overflow it. */
    private static final int DOCUMENT_CHARS = 400_000;

    private static final int DOCUMENTS = 10;

    @Inject
    private ContentletIndexOperationsOS opsOS;

    @Inject
    private OSIndexAPIImpl osIndexAPI;

    @Inject
    private OSClientProvider clientProvider;

    private String previousLimit;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void setUp() {
        previousLimit = Config.getStringProperty(LIMIT_KEY, null);
        Config.setProperty(LIMIT_KEY, 1);
        cleanup();
    }

    @After
    public void tearDown() {
        Config.setProperty(LIMIT_KEY, previousLimit);
        cleanup();
    }

    /**
     * Given Scenario: {@code OS_REINDEX_BULK_SIZE_MB=1} and ten index operations of about
     * 400,000 characters each, added to a processor from {@code createBulkProcessor} and closed.
     * Expected Result: The operations reach OpenSearch in five requests of two (each under 1 MB),
     * and all ten documents are indexed.
     */
    @Test
    public void test_largeDocuments_areSentInRequestsBoundedByTheByteLimit() throws Exception {
        osIndexAPI.createIndex(INDEX, 1);
        final String fullName = osIndexAPI.getNameWithClusterIDPrefix(INDEX);
        final List<Integer> batchSizes = Collections.synchronizedList(new ArrayList<>());

        final IndexBulkProcessor processor = opsOS.createBulkProcessor(recording(batchSizes));
        for (int i = 0; i < DOCUMENTS; i++) {
            opsOS.addIndexOpToProcessor(processor, fullName, "large-" + i + "_1_default",
                    document(i));
        }
        processor.close();
        refresh(fullName);

        assertEquals("every request must hold at most two ~400K documents under a 1 MB limit",
                List.of(2, 2, 2, 2, 2), batchSizes);
        assertEquals("every document must be indexed", (long) DOCUMENTS,
                opsOS.getIndexDocumentCount(fullName));
    }

    private static String document(final int i) {
        return "{\"identifier\":\"large-" + i + "\",\"title\":\"Large " + i + "\",\"body\":\""
                + "x".repeat(DOCUMENT_CHARS) + "\"}";
    }

    private static IndexBulkListener recording(final List<Integer> batchSizes) {
        return new IndexBulkListener() {
            @Override
            public void beforeBulk(final long executionId, final int actionCount) {
                batchSizes.add(actionCount);
            }

            @Override
            public void afterBulk(final long executionId, final List<IndexBulkItemResult> results) {
                // not needed
            }

            @Override
            public void afterBulk(final long executionId, final Throwable failure) {
                Logger.warn(OSIndexBulkProcessorByteLimitTest.class,
                        "bulk failed: " + failure.getMessage());
            }
        };
    }

    private void refresh(final String fullName) throws Exception {
        final OpenSearchClient client = clientProvider.getClient();
        client.indices().refresh(RefreshRequest.of(r -> r.index(fullName)));
    }

    private void cleanup() {
        try {
            if (osIndexAPI.indexExists(INDEX)) {
                osIndexAPI.delete(INDEX);
            }
        } catch (final Exception e) {
            Logger.warn(this, "Cleanup: error removing OS index '" + INDEX + "': " + e.getMessage());
        }
    }
}
