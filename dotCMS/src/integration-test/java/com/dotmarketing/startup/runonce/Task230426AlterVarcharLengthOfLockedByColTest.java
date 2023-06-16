package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
public class Task230426AlterVarcharLengthOfLockedByColTest {
    // Change the length to recreate the scenario
    private void setMinLengthBeforeTask (String tblName) throws SQLException {
        String query = "";
        if (DbConnectionFactory.isPostgres()){
            query = "alter table "+tblName+" alter column locked_by type varchar (36);";
        }

        final DotConnect dotConnect = new DotConnect();
        dotConnect.executeStatement(query);
    }

    /**
     * Method to test: {@link Task230426AlterVarcharLengthOfLockedByCol#executeUpgrade()}
     * Given Scenario: The varchar length of the column locked_by in the specified table should be increased
     * Specified Tables: contentlet_version_info, container_version_info, template_version_info, link_version_info
     * ExpectedResult: The length of the column locked_by should be more than 36. We set it in 100
     *
     */
    @Test
    public void test_executeUpgrade_GivenIncreaseLockedByLength_LengthShouldBeMoreThan36() throws SQLException, DotDataException {
        final String[] tableNames = { "contentlet_version_info", "container_version_info", "template_version_info", "link_version_info" };
        final String colName = "locked_by";
        Map<String, String> result;
        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();

        //The method is created and tested only for postgres
        if (DbConnectionFactory.isPostgres()){

            //iterate and test all the tables
            for (String tableName : tableNames) {
                setMinLengthBeforeTask(tableName);
                //set the length 36 to recreate the given scenario
                result = dotDatabaseMetaData.getModifiedColumnLength(tableName, colName);
                assertEquals("36", result.get("field_length"));
            }

            //Execute the task
            final Task230426AlterVarcharLengthOfLockedByCol taskToBeTested = new Task230426AlterVarcharLengthOfLockedByCol();
            taskToBeTested.executeUpgrade();

            //Check if the length was increased to 100
            for (String tableName : tableNames) {
                result = dotDatabaseMetaData.getModifiedColumnLength(tableName, colName);;
                assertEquals("100", result.get("field_length"));
            }
        }

    }

}
