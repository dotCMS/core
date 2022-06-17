package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertTrue;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;

public class Task210901UpdateDateTimezonesTest {

   static String selectedTimezone;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        selectedTimezone = new Task210901UpdateDateTimezones().selectTimeZone();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        updateTimeZone(selectedTimezone);
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
        assertTrue("Timezones have been updated",tztask.hasTimeZones());
    }

    @Test
    public void test_timezones_have_been_added() throws Exception{
        final Task210901UpdateDateTimezones tztask = new Task210901UpdateDateTimezones();
        assertTrue("Timezones are already added",tztask.hasTimeZones());
    }

    @Test
    public void test_timezone_offset() throws Exception{
        final Task210901UpdateDateTimezones tztask = new Task210901UpdateDateTimezones();
        updateTimeZone("UTC");
        assertTrue(tztask.calculateOffsetSeconds() == 0);
        updateTimeZone("US/Eastern");
        assertTrue(tztask.calculateOffsetSeconds() == -18000);
    }

    private static void updateTimeZone(final String newTimezone) throws DotDataException {
        if(UtilMethods.isEmpty(newTimezone)) {
            return;
        }
        new DotConnect().setSQL("update user_ set timezoneid = ? where userid='dotcms.org.default'").addParam(newTimezone).loadResult();
    }

}