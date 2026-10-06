package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for the failure verdict of
 * {@link ContentletIndexOperationsES#handleBulkResponse(BulkResponse)}.
 *
 * <p>Covers <a href="https://github.com/dotCMS/core/issues/37276">#37276</a> AC-003 and AC-004,
 * loss point L3.</p>
 *
 * <h2>Why this matters</h2>
 * <p>A bulk call can return normally while rejecting individual items — a saturated write queue
 * ({@code EsRejectedExecutionException}), an unavailable shard, a version conflict. Those took the
 * logged branch and the method returned as if everything had been applied, so the caller could not
 * distinguish a fully applied batch from one where every item was rejected. On the DEFER path,
 * where the refresh policy is NONE and nothing re-reads the document, the loss was invisible.</p>
 *
 * <p>These tests exercise the verdict alone. The HTTP call is not the interesting part and would
 * require a cluster; {@code handleBulkResponse} was extracted from {@code putToIndex} precisely so
 * the policy could be asserted without one.</p>
 */
public class ContentletIndexOperationsESPartialFailureTest {

    private static final String FAILURE_MESSAGE =
            "failure in bulk execution: [0]: index [working_x], id [abc_1_DEFAULT], "
                    + "message [EsRejectedExecutionException[rejected execution]]";

    private static ContentletIndexOperationsES operations() {
        return new ContentletIndexOperationsES(Mockito.mock(ESIndexAPI.class),
                Mockito.mock(MappingOperationsES.class));
    }

    private static BulkResponse responseWithFailures() {
        final BulkResponse response = Mockito.mock(BulkResponse.class);
        Mockito.when(response.hasFailures()).thenReturn(true);
        Mockito.when(response.buildFailureMessage()).thenReturn(FAILURE_MESSAGE);
        Mockito.when(response.getItems()).thenReturn(new BulkItemResponse[1]);
        return response;
    }

    /**
     * Given Scenario: A bulk call returns normally but the response reports per-item failures.
     * When : handleBulkResponse inspects it.
     * Then : the caller is told. Today the failure is logged and the method returns as success,
     *        which is loss point L3 — the caller commits a delete whose index removal never
     *        landed.
     */
    @Test
    public void test_partialFailure_isRaisedToCaller() {
        final RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> operations().handleBulkResponse(responseWithFailures()));

        assertTrue("The failure detail must survive into the exception, not only the log",
                thrown.getMessage() != null && thrown.getMessage().contains("rejected execution"));
    }

    /**
     * Given Scenario: A clean bulk response.
     * When : handleBulkResponse inspects it.
     * Then : nothing is raised. The escalation must not turn healthy writes into failures — this
     *        is the guard that keeps AC-003 from becoming a regression on the add path.
     */
    @Test
    public void test_cleanResponse_isSilent() {
        final BulkResponse response = Mockito.mock(BulkResponse.class);
        Mockito.when(response.hasFailures()).thenReturn(false);

        operations().handleBulkResponse(response);
    }

    /**
     * Given Scenario: A null response, which the original code tolerated.
     * When : handleBulkResponse inspects it.
     * Then : nothing is raised. Behaviour preserved — this is not the failure being escalated.
     */
    @Test
    public void test_nullResponse_isSilent() {
        operations().handleBulkResponse(null);
    }

    /**
     * Given Scenario: A failed bulk that carried removals.
     * When : the failure message is produced.
     * Then : it does not describe the operation as reindexing.
     *
     * <p>AC-004. The original message read {@code "Error reindexing"} for every operation type,
     * including deletes — which is why searching production logs for delete failures came back
     * empty and this defect went unnoticed. The wording is the diagnostic surface, so it is
     * asserted rather than left to review.</p>
     */
    @Test
    public void test_failureMessage_doesNotMisreportRemovalsAsReindexing() {
        final RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> operations().handleBulkResponse(responseWithFailures()));

        assertTrue("A bulk failure must not be reported as 'reindexing' — it may well be a "
                        + "removal, and that wording is why log searches missed this",
                !thrown.getMessage().toLowerCase().contains("error reindexing"));
    }
}
