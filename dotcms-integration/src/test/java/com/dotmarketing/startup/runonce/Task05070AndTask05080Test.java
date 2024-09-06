package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.DbType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task05070AndTask05080Test {

    @BeforeClass
    public static void prepare() throws Exception{
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade() {

        final DbType dbType = DbType.getDbType(DbConnectionFactory.getDBType());
        try{
            final Task05070AddIdentifierVirtualColumn AddColumnTask = new Task05070AddIdentifierVirtualColumn();
            AddColumnTask.executeUpgrade();
            final Task05080RecreateIdentifierIndex RecreateIdentifierTask = new Task05080RecreateIdentifierIndex();
            RecreateIdentifierTask.executeUpgrade();
        } catch (DotDataException e) {
            final String  errMessage = "Could not modify Identifier table and add index to it on db of type: " + dbType + " Err: " +  e.toString() ;
            Logger.error(getClass(),errMessage, e);
            Assert.fail(errMessage);
        }
    }

}
