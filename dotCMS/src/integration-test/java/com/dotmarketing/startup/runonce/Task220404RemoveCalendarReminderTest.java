package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;

import java.sql.Connection;
import java.sql.SQLException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task220404RemoveCalendarReminderTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <b>Method to Test:</b> {@link Task220404RemoveCalendarReminder#executeUpgrade()} <p>
     * <b>Given Scenario:</b> test we can delete calendar reminder <p>
     * <b>Expected Result:</b> Table is gone!
     * timezone
     */
    @Test
    public void Test_Upgrade_Task() throws Exception {
        try {
            final Connection connection = DbConnectionFactory.getConnection();
            connection.setAutoCommit(true);
            
            DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();
            DotConnect dotConnect =  new DotConnect();
            if(!databaseMetaData.tableExists(connection, "calendar_reminder")){
                if(DbConnectionFactory.isPostgres()) {
                    dotConnect.executeStatement("create table calendar_reminder (\n"
                            + "   user_id varchar(255) not null,\n"
                            + "   event_id varchar(36) not null,\n"
                            + "   send_date timestamptz not null,\n"
                            + "   primary key (user_id, event_id, send_date)\n"
                            + ")");
                }
                if(DbConnectionFactory.isMsSql()){
                    dotConnect.executeStatement("create table calendar_reminder (\n"
                                    + "   user_id NVARCHAR(255) not null,\n"
                                    + "   event_id NVARCHAR(36) not null,\n"
                                    + "   send_date datetimeoffset(3) not null,\n"
                                    + "   primary key (user_id, event_id, send_date)\n"
                                    + ")");
                }
            }

            if(DbConnectionFactory.isPostgres()){
                dotConnect.setSQL("INSERT INTO calendar_reminder(user_id, event_id, send_date) VALUES ('0', '0', now());").loadResult();
            }

            if(DbConnectionFactory.isMsSql()){
                dotConnect.setSQL("INSERT INTO dotcms.dbo.calendar_reminder(user_id, event_id, send_date) VALUES ('0', '0', GETDATE())").loadResult();
            }

            final Task220404RemoveCalendarReminder task =  new Task220404RemoveCalendarReminder();
            Assert.assertTrue(task.forceRun());
            task.executeUpgrade();
            Assert.assertFalse(databaseMetaData.tableExists(connection, "calendar_reminder"));

        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }


    }

}
