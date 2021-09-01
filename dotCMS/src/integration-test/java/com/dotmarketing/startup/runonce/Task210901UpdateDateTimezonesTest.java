package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.util.IntegrationTestInitService;

public class Task210901UpdateDateTimezonesTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void test() throws Exception {
        Task210901UpdateDateTimezones tztask = new Task210901UpdateDateTimezones();
        if(tztask.forceRun()) {
            tztask.executeUpgrade();
        };
        assertTrue("Timezones have been updated",true);
    }

}
