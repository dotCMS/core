package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.content.index.domain.IndexBulkItemResult;
import com.dotcms.content.index.domain.IndexBulkListener;
import com.dotcms.content.index.opensearch.ContentletIndexOperationsOS.OSIndexBulkProcessor;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;

/**
 * Unit tests for how {@link OSIndexBulkProcessor} closes a batch (#37905).
 *
 * <h2>What is at stake</h2>
 * <p>The OpenSearch reindex processor used to close a batch only when it reached
 * {@code maxActions}. A few large-but-legal documents in one batch then made a request larger than
 * OpenSearch accepts ({@code http.max_content_length}, HTTP 413, every document failed) and, while
 * the client built that request in memory, exhausted the heap of the reindex thread. The batch now
 * also closes by size: before an operation that would push it over {@code maxBytes} is added, the
 * pending batch is sent; an operation larger than the limit goes alone; small operations batch
 * exactly as before.</p>
 *
 * <p>The client is mocked; each flush is observed through the listener's
 * {@code beforeBulk(executionId, actionCount)}.</p>
 *
 * <pre>
 *   ./mvnw test -pl :dotcms-core -Dmaven.build.cache.enabled=false \
 *       -Dtest=OSIndexBulkProcessorBatchingTest
 * </pre>
 */
public class OSIndexBulkProcessorBatchingTest {

    private static final String INDEX = "cluster_test.working_test.os";

    private OSClientProvider clientProvider;
    private ContentletIndexOperationsOS operations;
    private final List<Integer> batchSizes = new ArrayList<>();

    /** Records the number of operations of every flushed batch. */
    private final IndexBulkListener listener = new IndexBulkListener() {
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
            // not needed
        }
    };

    @Before
    public void setUp() throws Exception {
        final BulkResponse response = mock(BulkResponse.class);
        when(response.items()).thenReturn(List.of());
        final OpenSearchClient client = mock(OpenSearchClient.class);
        when(client.bulk(any(BulkRequest.class))).thenReturn(response);
        clientProvider = mock(OSClientProvider.class);
        when(clientProvider.getClient()).thenReturn(client);
        operations = new ContentletIndexOperationsOS(clientProvider, mock(OSIndexAPIImpl.class),
                mock(MappingOperationsOS.class));
        batchSizes.clear();
    }

    /**
     * Given Scenario: Small operations, a count limit of 3 and a byte limit they never reach.
     * Expected Result: Batches close every 3 operations, exactly as before the change (AC-003).
     */
    @Test
    public void test_smallOperations_batchByCountAsBefore() throws Exception {
        final OSIndexBulkProcessor processor = processor(3, 10_000_000L);

        for (int i = 0; i < 7; i++) {
            add(processor, i, 100);
        }
        processor.close();

        assertEquals(List.of(3, 3, 1), batchSizes);
    }

    /**
     * Given Scenario: A byte limit of 1,000 and three 400-character operations.
     * Expected Result: The third operation would overflow the limit, so the first two are sent
     * before it is added: batches of 2 and 1.
     */
    @Test
    public void test_operationThatWouldOverflow_flushesPendingFirst() throws Exception {
        final OSIndexBulkProcessor processor = processor(250, 1_000L);

        for (int i = 0; i < 3; i++) {
            add(processor, i, 400);
        }
        processor.close();

        assertEquals(List.of(2, 1), batchSizes);
    }

    /**
     * Given Scenario: A byte limit of 1,000, a small operation, then one of 5,000 characters, then
     * another small one.
     * Expected Result: The large operation is sent in a batch of its own and is not dropped
     * (AC-004): batches of 1, 1 and 1.
     */
    @Test
    public void test_operationLargerThanLimit_isSentAlone() throws Exception {
        final OSIndexBulkProcessor processor = processor(250, 1_000L);

        add(processor, 0, 100);
        add(processor, 1, 5_000);
        add(processor, 2, 100);
        processor.close();

        assertEquals(List.of(1, 1, 1), batchSizes);
    }

    /**
     * Given Scenario: An operation of exactly the byte limit after a small one.
     * Expected Result: It does not share a batch with the small one, and nothing follows it in
     * its batch.
     */
    @Test
    public void test_operationOfExactlyTheLimit_isSentAlone() throws Exception {
        final OSIndexBulkProcessor processor = processor(250, 1_000L);

        add(processor, 0, 100);
        add(processor, 1, 1_000);
        add(processor, 2, 100);
        processor.close();

        assertEquals(List.of(1, 1, 1), batchSizes);
    }

    /**
     * Given Scenario: The byte limit is disabled ({@code -1}) and the operations are large.
     * Expected Result: Only the count limit applies, as before the change.
     */
    @Test
    public void test_disabledByteLimit_batchesByCountOnly() throws Exception {
        final OSIndexBulkProcessor processor = processor(2, -1L);

        for (int i = 0; i < 5; i++) {
            add(processor, i, 5_000);
        }
        processor.close();

        assertEquals(List.of(2, 2, 1), batchSizes);
    }

    /**
     * Given Scenario: A batch is flushed by size, then more operations follow.
     * Expected Result: The size count restarts after each flush, so the following operations are
     * batched from zero: batches of 2, 2 and 1 for five 400-character operations.
     */
    @Test
    public void test_sizeCountRestartsAfterEachFlush() throws Exception {
        final OSIndexBulkProcessor processor = processor(250, 1_000L);

        for (int i = 0; i < 5; i++) {
            add(processor, i, 400);
        }
        processor.close();

        assertEquals(List.of(2, 2, 1), batchSizes);
    }

    private OSIndexBulkProcessor processor(final int maxActions, final long maxBytes) {
        return new OSIndexBulkProcessor(clientProvider, listener, maxActions, maxBytes);
    }

    /** Adds an index operation whose JSON document is exactly {@code length} characters. */
    private void add(final OSIndexBulkProcessor processor, final int id, final int length) {
        operations.addIndexOpToProcessor(processor, INDEX, "doc-" + id, json(length));
    }

    /** A JSON object of exactly {@code length} characters ({@code {"body":"xxx"}}). */
    private static String json(final int length) {
        final String prefix = "{\"body\":\"";
        final String suffix = "\"}";
        return prefix + "x".repeat(length - prefix.length() - suffix.length()) + suffix;
    }
}
