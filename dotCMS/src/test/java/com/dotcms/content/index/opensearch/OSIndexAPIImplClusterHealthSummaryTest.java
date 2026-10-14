package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.ConnectException;
import java.util.function.Function;
import org.junit.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.cluster.HealthRequest;
import org.opensearch.client.opensearch.cluster.OpenSearchClusterClient;
import org.opensearch.client.util.ObjectBuilder;

/**
 * Unit tests for {@link OSIndexAPIImpl#getClusterHealthSummary()} (issue #37876).
 *
 * <p>The phase 2 read only falls back to Elasticsearch on a {@link RuntimeException}, while the
 * OpenSearch client reports an unreachable cluster with a checked {@link IOException}. This test
 * pins that the OpenSearch side turns it into an unchecked exception.</p>
 *
 * @author hassandotcms
 */
public class OSIndexAPIImplClusterHealthSummaryTest {

    @SuppressWarnings("unchecked")
    private static Function<HealthRequest.Builder, ObjectBuilder<HealthRequest>> anyHealthRequest() {
        return any(Function.class);
    }

    private static OSIndexAPIImpl apiWith(final OpenSearchClusterClient cluster) {
        final OpenSearchClient client = mock(OpenSearchClient.class);
        when(client.cluster()).thenReturn(cluster);
        final OSClientProvider provider = mock(OSClientProvider.class);
        when(provider.getClient()).thenReturn(client);
        return new OSIndexAPIImpl(provider);
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
            apiWith(cluster).getClusterHealthSummary();
            fail("An unreachable OpenSearch must surface as an exception, not an empty summary");
        } catch (final RuntimeException e) {
            assertTrue("The client failure must be kept as the cause",
                    e.getCause() instanceof ConnectException);
        }
    }
}
