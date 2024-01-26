package com.dotmarketing.startup.runalways;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.DbType;
import com.dotmarketing.util.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class Task00001LoadSchemaIntegrationTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * This must be ignored at all Times
     * Use this test exclusively to validate changes on the schema scripts.
     */
    @Ignore
    @Test
    public void testLoadScheme() {
        final DbType dbType = DbType.getDbType(DbConnectionFactory.getDBType());
        try {
            final Task00001LoadSchema task00001LoadSchema = new Task00001LoadSchema();
            task00001LoadSchema.executeUpgrade();
        } catch (Exception e) {
            final String errMessage =
                    "Could not execute load SchemeTask on db of type: " + dbType + " Err: " + e.toString();
            Logger.error(getClass(), errMessage, e);
            Assert.fail(errMessage);
        }
    }

}
