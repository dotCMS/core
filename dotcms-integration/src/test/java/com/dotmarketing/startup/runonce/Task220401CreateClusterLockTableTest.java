package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Task220401CreateClusterLockTableTest {

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
        //Create new content type with no html field
        try (Connection connection = DbConnectionFactory.getDataSource().getConnection()) {
            if(new DotDatabaseMetaData().tableExists(connection, "shedlock")) {

                new DotConnect().executeUpdate("DROP TABLE shedlock");
                DbConnectionFactory.commit();
            }
        }

        //Test upgrade
        final Task220401CreateClusterLockTable upgradeTask = new Task220401CreateClusterLockTable();
        assertTrue(upgradeTask.forceRun());
        upgradeTask.executeUpgrade();
        assertFalse(upgradeTask.forceRun());
    }
}
