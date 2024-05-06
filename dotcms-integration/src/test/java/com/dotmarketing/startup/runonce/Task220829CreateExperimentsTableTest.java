package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import java.sql.Connection;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task220829CreateExperimentsTableTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: test the methods: forceRun and executeUpgrade
     * Given Scenario: If the table exists, drops it and tries the upgrade task
     * ExpectedResult: The table will be create again after the upgrade task
     *
     */
    @Test
    public void testExecuteUpgrade() throws Exception {
        try (Connection connection = DbConnectionFactory.getDataSource().getConnection()) {
            if(new DotDatabaseMetaData().tableExists(connection, "experiment")) {

                new DotConnect().executeUpdate("DROP TABLE experiment");
                DbConnectionFactory.commit();
            }
        }

        //Test upgrade
        final Task220829CreateExperimentsTable upgradeTask = new Task220829CreateExperimentsTable();
        assertTrue(upgradeTask.forceRun());
        upgradeTask.executeUpgrade();
        assertFalse(upgradeTask.forceRun());
    }
}
