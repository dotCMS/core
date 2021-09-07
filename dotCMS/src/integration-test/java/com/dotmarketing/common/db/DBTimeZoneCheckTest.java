package com.dotmarketing.common.db;

import com.dotmarketing.db.DbConnectionFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.util.IntegrationTestInitService;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class DBTimeZoneCheckTest {
    
    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }
    
    @Test
    public void test_timezones_work() throws Exception {
        if (DbConnectionFactory.isPostgres()) {
            assertTrue(DBTimeZoneCheck.isTimeZoneValid("CST6CDT"));
            assertFalse(DBTimeZoneCheck.isTimeZoneValid("CST"));
        } else {
            assertTrue(DBTimeZoneCheck.isTimeZoneValid("CST6CDT"));
            assertTrue(DBTimeZoneCheck.isTimeZoneValid("CST"));
        }
    }

}
