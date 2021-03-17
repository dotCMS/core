package com.dotmarketing.common.db;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.util.IntegrationTestInitService;

public class DBTimeZoneCheckTest {
    
    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment

        IntegrationTestInitService.getInstance().init();

    }
    
    @Test
    public void test_timezones_work() throws Exception{
        
        DBTimeZoneCheck checker = new DBTimeZoneCheck();
        
        
        assert(checker.timeZoneValid("CST6CDT"));
        
        assert(! checker.timeZoneValid("CST"));

        assert(checker.timeZoneValid("EST"));
        
        assert(! checker.timeZoneValid("asdf435ergre"));

    }

}
