package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.DbType;
import com.dotmarketing.util.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task05170DefineFrontEndAndBackEndRolesTest {


    @BeforeClass
    public static void prepare() throws Exception{
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade()  {
        final DbType dbType = DbType.getDbType(DbConnectionFactory.getDBType());
        try{
            final Task05170DefineFrontEndAndBackEndRoles task05170DefineFrontEndAndBackEndRoles =
                    new Task05170DefineFrontEndAndBackEndRoles();

            if (task05170DefineFrontEndAndBackEndRoles.forceRun()) {

                task05170DefineFrontEndAndBackEndRoles.executeUpgrade();
            }

        } catch (Exception e) {
            final String  errMessage = "Could not execute upgrade task 05170 : " + dbType + " Err: " +  e.toString() ;
            Logger.error(getClass(),errMessage, e);
            Assert.fail(errMessage);
        }
    }




}
