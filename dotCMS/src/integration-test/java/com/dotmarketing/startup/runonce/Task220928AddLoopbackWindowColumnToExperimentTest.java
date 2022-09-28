package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import java.sql.Connection;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task220928AddLoopbackWindowColumnToExperimentTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: test the methods: forceRun and executeUpgrade
     * Given Scenario: If the column exists, drops it and tries the upgrade task
     * ExpectedResult: The column is created
     *
     */
    @Test
    public void testExecuteUpgrade() throws Exception {
        try (Connection connection = DbConnectionFactory.getDataSource().getConnection()) {
            if(new DotDatabaseMetaData().hasColumn("experiment", "loopback_window")) {
                new DotDatabaseMetaData().dropColumn(connection, "experiment", "loopback_window");
                DbConnectionFactory.commit();
            }
        }

        //Test upgrade
        final Task220928AddLoopbackWindowColumnToExperiment upgradeTask = new Task220928AddLoopbackWindowColumnToExperiment();
        assertTrue(upgradeTask.forceRun());
        upgradeTask.executeUpgrade();
        assertFalse(upgradeTask.forceRun());
    }
}
