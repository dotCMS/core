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
import com.dotmarketing.util.Config;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/** Runs the consumer's actual SQL and bound parameters against an isolated in-memory journal. */
public class ReindexQueueFactoryWorkerRoutingTest {

    private final List<String> members = new ArrayList<>(
            List.of("node-1", "node-2", "node-3", "node-4", "node-5", "node-6"));
    private final Map<String, Integer> settings = new HashMap<>();
    private String currentNode = "node-1";
    private Connection database;
    private ReindexQueueFactory factory;
    private MockedStatic<Config> config;
    private MockedStatic<APILocator> apis;
    private MockedStatic<DbConnectionFactory> connections;
    private MockedConstruction<DotConnect> queries;
    private Field detectorField;
    private Object originalDetector;

    /** Substitutes only DotConnect's transport; the production SQL and bindings remain unchanged. */
    @Before
    public void setUp() throws Exception {
        database = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        try (var statement = database.createStatement()) {
            statement.execute("create table dist_reindex_journal (id bigint primary key,"
                    + " ident_to_index varchar(100), priority int, dist_action int)");
        }
        config = mockStatic(Config.class);
        config.when(() -> Config.getIntProperty(anyString(), anyInt()))
                .thenAnswer(call -> settings.getOrDefault(call.getArgument(0), call.getArgument(1)));
        config.when(() -> Config.getLongProperty(anyString(), anyLong()))
                .thenAnswer(call -> call.getArgument(1));
        final ServerAPI servers = mock(ServerAPI.class);
        when(servers.getReindexingServers()).thenReturn(members);
        when(servers.readServerId()).thenAnswer(call -> currentNode);
        apis = mockStatic(APILocator.class);
        apis.when(APILocator::getServerAPI).thenReturn(servers);
        connections = mockStatic(DbConnectionFactory.class);
        queries = mockConstruction(DotConnect.class, withSettings().defaultAnswer(RETURNS_SELF),
                (query, context) -> {
                    final AtomicReference<String> sql = new AtomicReference<>();
                    final List<Object> parameters = new ArrayList<>();
                    when(query.setSQL(anyString())).thenAnswer(call -> {
                        sql.set(call.getArgument(0));
                        parameters.clear();
                        return query;
                    });
                    when(query.addParam(anyInt())).thenAnswer(call -> {
                        parameters.add(call.getArgument(0));
                        return query;
                    });
                    when(query.addParam(anyLong())).thenAnswer(call -> {
                        parameters.add(call.getArgument(0));
                        return query;
                    });
                    when(query.loadObjectResults()).thenAnswer(call -> select(sql.get(), parameters));
                });
        factory = new ReindexQueueFactory();
        resetQueue();
    }

    /** Closes the in-memory database and restores all static test state. */
    @After
    public void tearDown() throws Exception {
        if (detectorField != null) {
            detectorField.set(factory, originalDetector);
        }
        if (factory != null) {
            resetQueue();
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
        if (database != null) {
            database.close();
        }
    }

    /** Two participants must cover the whole bulk journal, not just two of six old partitions. */
    @Test
    public void testBulkPartitionCoversEveryEntryWithExactlyTwoWorkers() throws Exception {
        final Set<Long> expected = new HashSet<>();
        for (long id = 1; id <= 24; id++) {
            insert(id, id % 2 == 0 ? 200 : 300);
            expected.add(id);
        }
        final Set<Long> observed = new HashSet<>();
        int active = 0;
        for (final String node : members) {
            final Map<String, ReindexEntry> batch = fetchAt(node);
            if (!batch.isEmpty()) {
                active++;
            }
            for (final ReindexEntry entry : batch.values()) {
                assertTrue("Stable ownership must not duplicate id " + entry.getId(),
                        observed.add(entry.getId()));
            }
        }
        assertEquals(2, active);
        assertEquals(expected, observed);
        assertEquals("Fetching must not acknowledge or discard persisted entries", 24, rowCount());
    }

    /** A selected participant disappearing must leave every bulk row reachable by its replacements. */
    @Test
    public void testFailedParticipantLeavesEveryBulkRowReachable() throws Exception {
        final Set<Long> expected = new HashSet<>();
        for (long id = 1; id <= 24; id++) {
            insert(id, 200);
            expected.add(id);
        }
        assertEquals(12, fetchAt("node-1").size());
        members.remove("node-1");

        final Set<Long> observed = new HashSet<>();
        int active = 0;
        for (final String node : members) {
            final var batch = fetchAt(node);
            if (!batch.isEmpty()) {
                active++;
            }
            for (final ReindexEntry entry : batch.values()) {
                assertTrue(observed.add(entry.getId()));
            }
        }
        assertEquals(2, active);
        assertEquals(expected, observed);
        assertEquals(24, rowCount());
    }

    /** Normal updates continue to use every node rather than the restricted bulk membership. */
    @Test
    public void testNormalRowsRemainPartitionedAcrossAllSixNodes() throws Exception {
        for (long id = 1; id <= 12; id++) {
            insert(id, 100);
        }
        final Set<Long> observed = new HashSet<>();
        for (final String node : members) {
            final Map<String, ReindexEntry> batch = fetchAt(node);
            assertEquals(2, batch.size());
            for (final ReindexEntry entry : batch.values()) {
                assertTrue(observed.add(entry.getId()));
            }
        }
        assertEquals(12, observed.size());
    }

    /** A configured cutoff of 300 must leave structure rows distributed across all six nodes. */
    @Test
    public void testConfiguredCutoffControlsSqlOwnership() throws Exception {
        settings.put("REINDEX_NODE_LIMIT_MIN_PRIORITY", 300);
        for (long id = 1; id <= 24; id++) {
            insert(id, id <= 12 ? 200 : 300);
        }
        int structureWorkers = 0;
        int fullReindexWorkers = 0;
        for (final String node : members) {
            final var entries = fetchAt(node).values();
            if (entries.stream().anyMatch(entry -> entry.getPriority() == 200)) {
                structureWorkers++;
            }
            if (entries.stream().anyMatch(entry -> entry.getPriority() == 300)) {
                fullReindexWorkers++;
            }
        }
        assertEquals(6, structureWorkers);
        assertEquals(2, fullReindexWorkers);
    }

    /** A new owner must revisit lower ids that its previous ownership query could not see. */
    @Test
    public void testRuntimeOwnershipChangeResetsTheJournalCursor() throws Exception {
        currentNode = "node-3";
        insert(104, 100);
        insert(2, 200);
        assertEquals(Set.of("content-104"), factory.findContentToReindex(50).keySet());

        settings.put("MAX_REINDEXING_NODES", 3);

        assertTrue(factory.findContentToReindex(50).containsKey("content-2"));
    }

    /** Filtering stale prefetch must still let an excluded node refill with normal-priority work. */
    @Test
    public void testExcludedPrefetchDoesNotHideNormalWorkInTheDatabase() throws Exception {
        currentNode = "node-6";
        insert(5, 200);
        insert(11, 100);
        factory.getLocalQueue().add(ReindexEntry.builder().id(5).identToIndex("content-5")
                .priority(200).build());

        assertEquals(Set.of("content-11"), factory.findContentToReindex(50).keySet());
        assertEquals(2, rowCount());
    }

    /** Stalled-share takeover must never let a bulk-excluded node recover capped entries. */
    @Test
    public void testExcludedNodeCannotStealStalledBulkWork() throws Exception {
        currentNode = "node-6";
        assertTrue(factory.findContentToReindex(50).isEmpty());
        forceImmediateNormalRecovery();
        insert(1, 200);
        insert(2, 300);

        assertTrue(factory.findContentToReindex(50).isEmpty());
        assertEquals(2, rowCount());
    }

    /** Capping bulk work must not disable the same node's recovery of stalled normal updates. */
    @Test
    public void testExcludedBulkNodeCanStillRecoverStalledNormalWork() throws Exception {
        currentNode = "node-6";
        assertTrue(factory.findContentToReindex(50).isEmpty());
        forceImmediateNormalRecovery();
        insert(1, 100);

        assertEquals(Set.of("content-1"), factory.findContentToReindex(50).keySet());
    }

    /** Failed entries outside the existing eligibility ceiling must remain excluded. */
    @Test
    public void testLimitDoesNotReviveIneligibleFailedEntries() throws Exception {
        insert(1, 401);
        insert(2, 500);
        for (final String node : members) {
            assertTrue(fetchAt(node).isEmpty());
        }
        assertEquals(2, rowCount());
    }

    /** Selected workers can recover a stalled bulk share without stealing normal-priority rows. */
    @Test
    public void testSelectedWorkerCanRecoverOnlyItsCappedPriorityClass() throws Exception {
        assertTrue(factory.findContentToReindex(50).isEmpty());
        forceImmediateRecovery("bulkStarvedSliceDetector");
        insert(1, 200);
        insert(3, 100);

        assertEquals(Set.of("content-1"), factory.findContentToReindex(50).keySet());
        assertEquals(2, rowCount());
    }

    /** Raising the cutoff must rescan older structure rows newly owned by a normal-work node. */
    @Test
    public void testRuntimeCutoffChangeResetsTheJournalCursor() throws Exception {
        currentNode = "node-6";
        insert(11, 100);
        insert(5, 200);
        assertEquals(Set.of("content-11"), factory.findContentToReindex(50).keySet());

        settings.put("REINDEX_NODE_LIMIT_MIN_PRIORITY", 300);

        assertTrue(factory.findContentToReindex(50).containsKey("content-5"));
    }

    /** A prefetch-sized bulk backlog must not hide the excluded node's ordinary save work. */
    @Test
    public void testLargeExcludedPrefetchDoesNotStarveNormalWork() throws Exception {
        currentNode = "node-6";
        for (int i = 0; i < 2001; i++) {
            final long id = 5L + i * 6;
            factory.getLocalQueue().add(ReindexEntry.builder().id(id)
                    .identToIndex("old-bulk-" + id).priority(200).build());
        }
        insert(15005, 100);

        assertEquals(Set.of("content-15005"), factory.findContentToReindex(50).keySet());
    }

    /** Makes normal-share recovery immediate without waiting for wall-clock time. */
    private void forceImmediateNormalRecovery() throws Exception {
        forceImmediateRecovery("starvedSliceDetector");
    }

    /** Injects an expired recovery threshold without widening production visibility. */
    private void forceImmediateRecovery(final String fieldName) throws Exception {
        detectorField = ReindexQueueFactory.class.getDeclaredField(fieldName);
        detectorField.setAccessible(true);
        originalDetector = detectorField.get(factory);
        detectorField.set(factory, new StarvedSliceDetector(Duration.ofSeconds(-1)));
    }

    /** Fetches with fresh local cursor state to model a different JVM in the same membership. */
    private Map<String, ReindexEntry> fetchAt(final String node) throws Exception {
        currentNode = node;
        resetQueue();
        return factory.findContentToReindex(50);
    }

    /** Clears only in-memory consumer state, never persisted journal rows. */
    private void resetQueue() {
        factory.getLocalQueue().clear();
        ReindexQueueFactory.resetLastIdReindexed();
    }

    /** Inserts a uniquely identified fixture into the isolated journal. */
    private void insert(final long id, final int priority) throws SQLException {
        try (final PreparedStatement statement = database.prepareStatement(
                "insert into dist_reindex_journal values (?, ?, ?, ?)")) {
            statement.setLong(1, id);
            statement.setString(2, "content-" + id);
            statement.setInt(3, priority);
            statement.setInt(4, ReindexQueueFactory.ReindexAction.REINDEX.ordinal());
            statement.executeUpdate();
        }
    }

    /** Counts durable rows independently of the mocked DotConnect transport. */
    private int rowCount() throws SQLException {
        try (var statement = database.createStatement();
                var result = statement.executeQuery("select count(*) from dist_reindex_journal")) {
            result.next();
            return result.getInt(1);
        }
    }

    /** Executes the production query and bindings, mapping JDBC columns as DotConnect does. */
    private List<Map<String, Object>> select(final String sql, final List<Object> parameters)
            throws SQLException {
        assertTrue("Admission must only read the journal", sql.startsWith("select"));
        try (final PreparedStatement statement = database.prepareStatement(sql)) {
            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }
            try (final ResultSet result = statement.executeQuery()) {
                final List<Map<String, Object>> rows = new ArrayList<>();
                while (result.next()) {
                    final Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= result.getMetaData().getColumnCount(); i++) {
                        row.put(result.getMetaData().getColumnLabel(i).toLowerCase(Locale.ROOT),
                                result.getObject(i));
                    }
                    rows.add(row);
                }
                return rows;
            }
        }
    }
}
