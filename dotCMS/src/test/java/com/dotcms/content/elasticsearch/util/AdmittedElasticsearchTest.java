package com.dotcms.content.elasticsearch.util;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.content.elasticsearch.ESQueryCache;
import com.dotcms.content.elasticsearch.business.ContentFactoryIndexOperationsES;
import com.dotcms.content.index.SearchUnavailableException;
import com.dotcms.cost.RequestCostApi;
import com.dotmarketing.business.APILocator;
import java.util.Optional;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Cancellable;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHits;
import org.junit.After;
import org.junit.Test;
import org.mockito.MockedStatic;

/** Tests the real cache/admission boundary with controlled ES callbacks, including recovery. */
public class AdmittedElasticsearchTest {
    private final RestHighLevelClient client = mock(RestHighLevelClient.class);
    private final Cancellable cancellation = mock(Cancellable.class);

    /** Removes any sticky HTTP failure set by a test. */
    @After
    public void cleanup() {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
    }

    /** Rejected searches must propagate through the provider and never create negative cache hits. */
    @Test
    public void backendOverloadIsNotCachedAndNextSearchCanRecover() throws Exception {
        final ESQueryCache cache = mock(ESQueryCache.class);
        when(cache.get(any(SearchRequest.class))).thenReturn(Optional.empty());
        final RestHighLevelClientProvider provider = mock(RestHighLevelClientProvider.class);
        when(provider.getClient()).thenReturn(client);
        final SearchResponse successful = mock(SearchResponse.class);
        final SearchHits hits = SearchHits.empty();
        when(successful.getHits()).thenReturn(hits);
        doAnswer(invocation -> {
            final ActionListener<SearchResponse> listener = invocation.getArgument(2);
            listener.onFailure(new ElasticsearchStatusException("busy", RestStatus.TOO_MANY_REQUESTS));
            return cancellation;
        }).doAnswer(invocation -> {
            final ActionListener<SearchResponse> listener = invocation.getArgument(2);
            listener.onResponse(successful);
            return cancellation;
        }).when(client).searchAsync(any(SearchRequest.class), eq(RequestOptions.DEFAULT), any());

        try (MockedStatic<RestHighLevelClientProvider> providers = mockStatic(RestHighLevelClientProvider.class);
             MockedStatic<APILocator> apis = mockStatic(APILocator.class)) {
            providers.when(RestHighLevelClientProvider::getInstance).thenReturn(provider);
            apis.when(APILocator::getRequestCostAPI).thenReturn(mock(RequestCostApi.class));
            final ContentFactoryIndexOperationsES operations = new ContentFactoryIndexOperationsES(cache);
            assertThrows(SearchUnavailableException.class, () -> operations.search("*", 10, 0));
            verify(cache, never()).put(any(SearchRequest.class), any(SearchHits.class));
            assertTrue(operations.search("*", 10, 0).isEmpty());
            verify(cache).put(any(SearchRequest.class), same(hits));
        }
    }

    /** Counts use the same overload semantics as result-producing searches. */
    @Test
    public void backendOverloadCountIsNotCachedAsMinusOne() throws Exception {
        final ESQueryCache cache = mock(ESQueryCache.class);
        when(cache.get(any(CountRequest.class))).thenReturn(Optional.empty());
        final RestHighLevelClientProvider provider = mock(RestHighLevelClientProvider.class);
        when(provider.getClient()).thenReturn(client);
        doAnswer(invocation -> {
            final ActionListener<CountResponse> listener = invocation.getArgument(2);
            listener.onFailure(new ElasticsearchStatusException("busy", RestStatus.SERVICE_UNAVAILABLE));
            return cancellation;
        }).when(client).countAsync(any(CountRequest.class), eq(RequestOptions.DEFAULT), any());
        try (MockedStatic<RestHighLevelClientProvider> providers = mockStatic(RestHighLevelClientProvider.class);
             MockedStatic<APILocator> apis = mockStatic(APILocator.class)) {
            providers.when(RestHighLevelClientProvider::getInstance).thenReturn(provider);
            apis.when(APILocator::getRequestCostAPI).thenReturn(mock(RequestCostApi.class));
            final ContentFactoryIndexOperationsES operations = new ContentFactoryIndexOperationsES(cache);
            assertThrows(SearchUnavailableException.class,
                    () -> operations.cachedIndexCount(new CountRequest("working")));
            verify(cache, never()).put(any(CountRequest.class), anyLong());
        }
    }

    /** An existing successful cache hit must not touch admission or transport. */
    @Test
    public void cacheHitsBypassTransport() {
        final ESQueryCache cache = mock(ESQueryCache.class);
        when(cache.get(any(SearchRequest.class))).thenReturn(Optional.of(SearchHits.empty()));
        when(cache.get(any(CountRequest.class))).thenReturn(Optional.of(17L));
        final ContentFactoryIndexOperationsES operations = new ContentFactoryIndexOperationsES(cache);
        assertTrue(operations.search("*", 10, 0).isEmpty());
        assertEquals(Long.valueOf(17), operations.cachedIndexCount(new CountRequest("working")));
        verifyNoInteractions(client);
    }

    /** HTTP 200 does not make a server timeout or partial shard result safe to cache. */
    @Test
    public void serverTimeoutAndPartialShardsAreTemporaryFailures() throws Exception {
        for (boolean timedOut : new boolean[]{true, false}) {
            final SearchResponse response = mock(SearchResponse.class);
            when(response.isTimedOut()).thenReturn(timedOut);
            when(response.getFailedShards()).thenReturn(timedOut ? 0 : 1);
            doAnswer(invocation -> {
                final ActionListener<SearchResponse> listener = invocation.getArgument(2);
                listener.onResponse(response);
                return cancellation;
            }).when(client).searchAsync(any(SearchRequest.class), any(), any());
            assertThrows(SearchUnavailableException.class,
                    () -> AdmittedElasticsearch.search(client, new SearchRequest("working")));
        }
    }
}
