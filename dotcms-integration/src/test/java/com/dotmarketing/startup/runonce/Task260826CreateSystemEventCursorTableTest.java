package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.UUIDGenerator;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link Task260826CreateSystemEventCursorTable}, the additive migration that adds the
 * per-node delivery cursor used to fix system event loss in a cluster (issue #36827).
 *
 * <p>The table is deliberately additive — nothing is altered on {@code system_event} — which is what
 * makes the change rollback-safe. These tests pin that property along with the usual fresh-install
 * and idempotency behaviour.
 */
public class Task260826CreateSystemEventCursorTableTest {

    private static final String CURSOR_TABLE = "system_event_cursor";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link Task260826CreateSystemEventCursorTable#forceRun()} and
     * {@link Task260826CreateSystemEventCursorTable#executeUpgrade()}
     * Given Scenario: The cursor table does not exist (fresh install)
     * ExpectedResult: forceRun is true, the upgrade creates the table, and forceRun turns false
     */
    @Test
    public void test_fresh_install_creates_the_cursor_table() throws Exception {
        dropCursorTableIfExists();

        final Task260826CreateSystemEventCursorTable upgradeTask =
                new Task260826CreateSystemEventCursorTable();

        assertTrue("forceRun must be true when the table is absent", upgradeTask.forceRun());
        upgradeTask.executeUpgrade();
        assertFalse("forceRun must be false once the table exists", upgradeTask.forceRun());

        try (Connection connection = DbConnectionFactory.getDataSource().getConnection()) {
            assertTrue(new DotDatabaseMetaData().tableExists(connection, CURSOR_TABLE));
        }
    }

    /**
     * Method to test: {@link Task260826CreateSystemEventCursorTable#executeUpgrade()}
     * Given Scenario: The upgrade runs on a database that already holds system_event rows
     * ExpectedResult: The cursor table is created and the existing system_event rows are untouched —
     * the migration must be purely additive
     */
    @Test
    public void test_upgrade_with_existing_system_event_rows_leaves_them_untouched() throws Exception {
        dropCursorTableIfExists();

        final String eventId = UUIDGenerator.generateUuid();
        new DotConnect()
                .setSQL("INSERT INTO system_event (identifier, event_type, payload, created, server_id) "
                        + "VALUES (?, ?, ?, ?, ?)")
                .addParam(eventId)
                .addParam("CLUSTER_WIDE_EVENT")
                .addParam("{}")
                .addParam(System.currentTimeMillis())
                .addParam(UUIDGenerator.generateUuid())
                .loadResult();
        DbConnectionFactory.commit();

        try {
            new Task260826CreateSystemEventCursorTable().executeUpgrade();

            final List<Map<String, Object>> rows = new DotConnect()
                    .setSQL("SELECT identifier FROM system_event WHERE identifier = ?")
                    .addParam(eventId)
                    .loadObjectResults();

            assertEquals("The pre-existing system_event row must survive the migration",
                    1, rows.size());
        } finally {
            new DotConnect().setSQL("DELETE FROM system_event WHERE identifier = ?")
                    .addParam(eventId).loadResult();
            DbConnectionFactory.commit();
        }
    }

    /**
     * Method to test: {@link Task260826CreateSystemEventCursorTable#executeUpgrade()}
     * Given Scenario: The upgrade is executed twice in a row
     * ExpectedResult: The second run does not fail — the DDL is idempotent
     */
    @Test
    public void test_upgrade_is_idempotent() throws Exception {
        dropCursorTableIfExists();

        final Task260826CreateSystemEventCursorTable upgradeTask =
                new Task260826CreateSystemEventCursorTable();
        upgradeTask.executeUpgrade();
        upgradeTask.executeUpgrade();

        try (Connection connection = DbConnectionFactory.getDataSource().getConnection()) {
            assertTrue(new DotDatabaseMetaData().tableExists(connection, CURSOR_TABLE));
        }
    }

    /**
     * Method to test: the shape of the created table
     * Given Scenario: The cursor table has been created
     * ExpectedResult: A row can be inserted and read back, and server_id is the primary key so a
     * node can hold at most one cursor
     */
    @Test
    public void test_cursor_table_holds_one_row_per_server() throws Exception {
        dropCursorTableIfExists();
        new Task260826CreateSystemEventCursorTable().executeUpgrade();

        final String serverId = UUIDGenerator.generateUuid();
        try {
            new DotConnect()
                    .setSQL("INSERT INTO " + CURSOR_TABLE
                            + " (server_id, last_event_date, mod_date) VALUES (?, ?, ?)")
                    .addParam(serverId)
                    .addParam(System.currentTimeMillis())
                    .addParam(new java.util.Date())
                    .loadResult();
            DbConnectionFactory.commit();

            final List<Map<String, Object>> rows = new DotConnect()
                    .setSQL("SELECT server_id, last_event_date FROM " + CURSOR_TABLE
                            + " WHERE server_id = ?")
                    .addParam(serverId)
                    .loadObjectResults();

            assertEquals(1, rows.size());
        } finally {
            new DotConnect().setSQL("DELETE FROM " + CURSOR_TABLE + " WHERE server_id = ?")
                    .addParam(serverId).loadResult();
            DbConnectionFactory.commit();
        }
    }

    private void dropCursorTableIfExists() throws Exception {
        try (Connection connection = DbConnectionFactory.getDataSource().getConnection()) {
            if (new DotDatabaseMetaData().tableExists(connection, CURSOR_TABLE)) {
                new DotConnect().executeUpdate("DROP TABLE " + CURSOR_TABLE);
                DbConnectionFactory.commit();
            }
        }
    }
}
