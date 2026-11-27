package com.dotmarketing.common.reindex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dotcms.content.elasticsearch.ESQueryCache;
import com.dotcms.content.index.IndexTag;
import com.dotcms.content.index.domain.IndexBulkItemResult;
import com.dotcms.content.index.opensearch.OSQueryCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for how {@link BulkProcessorListener} acknowledges journal entries (#37270, path C).
 *
 * <h2>What is at stake</h2>
 * <p>One journal row stands for every document of an identifier: each language, the working and
 * the live version, the same version written to the working and the live index and, in Phase 2,
 * the Elasticsearch and the OpenSearch leg. Before this change a successful document deleted the
 * row as soon as its bulk response came back. When a sibling document of the same identifier was
 * rejected, its failure was erased: if the failure reported first the success deleted the failed
 * row; if the success reported first the failure's {@code UPDATE … WHERE id = ?} matched nothing.
 * Either way the rejected document stayed out of the index with no record.</p>
 *
 * <p>The listener now only records, per identifier, whether any document succeeded and whether
 * any failed. At the end of the batch {@link BulkProcessorListener#entriesToAcknowledge()} returns
 * the entries that succeeded and never failed, and {@link ReindexThread} deletes those. Each entry
 * is marked failed at most once per batch, which also keeps the parked-content notification from
 * firing twice for one entry.</p>
 *
 * <p>The arrival order of real bulk responses cannot be forced against a live engine, so both
 * orders are proven here by calling {@code afterBulk} directly.</p>
 *
 * <pre>
 *   ./mvnw test -pl :dotcms-core -Dmaven.build.cache.enabled=false -Dtest=BulkProcessorListenerAckTest
 * </pre>
 */
public class BulkProcessorListenerAckTest {

    private static final String SIBLINGS = "id-1";
    private static final String OTHER = "id-2";

    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<CacheLocator> cacheLocator;
    private ReindexQueueAPI queue;
    private ESQueryCache esCache;

    private BulkProcessorListener listener;
    private ReindexEntry siblingsEntry;
    private ReindexEntry otherEntry;

    @Before
    public void setUp() {
        queue = mock(ReindexQueueAPI.class);
        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(APILocator::getReindexQueueAPI).thenReturn(queue);
        esCache = mock(ESQueryCache.class);
        cacheLocator = mockStatic(CacheLocator.class);
        cacheLocator.when(CacheLocator::getESQueryCache).thenReturn(esCache);
        cacheLocator.when(CacheLocator::getOSQueryCache).thenReturn(mock(OSQueryCache.class));

        siblingsEntry = entry(1L, SIBLINGS);
        otherEntry = entry(2L, OTHER);
        listener = new BulkProcessorListener();
        listener.workingRecords.put(SIBLINGS, siblingsEntry);
        listener.workingRecords.put(OTHER, otherEntry);
    }

    @After
    public void tearDown() {
        cacheLocator.close();
        apiLocator.close();
    }

    /**
     * Given Scenario: In one bulk response the English document of {@code id-1} is rejected and
     * its Spanish document is indexed.
     * Expected Result: The entry is marked failed once and is not acknowledged.
     */
    @Test
    public void sameResponse_oneDocFails_oneSucceeds_entryMarkedFailed_notDeleted() throws Exception {
        listener.afterBulk(1L, List.of(failed(SIBLINGS + "_1_DEFAULT"),
                succeeded(SIBLINGS + "_2_DEFAULT")));

        verify(queue, never()).deleteReindexEntry(anyList());
        verify(queue, times(1)).markAsFailed(eq(siblingsEntry), anyString());
        assertNotAcknowledged(SIBLINGS);
    }

    /**
     * Given Scenario: The rejection of {@code id-1}'s working document reports first; the success
     * of its live document reports in a later response of the same batch.
     * Expected Result: The entry keeps its failure and is not acknowledged.
     */
    @Test
    public void failureReportsFirst_thenSiblingSuccess_entryNotDeleted() throws Exception {
        listener.afterBulk(1L, List.of(failed(SIBLINGS + "_1_DEFAULT")));
        listener.afterBulk(2L, List.of(succeeded(SIBLINGS + "_1_DEFAULT")));

        verify(queue, never()).deleteReindexEntry(anyList());
        assertNotAcknowledged(SIBLINGS);
    }

    /**
     * Given Scenario: The success of {@code id-1}'s live document reports first; the rejection of
     * its working document reports in a later response. This is the order the code before this
     * change lost: the success deleted the row and the failure's update then matched nothing.
     * Expected Result: Nothing is deleted while the batch runs, the entry is marked failed and is
     * not acknowledged.
     */
    @Test
    public void successReportsFirst_thenSiblingFailure_entryNotDeleted() throws Exception {
        listener.afterBulk(1L, List.of(succeeded(SIBLINGS + "_1_DEFAULT")));
        listener.afterBulk(2L, List.of(failed(SIBLINGS + "_1_DEFAULT")));

        verify(queue, never()).deleteReindexEntry(anyList());
        verify(queue, times(1)).markAsFailed(eq(siblingsEntry), anyString());
        assertNotAcknowledged(SIBLINGS);
    }

    /**
     * Given Scenario: The bulk request carrying one document of {@code id-1} fails as a whole
     * (connection reset); a later request of the same batch indexes its other document.
     * Expected Result: The entry is not acknowledged.
     */
    @Test
    public void wholeRequestFailure_thenSiblingSuccess_entryNotDeleted() throws Exception {
        listener.afterBulk(1L, new RuntimeException("connection reset"));
        listener.afterBulk(2L, List.of(succeeded(SIBLINGS + "_2_DEFAULT")));

        verify(queue, never()).deleteReindexEntry(anyList());
        assertNotAcknowledged(SIBLINGS);
    }

    /**
     * Given Scenario: A request indexes one document of {@code id-1}; a later request of the same
     * batch, carrying its other document, fails as a whole.
     * Expected Result: Nothing is deleted while the batch runs and the entry is not acknowledged.
     */
    @Test
    public void siblingSuccess_thenWholeRequestFailure_entryNotDeleted() throws Exception {
        listener.afterBulk(1L, List.of(succeeded(SIBLINGS + "_2_DEFAULT")));
        listener.afterBulk(2L, new RuntimeException("connection reset"));

        verify(queue, never()).deleteReindexEntry(anyList());
        assertNotAcknowledged(SIBLINGS);
    }

    /**
     * Given Scenario: Every document of both identifiers is indexed (control case).
     * Expected Result: The listener deletes nothing itself; both entries are returned for the
     * end-of-batch acknowledgement, exactly once each.
     */
    @Test
    public void allDocsSucceed_entryAcknowledgedOnlyAtEndOfBatch() throws Exception {
        listener.afterBulk(1L, List.of(succeeded(SIBLINGS + "_1_DEFAULT"),
                succeeded(OTHER + "_1_DEFAULT")));
        listener.afterBulk(2L, List.of(succeeded(SIBLINGS + "_2_DEFAULT")));

        verify(queue, never()).deleteReindexEntry(anyList());
        final List<ReindexEntry> acknowledged = listener.entriesToAcknowledge();
        assertEquals(2, acknowledged.size());
        assertEquals(Set.of(SIBLINGS, OTHER), identifiers(acknowledged));
    }

    /**
     * Given Scenario: Two documents of {@code id-1} are rejected in one response, and later the
     * whole request carrying a third one fails too.
     * Expected Result: The entry is marked failed exactly once in the batch. A second mark would
     * consume another retry and, when the entry parks, notify the administrators a second time.
     */
    @Test
    public void twoDocsFailSameEntry_markAsFailedCalledOnce() throws Exception {
        listener.afterBulk(1L, List.of(failed(SIBLINGS + "_1_DEFAULT"),
                failed(SIBLINGS + "_2_DEFAULT")));
        listener.afterBulk(2L, new RuntimeException("connection reset"));

        verify(queue, times(1)).markAsFailed(eq(siblingsEntry), anyString());
        verify(queue, times(1)).markAsFailed(eq(otherEntry), anyString());
    }

    /**
     * Given Scenario: Phase 2 — Elasticsearch and OpenSearch report to the same listener. The
     * Elasticsearch leg indexes {@code id-1}'s document; the OpenSearch leg rejects the same
     * document.
     * Expected Result: The entry is marked failed and not acknowledged; OpenSearch serves reads in
     * Phase 2, so its failure must be retried.
     */
    @Test
    public void sharedListener_esLegSucceeds_osLegFails_entryNotDeleted() throws Exception {
        listener.afterBulk(1L, List.of(succeeded(SIBLINGS + "_1_DEFAULT")));
        listener.afterBulk(1L, List.of(failed(SIBLINGS + "_1_DEFAULT")));

        verify(queue, never()).deleteReindexEntry(anyList());
        verify(queue, times(1)).markAsFailed(eq(siblingsEntry), anyString());
        assertNotAcknowledged(SIBLINGS);
    }

    /**
     * Given Scenario: One document of {@code id-1} was withheld at mapping time (#37269) and its
     * sibling is indexed.
     * Expected Result: The entry is neither acknowledged nor marked failed a second time.
     */
    @Test
    public void withheldIdentifier_siblingSuccess_notDeletedNotRemarked() throws Exception {
        listener.withhold(SIBLINGS);
        listener.afterBulk(1L, List.of(succeeded(SIBLINGS + "_2_DEFAULT"),
                succeeded(OTHER + "_1_DEFAULT")));

        verify(queue, never()).markAsFailed(eq(siblingsEntry), any());
        assertNotAcknowledged(SIBLINGS);
        assertTrue(identifiers(listener.entriesToAcknowledge()).contains(OTHER));
    }

    /**
     * Given Scenario: The batch returns results for {@code id-2} only; {@code id-1} produced no
     * document (skipped by the mapping guard, or its content was gone).
     * Expected Result: {@code id-1} is neither acknowledged nor marked failed, as before.
     */
    @Test
    public void entryWithNoResults_neitherAckedNorFailed() throws Exception {
        listener.afterBulk(1L, List.of(succeeded(OTHER + "_1_DEFAULT")));

        verify(queue, never()).markAsFailed(eq(siblingsEntry), any());
        assertNotAcknowledged(SIBLINGS);
    }

    /**
     * Given Scenario: A Phase 1 shadow listener (OpenSearch) sees a success and a failure.
     * Expected Result: It never touches the journal and has nothing to acknowledge (ADR-0009).
     */
    @Test
    public void shadowListener_neverTouchesJournal() throws Exception {
        final BulkProcessorListener shadow = BulkProcessorListener.forShadowProvider(IndexTag.OS);
        shadow.workingRecords.put(SIBLINGS, siblingsEntry);

        shadow.afterBulk(1L, List.of(succeeded(SIBLINGS + "_1_DEFAULT")));
        shadow.afterBulk(2L, List.of(failed(SIBLINGS + "_2_DEFAULT")));

        verify(queue, never()).deleteReindexEntry(anyList());
        verify(queue, never()).markAsFailed(any(), any());
        assertTrue(shadow.entriesToAcknowledge().isEmpty());
    }

    /**
     * Given Scenario: A response with a successful document comes back.
     * Expected Result: The query caches are cleared right away, as before, so a search issued
     * after the save does not read a stale cached result while the batch finishes.
     */
    @Test
    public void queryCacheClearedOnSuccessInAfterBulk() throws Exception {
        listener.afterBulk(1L, List.of(succeeded(OTHER + "_1_DEFAULT")));

        verify(esCache).clearCache();
    }

    private void assertNotAcknowledged(final String identifier) {
        assertTrue(identifier + " must not be acknowledged",
                !identifiers(listener.entriesToAcknowledge()).contains(identifier));
    }

    private static Set<String> identifiers(final List<ReindexEntry> entries) {
        return entries.stream().map(ReindexEntry::getIdentToIndex).collect(Collectors.toSet());
    }

    private static ReindexEntry entry(final long id, final String identifier) {
        return ReindexEntry.builder().id(id).identToIndex(identifier).priority(0).build();
    }

    private static IndexBulkItemResult succeeded(final String documentId) {
        return IndexBulkItemResult.builder().id(documentId).failed(false).build();
    }

    private static IndexBulkItemResult failed(final String documentId) {
        return IndexBulkItemResult.builder().id(documentId).failed(true)
                .failureMessage("429 circuit_breaking_exception").build();
    }
}
