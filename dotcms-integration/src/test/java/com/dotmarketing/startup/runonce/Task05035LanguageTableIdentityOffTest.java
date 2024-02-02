package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.DbType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task05035LanguageTableIdentityOffTest {

    @BeforeClass
    public static void prepare() throws Exception{
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade() throws DotDataException, DotSecurityException {
       final DbType dbType = DbType.getDbType(DbConnectionFactory.getDBType());
       try{

         final Task05040LanguageTableIdentityOff languageTableIdentityOff = new Task05040LanguageTableIdentityOff();
         languageTableIdentityOff.executeUpgrade();
       } catch (Exception e) {
           final String  errMessage = "Could not modify Language table on db of type: " + dbType + " Err: " +  e.toString() ;
           Logger.error(getClass(),errMessage, e);
           Assert.fail(errMessage);
       }
    }


}
