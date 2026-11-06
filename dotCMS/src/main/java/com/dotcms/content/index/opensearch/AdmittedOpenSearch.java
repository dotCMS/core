package com.dotcms.content.index.opensearch;

import com.dotcms.content.index.SearchAdmission;
import com.dotcms.content.index.SearchUnavailableException;
import java.io.IOException;
import com.dotmarketing.util.Logger;
import java.util.concurrent.CompletableFuture;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.CountRequest;
import org.opensearch.client.opensearch.core.CountResponse;
import org.opensearch.client.opensearch.core.ScrollRequest;
import org.opensearch.client.opensearch.core.ScrollResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.generic.Request;
import org.opensearch.client.opensearch.generic.Response;

/** OpenSearch content reads use the same bounded policy as Elasticsearch, with a separate budget. */
public final class AdmittedOpenSearch {
    private static final SearchAdmission ADMISSION = SearchAdmission.configured("opensearch");

    /** Utility class. */
    private AdmittedOpenSearch() { }

    /** Creates a lightweight async facade sharing the provider's transport and options. */
    private static OpenSearchAsyncClient async(final OpenSearchClient client) {
        return new OpenSearchAsyncClient(client._transport(), client._transportOptions());
    }

    /** Executes a typed content search without allocating a worker thread. */
    public static SearchResponse<Object> search(final OpenSearchClient client,
            final SearchRequest request) throws IOException {
        try {
            final SearchResponse<Object> response = ADMISSION.execute(completion ->
                    awaitTransport(async(client).search(request, Object.class), completion),
                    late -> clearScroll(client, late.scrollId()));
            if (response.timedOut() || response.shards().failed() > 0) {
                clearScroll(client, response.scrollId());
                throw unavailable(null);
            }
            return response;
        } catch (OpenSearchException failure) {
            throwIfUnavailable(failure);
            throw failure;
        } catch (IOException failure) {
            throw unavailable(failure);
        }
    }

    /** Bounds each scroll batch while allowing ordinary searches to run between batches. */
    public static ScrollResponse<Object> scroll(final OpenSearchClient client,
            final ScrollRequest request) throws IOException {
        try {
            final ScrollResponse<Object> response = ADMISSION.execute(completion ->
                    awaitTransport(async(client).scroll(request, Object.class), completion),
                    late -> clearScroll(client, late.scrollId()));
            if (response.timedOut() || response.shards().failed() > 0) {
                clearScroll(client, response.scrollId());
                throw unavailable(null);
            }
            return response;
        } catch (OpenSearchException failure) {
            throwIfUnavailable(failure);
            throw failure;
        } catch (IOException failure) {
            throw unavailable(failure);
        }
    }

    /** Counts share the engine's search admission budget. */
    public static CountResponse count(final OpenSearchClient client,
            final CountRequest request) throws IOException {
        try {
            final CountResponse response = ADMISSION.execute(completion ->
                    awaitTransport(async(client).count(request), completion));
            if (response.shards().failed() > 0) {
                throw unavailable(null);
            }
            return response;
        } catch (OpenSearchException failure) {
            throwIfUnavailable(failure);
            throw failure;
        } catch (IOException failure) {
            throw unavailable(failure);
        }
    }

    /** Preserves raw JSON and classifies overload before handing a response body to the caller. */
    public static Response raw(final OpenSearchClient client, final Request request) throws IOException {
        try {
            final Response response = ADMISSION.execute(completion ->
                    awaitTransport(client.generic().executeAsync(request), completion), late -> {
                        try {
                            late.close();
                        } catch (IOException failure) {
                            Logger.debug(AdmittedOpenSearch.class,
                                    "Unable to close abandoned search response", failure);
                        }
                    });
            if (response.getStatus() == 429 || response.getStatus() == 503) {
                response.close();
                throw unavailable(null);
            }
            return response;
        } catch (OpenSearchException failure) {
            throwIfUnavailable(failure);
            throw failure;
        } catch (IOException failure) {
            throw unavailable(failure);
        }
    }

    /** Cleanup is asynchronous and bypasses admission, so overload cannot leak scroll contexts. */
    public static void clearScroll(final OpenSearchClient client, final String scrollId) {
        if (scrollId != null) {
            try {
                async(client).clearScroll(request -> request.scrollId(scrollId))
                        .exceptionally(failure -> {
                            Logger.debug(AdmittedOpenSearch.class,
                                    "Unable to clear search scroll context", failure);
                            return null;
                        });
            } catch (IOException | RuntimeException failure) {
                Logger.debug(AdmittedOpenSearch.class,
                        "Unable to clear search scroll context", failure);
            }
        }
    }

    /**
     * The current OS transport does not connect CompletableFuture cancellation to HTTP cancellation.
     * Do not cancel that future: admission must remain held until the actual request completes.
     */
    private static <T> Runnable awaitTransport(final CompletableFuture<T> transport,
            final CompletableFuture<T> completion) {
        transport.whenComplete((result, failure) -> {
            if (failure == null) {
                completion.complete(result);
            } else {
                completion.completeExceptionally(failure);
            }
        });
        return () -> { };
    }

    /** Backend rejection is not a malformed query and must not become an empty cached result. */
    private static void throwIfUnavailable(final OpenSearchException failure) {
        if (failure.status() == 429 || failure.status() == 503) {
            throw unavailable(failure);
        }
    }

    /** Records a failure without leaking the query into public messages or metric tags. */
    public static SearchUnavailableException unavailable(final Throwable cause) {
        return ADMISSION.unavailable(cause);
    }
}
