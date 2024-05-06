package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class Task210901UpdateDateTimezonesTest {

   static String selectedTimezone;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        if (DbConnectionFactory.isPostgres()) {
            selectedTimezone = new Task210901UpdateDateTimezones().selectTimeZone();
        }
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
        if (!DbConnectionFactory.isPostgres()) {
            return;
        }
        final Task210901UpdateDateTimezones tztask = new Task210901UpdateDateTimezones();
        if(tztask.forceRun()) {
            tztask.executeUpgrade();
        }
        assertTrue("Timezones have been updated",tztask.hasTimeZones());
    }

    /**
     * <b>Method to Test:</b> {@link Task210901UpdateDateTimezones#calculateOffsetSeconds()} <p>
     * <b>Given Scenario:</b> When postgres is used, checks that the offset of a given timezone in millis is calculated
     * correctly.<p>
     * <b>Expected Result:</b> Checks that the offset for UTC is {@code 0}, and the offset for Us/Eastern is
     * {@code 18000}. Even though the right value is {@code 18000} (minus 18000), the Upgrade Task multiplies it by -1
     * depending on the hemisphere that the Time Zone is located in.
     *
     * @throws Exception An error occurred when interacting with the data source.
     */
    @Test
    public void test_timezone_offset() throws Exception {
        if (!DbConnectionFactory.isPostgres()) {
            return;
        }
        final Task210901UpdateDateTimezones tztask = new Task210901UpdateDateTimezones();
        updateTimeZone("UTC");
        assertTrue(tztask.calculateOffsetSeconds() == 0);
        updateTimeZone("US/Eastern");
        assertTrue(tztask.calculateOffsetSeconds() == 18000);
    }

    /**
     * <b>Method to Test:</b> {@link Task210901UpdateDateTimezones#hasTimeZones()} <p>
     * <b>Given Scenario:</b> When postgres is used, checks if all date columns in the {@code contentlet} table have the
     * timezone setting.<p>
     * <b>Expected Result:</b> All date columns should be of type {@code timestamptz}.
     */
    @Test
    public void test_timezones_have_been_added() {
        if (!DbConnectionFactory.isPostgres()) {
            return;
        }
        final Task210901UpdateDateTimezones tztask = new Task210901UpdateDateTimezones();
        assertTrue("Timezones are already added",tztask.hasTimeZones());
    }

    private static void updateTimeZone(final String newTimezone) throws DotDataException {
        if(UtilMethods.isEmpty(newTimezone)) {
            return;
        }
        new DotConnect().setSQL("update user_ set timezoneid = ? where userid='dotcms.org.default'").addParam(newTimezone).loadResult();
    }

}