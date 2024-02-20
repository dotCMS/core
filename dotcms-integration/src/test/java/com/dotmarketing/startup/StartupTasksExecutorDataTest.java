package com.dotmarketing.startup;

import com.dotcms.IntegrationTestBase;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class StartupTasksExecutorDataTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Drops the db_version table
     */
    private void dropDBVersionTable() {

        try {
            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL("DROP TABLE IF EXISTS db_version");
            dotConnect.loadResult();
        } catch (DotDataException e) {
            Logger.error(this, "Error dropping db_version table", e);
            fail(String.format("Error dropping db_version table: %s", e.getMessage()));
        }
    }

    /**
     * Drops the data_version table
     */
    private void dropDataVersionTable() {

        try {
            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL("DROP TABLE IF EXISTS data_version");
            dotConnect.loadResult();
        } catch (DotDataException e) {
            Logger.error(this, "Error dropping data_version table", e);
            fail(String.format("Error dropping data_version table: %s", e.getMessage()));
        }
    }

    /**
     * <b>Method to test:</b> StartupTasksExecutor.getInstance().insureDataVersionTable()
     * <p>
     * <b>Given scenario:</b> Drop the data_version table, run the StartupTasksExecutor.getInstance().insureDataVersionTable()
     * <p>
     * <b>Expected result:</b> The data_version table should exist
     */
    @Test
    @CloseDBIfOpened
    public void testStartupTasksExecutorGetDataVersionTable() throws Exception {

        // Drop the data_version table
        dropDataVersionTable();

        // Create the table
        var executor = StartupTasksExecutor.getInstance();
        executor.insureDataVersionTable();

        try {
            // Checking the latest version
            var dataVersion = executor.currentDataVersion();
            assertEquals(0, dataVersion);
        } catch (Exception e) {
            Logger.error(this, "Error checking latest version in data_version table", e);
            fail(String.format("Error checking latest version in data_version table: %s", e.getMessage()));
        }
    }

    /**
     * <b>Method to test:</b> StartupTasksExecutor.getInstance().executeDataUpgrades()
     * <p>
     * <b>Given scenario:</b> Drop the data_version table, run the StartupTasksExecutor.getInstance().insureDataVersionTable(),
     * to finally execute the data upgrades.
     * <p>
     * <b>Expected result:</b> The data_version table should exist and the data_version table current version should be greater
     * than 1
     */
    @Test
    @CloseDBIfOpened
    public void testStartupTasksExecutorExecuteDataUpgrades() throws Exception {

        // Drop the version tables
        dropDataVersionTable();
        dropDBVersionTable();

        // Create the table
        var executor = StartupTasksExecutor.getInstance();
        executor.insureDataVersionTable();

        // Run the data upgrades
        executor.executeDataUpgrades();

        try {
            // Checking the latest version
            var dataVersion = executor.currentDataVersion();
            assertTrue(dataVersion > 1);
        } catch (Exception e) {
            Logger.error(this, "Error checking latest version in data_version table", e);
            fail(String.format("Error checking latest version in data_version table: %s", e.getMessage()));
        }
    }

}