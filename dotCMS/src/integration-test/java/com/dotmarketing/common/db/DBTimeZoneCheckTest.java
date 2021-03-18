package com.dotmarketing.common.db;

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
        assertTrue(DBTimeZoneCheck.timeZoneValid("CST6CDT"));
        assertFalse(DBTimeZoneCheck.timeZoneValid("CST"));
        assertTrue(DBTimeZoneCheck.timeZoneValid("EST"));
        assertFalse(DBTimeZoneCheck.timeZoneValid("asdf435ergre"));
    }

}
