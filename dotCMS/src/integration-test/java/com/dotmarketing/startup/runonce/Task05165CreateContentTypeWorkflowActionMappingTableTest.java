package com.dotmarketing.startup.runonce;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.DbType;
import com.dotmarketing.util.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;

public class Task05165CreateContentTypeWorkflowActionMappingTableTest {

    @BeforeClass
    public static void prepare() throws Exception{
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade()  {
        final DbType dbType = DbType.getDbType(DbConnectionFactory.getDBType());
        try{
            final Task05165CreateContentTypeWorkflowActionMappingTable task05165CreateContentTypeWorkflowActionMappingTable =
                    new Task05165CreateContentTypeWorkflowActionMappingTable();

            if (!task05165CreateContentTypeWorkflowActionMappingTable.forceRun()) {

                this.removeContentTypeWorkflowActionMappingTable();
            }

            if (task05165CreateContentTypeWorkflowActionMappingTable.forceRun()) {
                task05165CreateContentTypeWorkflowActionMappingTable.executeUpgrade();
            }
        } catch (Exception e) {
            final String  errMessage = "Could not modify content_type_workflow_action_mapping table on db of type: " + dbType + " Err: " +  e.toString() ;
            Logger.debug(getClass(),errMessage, e);
            Assert.fail(errMessage);
        }
    }

    @WrapInTransaction
    private void removeContentTypeWorkflowActionMappingTable() throws SQLException {

        new DotConnect().executeStatement("DROP INDEX idx_content_type_workflow_action_mapping");
        System.out.println("Removed idx_content_type_workflow_action_mapping");

        new DotConnect().executeStatement("DROP TABLE content_type_workflow_action_mapping");
    }

}
