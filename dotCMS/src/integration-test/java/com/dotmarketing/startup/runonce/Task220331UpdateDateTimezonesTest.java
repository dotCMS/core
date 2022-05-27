package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task220331UpdateDateTimezonesTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <b>Method to Test:</b> {@link Task220331UpdateDateTimezones#executeUpgrade()} <p>
     * <b>Given Scenario:</b> When ms-sql is used, the upgrade task should be executed <p>
     * <b>Expected Result:</b> When using postgres, dates should be declared as timestamps with
     * timezone
     */
    @Test
    public void Test_Upgrade_Task() throws Exception {

        final Task220331UpdateDateTimezones task = new Task220331UpdateDateTimezones();
        if (task.forceRun()) {

            try {
                DbConnectionFactory.getConnection().setAutoCommit(true);
            } catch (SQLException e) {
                throw new DotDataException(e.getMessage(), e);
            }

            final DotConnect dotConnect = new DotConnect();
            final String tableName = "a1_"+System.currentTimeMillis();

            dotConnect.executeStatement(String.format("CREATE TABLE %s (test  datetime)",tableName));
            dotConnect.executeStatement(String.format("INSERT INTO %s (test) VALUES(GetDate())",tableName));

            task.executeUpgrade();
            assertTrue(task.getTablesCount() >= 1);
        }
    }

}
