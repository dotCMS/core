package com.dotmarketing.startup.runonce;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.DbType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobExecutionException;

public class Task04335CreateSystemWorkflowTest extends IntegrationTestBase {


    @BeforeClass
    public static void prepare() throws Exception{
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void addPermission_onDiffDataBase_Success() throws DotDataException, DotSecurityException, JobExecutionException {

        /**
         * {
         "id": 0,
         "inode": "4da13a42-5d59-480c-ad8f-94a3adf809fe",
         "permission": 1,
         "type": "individual",
         "bitPermission": false,
         "individualPermission": true,
         "roleId": "617f7300-5c7b-463f-9554-380b918520bc"
         },
         */
        final DbType dbType = DbType.getDbType(DbConnectionFactory.getDBType());
        try {
            new DotConnect().setSQL(Task04335CreateSystemWorkflow.insertPermissionMap.get(dbType))
                    .addParam("individual")
                    .addParam("4da13a42-5d59-480c-ad8f-94a3adf809fe")
                    .addParam("617f7300-5c7b-463f-9554-380b918520bc")
                    .addParam(1)
                    .loadResult();
        } catch (Exception e) {

            Assert.fail("Could not insert on the db: " + dbType + ", a permission");
        }
    }
}
