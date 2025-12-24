package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task251212AddVersionColumnIndicesTableTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    private void dropColumn(final DotConnect dotConnect) {
        try {
            final String dropConstraintSQL = "ALTER TABLE indicies DROP CONSTRAINT IF EXISTS uq_index_type_version";
            dotConnect.executeStatement(dropConstraintSQL);

            final String dropColumnSQL = "ALTER TABLE indicies DROP COLUMN IF EXISTS index_version";
            dotConnect.executeStatement(dropColumnSQL);
        } catch (Exception e) {
            Logger.error(Task251212AddVersionColumnIndicesTableTest.class,
                "Failed removing index_version column or constraint. Maybe it didn't exist?");
        }
    }

    private void addOldConstraintBack(final DotConnect dotConnect) {
        try {
            final String addConstraintSQL = "ALTER TABLE indicies ADD CONSTRAINT indicies_index_type_key UNIQUE (index_type)";
            dotConnect.executeStatement(addConstraintSQL);
        } catch (Exception e) {
            Logger.error(Task251212AddVersionColumnIndicesTableTest.class,
                "Failed adding old constraint back. Maybe it already exists?");
        }
    }

    private boolean columnExists() throws SQLException {
        return new DotDatabaseMetaData().hasColumn("indicies", "index_version");
    }

    /**
     * Test scenario: Database does NOT have the index_version column
     * Expected Results:
     * - forceRun() should return true (indicating the task needs to run)
     * - After executeUpgrade(), the column should exist
     * - forceRun() should then return false (indicating the task no longer needs to run)
     *
     * @throws DotDataException
     * @throws SQLException
     */
    @Test
    public void test_UpgradeTask_WhenColumnDoesNotExist_ShouldAddColumn() throws DotDataException, SQLException {
        final DotConnect dotConnect = new DotConnect();
        Logger.debug(this, "Testing task when index_version column does not exist");

        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        // Ensure column doesn't exist by dropping it
        dropColumn(dotConnect);
        addOldConstraintBack(dotConnect);

        // Verify column doesn't exist
        assertFalse("Column should not exist before test", columnExists());

        final Task251212AddVersionColumnIndicesTable task = new Task251212AddVersionColumnIndicesTable();

        // Test 1: forceRun should return true when column doesn't exist
        assertTrue("forceRun() should return true when column doesn't exist", task.forceRun());

        // Execute the upgrade
        task.executeUpgrade();

        // Test 2: Column should now exist
        assertTrue("Column should exist after upgrade", columnExists());

        // Test 3: forceRun should return false when column exists
        assertFalse("forceRun() should return false when column exists", task.forceRun());

        // Verify the constraint was also created
        dotConnect.setSQL("SELECT constraint_name FROM information_schema.table_constraints " +
                         "WHERE table_name = 'indicies' AND constraint_name = 'uq_index_type_version'");
        assertTrue("Unique constraint should exist", !dotConnect.loadObjectResults().isEmpty());
    }

    /**
     * Test scenario: Database ALREADY has the index_version column
     * Expected Results:
     * - forceRun() should return false (indicating the task doesn't need to run)
     * - executeUpgrade() should not cause any errors
     * - Column should still exist after executeUpgrade()
     * - forceRun() should still return false
     *
     * @throws DotDataException
     * @throws SQLException
     */
    @Test
    public void test_UpgradeTask_WhenColumnExists_ShouldNotCreateAnything() throws DotDataException, SQLException {
        final DotConnect dotConnect = new DotConnect();
        Logger.debug(this, "Testing task when index_version column already exists");

        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        final Task251212AddVersionColumnIndicesTable task = new Task251212AddVersionColumnIndicesTable();

        // Ensure the column exists by running the upgrade task first
        if (!columnExists()) {
            dropColumn(dotConnect);
            addOldConstraintBack(dotConnect);
            task.executeUpgrade();
        }

        // Verify column exists before test
        assertTrue("Column should exist before test", columnExists());

        // Test 1: forceRun should return false when the column already exists
        assertFalse("forceRun() should return false when column already exists", task.forceRun());

        // Execute the upgrade again (should be idempotent)
        task.executeUpgrade();

        // Test 2: Column should still exist
        assertTrue("Column should still exist after running upgrade again", columnExists());

        // Test 3: forceRun should still return false
        assertFalse("forceRun() should still return false after running upgrade again", task.forceRun());
    }
}