package com.dotcms.content.index.opensearch;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.dotcms.content.index.SearchUnavailableException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.CountRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.transport.OpenSearchTransport;

/** Exercises OS's async transport facade rather than mocking the synchronous search method. */
public class AdmittedOpenSearchTest {

    /** OS backend rejection must be raised as temporary unavailability, never an empty hit set. */
    @Test
    public void backendRejectionPropagatesThroughAsyncWrapper() {
        final OpenSearchTransport transport = mock(OpenSearchTransport.class);
        final OpenSearchException rejection = new OpenSearchException(new org.opensearch.client.opensearch._types.ErrorResponse.Builder()
                .status(429).error(error -> error.type("rejected_execution_exception").reason("busy")).build());
        doReturn(CompletableFuture.failedFuture(rejection)).when(transport)
                .performRequestAsync(any(), any(), any());
        final OpenSearchClient client = new OpenSearchClient(transport);
        assertThrows(SearchUnavailableException.class,
                () -> AdmittedOpenSearch.search(client, new SearchRequest.Builder().index("working").build()));
        assertThrows(SearchUnavailableException.class,
                () -> AdmittedOpenSearch.count(client, new CountRequest.Builder().index("working").build()));
    }

    /** A successful response is returned unchanged; HTTP-200 server timeouts must be rejected. */
    @Test
    public void serverTimeoutIsNotSuccessfulSearch() throws Exception {
        final OpenSearchTransport transport = mock(OpenSearchTransport.class);
        final OpenSearchClient client = new OpenSearchClient(transport);
        for (boolean timedOut : new boolean[]{false, true}) {
            final SearchResponse<Object> response = new SearchResponse.Builder<>()
                    .took(1).timedOut(timedOut)
                    .shards(shards -> shards.total(1).successful(1).failed(0))
                    .hits(hits -> hits.hits(List.of())).build();
            doReturn(CompletableFuture.completedFuture(response)).when(transport)
                    .performRequestAsync(any(), any(), any());
            final SearchRequest request = new SearchRequest.Builder().index("working").build();
            if (timedOut) {
                assertThrows(SearchUnavailableException.class, () -> AdmittedOpenSearch.search(client, request));
            } else {
                assertSame(response, AdmittedOpenSearch.search(client, request));
            }
        }
    }

    /** Raw searches use the async transport too; overload responses are closed before propagating. */
    @Test
    public void rawBackendOverloadClosesResponseAndDoesNotBecomeQueryError() {
        final OpenSearchTransport transport = mock(OpenSearchTransport.class);
        final OpenSearchClient client = new OpenSearchClient(transport);
        final org.opensearch.client.opensearch.generic.Response response =
                mock(org.opensearch.client.opensearch.generic.Response.class);
        when(response.getStatus()).thenReturn(503);
        doReturn(CompletableFuture.completedFuture(response)).when(transport)
                .performRequestAsync(any(), any(), any());
        assertThrows(SearchUnavailableException.class, () -> AdmittedOpenSearch.raw(client,
                org.opensearch.client.opensearch.generic.Requests.builder()
                        .method("POST").endpoint("/working/_search").json("{}").build()));
        try {
            verify(response).close();
        } catch (java.io.IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    /** Connection-pool gauges must follow client rebuilds instead of retaining the old pool. */
    @Test
    public void poolMetricsFollowRebuiltTransport() throws Exception {
        final io.micrometer.core.instrument.simple.SimpleMeterRegistry registry =
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        io.micrometer.core.instrument.Metrics.addRegistry(registry);
        final ConfigurableOpenSearchProvider provider = new ConfigurableOpenSearchProvider(
                OSClientConfig.builder().endpoints(List.of("http://localhost:9200"))
                        .maxConnections(4).maxConnectionsPerRoute(2).build());
        try {
            assertEquals(4, registry.get("dotcms.search.connections.max").gauge().value(), 0);
            assertEquals(0, registry.get("dotcms.search.connections.leased").gauge().value(), 0);
            assertEquals(0, registry.get("dotcms.search.connections.pending").gauge().value(), 0);
            provider.rebuildClient(OSClientConfig.builder().endpoints(List.of("http://localhost:9200"))
                    .maxConnections(6).maxConnectionsPerRoute(2).build());
            assertEquals(6, registry.get("dotcms.search.connections.max").gauge().value(), 0);
        } finally {
            provider.close();
            io.micrometer.core.instrument.Metrics.removeRegistry(registry);
            registry.close();
        }
    }

    /** Finite timeouts and coherent pool limits are mandatory for configured OS clients. */
    @Test
    public void invalidTransportBoundsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> OSClientConfig.builder()
                .endpoints(List.of("http://localhost:9200")).connectionRequestTimeout(Duration.ZERO).build());
        assertThrows(IllegalArgumentException.class, () -> OSClientConfig.builder()
                .endpoints(List.of("http://localhost:9200")).socketTimeout(Duration.ofMillis(-1)).build());
        assertThrows(IllegalArgumentException.class, () -> OSClientConfig.builder()
                .endpoints(List.of("http://localhost:9200")).maxConnections(1).maxConnectionsPerRoute(2).build());
    }
}
