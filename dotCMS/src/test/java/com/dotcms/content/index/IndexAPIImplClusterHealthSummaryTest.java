package com.dotcms.content.index;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.content.index.domain.SearchClusterHealth;
import com.dotcms.content.index.domain.SearchClusterHealth.Engine;
import com.dotcms.content.index.opensearch.OSIndexAPIImpl;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for {@link IndexAPIImpl#getClusterHealthSummary()}: the cluster status report must ask
 * the engine that serves reads in the current migration phase, through the same router every other
 * read uses (issue #37876).
 *
 * @author hassandotcms
 */
public class IndexAPIImplClusterHealthSummaryTest {

    @After
    public void clearPhase() {
        Config.setProperty(FLAG_KEY, null);
    }

    private static void setPhase(final MigrationPhase phase) {
        Config.setProperty(FLAG_KEY, String.valueOf(phase.ordinal()));
    }

    private static SearchClusterHealth health(final Engine engine) {
        return SearchClusterHealth.builder()
                .engine(engine)
                .clusterName(engine.name().toLowerCase())
                .status("green")
                .timedOut(false)
                .numberOfNodes(1)
                .numberOfDataNodes(1)
                .activePrimaryShards(2)
                .activeShards(2)
                .relocatingShards(0)
                .initializingShards(0)
                .unassignedShards(0)
                .delayedUnassignedShards(0)
                .numberOfPendingTasks(0)
                .numberOfInFlightFetch(0)
                .taskMaxWaitingInQueueMillis(0L)
                .activeShardsPercent(100.0)
                .build();
    }

    /**
     * Given : phase 1, where OpenSearch only receives dual writes.
     * When  : the health summary is read.
     * Then  : only Elasticsearch, the engine serving reads, is asked.
     */
    @Test
    public void phase1_asksOnlyElasticsearch() {
        setPhase(MigrationPhase.PHASE_1_DUAL_WRITE_ES_READS);
        final ESIndexAPI es = mock(ESIndexAPI.class);
        final OSIndexAPIImpl os = mock(OSIndexAPIImpl.class);
        when(es.getClusterHealthSummary()).thenReturn(health(Engine.ELASTICSEARCH));

        new IndexAPIImpl(es, os).getClusterHealthSummary();

        verify(es).getClusterHealthSummary();
        verifyNoInteractions(os);
    }

    /**
     * Given : phase 2, where Elasticsearch only receives dual writes.
     * When  : the health summary is read and OpenSearch answers.
     * Then  : only OpenSearch, the engine serving reads, is asked.
     */
    @Test
    public void phase2_asksOnlyOpenSearch() {
        setPhase(MigrationPhase.PHASE_2_DUAL_WRITE_OS_READS);
        final ESIndexAPI es = mock(ESIndexAPI.class);
        final OSIndexAPIImpl os = mock(OSIndexAPIImpl.class);
        when(os.getClusterHealthSummary()).thenReturn(health(Engine.OPENSEARCH));

        new IndexAPIImpl(es, os).getClusterHealthSummary();

        verify(os).getClusterHealthSummary();
        verifyNoInteractions(es);
    }

    /**
     * Given : phase 2 and an OpenSearch that can't be reached.
     * When  : the health summary is read.
     * Then  : the router falls back to Elasticsearch, and the result names Elasticsearch as the
     *         engine that answered.
     */
    @Test
    public void phase2_openSearchFails_fallsBackToElasticsearch() {
        setPhase(MigrationPhase.PHASE_2_DUAL_WRITE_OS_READS);
        final ESIndexAPI es = mock(ESIndexAPI.class);
        final OSIndexAPIImpl os = mock(OSIndexAPIImpl.class);
        when(os.getClusterHealthSummary())
                .thenThrow(new DotRuntimeException("Could not read OpenSearch cluster health: Connection refused"));
        when(es.getClusterHealthSummary()).thenReturn(health(Engine.ELASTICSEARCH));

        final SearchClusterHealth result = new IndexAPIImpl(es, os).getClusterHealthSummary();

        assertEquals(Engine.ELASTICSEARCH, result.engine());
        verify(os).getClusterHealthSummary();
        verify(es).getClusterHealthSummary();
    }

    /**
     * Given : phase 3 (no Elasticsearch configured) and an OpenSearch that can't be reached.
     * When  : the health summary is read.
     * Then  : the failure propagates to the caller, and Elasticsearch is never asked, so an
     *         OpenSearch-only install never reaches for the default localhost:9200.
     */
    @Test
    public void phase3_openSearchFails_propagatesWithoutTouchingElasticsearch() {
        setPhase(MigrationPhase.PHASE_3_OPENSEARCH_ONLY);
        final ESIndexAPI es = mock(ESIndexAPI.class);
        final OSIndexAPIImpl os = mock(OSIndexAPIImpl.class);
        when(os.getClusterHealthSummary())
                .thenThrow(new DotRuntimeException("Could not read OpenSearch cluster health: Connection refused"));

        try {
            new IndexAPIImpl(es, os).getClusterHealthSummary();
            fail("A phase 3 OpenSearch failure must reach the caller");
        } catch (final DotRuntimeException expected) {
            // expected
        }
        verifyNoInteractions(es);
    }
}
