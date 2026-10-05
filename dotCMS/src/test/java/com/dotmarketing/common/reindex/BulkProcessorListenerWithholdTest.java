package com.dotmarketing.common.reindex;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.dotcms.content.elasticsearch.ESQueryCache;
import com.dotcms.content.index.IndexTag;
import com.dotcms.content.index.domain.IndexBulkItemResult;
import com.dotcms.content.index.opensearch.OSQueryCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link BulkProcessorListener#withhold(String)} (#37269).
 *
 * <h2>What is at stake</h2>
 * <p>One journal row covers every document of an identifier: each language and both the working
 * and the live version. When the mapping step rejects one of those documents, the row is marked
 * failed, but the identifier's healthy documents are still sent. Today a bulk success for any of
 * them deletes the row, which erases the failure of the rejected document: it never reaches the
 * index and nobody is told.</p>
 *
 * <p>{@code withhold(identifier)} is how the indexer tells the listener "this identifier already
 * failed in this batch": from then on the listener neither deletes its row on a sibling's success
 * nor marks it failed a second time when the whole request fails.</p>
 *
 * <pre>
 *   ./mvnw test -pl :dotcms-core -Dmaven.build.cache.enabled=false \
 *       -Dtest=BulkProcessorListenerWithholdTest
 * </pre>
 */
public class BulkProcessorListenerWithholdTest {

    private static final String WITHHELD = "id-1";
    private static final String OTHER = "id-2";

    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<CacheLocator> cacheLocator;
    private ReindexQueueAPI queue;

    private BulkProcessorListener listener;
    private ReindexEntry withheldEntry;
    private ReindexEntry otherEntry;

    @Before
    public void setUp() {
        queue = mock(ReindexQueueAPI.class);
        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(APILocator::getReindexQueueAPI).thenReturn(queue);
        cacheLocator = mockStatic(CacheLocator.class);
        cacheLocator.when(CacheLocator::getESQueryCache).thenReturn(mock(ESQueryCache.class));
        cacheLocator.when(CacheLocator::getOSQueryCache).thenReturn(mock(OSQueryCache.class));

        withheldEntry = entry(1L, WITHHELD);
        otherEntry = entry(2L, OTHER);
        listener = new BulkProcessorListener();
        listener.workingRecords.put(WITHHELD, withheldEntry);
        listener.workingRecords.put(OTHER, otherEntry);
    }

    @After
    public void tearDown() {
        cacheLocator.close();
        apiLocator.close();
    }

    /**
     * Given Scenario: The working version of {@code id-1} was rejected and withheld; its live
     * version and the unrelated {@code id-2} are indexed successfully in the same bulk request.
     * Expected Result: The row of {@code id-2} is deleted as usual; the row of {@code id-1} is not,
     * so the failure recorded for its rejected document survives (AC-005).
     */
    @Test
    public void test_siblingSuccess_doesNotDeleteWithheldRow() throws Exception {
        listener.withhold(WITHHELD);

        listener.afterBulk(1L, List.of(succeeded(WITHHELD + "_1_DEFAULT"),
                succeeded(OTHER + "_1_DEFAULT")));

        verify(queue).deleteReindexEntry(argThat(
                (List<ReindexEntry> rows) -> rows.contains(otherEntry)
                        && !rows.contains(withheldEntry)));
    }

    /**
     * Given Scenario: The only document sent for {@code id-1} after the withhold is indexed
     * successfully, and nothing else is in the request.
     * Expected Result: No row is deleted at all — the withheld identifier keeps its failure.
     */
    @Test
    public void test_onlyWithheldSiblingInRequest_deletesNothing() throws Exception {
        listener.withhold(WITHHELD);

        listener.afterBulk(1L, List.of(succeeded(WITHHELD + "_2_DEFAULT")));

        verify(queue, never()).deleteReindexEntry(anyList());
    }

    /**
     * Given Scenario: {@code id-1} was withheld and then the whole bulk request fails.
     * Expected Result: {@code id-2} is marked failed with the request's message; {@code id-1} is
     * not marked again, so its own reason (field and real size) is not overwritten.
     */
    @Test
    public void test_wholeRequestFailure_doesNotRemarkWithheldRow() throws Exception {
        listener.withhold(WITHHELD);

        listener.afterBulk(1L, new RuntimeException("connection reset"));

        verify(queue).markAsFailed(eq(otherEntry), anyString());
        verify(queue, never()).markAsFailed(eq(withheldEntry), any());
    }

    /**
     * Given Scenario: Nothing was withheld (control case).
     * Expected Result: Both rows are deleted on success, exactly as before the change.
     */
    @Test
    public void test_withoutWithhold_successDeletesEveryRow() throws Exception {
        listener.afterBulk(1L, List.of(succeeded(WITHHELD + "_1_DEFAULT"),
                succeeded(OTHER + "_1_DEFAULT")));

        verify(queue).deleteReindexEntry(argThat(
                (List<ReindexEntry> rows) -> rows.contains(otherEntry)
                        && rows.contains(withheldEntry)));
    }

    /**
     * Given Scenario: A shadow listener (OpenSearch in Phase 1) is told to withhold.
     * Expected Result: Nothing happens — the shadow never touches the journal.
     */
    @Test
    public void test_shadowListener_withholdIsHarmless() throws Exception {
        final BulkProcessorListener shadow = BulkProcessorListener.forShadowProvider(IndexTag.OS);

        shadow.withhold(WITHHELD);
        shadow.afterBulk(1L, List.of(succeeded(WITHHELD + "_1_DEFAULT")));

        verify(queue, never()).deleteReindexEntry(anyList());
        verify(queue, never()).markAsFailed(any(), any());
    }

    private static ReindexEntry entry(final long id, final String identifier) {
        return ReindexEntry.builder().id(id).identToIndex(identifier).priority(0).build();
    }

    private static IndexBulkItemResult succeeded(final String documentId) {
        return IndexBulkItemResult.builder().id(documentId).failed(false).build();
    }
}
