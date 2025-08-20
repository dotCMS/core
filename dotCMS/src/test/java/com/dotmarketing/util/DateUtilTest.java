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

    /**
     * Method to test: {@link DateUtil#convertDate(String, TimeZone, boolean, String...)}
     * Given Scenario: Test lenient vs strict date parsing with ImportUtil supported formats
     * Expected Result: Lenient mode allows invalid dates, strict mode rejects them
     * Focus: Test all IMP_DATE_FORMATS including US (M/d/y) and European (d/M/y) style formats
     */
    @Test
    public void test_convert_date_with_lenient_parameter() throws ParseException {

        final TimeZone defaultTimeZone = TimeZone.getDefault();

        // Import date formats from ImportUtil (mirrored here to avoid direct dependency)
        final String[] dateFormatPatterns = new String[] {
            "d-MMM-yy", "MMM-yy", "MMMM-yy", "d-MMM", "dd-MMM-yyyy",
            "MM/dd/yy hh:mm aa", "MM/dd/yyyy hh:mm aa", "MM/dd/yy HH:mm", "MM/dd/yyyy HH:mm", 
            "MMMM dd, yyyy", "M/d/y", "M/d", "EEEE, MMMM dd, yyyy", "MM/dd/yyyy", 
            "hh:mm:ss aa", "HH:mm:ss", "hh:mm aa", "yyyy-MM-dd"
        };

        // Additional European formats to test
        final String[] europeanDateFormats = new String[] {
            "d/M/y", "dd/MM/yyyy", "d/M/yyyy", "dd/MM/yy"
        };

        // Combine all formats for comprehensive testing
        final String[] allFormats = new String[dateFormatPatterns.length + europeanDateFormats.length];
        System.arraycopy(dateFormatPatterns, 0, allFormats, 0, dateFormatPatterns.length);
        System.arraycopy(europeanDateFormats, 0, allFormats, dateFormatPatterns.length, europeanDateFormats.length);

        // === Test 1: Valid dates should work in both lenient and strict modes ===

        // US format: M/d/y (12/25/2024 = December 25, 2024)
        Date lenientUSDate = DateUtil.convertDate("12/25/2024", defaultTimeZone, true, "M/d/y", "MM/dd/yyyy");
        assertNotNull("Valid US date should parse in lenient mode", lenientUSDate);
        
        // Validate the US date matches the input intention: 12/25/2024 = December 25, 2024
        assertEquals("US format: Month should be December (11)", Calendar.DECEMBER, lenientUSDate.getMonth());
        assertEquals("US format: Day should be 25", 25, lenientUSDate.getDate());
        assertEquals("US format: Year should be 2024", 2024, lenientUSDate.getYear() + 1900); // Date.getYear() returns year - 1900

        Date strictUSDate = DateUtil.convertDate("12/25/2024", defaultTimeZone, false, "M/d/y", "MM/dd/yyyy");
        assertNotNull("Valid US date should parse in strict mode", strictUSDate);
        
        // Validate strict mode produces the same result
        assertEquals("US format strict: Month should be December", Calendar.DECEMBER, strictUSDate.getMonth());
        assertEquals("US format strict: Day should be 25", 25, strictUSDate.getDate());
        assertEquals("US format strict: Year should be 2024", 2024, strictUSDate.getYear() + 1900);

        // European format: d/M/y (25/12/2024 = December 25, 2024)
        Date lenientEUDate = DateUtil.convertDate("25/12/2024", defaultTimeZone, true, "d/M/y", "dd/MM/yyyy");
        assertNotNull("Valid European date should parse in lenient mode", lenientEUDate);
        
        // Validate the European date matches the input intention: 25/12/2024 = December 25, 2024
        assertEquals("EU format: Month should be December (11)", Calendar.DECEMBER, lenientEUDate.getMonth());
        assertEquals("EU format: Day should be 25", 25, lenientEUDate.getDate());
        assertEquals("EU format: Year should be 2024", 2024, lenientEUDate.getYear() + 1900);

        Date strictEUDate = DateUtil.convertDate("25/12/2024", defaultTimeZone, false, "d/M/y", "dd/MM/yyyy");
        assertNotNull("Valid European date should parse in strict mode", strictEUDate);
        
        // Validate strict mode produces the same result
        assertEquals("EU format strict: Month should be December", Calendar.DECEMBER, strictEUDate.getMonth());
        assertEquals("EU format strict: Day should be 25", 25, strictEUDate.getDate());
        assertEquals("EU format strict: Year should be 2024", 2024, strictEUDate.getYear() + 1900);

        // Verify both formats produce the same date (December 25, 2024)
        assertEquals("US and European formats should produce same year", 
            lenientUSDate.getYear(), lenientEUDate.getYear());
        assertEquals("US and European formats should produce same month", 
            lenientUSDate.getMonth(), lenientEUDate.getMonth());
        assertEquals("US and European formats should produce same day", 
            lenientUSDate.getDate(), lenientEUDate.getDate());

        // === Test 2: More valid dates with various IMP_DATE_FORMATS ===

        // ISO format: 2024-12-25 = December 25, 2024
        Date isoDate = DateUtil.convertDate("2024-12-25", defaultTimeZone, false, "yyyy-MM-dd");
        assertNotNull("ISO date should parse in strict mode", isoDate);
        
        // Validate ISO date matches input intention: 2024-12-25 = December 25, 2024
        assertEquals("ISO format: Year should be 2024", 2024, isoDate.getYear() + 1900);
        assertEquals("ISO format: Month should be December", Calendar.DECEMBER, isoDate.getMonth());
        assertEquals("ISO format: Day should be 25", 25, isoDate.getDate());

        // Long format: December 25, 2024 = December 25, 2024
        Date longDate = DateUtil.convertDate("December 25, 2024", defaultTimeZone, false, "MMMM dd, yyyy");
        assertNotNull("Long date format should parse in strict mode", longDate);
        
        // Validate long format date matches input intention
        assertEquals("Long format: Year should be 2024", 2024, longDate.getYear() + 1900);
        assertEquals("Long format: Month should be December", Calendar.DECEMBER, longDate.getMonth());
        assertEquals("Long format: Day should be 25", 25, longDate.getDate());

        // Short month format: 25-Dec-24 = December 25, 2024
        Date shortMonthDate = DateUtil.convertDate("25-Dec-24", defaultTimeZone, false, "d-MMM-yy", "dd-MMM-yy");
        assertNotNull("Short month format should parse in strict mode", shortMonthDate);
        
        // Validate short month format (note: 2-digit year 24 should be interpreted as 2024)
        assertEquals("Short month format: Year should be 2024", 2024, shortMonthDate.getYear() + 1900);
        assertEquals("Short month format: Month should be December", Calendar.DECEMBER, shortMonthDate.getMonth());
        assertEquals("Short month format: Day should be 25", 25, shortMonthDate.getDate());
        
        // Verify all date formats produce the same date (December 25, 2024)
        assertEquals("All formats should produce same year", lenientUSDate.getYear(), isoDate.getYear());
        assertEquals("All formats should produce same month", lenientUSDate.getMonth(), longDate.getMonth());
        assertEquals("All formats should produce same day", lenientUSDate.getDate(), shortMonthDate.getDate());

        // === Test 3: Invalid dates in lenient mode (should auto-correct) ===

        // US format with invalid date (13/32/2024 - month 13, day 32)
        Date lenientInvalidUS = DateUtil.convertDate("13/32/2024", defaultTimeZone, true, allFormats);
        assertNotNull("Invalid US date should be auto-corrected in lenient mode", lenientInvalidUS);

        // European format with invalid date (32/13/2024 - day 32, month 13)  
        Date lenientInvalidEU = DateUtil.convertDate("32/13/2024", defaultTimeZone, true, allFormats);
        assertNotNull("Invalid European date should be auto-corrected in lenient mode", lenientInvalidEU);

        // February 30th (impossible date)
        Date lenientFeb30 = DateUtil.convertDate("30/2/2024", defaultTimeZone, true, "d/M/y", "dd/MM/yyyy");
        assertNotNull("February 30th should be auto-corrected in lenient mode", lenientFeb30);

        // === Test 4: Invalid dates in strict mode (should throw ParseException) ===

        // Test the specific example requested: 32/45/2025
        try {
            DateUtil.convertDate("32/45/2025", defaultTimeZone, false, "d/M/y", "dd/MM/yyyy");
            Assert.fail("Expected ParseException for '32/45/2025' in strict mode");
        } catch (ParseException e) {
            assertTrue("ParseException should contain the invalid date", 
                e.getMessage().contains("32/45/2025"));
        }

        // US format: Invalid month (13/25/2024)
        try {
            DateUtil.convertDate("13/25/2024", defaultTimeZone, false, "M/d/y", "MM/dd/yyyy");
            Assert.fail("Expected ParseException for invalid US month '13' in strict mode");
        } catch (ParseException e) {
            assertTrue("ParseException should contain meaningful message", 
                e.getMessage().contains("13/25/2024"));
        }

        // European format: Invalid day (32/12/2024)
        try {
            DateUtil.convertDate("32/12/2024", defaultTimeZone, false, "d/M/y", "dd/MM/yyyy");
            Assert.fail("Expected ParseException for invalid European day '32' in strict mode");
        } catch (ParseException e) {
            assertTrue("ParseException should contain meaningful message", 
                e.getMessage().contains("32/12/2024"));
        }

        // Zero month in US format (0/15/2025)
        try {
            DateUtil.convertDate("0/15/2025", defaultTimeZone, false, "M/d/y");
            Assert.fail("Expected ParseException for zero month in US format in strict mode");
        } catch (ParseException e) {
            assertTrue("ParseException should contain meaningful message", 
                e.getMessage().contains("0/15/2025"));
        }

        // Zero day in European format (0/12/2025)
        try {
            DateUtil.convertDate("0/12/2025", defaultTimeZone, false, "d/M/y");
            Assert.fail("Expected ParseException for zero day in European format in strict mode");
        } catch (ParseException e) {
            assertTrue("ParseException should contain meaningful message", 
                e.getMessage().contains("0/12/2025"));
        }

        // February 30th in strict mode
        try {
            DateUtil.convertDate("02/30/2024", defaultTimeZone, false, "MM/dd/yyyy");
            Assert.fail("Expected ParseException for February 30th in strict mode");
        } catch (ParseException e) {
            assertTrue("ParseException should contain meaningful message", 
                e.getMessage().contains("02/30/2024"));
        }

        // Extremely invalid date
        try {
            DateUtil.convertDate("99/99/2025", defaultTimeZone, false, allFormats);
            Assert.fail("Expected ParseException for extremely invalid date '99/99/2025' in strict mode");
        } catch (ParseException e) {
            assertTrue("ParseException should contain meaningful message", 
                e.getMessage().contains("99/99/2025"));
        }

        // === Test 5: Format disambiguation - same input, different interpretation ===

        // "1/2/25" could be:
        // - US format (M/d/y): January 2, 2025
        // - European format (d/M/y): February 1, 2025

        Date usInterpretation = DateUtil.convertDate("1/2/25", defaultTimeZone, false, "M/d/y");
        Date euInterpretation = DateUtil.convertDate("1/2/25", defaultTimeZone, false, "d/M/y");

        // CRITICAL: Verify they produce different dates to confirm format interpretation
        assertNotEquals("US and European interpretations of '1/2/25' should be different",
            usInterpretation.getMonth(), euInterpretation.getMonth());

        // US interpretation: 1/2/25 = January 2, 2025 (M/d/y)
        assertEquals("US format should interpret as January", Calendar.JANUARY, usInterpretation.getMonth());
        assertEquals("US format should interpret as day 2", 2, usInterpretation.getDate());
        assertEquals("US format should interpret as year 2025", 2025, usInterpretation.getYear() + 1900);
        
        // European interpretation: 1/2/25 = February 1, 2025 (d/M/y)  
        assertEquals("European format should interpret as February", Calendar.FEBRUARY, euInterpretation.getMonth());
        assertEquals("European format should interpret as day 1", 1, euInterpretation.getDate());
        assertEquals("European format should interpret as year 2025", 2025, euInterpretation.getYear() + 1900);

        // === Test 6: Additional format validation cases ===

        // Test "12/1/2024" - should be unambiguous
        // US format (M/d/y): December 1, 2024 
        // European format (d/M/y): January 12, 2024
        Date usAmbiguous = DateUtil.convertDate("12/1/2024", defaultTimeZone, false, "M/d/y");
        Date euAmbiguous = DateUtil.convertDate("12/1/2024", defaultTimeZone, false, "d/M/y");

        // US: December 1, 2024
        assertEquals("US '12/1/2024': Month should be December", Calendar.DECEMBER, usAmbiguous.getMonth());
        assertEquals("US '12/1/2024': Day should be 1", 1, usAmbiguous.getDate());
        assertEquals("US '12/1/2024': Year should be 2024", 2024, usAmbiguous.getYear() + 1900);

        // European: January 12, 2024  
        assertEquals("EU '12/1/2024': Month should be January", Calendar.JANUARY, euAmbiguous.getMonth());
        assertEquals("EU '12/1/2024': Day should be 12", 12, euAmbiguous.getDate());
        assertEquals("EU '12/1/2024': Year should be 2024", 2024, euAmbiguous.getYear() + 1900);

        // Test edge case: "31/12/2024" - only valid in European format
        Date euOnly = DateUtil.convertDate("31/12/2024", defaultTimeZone, false, "d/M/y", "dd/MM/yyyy");
        
        // European: December 31, 2024
        assertEquals("EU '31/12/2024': Month should be December", Calendar.DECEMBER, euOnly.getMonth());
        assertEquals("EU '31/12/2024': Day should be 31", 31, euOnly.getDate());
        assertEquals("EU '31/12/2024': Year should be 2024", 2024, euOnly.getYear() + 1900);

        // Test edge case: "12/31/2024" - only valid in US format  
        Date usOnly = DateUtil.convertDate("12/31/2024", defaultTimeZone, false, "M/d/y", "MM/dd/yyyy");
        
        // US: December 31, 2024
        assertEquals("US '12/31/2024': Month should be December", Calendar.DECEMBER, usOnly.getMonth());
        assertEquals("US '12/31/2024': Day should be 31", 31, usOnly.getDate());
        assertEquals("US '12/31/2024': Year should be 2024", 2024, usOnly.getYear() + 1900);

        // Verify both edge cases represent the same logical date (December 31, 2024)
        assertEquals("Both '31/12/2024' and '12/31/2024' should represent same year", 
            euOnly.getYear(), usOnly.getYear());
        assertEquals("Both '31/12/2024' and '12/31/2024' should represent same month", 
            euOnly.getMonth(), usOnly.getMonth());
        assertEquals("Both '31/12/2024' and '12/31/2024' should represent same day", 
            euOnly.getDate(), usOnly.getDate());
    }
}
