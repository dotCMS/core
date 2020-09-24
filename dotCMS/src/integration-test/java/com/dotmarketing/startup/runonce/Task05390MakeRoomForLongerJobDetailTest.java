package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.DbType;
import com.dotmarketing.util.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task05390MakeRoomForLongerJobDetailTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade() {
        final DbType dbType = DbType.getDbType(DbConnectionFactory.getDBType());
        try{
            final Task05390MakeRoomForLongerJobDetail upgradeTask = new Task05390MakeRoomForLongerJobDetail();
            final boolean forceRun = upgradeTask.forceRun();
            if(forceRun){
                Assert.assertEquals("should be applied only for mySQL ", DbType.MYSQL, dbType );
                upgradeTask.executeUpgrade();
            } else {
                Assert.assertNotEquals(String.format("should not be applied on dbs ot type `%s` ",dbType), DbType.MYSQL, dbType );
            }
        } catch (Exception e) {
            final String  errMessage = "Could not modify table on db of type: " + dbType + " Err: " +  e.toString() ;
            Logger.error(getClass(),errMessage, e);
            Assert.fail(errMessage);
        }
    }


}
