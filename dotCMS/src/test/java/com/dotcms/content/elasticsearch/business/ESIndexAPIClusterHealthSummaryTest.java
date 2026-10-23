package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.content.index.domain.SearchClusterHealth;
import com.dotmarketing.util.Config;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.ClusterClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.unit.TimeValue;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link ESIndexAPI#getClusterHealthSummary()} (issue #37876): the health call is
 * limited to this install's indices, and a client-side time limit stops a slow Elasticsearch from
 * holding the cluster status report past the requester's wait window.
 *
 * @author hassandotcms
 */
public class ESIndexAPIClusterHealthSummaryTest {

    private static final String PREFIX = "cluster_test.";

    @After
    public void clearTimeLimit() {
        Config.setProperty(SearchClusterHealth.TIMEOUT_PROPERTY, null);
    }

    private static ESIndexAPI apiWithPrefix() {
        // the real constructor needs the CDI container; the health call uses none of its fields
        final ESIndexAPI api = mock(ESIndexAPI.class, CALLS_REAL_METHODS);
        doReturn(PREFIX).when(api).getClusterPrefix();
        return api;
    }

    private static RestHighLevelClientProvider providerFor(final RestHighLevelClient client) {
        final RestHighLevelClientProvider provider = mock(RestHighLevelClientProvider.class);
        when(provider.getClient()).thenReturn(client);
        return provider;
    }

    /**
     * Given : a healthy Elasticsearch.
     * When  : the health summary is read.
     * Then  : the result names Elasticsearch, the request covers only this install's indices, and
     *         it carries the client-side time limit (default 750 ms) for connecting and for the answer.
     */
    @Test
    public void request_isLimitedToOurIndices_andCarriesTheTimeLimit() throws Exception {
        final ClusterHealthResponse response = mock(ClusterHealthResponse.class);
        when(response.getClusterName()).thenReturn("test");
        when(response.getStatus()).thenReturn(ClusterHealthStatus.GREEN);
        when(response.getTaskMaxWaitingTime()).thenReturn(TimeValue.ZERO);
        final ClusterClient cluster = mock(ClusterClient.class);
        final ArgumentCaptor<ClusterHealthRequest> request = ArgumentCaptor.forClass(ClusterHealthRequest.class);
        final ArgumentCaptor<RequestOptions> options = ArgumentCaptor.forClass(RequestOptions.class);
        when(cluster.health(request.capture(), options.capture())).thenReturn(response);
        final RestHighLevelClient client = mock(RestHighLevelClient.class);
        when(client.cluster()).thenReturn(cluster);

        final SearchClusterHealth health;
        try (MockedStatic<RestHighLevelClientProvider> provider = mockStatic(RestHighLevelClientProvider.class)) {
            final RestHighLevelClientProvider instance = providerFor(client);
            provider.when(RestHighLevelClientProvider::getInstance).thenReturn(instance);

            health = apiWithPrefix().getClusterHealthSummary();
        }

        assertEquals(SearchClusterHealth.Engine.ELASTICSEARCH, health.engine());
        assertArrayEquals(new String[]{PREFIX + "*"}, request.getValue().indices());
        assertEquals(750, options.getValue().getRequestConfig().getSocketTimeout());
        assertEquals(750, options.getValue().getRequestConfig().getConnectTimeout());
        assertEquals(750, options.getValue().getRequestConfig().getConnectionRequestTimeout());
    }

    /**
     * Given : an Elasticsearch that accepts the connection but never answers, and a 300 ms limit.
     * When  : the health summary is read.
     * Then  : the call gives up at the limit with an unchecked exception, instead of waiting for
     *         the client's 30 s socket timeout.
     */
    @Test
    public void neverAnsweringEngine_givesUpAtTheTimeLimit() throws Exception {
        Config.setProperty(SearchClusterHealth.TIMEOUT_PROPERTY, 300);
        final List<Socket> accepted = new CopyOnWriteArrayList<>();
        try (ServerSocket silent = new ServerSocket(0)) {
            final Thread acceptor = new Thread(() -> {
                try {
                    while (!silent.isClosed()) {
                        accepted.add(silent.accept());
                    }
                } catch (final Exception ignored) {
                    // the socket is closed when the test ends
                }
            });
            acceptor.setDaemon(true);
            acceptor.start();
            final RestHighLevelClient client = new RestHighLevelClient(
                    RestClient.builder(new HttpHost("127.0.0.1", silent.getLocalPort(), "http")));

            final long start = System.currentTimeMillis();
            try (MockedStatic<RestHighLevelClientProvider> provider = mockStatic(RestHighLevelClientProvider.class)) {
                final RestHighLevelClientProvider instance = providerFor(client);
                provider.when(RestHighLevelClientProvider::getInstance).thenReturn(instance);

                apiWithPrefix().getClusterHealthSummary();
                fail("A silent Elasticsearch must make the call fail");
            } catch (final RuntimeException expected) {
                final long elapsed = System.currentTimeMillis() - start;
                assertTrue("gave up after " + elapsed + " ms", elapsed < 3_000);
            } finally {
                client.close();
                for (final Socket socket : accepted) {
                    socket.close();
                }
            }
        }
    }

    /**
     * Given : a time limit of zero, which the HTTP clients would read as "no limit".
     * When  : the time limit is read.
     * Then  : the default of 750 ms is used instead.
     */
    @Test
    public void nonPositiveTimeLimit_usesTheDefault() {
        Config.setProperty(SearchClusterHealth.TIMEOUT_PROPERTY, 0);

        assertEquals(SearchClusterHealth.DEFAULT_TIMEOUT_MILLIS, SearchClusterHealth.timeoutMillis());
    }
}
