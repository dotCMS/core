package com.dotmarketing.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.unittest.TestUtil;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.servlet.ServletContext;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit test for {@link DateUtil}
 * @author jsanca
 */
@RunWith(DataProviderRunner.class)
public class DateUtilTest extends UnitTestBase {

    @DataProvider
    public static Object[][] dateTimeCases() {

        List<TestCase> data = new ArrayList<>();

        LocalDateTime now =LocalDateTime.now();

        data.add(new TestCase(now.format(DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ssa")),
                "\\\"?(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}(?:AM|PM|am|pm))\\\"?",
                "MM/dd/yyyy hh:mm:ssa"));

        data.add(new TestCase(now.format(DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a")),
                "\\\"?(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}\\s+(?:AM|PM|am|pm))\\\"?",
                "MM/dd/yyyy hh:mm:ss a"));

        data.add(new TestCase(now.format(DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a")),
                "\\\"?(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{1,2}\\s+(?:AM|PM|am|pm))\\\"?",
                "MM/dd/yyyy hh:mm a"));

        data.add(new TestCase(now.format(DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mma")),
                "\\\"?(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{1,2}(?:AM|PM|am|pm))\\\"?",
                "MM/dd/yyyy hh:mma"));

        data.add(new TestCase(now.format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")),
                "\\\"?(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2})\\\"?",
                "MM/dd/yyyy HH:mm:ss"));

        data.add(new TestCase(now.format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm")),
                "\\\"?(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{1,2})\\\"?",
                "MM/dd/yyyy HH:mm"));

        data.add(new TestCase(now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
                "\\\"?(\\d{1,2}\\d{1,2}\\d{4}\\d{1,2}\\d{1,2}\\d{1,2})\\\"?",
                "yyyyMMddHHmmss"));

        return TestUtil.toCaseArray(data);
    }

    @DataProvider
    public static Object[] timeZones() {
        return new TimeZone[]{TimeZone.getTimeZone("US/Western"),
                TimeZone.getTimeZone("US/Eastern"), TimeZone.getTimeZone("UTC")};
    }

    @DataProvider
    public static Object[][] timeCases() {

        List<TestCase> data = new ArrayList<>();

        LocalDateTime now =LocalDateTime.now();

        data.add(new TestCase(now.format(DateTimeFormatter.ofPattern("hh:mm:ssa")),
                "\\\"?(\\d{1,2}:\\d{1,2}:\\d{1,2}(?:AM|PM|am|pm))\\\"?",
                "hh:mm:ssa"));

        data.add(new TestCase(now.format(DateTimeFormatter.ofPattern("hh:mm:ss a")),
                "\\\"?(\\d{1,2}:\\d{1,2}:\\d{1,2}\\s+(?:AM|PM|am|pm))\\\"?",
                "hh:mm:ss a"));

        data.add(new TestCase(now.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                "\\\"?(\\d{1,2}:\\d{1,2}:\\d{1,2})\\\"?","HH:mm:ss"));

        data.add(new TestCase(now.format(DateTimeFormatter.ofPattern("hh:mma")),
                "\\\"?(\\d{1,2}:\\d{1,2}(?:AM|PM|am|pm))\\\"?","hh:mma"));

        data.add(new TestCase(now.format(DateTimeFormatter.ofPattern("hh:mm a")),
                "\\\"?(\\d{1,2}:\\d{1,2}\\s+(?:AM|PM|am|pm))\\\"?",
                "hh:mm a"));

        data.add(new TestCase(now.format(DateTimeFormatter.ofPattern("HH:mm")),
                "\\\"?(\\d{1,2}:\\d{1,2})\\\"?",
                "HH:mm"));

        return TestUtil.toCaseArray(data);
    }

    private static class TestCase{

        private String dateValue;
        private String regex;
        private String pattern;
        public TestCase(String dateValue, String regex, String pattern){
            this.dateValue = dateValue;
            this.pattern   = pattern;
            this.regex     = regex;
        }
    }


    @Test
    public void testaddDate () throws ParseException {

        final SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");

        format.setLenient(true);

        final Date now    = format.parse("2016/02/01");
        final Date newDay = DateUtil.addDate(now, Calendar.DAY_OF_MONTH, 2);

        assertNotNull(newDay);
        assertTrue(newDay.getDay() - 2 == now.getDay());

        final Date newMonth = DateUtil.addDate(now, Calendar.MONTH, 3);
        assertNotNull(newMonth);
        assertTrue(newMonth.getMonth() - 3 == now.getDay());
    }


    @Test
    public void testMinTime() {

        final Date now = new Date();

        final Date minDate = DateUtil.minTime(now);

        assertNotNull(minDate);
        assertNotNull(minDate.getHours() == 0);
        assertNotNull(minDate.getMinutes() == 0);
        assertNotNull(minDate.getSeconds() == 0);
    }

    @Test
    public void testMaxTime() {

        final Date now = new Date();

        final Date maxDate = DateUtil.minTime(now);

        assertNotNull(maxDate);
        assertNotNull(maxDate.getHours() == 23);
        assertNotNull(maxDate.getMinutes() == 59);
        assertNotNull(maxDate.getSeconds() == 59);
    }

    @Test
    public void testDiffDates() throws ParseException {

        final SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");

        format.setLenient(true);

        final Date oldDate    = format.parse("2015/01/01");

        final Date newDate    = format.parse("2016/02/01");

        final Map emptyMap = DateUtil.diffDates(null, null);
        assertNotNull(emptyMap);
        assertTrue(emptyMap.isEmpty());

        final Map emptyMap2 = DateUtil.diffDates(oldDate, null);
        assertNotNull(emptyMap2);
        assertTrue(emptyMap2.isEmpty());

        final Map emptyMap3 = DateUtil.diffDates(null, newDate);
        assertNotNull(emptyMap3);
        assertTrue(emptyMap3.isEmpty());

        final Map<String, Long> resultMap = DateUtil.diffDates(oldDate, newDate);
        assertNotNull(resultMap);
        assertTrue(!resultMap.isEmpty());
        System.out.println(resultMap);
        assertTrue(resultMap.get(DateUtil.DIFF_YEARS) == 1L);
        assertTrue(resultMap.get(DateUtil.DIFF_MONTHS) == 12L);
        assertTrue(resultMap.get(DateUtil.DIFF_DAYS) == 396L);
        assertTrue(resultMap.get(DateUtil.DIFF_HOURS) == 9504L);
        assertTrue(resultMap.get(DateUtil.DIFF_MINUTES) == 0L);
    }

    @Test
    public void testMillisToSeconds1() throws ParseException {

        int seconds1 = DateUtil.millisToSeconds(3000);
        assertEquals(seconds1, 3);
    }

    @Test
    public void testMillisToSeconds2() throws ParseException {

        int seconds1 = DateUtil.millisToSeconds(3500);
        assertEquals(seconds1, 3);
    }

    @Test
    public void testMillisToSeconds3() throws ParseException {

        int seconds1 = DateUtil.millisToSeconds(3999);
        assertEquals(seconds1, 3);
    }

    @Test
    public void testPrettyDateSince() throws ParseException {

        final ServletContext context = mock(ServletContext.class);

        this.initMessages();
        Config.CONTEXT = context;
        try {
            when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);

            final SimpleDateFormat format = new SimpleDateFormat("yyyy/MMM/dd");

            format.setLenient(true);

            final Date toDate    = format.parse("2016/Apr/01");
            Date date    = format.parse("2015/Mar/01");

            String prettyDate = DateUtil.prettyDateSince(date, new Locale.Builder().setLanguage("en").setRegion("US").build(), toDate);

            assertNotNull(prettyDate);
            assertEquals("more-than-a-year-ago", prettyDate);

            //////////******************
            date    = format.parse("2016/Feb/25");
            prettyDate = DateUtil.prettyDateSince(date, new Locale.Builder().setLanguage("en").setRegion("US").build(), toDate);

            System.out.println(MessageFormat.format("Pretty Date: {0}, from {1} to {2}", prettyDate, date, toDate));
            assertNotNull(prettyDate);
            assertEquals("last-month", prettyDate);

            //////////******************
            date    = format.parse("2016/Mar/04");
            prettyDate = DateUtil.prettyDateSince(date, new Locale.Builder().setLanguage("en").setRegion("US").build(), toDate);

            System.out.println(MessageFormat.format("Pretty Date: {0}, from {1} to {2}", prettyDate, date, toDate));
            assertNotNull(prettyDate);
            assertEquals("x-weeks-ago", prettyDate);

            //////////******************
            date    = format.parse("2016/Jan/04");
            prettyDate = DateUtil.prettyDateSince(date, new Locale.Builder().setLanguage("en").setRegion("US").build(), toDate);

            System.out.println(MessageFormat.format("Pretty Date: {0}, from {1} to {2}", prettyDate, date, toDate));
            assertNotNull(prettyDate);
            assertEquals("x-months-ago", prettyDate);

            //////////******************
            date    = format.parse("2016/Mar/20");
            prettyDate = DateUtil.prettyDateSince(date, new Locale.Builder().setLanguage("en").setRegion("US").build(), toDate);

            System.out.println(MessageFormat.format("Pretty Date: {0}, from {1} to {2}", prettyDate, date, toDate));
            assertNotNull(prettyDate);
            assertEquals("last-week", prettyDate);

            //////////******************
            date    = format.parse("2016/Mar/27");
            prettyDate = DateUtil.prettyDateSince(date, new Locale.Builder().setLanguage("en").setRegion("US").build(), toDate);

            System.out.println(MessageFormat.format("Pretty Date: {0}, from {1} to {2}", prettyDate, date, toDate));
            assertNotNull(prettyDate);
            assertEquals("x-days-ago", prettyDate);

            //////////******************
            date    = format.parse("2016/Mar/31");
            prettyDate = DateUtil.prettyDateSince(date, new Locale.Builder().setLanguage("en").setRegion("US").build(), toDate);

            System.out.println(MessageFormat.format("Pretty Date: {0}, from {1} to {2}", prettyDate, date, toDate));
            assertNotNull(prettyDate);
            assertEquals("yesterday", prettyDate);
        } finally {
            Config.CONTEXT = null;
        }

    }

    @Test
    @UseDataProvider("dateTimeCases")
    public void testReplaceDateTimeWithFormatSuccess(TestCase test) throws Exception{

        String result = DateUtil.replaceDateTimeWithFormat(test.dateValue, test.regex, test.pattern);

        assertNotNull(result);
        assertFalse(result.contains(DateUtil.ERROR_DATE));
        assertNotNull(LocalDateTime.parse(result.replaceAll("\\\\:", ":"),
                DateTimeFormatter.ofPattern(DateUtil.LUCENE_DATE_TIME_PATTERN)));

    }

    @Test
    public void testReplaceDateTimeWithFormatWithError(){
        String result = DateUtil.replaceDateTimeWithFormat("12-20-2005 05:37",
                "\\\"?(\\d{1,2}-\\d{1,2}-\\d{4}\\s+\\d{1,2}:\\d{1,2})\\\"?",
                "MM/dd/yyyy HH:mm");

        assertNotNull(result);
        assertTrue(result.contains(DateUtil.ERROR_DATE));
    }

    @Test
    public void testReplaceDateWithFormatSuccess(){
        String result = DateUtil.replaceDateWithFormat("12/20/2005",
                "\\\"?(\\d{1,2}/\\d{1,2}/\\d{4})\\\"?");

        assertNotNull(result);
        assertFalse(result.contains(DateUtil.ERROR_DATE));
        assertNotNull(LocalDate.parse(result,
                DateTimeFormatter.ofPattern(DateUtil.LUCENE_DATE_PATTERN)));
    }

    @Test
    public void testReplaceDateWithFormatWithError(){
        String result = DateUtil.replaceDateWithFormat("12-20-2005",
                "\\\"?(\\d{1,2}-\\d{1,2}-\\d{4})\\\"?");

        assertNotNull(result);
        assertTrue(result.contains(DateUtil.ERROR_DATE));
    }

    @Test
    @UseDataProvider("timeCases")
    public void testReplaceTimeWithFormatSuccess(TestCase test) throws Exception{

        String result = DateUtil.replaceTimeWithFormat(test.dateValue, test.regex, test.pattern);

        assertNotNull(result);
        assertFalse(result.contains(DateUtil.ERROR_DATE));
        assertNotNull(LocalDateTime.parse(result.replaceAll("\\\\:", ":"),
                DateTimeFormatter.ofPattern(DateUtil.LUCENE_DATE_TIME_PATTERN)));

    }

    @Test
    public void testReplaceTimeWithFormatWithError(){
        String result = DateUtil.replaceTimeWithFormat("051015","\\d{6}","HH:mm");

        assertNotNull(result);
        assertTrue(result.contains(DateUtil.ERROR_DATE));
    }

    @Test
    public void test_parseISO_null()  throws ParseException {
        final Date date1 = DateUtil.parseISO(null);

        assertNull(date1);

        final Date date2 = DateUtil.parseISO("");

        assertNull(date2);
    }


    @Test(expected=ParseException.class)
    public void test_parseISO_bad_date()  throws ParseException {

        final Date date1 = DateUtil.parseISO("2010");

        assertNull(date1);
    }

    @Test()
    public void test_parseISO_short_date() throws ParseException {

        final Date date1 = DateUtil.parseISO("1981-10-04");

        assertNotNull(date1);
        assertEquals("Year should be 1981", 81,date1.getYear());
        assertEquals("Month should be Octuber", Calendar.OCTOBER, date1.getMonth());
        assertEquals("Day should be 4", 4, date1.getDate());
    }

    @Test()
    public void test_parseISO_longiso_date() throws ParseException {

        final Date date1 = DateUtil.parseISO("2015-02-04T12:05:17+00:00");

        assertNotNull(date1);
        assertEquals("Year should be 2015", 115,date1.getYear());
        assertEquals("Month should be Feb", Calendar.FEBRUARY, date1.getMonth());
        assertEquals("Day should be 4", 4, date1.getDate());
    }

    /**
     * Method to test: convert Date
     * Given Scenario: Passing a Date with a specific time zone.
     * ExpectedResult: The date is converted using the desired time zone.
     *
     */
    @Test
    @UseDataProvider("timeZones")
    public void test_time_zone(final TimeZone timeZone) throws ParseException {

        Date date1 = DateUtil.convertDate("2015-02-04 11", timeZone, "yyyy-MM-dd HH");

        assertNotNull(date1);

        final ZonedDateTime zonedDateTime = date1.toInstant().atZone(timeZone.toZoneId());
        assertEquals("Year should be 2015", 2015, zonedDateTime.getYear());
        assertEquals("Month should be Feb", Month.FEBRUARY, zonedDateTime.getMonth());
        assertEquals("Day should be 4", 4, zonedDateTime.getDayOfMonth());
        assertEquals("Hour should be 11",
                11, zonedDateTime.getHour());

    }

    /**
     * Method to test: convert Date
     * Given Scenario: Passing a Date with a specific time zone into the pattern
     * ExpectedResult: The date is converted using the desired time zone.
     *
     */
    @Test()
    public void test_time_zone_string() throws ParseException {

        final String   gmt12TimeZone   = "GMT+1400";
        final TimeZone timeZone        = TimeZone.getTimeZone(gmt12TimeZone);
        final TimeZone defaultTimeZone = TimeZone.getDefault();

        final Date date1 = DateUtil.convertDate("2015-02-04 11 GMT +1400", defaultTimeZone, "yyyy-MM-dd HH z Z");

        assertNotNull(date1);
        assertEquals("Year should be 2015", 115,date1.getYear());
        assertEquals("Month should be Feb", Calendar.FEBRUARY, date1.getMonth());

        if (timeZone.getRawOffset() != date1.getTimezoneOffset()) {
            assertNotEquals("If the date is not in the same time zone, hour should be diff",
                    11, date1.getHours());
            assertEquals("If the date is in the same time zone, hour should be same",
                    11, date1.toInstant().atZone(timeZone.toZoneId()).getHour());

            final GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(date1);
            assertEquals("Default time zone and date time zome should be the same",
                    defaultTimeZone.getRawOffset(), calendar.getTimeZone().getRawOffset());
        }
    }

    /**
     * Method to test: convert Date
     * Given Scenario: Passing a Date without any formats
     * ExpectedResult: The date is converted using one of the time zone on the formats.
     *
     */
    @Test()
    public void test_time_zone_string_no_formats() throws ParseException {

        final Date date1 = DateUtil.convertDate("2015-02-04 GMT +1400");

        assertNotNull(date1);
        assertEquals("Year should be 2015", 115, date1.getYear());
        assertEquals("Month should be Feb", Calendar.FEBRUARY, date1.getMonth());
    }

    /**
     * Method to test: {@link DateUtil#isTimeReach(Instant)}
     */
    @Test
    public void timeReach() {
        //Now must be Reach
        assertTrue(DateUtil.isTimeReach(Instant.now()));

        //Tomorrow must not be reach
        assertFalse(DateUtil.isTimeReach(Instant.now().plus(1, ChronoUnit.DAYS)));

        //Yesterday must be reach
        assertTrue(DateUtil.isTimeReach(Instant.now().plus(-1, ChronoUnit.DAYS)));
    }

    /**
     * Method to test: {@link DateUtil#isTimeReach(Instant)}
     * When: the time parameter is null
     * Should: throw {@link IllegalArgumentException}
     */
    @Test(expected = IllegalArgumentException.class)
    public void timeReachNullPointerException(){
        //Null must retur false
        assertFalse(DateUtil.isTimeReach(null));
    }


    /**
     * Method to test: {@link DateUtil#isTimeReach(Instant, TemporalUnit)}
     * When: You call the method with now no matter the {@link TemporalUnit}
     * Should: Allways return true
     */
    @Test
    public void timeReachWithTemporalUnit(){
        final ChronoUnit[] chronoUnits = new ChronoUnit[]{ChronoUnit.DAYS, ChronoUnit.MINUTES, ChronoUnit.SECONDS, ChronoUnit.MILLIS};

        for (ChronoUnit chronoUnit : chronoUnits) {
            assertTrue(DateUtil.isTimeReach(Instant.now(), chronoUnit));
        }
    }

    /**
     * Method to test: {@link DateUtil#isTimeReach(Instant, TemporalUnit)}
     * When: You call the method with two future times and {@link ChronoUnit#HOURS}:
     * - One Day plus to now it should be false because the different is in DAYS
     * - 30 Minutes plus to now should be true because the different is in MINUTES and the Minutes are truncate
     */
    @Test
    public void timeReachWithTemporalUnitAndFuture(){
        final Instant futureByDays = Instant.now().plus(1, ChronoUnit.DAYS);
        assertFalse(DateUtil.isTimeReach(futureByDays, ChronoUnit.HOURS));

        final Calendar futureByMinutes = Calendar.getInstance();
        futureByMinutes.roll(Calendar.MINUTE, 30);

        assertTrue(DateUtil.isTimeReach(futureByMinutes.toInstant(), ChronoUnit.HOURS));
    }

    /**
     * Method to test: {@link DateUtil#convertDate(Number)}
     * When: call the method with an unix timestamp
     * Expect: the date is properly parsed
     */
    @Test
    public void test_convert_date_number(){

        /*
        Unix Timestamp	1704131112000l
        GMT	Mon Jan 01 2024 11:45:12 GMT+0000
        Your Time Zone	Mon Jan 01 2024 05:45:12 GMT-0600 (Central Standard Time)
        Relative	in a month
         */

        final long timestamp = 1704131112000l;
        final Date date = DateUtil.convertDate(timestamp);
        final String dateString = DateUtil.formatDate(date, "yyyy-MM-dd");
        Assert.assertEquals("2024-01-01", dateString);

        /*
        Input format: RFC 2822, D-M-Y, M/D/Y, Y-M-D, etc. Strip 'GMT' to convert to local time.
        Epoch timestamp: 1701787575
        Timestamp in milliseconds: 1701787575000
        Date and time (GMT): Tuesday, 5 December 2023 14:46:15
        Date and time (Your time zone): martes, 5 de diciembre de 2023 8:46:15 GMT-06:00
         */

        final long timestamp2 = 1701787575000l;
        final Date date2 = DateUtil.convertDate(timestamp2);
        final String dateString2 = DateUtil.formatDate(date2, "yyyy-MM-dd");
        Assert.assertEquals("2023-12-05", dateString2);

    }
}
