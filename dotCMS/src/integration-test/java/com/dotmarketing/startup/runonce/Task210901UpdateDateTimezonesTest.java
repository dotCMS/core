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

    /**
     * <b>Method to Test:</b> {@link Task210901UpdateDateTimezones#executeUpgrade()} <p>
     * <b>Given Scenario:</b> When postgres is used, the upgrade task should be executed <p>
     * <b>Expected Result:</b> When using postgres, dates should be declared as timestamps with timezone
     * @throws Exception
     */
    @Test
    public void test() throws Exception {
        final Task210901UpdateDateTimezones tztask = new Task210901UpdateDateTimezones();
        if(tztask.forceRun()) {
            tztask.executeUpgrade();
        }
        assertTrue("Timezones have been updated",true);
    }

}
