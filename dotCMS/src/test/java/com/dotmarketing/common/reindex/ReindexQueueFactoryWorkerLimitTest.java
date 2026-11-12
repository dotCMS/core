package com.dotmarketing.common.reindex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.dotcms.cluster.business.ServerAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * Exercises priority-aware admission through the existing journal consumer, without a database
 * or search cluster. Prefetched entries model work loaded before a membership/configuration change.
 */
public class ReindexQueueFactoryWorkerLimitTest {

    private static final List<String> SIX_NODES =
            List.of("node-1", "node-2", "node-3", "node-4", "node-5", "node-6");
    private static final String MAX_NODES = "MAX_REINDEXING_NODES";
    private static final String MIN_PRIORITY = "REINDEX_NODE_LIMIT_MIN_PRIORITY";

    private final Map<String, Integer> settings = new HashMap<>();
    private List<String> members;
    private String currentNode;
    private ReindexQueueFactory factory;
    private MockedStatic<Config> config;
    private MockedStatic<APILocator> apis;
    private MockedStatic<DbConnectionFactory> connections;
    private MockedConstruction<DotConnect> queries;

    /** Sets up a six-node membership and prevents journal refill from accessing a real database. */
    @Before
    public void setUp() throws Exception {
        members = new ArrayList<>(SIX_NODES);
        currentNode = members.getFirst();
        config = mockStatic(Config.class);
        config.when(() -> Config.getIntProperty(anyString(), anyInt()))
                .thenAnswer(call -> settings.getOrDefault(call.getArgument(0), call.getArgument(1)));
        config.when(() -> Config.getLongProperty(anyString(), anyLong()))
                .thenAnswer(call -> call.getArgument(1));

        final ServerAPI servers = mock(ServerAPI.class);
        when(servers.getReindexingServers()).thenAnswer(call -> new ArrayList<>(members));
        when(servers.readServerId()).thenAnswer(call -> currentNode);
        apis = mockStatic(APILocator.class);
        apis.when(APILocator::getServerAPI).thenReturn(servers);
        connections = mockStatic(DbConnectionFactory.class);
        queries = mockConstruction(DotConnect.class, withSettings().defaultAnswer(RETURNS_SELF),
                (query, context) -> when(query.loadObjectResults()).thenReturn(List.of()));
        resetFactory();
    }

    /** Restores static mocking and journal state so other queue tests are unaffected. */
    @After
    public void tearDown() {
        if (factory != null) {
            factory.getLocalQueue().clear();
            ReindexQueueFactory.resetLastIdReindexed();
        }
        if (queries != null) {
            queries.close();
        }
        if (connections != null) {
            connections.close();
        }
        if (apis != null) {
            apis.close();
        }
        if (config != null) {
            config.close();
        }
    }

    /** An absent maximum must protect a six-node deployment with two bulk workers by default. */
    @Test
    public void testDefaultLimitsStructureAndFullReindexToTwoNodes() throws DotDataException {
        assertEquals(2, admittedNodes(200));
        assertEquals(2, admittedNodes(201));
        assertEquals(2, admittedNodes(300));
    }

    /** Ordinary saves, including retries, must remain eligible on every serving node. */
    @Test
    public void testDefaultKeepsNormalSavesOnAllSixNodes() throws DotDataException {
        for (final int priority : List.of(0, 100, 101, 199)) {
            assertEquals("Uncapped priority " + priority, 6, admittedNodes(priority));
        }
    }

    /** The cutoff is inclusive, configurable, and distinct from the fixed full-reindex category. */
    @Test
    public void testConfiguredCutoffsUseInclusiveNumericPriority() throws DotDataException {
        for (final int cutoff : List.of(0, 100, 200, 300)) {
            settings.put(MIN_PRIORITY, cutoff);
            resetFactory();
            for (final int priority : List.of(0, 99, 100, 101, 199, 200, 201, 299, 300, 301)) {
                assertEquals("cutoff=" + cutoff + ", priority=" + priority,
                        priority >= cutoff ? 2 : 6, admittedNodes(priority));
            }
        }
    }

    /** Explicit nonpositive maximum values retain the operator's unrestricted escape hatch. */
    @Test
    public void testNonpositiveMaximumDisablesTheLimit() throws DotDataException {
        for (final int maximum : List.of(0, -1)) {
            settings.put(MAX_NODES, maximum);
            resetFactory();
            assertEquals(6, admittedNodes(200));
            assertEquals(6, admittedNodes(300));
        }
    }

    /** Positive maximum values are honored without withholding work from a smaller cluster. */
    @Test
    public void testMaximumOneThreeAndAboveMembershipSize() throws DotDataException {
        for (final int maximum : List.of(1, 3, 10)) {
            settings.put(MAX_NODES, maximum);
            resetFactory();
            assertEquals(Math.min(maximum, members.size()), admittedNodes(200));
            assertEquals(6, admittedNodes(100));
        }
    }

    /** A single-node installation must still be able to process structure and full reindexes. */
    @Test
    public void testSingleNodeRemainsEligibleWithDefaultMaximum() throws DotDataException {
        members = List.of("node-1");
        assertEquals(1, admittedNodes(200));
        assertEquals(1, admittedNodes(300));
    }

    /** A negative cutoff must fall back to 200 instead of unexpectedly capping ordinary saves. */
    @Test
    public void testNegativeCutoffFallsBackToStructurePriority() throws DotDataException {
        settings.put(MIN_PRIORITY, -1);
        resetFactory();
        assertEquals(6, admittedNodes(100));
        assertEquals(2, admittedNodes(200));
    }

    /** Capped entries at the front of prefetch must not block later ordinary saves or retries. */
    @Test
    public void testExcludedNodeSkipsPrefetchedBulkWorkButFindsNormalSaves()
            throws DotDataException {
        final Map<String, ReindexEntry> batch = batchAt("node-6", 200, 201, 300, 100, 101);
        assertEquals(Set.of("content-3", "content-4"), batch.keySet());
    }

    /** A selected node must not lose either class when normal and bulk entries are prefetched. */
    @Test
    public void testSelectedNodeCanConsumeBothPriorityClasses() throws DotDataException {
        assertEquals(5, batchAt("node-1", 0, 100, 101, 200, 300).size());
    }

    /** Queue action does not bypass admission, and an ordinary queued delete keeps its action. */
    @Test
    public void testExcludedNodeStillProcessesNormalDeletes() throws DotDataException {
        currentNode = "node-6";
        factory.getLocalQueue().add(entry(6005, "bulk-delete", 200, true));
        factory.getLocalQueue().add(entry(6011, "normal-delete", 100, true));

        final Map<String, ReindexEntry> batch = factory.findContentToReindex(10);

        assertEquals(Set.of("normal-delete"), batch.keySet());
        assertTrue(batch.get("normal-delete").isDelete());
    }

    /** After the oldest participant disappears, the next eligible node can take its place. */
    @Test
    public void testMembershipReplacementSelectsTheNextTwoNodes() throws DotDataException {
        assertEquals(1, batchAt("node-2", 200).size());
        assertTrue(batchAt("node-3", 200).isEmpty());

        members.remove("node-1");

        assertEquals(1, batchAt("node-2", 200).size());
        assertEquals(1, batchAt("node-3", 200).size());
        assertTrue(batchAt("node-4", 200).isEmpty());
        assertEquals(5, admittedNodes(100));
    }

    /** Runtime maximum changes must affect the same consumer, including the unlimited opt-out. */
    @Test
    public void testMaximumChangesApplyWithoutRecreatingTheConsumer() throws DotDataException {
        assertTrue(batchAt("node-3", 200).isEmpty());

        settings.put(MAX_NODES, 3);
        assertEquals(1, batchAt("node-3", 200).size());

        settings.put(MAX_NODES, 1);
        assertTrue(batchAt("node-2", 200).isEmpty());

        settings.put(MAX_NODES, 0);
        assertEquals(1, batchAt("node-6", 200).size());

        settings.put(MAX_NODES, 2);
        assertTrue(batchAt("node-6", 200).isEmpty());
    }

    /** Runtime cutoff changes must reclassify work before the next batch on the same consumer. */
    @Test
    public void testCutoffChangesApplyWithoutRecreatingTheConsumer() throws DotDataException {
        assertEquals(Set.of("content-0"), batchAt("node-6", 100, 200, 300).keySet());

        settings.put(MIN_PRIORITY, 300);
        assertEquals(Set.of("content-0"), batchAt("node-6", 200, 300).keySet());

        settings.put(MIN_PRIORITY, 100);
        assertEquals(Set.of("content-0"), batchAt("node-6", 0, 100).keySet());

        settings.put(MIN_PRIORITY, 0);
        assertTrue(batchAt("node-6", 0).isEmpty());
    }

    /** Reducing the maximum must exclude bulk entries already prefetched by a former participant. */
    @Test
    public void testReducingMaximumRechecksAlreadyPrefetchedEntries() throws DotDataException {
        settings.put(MAX_NODES, 3);
        assertEquals(1, batchAt("node-3", 200).size());
        factory.getLocalQueue().add(entry(6002, "structure", 200, false));
        factory.getLocalQueue().add(entry(6008, "normal", 100, false));

        settings.put(MAX_NODES, 2);

        assertEquals(Set.of("normal"), factory.findContentToReindex(10).keySet());
    }

    /** Lowering the cutoff must also reclassify already-prefetched entries without a restart. */
    @Test
    public void testLoweringCutoffRechecksAlreadyPrefetchedEntries() throws DotDataException {
        settings.put(MIN_PRIORITY, 300);
        assertEquals(1, batchAt("node-6", 200).size());
        factory.getLocalQueue().add(entry(6005, "structure", 200, false));
        factory.getLocalQueue().add(entry(6011, "normal", 100, false));

        settings.put(MIN_PRIORITY, 200);

        assertEquals(Set.of("normal"), factory.findContentToReindex(10).keySet());
    }

    /** Creates a fresh consumer and clears static journal state between configuration scenarios. */
    private void resetFactory() {
        factory = new ReindexQueueFactory();
        factory.getLocalQueue().clear();
        ReindexQueueFactory.resetLastIdReindexed();
    }

    /** Counts which nodes accept one otherwise-prefetched entry of the given priority. */
    private int admittedNodes(final int priority) throws DotDataException {
        int admitted = 0;
        for (final String node : members) {
            if (!batchAt(node, priority).isEmpty()) {
                admitted++;
            }
        }
        return admitted;
    }

    /** Seeds work owned under the full membership so the test exercises prefetch admission. */
    private Map<String, ReindexEntry> batchAt(final String node, final int... priorities)
            throws DotDataException {
        currentNode = node;
        factory.getLocalQueue().clear();
        ReindexQueueFactory.resetLastIdReindexed();
        final int owner = members.indexOf(node);
        for (int i = 0; i < priorities.length; i++) {
            factory.getLocalQueue().add(entry(6000L + i * members.size() + owner,
                    "content-" + i, priorities[i], false));
        }
        return factory.findContentToReindex(20);
    }

    /** Builds a persisted-journal-shaped entry without requiring database fixtures. */
    private static ReindexEntry entry(final long id, final String identifier,
            final int priority, final boolean delete) {
        return ReindexEntry.builder().id(id).identToIndex(identifier).priority(priority)
                .isDelete(delete).build();
    }
}
