package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import org.mockito.Mockito;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;

/**
 * Unit tests for the failure verdict of
 * {@link ContentletIndexOperationsOS#handleBulkResponse(BulkResponse)}.
 *
 * <p>Covers <a href="https://github.com/dotCMS/core/issues/37276">#37276</a> AC-003, loss point L3
 * on the OpenSearch side.</p>
 *
 * <h2>Why OpenSearch matters here as much as Elasticsearch</h2>
 * <p>The issue and the spec name only the Elasticsearch provider, but the OpenSearch one carries
 * the identical defect: {@code response.errors()} is inspected, each failing item is logged, and
 * the method returns normally. Fixing only Elasticsearch would leave <b>phase 3</b> — where
 * OpenSearch is the sole provider and there is no shadow leg to absorb a failure — with the
 * original defect intact. That is the phase the migration is heading toward.</p>
 *
 * <p>In dual-write phases the router isolates the shadow leg
 * ({@code ContentletIndexAPIImpl#putToIndex}), so raising here does not violate ADR-0009: an
 * OpenSearch failure is still swallowed while it is the shadow, and propagates once it is
 * primary.</p>
 */
public class ContentletIndexOperationsOSPartialFailureTest {

    private static final String REJECTION_REASON =
            "rejected execution of coordinating operation, queue capacity exceeded";

    private static ContentletIndexOperationsOS operations() {
        return new ContentletIndexOperationsOS(Mockito.mock(OSClientProvider.class),
                Mockito.mock(OSIndexAPIImpl.class), Mockito.mock(MappingOperationsOS.class));
    }

    private static BulkResponse responseWithErrors() {
        final ErrorCause cause = Mockito.mock(ErrorCause.class);
        Mockito.when(cause.type()).thenReturn("es_rejected_execution_exception");
        Mockito.when(cause.reason()).thenReturn(REJECTION_REASON);

        final BulkResponseItem item = Mockito.mock(BulkResponseItem.class);
        Mockito.when(item.id()).thenReturn("abc_1_DEFAULT");
        // operationType() is left unstubbed — it only decorates the message.
        Mockito.when(item.error()).thenReturn(cause);

        final BulkResponse response = Mockito.mock(BulkResponse.class);
        Mockito.when(response.errors()).thenReturn(true);
        Mockito.when(response.items()).thenReturn(List.of(item));
        return response;
    }

    /**
     * Given Scenario: An OpenSearch bulk returns normally but reports per-item errors.
     * When : handleBulkResponse inspects it.
     * Then : the caller is told, so a lost removal can be retried instead of being assumed done.
     */
    @Test
    public void test_partialFailure_isRaisedToCaller() {
        final RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> operations().handleBulkResponse(responseWithErrors()));

        assertTrue("The rejection reason must reach the caller, not just the log",
                thrown.getMessage() != null
                        && thrown.getMessage().contains("queue capacity exceeded"));
    }

    /**
     * Given Scenario: A clean OpenSearch bulk response.
     * When : handleBulkResponse inspects it.
     * Then : nothing is raised.
     */
    @Test
    public void test_cleanResponse_isSilent() {
        final BulkResponse response = Mockito.mock(BulkResponse.class);
        Mockito.when(response.errors()).thenReturn(false);

        operations().handleBulkResponse(response);
    }

    /**
     * Given Scenario: A null response.
     * When : handleBulkResponse inspects it.
     * Then : nothing is raised. Behaviour preserved.
     */
    @Test
    public void test_nullResponse_isSilent() {
        operations().handleBulkResponse(null);
    }
}
