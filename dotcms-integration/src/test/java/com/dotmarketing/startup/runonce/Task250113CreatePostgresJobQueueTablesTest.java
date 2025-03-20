package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class for {@link Task250113CreatePostgresJobQueueTables}.
 * <p>
 * This test class ensures that the job queue tables are properly created by the upgrade task.
 * <p>
 * The test first drops the tables if they exist, then executes the upgrade task and validates that
 * the tables were successfully created.
 */
public class Task250113CreatePostgresJobQueueTablesTest extends IntegrationTestBase {

    /**
     * Initializes the test environment and ensures the job queue tables do not exist.
     *
     * @throws Exception if an error occurs during initialization.
     */
    @BeforeClass
    public static void setup() throws Exception {

        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Drops the job queue tables if they exist.
     */
    private void dropTablesIfExist() {

        try {

            final Connection connection = DbConnectionFactory.getConnection();
            final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();

            if (databaseMetaData.tableExists(connection, "job_queue")) {
                databaseMetaData.dropTable(connection, "job_queue");
            }
            if (databaseMetaData.tableExists(connection, "job_history")) {
                databaseMetaData.dropTable(connection, "job_history");
            }
            if (databaseMetaData.tableExists(connection, "job")) {
                databaseMetaData.dropTable(connection, "job");
            }

        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Method to test {@link Task250113CreatePostgresJobQueueTables#executeUpgrade()} and
     * {@link Task250113CreatePostgresJobQueueTables#forceRun()}.
     * <p>
     * Given Scenario: The job queue tables do not exist.
     * <p>
     * Expected Result: The job queue tables will be created after running the upgrade task.
     *
     * @throws SQLException         if a SQL error occurs.
     * @throws DotDataException     if a data access error occurs.
     * @throws DotSecurityException if a security error occurs.
     */
    @Test
    public void executeTaskUpgrade() throws SQLException, DotDataException, DotSecurityException {

        try {
            // First, ensure the tables do not exist
            LocalTransaction.wrap(() -> {
                try {
                    dropTablesIfExist();
                } catch (Exception e) {
                    throw new DotRuntimeException(e);
                }
            });

            // Running the upgrade task and validating the tables were created
            executeUpgradeAndValidate();
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }

    /**
     * Method to test {@link Task250113CreatePostgresJobQueueTables#executeUpgrade()} and
     * {@link Task250113CreatePostgresJobQueueTables#forceRun()}.
     * <p>
     * Given Scenario: The job queue tables do not exist, and the upgrade task is run twice.
     * <p>
     * Expected Result: The job queue tables will be created after running the upgrade task the
     * first time and should not fail when running the upgrade task again.
     *
     * @throws SQLException         if a SQL error occurs.
     * @throws DotDataException     if a data access error occurs.
     * @throws DotSecurityException if a security error occurs.
     */
    @Test
    public void executeTaskUpgradeTwice()
            throws SQLException, DotDataException, DotSecurityException {

        try {
            // First, ensure the tables do not exist
            LocalTransaction.wrap(() -> {
                try {
                    dropTablesIfExist();
                } catch (Exception e) {
                    throw new DotRuntimeException(e);
                }
            });

            // Run the upgrade task for the first time, it should create the tables
            executeUpgradeAndValidate();

            // Run the upgrade task again, should not fail
            LocalTransaction.wrap(() -> {
                try {
                    final var task = new Task250113CreatePostgresJobQueueTables();
                    task.executeUpgrade();
                } catch (Exception e) {
                    final var message = "The upgrade task should not fail when the tables already exist";
                    Logger.error(message, e);
                    Assert.fail(message);
                }
            });
        } finally {
            DbConnectionFactory.closeSilently();
        }
    }

    /**
     * Executes the upgrade task and validates the job queue tables were created.
     */
    private static void executeUpgradeAndValidate()
            throws SQLException, DotDataException, DotSecurityException {

        final var task = new Task250113CreatePostgresJobQueueTables();
        final Connection connection = DbConnectionFactory.getConnection();
        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();

        // Ensure the tables do not exist before the upgrade
        assertFalse(databaseMetaData.tableExists(connection, "job_queue"));
        assertFalse(databaseMetaData.tableExists(connection, "job"));
        assertFalse(databaseMetaData.tableExists(connection, "job_history"));

        assertTrue(task.forceRun());
        LocalTransaction.wrap(() -> {
            try {
                task.executeUpgrade();
            } catch (Exception e) {
                throw new DotRuntimeException(e);
            }
        });

        // Validate the tables were created after the upgrade
        assertTrue(databaseMetaData.tableExists(connection, "job_queue"));
        assertTrue(databaseMetaData.tableExists(connection, "job"));
        assertTrue(databaseMetaData.tableExists(connection, "job_history"));
    }

}