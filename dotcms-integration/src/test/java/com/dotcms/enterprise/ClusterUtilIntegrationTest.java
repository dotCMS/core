package com.dotcms.enterprise;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.content.index.opensearch.OSClientProvider;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotcms.cluster.business.ServerAPI;
import com.dotmarketing.util.Config;
import io.vavr.control.Try;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the search part of {@link ClusterUtil#getNodeInfo()}, the status report
 * every server sends to the Network screens (issue #37876).
 *
 * <p>The report must ask the engine that serves reads in the current migration phase, through
 * {@code IndexAPI}, instead of always asking Elasticsearch. When that engine can't be reached, only
 * the search keys are emptied and the rest of the report is still filled.</p>
 *
 * <p>The phase is saved before each test and put back after it, never cleared: the weekly
 * OpenSearch phase sweep runs this suite under a phase of its own, and later tests rely on it.</p>
 *
 * <p>The report reads this server's own {@code cluster_server} row, which a running dotCMS creates
 * at startup but the integration environment doesn't. The class creates it the same way
 * ({@link ServerAPI#getOrCreateMyServer()}) and removes it afterwards if it did.</p>
 *
 * @author hassandotcms
 */
public class ClusterUtilIntegrationTest {

    /** The search keys the report has always written; the old Network tab reads them by name. */
    private static final List<String> SEARCH_KEYS = List.of(
            "esClusterName", "esTimedout", "esNumberOfNodes", "esNumberOfDataNodes",
            "esActivePrimaryShards", "esActiveShards", "esRelocatingShards", "esInitializingShards",
            "esUnassignedShards", "esDelayedUnasignedShards", "esNumberOfPendingTasks",
            "esNumberOfInFlightFetch", "esTaskMaxWaitingInQueueMillis", "esActiveShardsPercentAsNumber");

    private static boolean createdMyServer;

    private String originalPhase;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        final ServerAPI serverAPI = APILocator.getServerAPI();
        createdMyServer = serverAPI.getServer(serverAPI.readServerId()) == null;
        serverAPI.getOrCreateMyServer();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (createdMyServer) {
            final ServerAPI serverAPI = APILocator.getServerAPI();
            serverAPI.removeServerFromClusterTable(serverAPI.readServerId());
        }
    }

    @Before
    public void savePhase() {
        originalPhase = Config.getStringProperty(FLAG_KEY, null);
    }

    @After
    public void restorePhase() {
        Config.setProperty(FLAG_KEY, originalPhase);
    }

    private static void setPhase(final MigrationPhase phase) {
        Config.setProperty(FLAG_KEY, String.valueOf(phase.ordinal()));
    }

    private static boolean openSearchAnswers() {
        return Try.of(() -> CDIUtils.getBeanThrows(OSClientProvider.class).getClient().info() != null)
                .getOrElse(false);
    }

    /**
     * Given : phase 0, where Elasticsearch serves reads.
     * When  : the status report is built.
     * Then  : the search values come from Elasticsearch, esEngine names it, the queue wait is
     *         also given as a number of milliseconds, and every search key the old tab reads keeps
     *         its name and form (the queue wait stays a time string such as "0s").
     */
    @Test
    public void phase0_reportsElasticsearchAndKeepsTheSearchKeys() {
        setPhase(MigrationPhase.PHASE_0_MIGRATION_NOT_STARTED);

        final Map<String, Serializable> report = ClusterUtil.getNodeInfo();

        assertEquals("ELASTICSEARCH", report.get("esEngine"));
        assertTrue("the queue wait must also be sent as a number",
                report.get("esTaskMaxWaitingTimeMillis") instanceof Long);
        final Serializable legacyWait = report.get("esTaskMaxWaitingInQueueMillis");
        assertTrue("the old queue-wait key must stay a time string, got " + legacyWait,
                legacyWait instanceof String && ((String) legacyWait).matches("\\d+(\\.\\d+)?[a-z]+"));
        SEARCH_KEYS.forEach(key -> assertTrue("missing search key " + key, report.containsKey(key)));
        assertFalse("".equals(report.get("esClusterName")));
    }

    /**
     * Given : phase 3, where OpenSearch alone serves reads, and no OpenSearch to answer.
     * When  : the status report is built.
     * Then  : it doesn't fail: only the search keys are empty, esStatus is red, and the cache,
     *         assets and identity parts are still filled.
     */
    @Test
    public void phase3_openSearchUnreachable_emptiesOnlyTheSearchKeys() {
        Assume.assumeFalse("OpenSearch is reachable (phase sweep); the failure path can't be shown",
                openSearchAnswers());
        setPhase(MigrationPhase.PHASE_3_OPENSEARCH_ONLY);

        final Map<String, Serializable> report = ClusterUtil.getNodeInfo();

        SEARCH_KEYS.forEach(key -> assertEquals("search key " + key + " must be empty", "", report.get(key)));
        assertEquals("", report.get("esEngine"));
        assertEquals("", report.get("esTaskMaxWaitingTimeMillis"));
        assertEquals("red", report.get("esStatus"));
        assertFalse("".equals(report.get("cacheTransportClass")));
        assertFalse("".equals(report.get("assetsPath")));
        assertFalse("".equals(report.get("serverId")));
    }
}
