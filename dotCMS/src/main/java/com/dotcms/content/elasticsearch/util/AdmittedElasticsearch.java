package com.dotcms.content.elasticsearch.util;

import com.dotcms.content.index.SearchAdmission;
import com.dotcms.content.index.SearchUnavailableException;
import java.io.IOException;
import com.dotmarketing.util.Logger;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;

/** All content search and count entry points share a single admission budget per JVM. */
public final class AdmittedElasticsearch {
    private static final SearchAdmission ADMISSION = SearchAdmission.configured("elasticsearch");

    /** Utility class. */
    private AdmittedElasticsearch() { }

    /** Runs a cache-miss search with a bounded caller deadline and transport cancellation. */
    public static SearchResponse search(final RestHighLevelClient client,
            final SearchRequest request) throws IOException {
        try {
            final SearchResponse response = ADMISSION.execute(completion -> client.searchAsync(
                    request, RequestOptions.DEFAULT,
                    ActionListener.wrap(completion::complete, completion::completeExceptionally))::cancel,
                    late -> clearScroll(client, late.getScrollId()));
            checkResponse(client, response);
            return response;
        } catch (ElasticsearchStatusException failure) {
            throwIfUnavailable(failure);
            throw failure;
        } catch (IOException failure) {
            throw unavailable(failure);
        }
    }

    /** Bounds each scroll batch independently, rather than retaining a permit between batches. */
    public static SearchResponse scroll(final RestHighLevelClient client,
            final SearchScrollRequest request) throws IOException {
        try {
            final SearchResponse response = ADMISSION.execute(completion -> client.scrollAsync(
                    request, RequestOptions.DEFAULT,
                    ActionListener.wrap(completion::complete, completion::completeExceptionally))::cancel,
                    late -> clearScroll(client, late.getScrollId()));
            checkResponse(client, response);
            return response;
        } catch (ElasticsearchStatusException failure) {
            throwIfUnavailable(failure);
            throw failure;
        } catch (IOException failure) {
            throw unavailable(failure);
        }
    }

    /** Shares admission with searches so uncached counts cannot exhaust web workers either. */
    public static CountResponse count(final RestHighLevelClient client,
            final CountRequest request) throws IOException {
        try {
            final CountResponse response = ADMISSION.execute(completion -> client.countAsync(
                    request, RequestOptions.DEFAULT,
                    ActionListener.wrap(completion::complete, completion::completeExceptionally))::cancel);
            if (response.getFailedShards() > 0) {
                throw unavailable(null);
            }
            return response;
        } catch (ElasticsearchStatusException failure) {
            throwIfUnavailable(failure);
            throw failure;
        } catch (IOException failure) {
            throw unavailable(failure);
        }
    }

    /** Best-effort async cleanup must not block a timed-out caller or require search admission. */
    public static void clearScroll(final RestHighLevelClient client, final String scrollId) {
        if (scrollId != null) {
            final ClearScrollRequest request = new ClearScrollRequest();
            request.addScrollId(scrollId);
            try {
                client.clearScrollAsync(request, RequestOptions.DEFAULT,
                        ActionListener.wrap(result -> { }, failure ->
                                Logger.debug(AdmittedElasticsearch.class,
                                        "Unable to clear search scroll context", failure)));
            } catch (RuntimeException failure) {
                Logger.debug(AdmittedElasticsearch.class,
                        "Unable to start search scroll cleanup", failure);
            }
        }
    }

    /** Partial results and server-side timeouts must not populate successful query/page caches. */
    private static void checkResponse(final RestHighLevelClient client, final SearchResponse response) {
        if (response.isTimedOut() || response.getFailedShards() > 0) {
            clearScroll(client, response.getScrollId());
            throw unavailable(null);
        }
    }

    /** Keeps ordinary query errors on their existing path, but propagates backend overload. */
    private static void throwIfUnavailable(final ElasticsearchStatusException failure) {
        if (failure.status().getStatus() == 429 || failure.status().getStatus() == 503) {
            throw unavailable(failure);
        }
    }

    /** Records a safe failure for rendering layers that historically swallow exceptions. */
    private static SearchUnavailableException unavailable(final Throwable cause) {
        return ADMISSION.unavailable(cause);
    }
}
