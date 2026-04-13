package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.exception.DotDataException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for {@link Task260407AddBaseTypeColumnToIdentifier}.
 * Verifies that the {@code base_type} column and {@code idx_identifier_base_type} index are
 * correctly created on the {@code identifier} table, and that the task is idempotent.
 */
public class Task260407AddBaseTypeColumnToIdentifierTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        cleanUp();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Always restore — executeUpgrade() uses IF NOT EXISTS so it's safe to call unconditionally
        new Task260407AddBaseTypeColumnToIdentifier().executeUpgrade();
    }

    /**
     * Method to test: {@link Task260407AddBaseTypeColumnToIdentifier#executeUpgrade()}
     * When: Column and index do not exist
     * Should: Add the base_type column and create idx_identifier_base_type index
     */
    @Test
    public void test_executeUpgrade_addsColumnAndIndex() throws SQLException, DotDataException {
        cleanUp();

        final var metaData = new DotDatabaseMetaData();
        assertFalse("base_type column should not exist before task runs",
                metaData.hasColumn("identifier", "base_type"));
        assertFalse("index should not exist before task runs", indexExists());

        final var task = new Task260407AddBaseTypeColumnToIdentifier();
        assertTrue("forceRun() should return true when index is absent", task.forceRun());

        task.executeUpgrade();

        assertTrue("base_type column should exist after task runs",
                metaData.hasColumn("identifier", "base_type"));
        assertTrue("idx_identifier_base_type index should exist after task runs", indexExists());
    }

    /**
     * Method to test: {@link Task260407AddBaseTypeColumnToIdentifier#forceRun()}
     * When: Index exists AND all rows are already populated (migration complete)
     * Should: Return false — no work to do
     */
    @Test
    public void test_forceRun_returnsFalseWhenMigrationComplete() throws DotDataException, SQLException {
        cleanUp();

        final var task = new Task260407AddBaseTypeColumnToIdentifier();
        task.executeUpgrade();

        // Capture only the IDs that are currently NULL so we restore exactly those rows
        final List<String> nullIds = new DotConnect()
                .setSQL("SELECT id FROM identifier WHERE base_type IS NULL AND asset_subtype IS NOT NULL")
                .loadObjectResults()
                .stream()
                .map(row -> (String) row.get("id"))
                .collect(Collectors.toList());

        if (!nullIds.isEmpty()) {
            final String idList = nullIds.stream()
                    .map(id -> "'" + id + "'")
                    .collect(Collectors.joining(","));
            new DotConnect().executeStatement(
                    "UPDATE identifier SET base_type = 1 WHERE id IN (" + idList + ")");
        }

        try {
            assertFalse("forceRun() should return false when index exists and no pending rows remain",
                    task.forceRun());
        } finally {
            // Restore only the rows we modified
            if (!nullIds.isEmpty()) {
                final String idList = nullIds.stream()
                        .map(id -> "'" + id + "'")
                        .collect(Collectors.joining(","));
                new DotConnect().executeStatement(
                        "UPDATE identifier SET base_type = NULL WHERE id IN (" + idList + ")");
            }
        }
    }

    /**
     * Method to test: {@link Task260407AddBaseTypeColumnToIdentifier#forceRun()}
     * When: Index exists BUT rows still have null base_type (server restarted mid-backfill)
     * Should: Return true so the backfill job is re-scheduled
     */
    @Test
    public void test_forceRun_returnsTrueWhenBackfillIncomplete() throws DotDataException, SQLException {
        cleanUp();

        final var task = new Task260407AddBaseTypeColumnToIdentifier();
        task.executeUpgrade();

        // Simulate mid-backfill restart: index exists but some rows are still NULL
        new DotConnect().executeStatement(
                "UPDATE identifier SET base_type = NULL " +
                "WHERE asset_subtype IS NOT NULL AND id = (" +
                "  SELECT id FROM identifier WHERE asset_subtype IS NOT NULL LIMIT 1)");

        assertTrue("forceRun() should return true when index exists but backfill is incomplete",
                task.forceRun());
    }

    /**
     * Method to test: {@link Task260407AddBaseTypeColumnToIdentifier#executeUpgrade()}
     * When: Task is executed twice
     * Should: Not throw any exception (idempotent via IF NOT EXISTS)
     */
    @Test
    public void test_executeUpgrade_isIdempotent() throws DotDataException, SQLException {
        cleanUp();

        final var task = new Task260407AddBaseTypeColumnToIdentifier();
        task.executeUpgrade();
        // Second run must not throw
        task.executeUpgrade();

        assertTrue("base_type column should still exist", new DotDatabaseMetaData().hasColumn("identifier", "base_type"));
        assertTrue("index should still exist", indexExists());
    }

    /**
     * Method to test: {@link Task260407AddBaseTypeColumnToIdentifier#executeUpgrade()}
     * When: Column is added
     * Should: Column allows NULL and stores integer values
     */
    @Test
    public void test_baseTypeColumn_isNullableInt() throws DotDataException, SQLException {
        cleanUp();

        new Task260407AddBaseTypeColumnToIdentifier().executeUpgrade();

        final Connection conn = com.dotmarketing.db.DbConnectionFactory.getConnection();
        final ResultSet rs = DotDatabaseMetaData.getColumnsMetaData(conn, "identifier");

        boolean found = false;
        while (rs.next()) {
            if ("base_type".equalsIgnoreCase(rs.getString("COLUMN_NAME"))) {
                found = true;
                final String typeName = rs.getString("TYPE_NAME").toLowerCase();
                assertTrue("base_type should be an integer type",
                        typeName.contains("int") || typeName.contains("int4"));
                final int nullable = rs.getInt("NULLABLE");
                assertTrue("base_type should be nullable",
                        nullable == java.sql.DatabaseMetaData.columnNullable);
                break;
            }
        }
        assertTrue("base_type column should be found in metadata", found);
    }

    // -----------------------------------------------------------------------

    private static boolean indexExists() throws DotDataException {
        final var result = new DotConnect()
                .setSQL("SELECT 1 FROM pg_indexes WHERE tablename = 'identifier' AND indexname = 'idx_identifier_base_type'")
                .loadObjectResults();
        return !result.isEmpty();
    }

    private static void cleanUp() throws SQLException {
        try {
            new DotConnect().executeStatement(
                    "DROP INDEX IF EXISTS idx_identifier_base_type");
        } catch (SQLException e) {
            // ignore
        }
        try {
            new DotConnect().executeStatement(
                    "ALTER TABLE identifier DROP COLUMN IF EXISTS base_type");
        } catch (SQLException e) {
            // ignore
        }
    }

}
