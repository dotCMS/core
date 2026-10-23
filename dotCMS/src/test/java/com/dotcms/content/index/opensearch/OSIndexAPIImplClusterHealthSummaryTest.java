package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.dotcms.content.index.domain.SearchClusterHealth;
import com.dotmarketing.util.Config;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.HttpHost;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.opensearch.client.opensearch.cluster.HealthRequest;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.opensearch.client.opensearch.cluster.OpenSearchClusterClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.TransportOptions;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5Options;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.opensearch.client.util.ObjectBuilder;

/**
 * Unit tests for {@link OSIndexAPIImpl#getClusterHealthSummary()} (issue #37876).
 *
 * <p>The phase 2 read only falls back to Elasticsearch on a {@link RuntimeException}, while the
 * OpenSearch client reports an unreachable cluster with a checked {@link IOException}. These tests
 * pin that the OpenSearch side turns it into an unchecked exception, that the call is limited to
 * this install's indices, and that a client-side time limit stops a silent OpenSearch from holding
 * the cluster status report past the requester's wait window.</p>
 *
 * @author hassandotcms
 */
public class OSIndexAPIImplClusterHealthSummaryTest {

    private static final String PREFIX = "cluster_test.";

    @After
    public void clearTimeLimit() {
        Config.setProperty(SearchClusterHealth.TIMEOUT_PROPERTY, null);
    }

    @SuppressWarnings("unchecked")
    private static Function<HealthRequest.Builder, ObjectBuilder<HealthRequest>> anyHealthRequest() {
        return any(Function.class);
    }

    private static OSIndexAPIImpl apiWith(final OpenSearchClient client) {
        final OSClientProvider provider = mock(OSClientProvider.class);
        when(provider.getClient()).thenReturn(client);
        final OSIndexAPIImpl api = spy(new OSIndexAPIImpl(provider));
        doReturn(PREFIX).when(api).getClusterPrefix();
        return api;
    }

    private static OpenSearchClient clientWith(final OpenSearchClusterClient cluster) {
        when(cluster.withTransportOptions(any(TransportOptions.class))).thenReturn(cluster);
        final OpenSearchClient client = mock(OpenSearchClient.class);
        when(client.cluster()).thenReturn(cluster);
        return client;
    }

    /**
     * Given : an OpenSearch cluster that refuses the connection (the client throws IOException).
     * When  : the health summary is read.
     * Then  : an unchecked exception carrying the cause is thrown, so PhaseRouter's phase 2
     *         fallback to Elasticsearch can catch it.
     */
    @Test
    public void unreachableCluster_throwsUncheckedException() throws Exception {
        final OpenSearchClusterClient cluster = mock(OpenSearchClusterClient.class);
        when(cluster.health(anyHealthRequest())).thenThrow(new ConnectException("Connection refused"));

        try {
            apiWith(clientWith(cluster)).getClusterHealthSummary();
            fail("An unreachable OpenSearch must surface as an exception, not an empty summary");
        } catch (final RuntimeException e) {
            assertTrue("The client failure must be kept as the cause",
                    e.getCause() instanceof ConnectException);
        }
    }

    /**
     * Given : a healthy OpenSearch.
     * When  : the health summary is read.
     * Then  : the result names OpenSearch, the request covers only this install's indices, and the
     *         call carries the client-side time limit (default 750 ms).
     */
    @Test
    @SuppressWarnings("unchecked")
    public void request_isLimitedToOurIndices_andCarriesTheTimeLimit() throws Exception {
        final HealthResponse response = mock(HealthResponse.class);
        when(response.clusterName()).thenReturn("test");
        when(response.status()).thenReturn(HealthStatus.Green);
        final OpenSearchClusterClient cluster = mock(OpenSearchClusterClient.class);
        final ArgumentCaptor<Function<HealthRequest.Builder, ObjectBuilder<HealthRequest>>> request =
                ArgumentCaptor.forClass(Function.class);
        when(cluster.health(request.capture())).thenReturn(response);
        final ArgumentCaptor<TransportOptions> options = ArgumentCaptor.forClass(TransportOptions.class);
        when(cluster.withTransportOptions(options.capture())).thenReturn(cluster);
        final OpenSearchClient client = mock(OpenSearchClient.class);
        when(client.cluster()).thenReturn(cluster);

        final SearchClusterHealth health = apiWith(client).getClusterHealthSummary();

        assertEquals(SearchClusterHealth.Engine.OPENSEARCH, health.engine());

        final HealthRequest sent = request.getValue().apply(new HealthRequest.Builder()).build();
        assertEquals(List.of(PREFIX + "*"), sent.index());
        final RequestConfig config = ((ApacheHttpClient5Options) options.getValue()).getRequestConfig();
        assertEquals(750L, config.getResponseTimeout().toMilliseconds());
        assertEquals(750L, config.getConnectTimeout().toMilliseconds());
        assertEquals(750L, config.getConnectionRequestTimeout().toMilliseconds());
    }

    /**
     * Given : an OpenSearch that accepts the connection but never answers, and a 300 ms limit.
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
            final OpenSearchTransport transport = ApacheHttpClient5TransportBuilder
                    .builder(new HttpHost("http", "127.0.0.1", silent.getLocalPort())).build();

            final long start = System.currentTimeMillis();
            try {
                apiWith(new OpenSearchClient(transport)).getClusterHealthSummary();
                fail("A silent OpenSearch must make the call fail");
            } catch (final RuntimeException expected) {
                final long elapsed = System.currentTimeMillis() - start;
                assertTrue("gave up after " + elapsed + " ms", elapsed < 3_000);
            } finally {
                transport.close();
                for (final Socket socket : accepted) {
                    socket.close();
                }
            }
        }
    }
}
