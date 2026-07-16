package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class for {@link Task251103AddStylePropertiesColumnInMultiTree}. Verifies that the
 * style_properties column is correctly added to the multi_tree table.
 */
public class Task251103AddStylePropertiesColumnInMultiTreeTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link Task251103AddStylePropertiesColumnInMultiTree#executeUpgrade()} and
     * When: Run the {@link Task251103AddStylePropertiesColumnInMultiTree#executeUpgrade()}
     * Should: Create a new style_properties field in the multi_tree table with correct data type
     *
     * @throws SQLException     thrown if an error occurs while executing the SQL statement
     * @throws DotDataException thrown if an error occurs while executing the task
     */
    @Test
    public void test_createStylePropertiesColumn_success() throws SQLException, DotDataException {
        // Clean up any previous test runs
        cleanUp();

        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();

        // Verify column doesn't exist before running task
        assertFalse("style_properties column should not exist before task runs",
                databaseMetaData.hasColumn("multi_tree", "style_properties"));

        // Create task and verify forceRun returns true (needs to run)
        final Task251103AddStylePropertiesColumnInMultiTree task =
                new Task251103AddStylePropertiesColumnInMultiTree();
        assertTrue("forceRun should return true before executing", task.forceRun());

        // Execute the upgrade
        task.executeUpgrade();

        // Verify column now exists
        assertTrue("style_properties column should exist after task runs",
                databaseMetaData.hasColumn("multi_tree", "style_properties"));

        // Verify correct data type
        verifyColumnDataType();
    }

    /**
     * Method to test: {@link Task251103AddStylePropertiesColumnInMultiTree#executeUpgrade()}
     * When: Run the {@link Task251103AddStylePropertiesColumnInMultiTree#executeUpgrade()} twice
     * Should: Not throw any exception (idempotency test)
     *
     * @throws SQLException     thrown if an error occurs while executing the SQL statement
     * @throws DotDataException thrown if an error occurs while executing the task
     */
    @Test
    public void test_runTwice_shouldBeIdempotent() throws SQLException, DotDataException {
        cleanUp();

        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();
        final Task251103AddStylePropertiesColumnInMultiTree task =
                new Task251103AddStylePropertiesColumnInMultiTree();

        // Verify initial state
        assertFalse(databaseMetaData.hasColumn("multi_tree", "style_properties"));
        assertTrue(task.forceRun());

        // Run twice - should not throw exception
        task.executeUpgrade();
        task.executeUpgrade();

        // Verify column exists and forceRun returns false
        assertTrue("style_properties column should exist after task runs",
                databaseMetaData.hasColumn("multi_tree", "style_properties"));
    }

    /**
     * Method to test: {@link Task251103AddStylePropertiesColumnInMultiTree#executeUpgrade()}
     * When: The style_properties column is added
     * Should: Allow NULL values and accept valid JSON data
     *
     * @throws SQLException     thrown if an error occurs while executing the SQL statement
     * @throws DotDataException thrown if an error occurs while executing the task
     */
    @Test
    public void test_stylePropertiesColumn_acceptsJsonData() throws SQLException, DotDataException {
        cleanUp();

        final Task251103AddStylePropertiesColumnInMultiTree task =
                new Task251103AddStylePropertiesColumnInMultiTree();
        task.executeUpgrade();

        final DotConnect dotConnect = new DotConnect();

        // Insert test data with NULL style_properties
        dotConnect.executeStatement(
                "INSERT INTO multi_tree (parent1, parent2, child, relation_type, personalization, variant_id, tree_order, style_properties) "
                        +
                        "VALUES('test-page-1', 'test-container-1', 'test-content-1', 'test-relation', 'dot:default', 'DEFAULT', 1, NULL)");

        // Verify NULL is accepted
        Object result = dotConnect.setSQL(
                        "SELECT style_properties FROM multi_tree WHERE parent1 = 'test-page-1'")
                .loadObjectResults()
                .get(0)
                .get("style_properties");

        assertNull("style_properties should accept NULL", result);

        // Update with JSON data (database-specific syntax)
        final String style_properties = "'{\"backgroundColor\": \"blue\", \"fontSize\": \"16px\"}'";

        dotConnect.executeStatement(
                "UPDATE multi_tree SET style_properties =" + style_properties
                        + "::jsonb WHERE parent1 = 'test-page-1'");

        // Clean up test data
        dotConnect.executeStatement("DELETE FROM multi_tree WHERE parent1 = 'test-page-1'");
    }

    /**
     * Removes the style_properties column if it exists to ensure clean test state
     */
    private void cleanUp() throws SQLException {
        try {
            new DotConnect().executeStatement(
                    "ALTER TABLE multi_tree DROP COLUMN style_properties");
        } catch (SQLException e) {
            // Ignore if column doesn't exist
        }
    }

    /**
     * Verifies the style_properties column has the correct data type for each database
     */
    private void verifyColumnDataType() throws SQLException {
        final Connection connection = DbConnectionFactory.getConnection();
        final ResultSet columnsMetaData = DotDatabaseMetaData
                .getColumnsMetaData(connection, "multi_tree");

        boolean stylePropertiesFound = false;

        while (columnsMetaData.next()) {
            final String columnName = columnsMetaData.getString("COLUMN_NAME");

            if ("style_properties".equalsIgnoreCase(columnName)) {
                stylePropertiesFound = true;
                final String columnType = columnsMetaData.getString("TYPE_NAME").toLowerCase();

                assertTrue("PostgreSQL should use jsonb type", columnType.contains("jsonb"));

                // Verify column is nullable
                final int nullable = columnsMetaData.getInt("NULLABLE");
                assertEquals("style_properties should be nullable",
                        java.sql.DatabaseMetaData.columnNullable, nullable);

                break;
            }
        }

        assertTrue("style_properties column should exist", stylePropertiesFound);
    }
}