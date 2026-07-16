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

    // Static date format arrays (mirrored from ImportUtil to avoid direct dependency)
    private static final String[] dateFormatPatterns = new String[] {
        "d-MMM-yy", "MMM-yy", "MMMM-yy", "d-MMM", "dd-MMM-yyyy",
        "MM/dd/yy hh:mm aa", "MM/dd/yyyy hh:mm aa", "MM/dd/yy HH:mm", "MM/dd/yyyy HH:mm", 
        "MMMM dd, yyyy", "M/d/y", "M/d", "EEEE, MMMM dd, yyyy", "MM/dd/yyyy", 
        "hh:mm:ss aa", "HH:mm:ss", "hh:mm aa", "yyyy-MM-dd"
    };

    // Static European date formats (mirrored from ImportUtil.EUROPEAN_DATE_FORMATS)
    private static final String[] europeanDateFormats = new String[] {
        "d/M/y", "dd/MM/yyyy", "d/M/yyyy", "dd/MM/yy", "dd-MM-yyyy"
    };
    
    // Combined all formats for testing
    private static final String[] allFormats = combineFormats(dateFormatPatterns, europeanDateFormats);
    
    private static String[] combineFormats(String[] array1, String[] array2) {
        String[] combined = new String[array1.length + array2.length];
        System.arraycopy(array1, 0, combined, 0, array1.length);
        System.arraycopy(array2, 0, combined, array1.length, array2.length);
        return combined;
    }

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

        // Combine all formats for comprehensive testing (mirrored from ImportUtil.ALL_DATE_FORMATS)
        final String[] allFormats = new String[dateFormatPatterns.length + europeanDateFormats.length];
        System.arraycopy(dateFormatPatterns, 0, allFormats, 0, dateFormatPatterns.length);
        System.arraycopy(europeanDateFormats, 0, allFormats, dateFormatPatterns.length, europeanDateFormats.length);
        // === Test 1: Valid dates in strict mode (like ImportUtil behavior) ===

        // US format: 12/25/2024 = December 25, 2024 (will match M/d/y pattern from allFormats)
        Date usDate = DateUtil.convertDate("12/25/2024", defaultTimeZone, false, allFormats);
        assertNotNull("Valid US date should parse in strict mode", usDate);
        
        // Validate the US date matches the input intention: 12/25/2024 = December 25, 2024
        assertEquals("US format: Month should be December", Calendar.DECEMBER, usDate.getMonth());
        assertEquals("US format: Day should be 25", 25, usDate.getDate());
        assertEquals("US format: Year should be 2024", 2024, usDate.getYear() + 1900);

        // European format: 25/12/2024 = December 25, 2024 (will match d/M/y pattern from allFormats)
        Date euDate = DateUtil.convertDate("25/12/2024", defaultTimeZone, false, allFormats);
        assertNotNull("Valid European date should parse in strict mode", euDate);
        
        // Validate the European date matches the input intention: 25/12/2024 = December 25, 2024
        assertEquals("EU format: Month should be December", Calendar.DECEMBER, euDate.getMonth());
        assertEquals("EU format: Day should be 25", 25, euDate.getDate());
        assertEquals("EU format: Year should be 2024", 2024, euDate.getYear() + 1900);

        // Verify both formats produce the same date (December 25, 2024)
        assertEquals("US and European formats should produce same year", 
            usDate.getYear(), euDate.getYear());
        assertEquals("US and European formats should produce same month", 
            usDate.getMonth(), euDate.getMonth());
        assertEquals("US and European formats should produce same day", 
            usDate.getDate(), euDate.getDate());

        // === CRITICAL TEST: Test with entire format patterns set ===
        
        // Test 1: American date 25/12/2025 using all available formats - should parse as December 25, 2025
        Date americanDate = DateUtil.convertDate("25/12/2025", defaultTimeZone, false, allFormats);
        assertNotNull("25/12/2025 should parse with dateFormatPatterns", americanDate);
        assertEquals("25/12/2025 with all formats: Month should be December", Calendar.DECEMBER, americanDate.getMonth());
        assertEquals("25/12/2025 with all formats: Day should be 25", 25, americanDate.getDate());
        assertEquals("25/12/2025 with all formats: Year should be 2025", 2025, americanDate.getYear() + 1900);

        // Test 2: European date 25/12/2025 using all available formats - should also parse as December 25, 2025
        Date europeanDate = DateUtil.convertDate("25/12/2025", defaultTimeZone, false, allFormats);
        assertNotNull("25/12/2025 should parse with allFormats", europeanDate);
        assertEquals("25/12/2025 with all formats: Month should be December", Calendar.DECEMBER, europeanDate.getMonth());
        assertEquals("25/12/2025 with all formats: Day should be 25", 25, europeanDate.getDate());
        assertEquals("25/12/2025 with all formats: Year should be 2025", 2025, europeanDate.getYear() + 1900);

        // Verify both produce the same result (December 25, 2025)
        assertEquals("Both format sets should produce same year", americanDate.getYear(), europeanDate.getYear());
        assertEquals("Both format sets should produce same month", americanDate.getMonth(), europeanDate.getMonth());
        assertEquals("Both format sets should produce same day", americanDate.getDate(), europeanDate.getDate());

        // === Test 2: Additional valid date formats in strict mode ===

        // ISO format: 2024-12-25 = December 25, 2024 (will match yyyy-MM-dd pattern from allFormats)
        Date isoDate = DateUtil.convertDate("2024-12-25", defaultTimeZone, false, allFormats);
        assertNotNull("ISO date should parse in strict mode", isoDate);
        
        // Validate ISO date matches input intention: 2024-12-25 = December 25, 2024
        assertEquals("ISO format: Year should be 2024", 2024, isoDate.getYear() + 1900);
        assertEquals("ISO format: Month should be December", Calendar.DECEMBER, isoDate.getMonth());
        assertEquals("ISO format: Day should be 25", 25, isoDate.getDate());

        // Long format: December 25, 2024 = December 25, 2024 (will match MMMM dd, yyyy pattern from allFormats)
        Date longDate = DateUtil.convertDate("December 25, 2024", defaultTimeZone, false, allFormats);
        assertNotNull("Long date format should parse in strict mode", longDate);
        
        // Validate long format date matches input intention
        assertEquals("Long format: Year should be 2024", 2024, longDate.getYear() + 1900);
        assertEquals("Long format: Month should be December", Calendar.DECEMBER, longDate.getMonth());
        assertEquals("Long format: Day should be 25", 25, longDate.getDate());

        // Short month format: 25-Dec-24 = December 25, 2024 (will match d-MMM-yy pattern from allFormats)
        Date shortMonthDate = DateUtil.convertDate("25-Dec-24", defaultTimeZone, false, allFormats);
        assertNotNull("Short month format should parse in strict mode", shortMonthDate);
        
        // Validate short month format (note: 2-digit year 24 should be interpreted as 2024)
        assertEquals("Short month format: Year should be 2024", 2024, shortMonthDate.getYear() + 1900);
        assertEquals("Short month format: Month should be December", Calendar.DECEMBER, shortMonthDate.getMonth());
        assertEquals("Short month format: Day should be 25", 25, shortMonthDate.getDate());
        
        // Verify all date formats produce the same date (December 25, 2024)
        assertEquals("All formats should produce same year", usDate.getYear(), isoDate.getYear());
        assertEquals("All formats should produce same month", usDate.getMonth(), longDate.getMonth());
        assertEquals("All formats should produce same day", usDate.getDate(), shortMonthDate.getDate());

        // === Test 3: Invalid dates in strict mode (should throw ParseException like ImportUtil) ===

        // Test the specific example requested: 32/45/2025
        try {
            DateUtil.convertDate("32/45/2025", defaultTimeZone, false, allFormats);
            Assert.fail("Expected ParseException for '32/45/2025' in strict mode");
        } catch (ParseException e) {
            assertTrue("ParseException should contain the invalid date", 
                e.getMessage().contains("32/45/2025"));
        }

        // US format: Invalid month (13/25/2024)
        try {
            DateUtil.convertDate("13/25/2024", defaultTimeZone, false, allFormats);
            Assert.fail("Expected ParseException for invalid US month '13' in strict mode");
        } catch (ParseException e) {
            assertTrue("ParseException should contain meaningful message", 
                e.getMessage().contains("13/25/2024"));
        }

        // European format: Invalid day (32/12/2024)
        try {
            DateUtil.convertDate("32/12/2024", defaultTimeZone, false, allFormats);
            Assert.fail("Expected ParseException for invalid European day '32' in strict mode");
        } catch (ParseException e) {
            assertTrue("ParseException should contain meaningful message", 
                e.getMessage().contains("32/12/2024"));
        }

        // Zero month in US format (0/15/2025)
        try {
            DateUtil.convertDate("0/15/2025", defaultTimeZone, false, allFormats);
            Assert.fail("Expected ParseException for zero month in US format in strict mode");
        } catch (ParseException e) {
            assertTrue("ParseException should contain meaningful message", 
                e.getMessage().contains("0/15/2025"));
        }

        // Zero day in European format (0/12/2025)
        try {
            DateUtil.convertDate("0/12/2025", defaultTimeZone, false, allFormats);
            Assert.fail("Expected ParseException for zero day in European format in strict mode");
        } catch (ParseException e) {
            assertTrue("ParseException should contain meaningful message", 
                e.getMessage().contains("0/12/2025"));
        }

        // February 30th in strict mode
        try {
            DateUtil.convertDate("02/30/2024", defaultTimeZone, false, allFormats);
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

        // === Test 4: Format precedence - demonstrates first matching format wins ===

        // "1/2/25" will be parsed using the first matching format in allFormats
        // Since M/d/y appears before d/M/y in our combined array, it will be interpreted as US format
        // This demonstrates format precedence in the combined array

        Date combinedInterpretation = DateUtil.convertDate("1/2/25", defaultTimeZone, false, allFormats);
        
        // The combined format should match the first applicable pattern (M/d/y from IMP_DATE_FORMATS)
        // So "1/2/25" should be interpreted as January 2, 2025 (US format)
        assertEquals("Combined format should interpret as January", Calendar.JANUARY, combinedInterpretation.getMonth());
        assertEquals("Combined format should interpret as day 2", 2, combinedInterpretation.getDate());
        assertEquals("Combined format should interpret as year 2025", 2025, combinedInterpretation.getYear() + 1900);


        // === Test 5: Edge cases - dates valid in only one format ===

        // Test "12/1/2024" - with combined formats, will be interpreted using first matching format
        // Since M/d/y appears first in our combined array, this will be December 1, 2024 (US format)
        Date ambiguousDate = DateUtil.convertDate("12/1/2024", defaultTimeZone, false, allFormats);

        // Combined format should interpret as US format: December 1, 2024
        assertEquals("Combined '12/1/2024': Month should be December", Calendar.DECEMBER, ambiguousDate.getMonth());
        assertEquals("Combined '12/1/2024': Day should be 1", 1, ambiguousDate.getDate());
        assertEquals("Combined '12/1/2024': Year should be 2024", 2024, ambiguousDate.getYear() + 1900);

        // Test edge case: "31/12/2024" - only valid in European format (d/M/y), will be parsed correctly
        Date euOnly = DateUtil.convertDate("31/12/2024", defaultTimeZone, false, allFormats);
        
        // Should be parsed as European format: December 31, 2024
        assertEquals("Combined '31/12/2024': Month should be December", Calendar.DECEMBER, euOnly.getMonth());
        assertEquals("Combined '31/12/2024': Day should be 31", 31, euOnly.getDate());
        assertEquals("Combined '31/12/2024': Year should be 2024", 2024, euOnly.getYear() + 1900);

        // Test edge case: "12/31/2024" - only valid in US format (M/d/y), will be parsed correctly  
        Date usOnly = DateUtil.convertDate("12/31/2024", defaultTimeZone, false, allFormats);
        
        // Should be parsed as US format: December 31, 2024
        assertEquals("Combined '12/31/2024': Month should be December", Calendar.DECEMBER, usOnly.getMonth());
        assertEquals("Combined '12/31/2024': Day should be 31", 31, usOnly.getDate());
        assertEquals("Combined '12/31/2024': Year should be 2024", 2024, usOnly.getYear() + 1900);

        // Verify both edge cases represent the same logical date (December 31, 2024)
        assertEquals("Both '31/12/2024' and '12/31/2024' should represent same year", 
            euOnly.getYear(), usOnly.getYear());
        assertEquals("Both '31/12/2024' and '12/31/2024' should represent same month", 
            euOnly.getMonth(), usOnly.getMonth());
        assertEquals("Both '31/12/2024' and '12/31/2024' should represent same day", 
            euOnly.getDate(), usOnly.getDate());
    }

    /**
     * Method to test: {@link DateUtil#convertDate(String, TimeZone, boolean, String...)}
     * Given Scenario: Test all ImportUtil date format patterns with valid examples
     * Expected Result: Each format pattern correctly parses its corresponding date string
     * Focus: Comprehensive validation of all supported date format patterns
     */
    @Test
    public void test_all_import_date_format_patterns() throws ParseException {
        
        final TimeZone defaultTimeZone = TimeZone.getDefault();
        
        // === Test US/Standard Date Format Patterns ===
        
        // d-MMM-yy: "25-Dec-24" 
        Date datePattern1 = DateUtil.convertDate("25-Dec-24", defaultTimeZone, false, allFormats);
        assertNotNull("d-MMM-yy format should parse", datePattern1);
        assertEquals("d-MMM-yy: Month should be December", Calendar.DECEMBER, datePattern1.getMonth());
        assertEquals("d-MMM-yy: Day should be 25", 25, datePattern1.getDate());
        
        // MMM-yy: "Dec-24"
        Date datePattern2 = DateUtil.convertDate("Dec-24", defaultTimeZone, false, allFormats);
        assertNotNull("MMM-yy format should parse", datePattern2);
        assertEquals("MMM-yy: Month should be December", Calendar.DECEMBER, datePattern2.getMonth());
        
        // MMMM-yy: "December-24"
        Date datePattern3 = DateUtil.convertDate("December-24", defaultTimeZone, false, allFormats);
        assertNotNull("MMMM-yy format should parse", datePattern3);
        assertEquals("MMMM-yy: Month should be December", Calendar.DECEMBER, datePattern3.getMonth());
        
        // d-MMM: "25-Dec"
        Date datePattern4 = DateUtil.convertDate("25-Dec", defaultTimeZone, false, allFormats);
        assertNotNull("d-MMM format should parse", datePattern4);
        assertEquals("d-MMM: Month should be December", Calendar.DECEMBER, datePattern4.getMonth());
        assertEquals("d-MMM: Day should be 25", 25, datePattern4.getDate());
        
        // dd-MMM-yyyy: "25-Dec-2024"
        Date datePattern5 = DateUtil.convertDate("25-Dec-2024", defaultTimeZone, false, allFormats);
        assertNotNull("dd-MMM-yyyy format should parse", datePattern5);
        assertEquals("dd-MMM-yyyy: Year should be 2024", 2024, datePattern5.getYear() + 1900);
        assertEquals("dd-MMM-yyyy: Month should be December", Calendar.DECEMBER, datePattern5.getMonth());
        assertEquals("dd-MMM-yyyy: Day should be 25", 25, datePattern5.getDate());
        
        // MM/dd/yy hh:mm aa: "12/25/24 03:30 PM"
        Date datePattern6 = DateUtil.convertDate("12/25/24 03:30 PM", defaultTimeZone, false, allFormats);
        assertNotNull("MM/dd/yy hh:mm aa format should parse", datePattern6);
        assertEquals("MM/dd/yy hh:mm aa: Month should be December", Calendar.DECEMBER, datePattern6.getMonth());
        assertEquals("MM/dd/yy hh:mm aa: Day should be 25", 25, datePattern6.getDate());
        
        // MM/dd/yyyy hh:mm aa: "12/25/2024 03:30 PM"
        Date datePattern7 = DateUtil.convertDate("12/25/2024 03:30 PM", defaultTimeZone, false, allFormats);
        assertNotNull("MM/dd/yyyy hh:mm aa format should parse", datePattern7);
        assertEquals("MM/dd/yyyy hh:mm aa: Year should be 2024", 2024, datePattern7.getYear() + 1900);
        assertEquals("MM/dd/yyyy hh:mm aa: Month should be December", Calendar.DECEMBER, datePattern7.getMonth());
        assertEquals("MM/dd/yyyy hh:mm aa: Day should be 25", 25, datePattern7.getDate());
        
        // MM/dd/yy HH:mm: "12/25/24 15:30"
        Date datePattern8 = DateUtil.convertDate("12/25/24 15:30", defaultTimeZone, false, allFormats);
        assertNotNull("MM/dd/yy HH:mm format should parse", datePattern8);
        assertEquals("MM/dd/yy HH:mm: Month should be December", Calendar.DECEMBER, datePattern8.getMonth());
        assertEquals("MM/dd/yy HH:mm: Day should be 25", 25, datePattern8.getDate());
        
        // MM/dd/yyyy HH:mm: "12/25/2024 15:30"
        Date datePattern9 = DateUtil.convertDate("12/25/2024 15:30", defaultTimeZone, false, allFormats);
        assertNotNull("MM/dd/yyyy HH:mm format should parse", datePattern9);
        assertEquals("MM/dd/yyyy HH:mm: Year should be 2024", 2024, datePattern9.getYear() + 1900);
        assertEquals("MM/dd/yyyy HH:mm: Month should be December", Calendar.DECEMBER, datePattern9.getMonth());
        assertEquals("MM/dd/yyyy HH:mm: Day should be 25", 25, datePattern9.getDate());
        
        // MMMM dd, yyyy: "December 25, 2024"
        Date datePattern10 = DateUtil.convertDate("December 25, 2024", defaultTimeZone, false, allFormats);
        assertNotNull("MMMM dd, yyyy format should parse", datePattern10);
        assertEquals("MMMM dd, yyyy: Year should be 2024", 2024, datePattern10.getYear() + 1900);
        assertEquals("MMMM dd, yyyy: Month should be December", Calendar.DECEMBER, datePattern10.getMonth());
        assertEquals("MMMM dd, yyyy: Day should be 25", 25, datePattern10.getDate());
        
        // M/d/y: "12/25/2024"
        Date datePattern11 = DateUtil.convertDate("12/25/2024", defaultTimeZone, false, allFormats);
        assertNotNull("M/d/y format should parse", datePattern11);
        assertEquals("M/d/y: Year should be 2024", 2024, datePattern11.getYear() + 1900);
        assertEquals("M/d/y: Month should be December", Calendar.DECEMBER, datePattern11.getMonth());
        assertEquals("M/d/y: Day should be 25", 25, datePattern11.getDate());
        
        // M/d: "12/25" (current year assumed)
        Date datePattern12 = DateUtil.convertDate("12/25", defaultTimeZone, false, allFormats);
        assertNotNull("M/d format should parse", datePattern12);
        assertEquals("M/d: Month should be December", Calendar.DECEMBER, datePattern12.getMonth());
        assertEquals("M/d: Day should be 25", 25, datePattern12.getDate());
        
        // EEEE, MMMM dd, yyyy: "Wednesday, December 25, 2024"
        Date datePattern13 = DateUtil.convertDate("Wednesday, December 25, 2024", defaultTimeZone, false, allFormats);
        assertNotNull("EEEE, MMMM dd, yyyy format should parse", datePattern13);
        assertEquals("EEEE, MMMM dd, yyyy: Year should be 2024", 2024, datePattern13.getYear() + 1900);
        assertEquals("EEEE, MMMM dd, yyyy: Month should be December", Calendar.DECEMBER, datePattern13.getMonth());
        assertEquals("EEEE, MMMM dd, yyyy: Day should be 25", 25, datePattern13.getDate());
        
        // MM/dd/yyyy: "12/25/2024"
        Date datePattern14 = DateUtil.convertDate("12/25/2024", defaultTimeZone, false, allFormats);
        assertNotNull("MM/dd/yyyy format should parse", datePattern14);
        assertEquals("MM/dd/yyyy: Year should be 2024", 2024, datePattern14.getYear() + 1900);
        assertEquals("MM/dd/yyyy: Month should be December", Calendar.DECEMBER, datePattern14.getMonth());
        assertEquals("MM/dd/yyyy: Day should be 25", 25, datePattern14.getDate());
        
        // yyyy-MM-dd: "2024-12-25" (ISO format)
        Date datePattern15 = DateUtil.convertDate("2024-12-25", defaultTimeZone, false, allFormats);
        assertNotNull("yyyy-MM-dd format should parse", datePattern15);
        assertEquals("yyyy-MM-dd: Year should be 2024", 2024, datePattern15.getYear() + 1900);
        assertEquals("yyyy-MM-dd: Month should be December", Calendar.DECEMBER, datePattern15.getMonth());
        assertEquals("yyyy-MM-dd: Day should be 25", 25, datePattern15.getDate());
        
        // === Test European Date Format Patterns ===
        
        // d/M/y: "25/12/2024"
        Date euPattern1 = DateUtil.convertDate("25/12/2024", defaultTimeZone, false, allFormats);
        assertNotNull("d/M/y European format should parse", euPattern1);
        assertEquals("d/M/y: Year should be 2024", 2024, euPattern1.getYear() + 1900);
        assertEquals("d/M/y: Month should be December", Calendar.DECEMBER, euPattern1.getMonth());
        assertEquals("d/M/y: Day should be 25", 25, euPattern1.getDate());
        
        // dd/MM/yyyy: "25/12/2024"
        Date euPattern2 = DateUtil.convertDate("25/12/2024", defaultTimeZone, false, allFormats);
        assertNotNull("dd/MM/yyyy European format should parse", euPattern2);
        assertEquals("dd/MM/yyyy: Year should be 2024", 2024, euPattern2.getYear() + 1900);
        assertEquals("dd/MM/yyyy: Month should be December", Calendar.DECEMBER, euPattern2.getMonth());
        assertEquals("dd/MM/yyyy: Day should be 25", 25, euPattern2.getDate());
        
        // d/M/yyyy: "25/12/2024"
        Date euPattern3 = DateUtil.convertDate("25/12/2024", defaultTimeZone, false, allFormats);
        assertNotNull("d/M/yyyy European format should parse", euPattern3);
        assertEquals("d/M/yyyy: Year should be 2024", 2024, euPattern3.getYear() + 1900);
        assertEquals("d/M/yyyy: Month should be December", Calendar.DECEMBER, euPattern3.getMonth());
        assertEquals("d/M/yyyy: Day should be 25", 25, euPattern3.getDate());
        
        // dd/MM/yy: "25/12/24"
        Date euPattern4 = DateUtil.convertDate("25/12/24", defaultTimeZone, false, allFormats);
        assertNotNull("dd/MM/yy European format should parse", euPattern4);
        assertEquals("dd/MM/yy: Month should be December", Calendar.DECEMBER, euPattern4.getMonth());
        assertEquals("dd/MM/yy: Day should be 25", 25, euPattern4.getDate());
        
        // === Test Time Format Patterns (standalone time patterns) ===
        
        // hh:mm:ss aa: "03:30:45 PM"
        Date timePattern1 = DateUtil.convertDate("03:30:45 PM", defaultTimeZone, false, allFormats);
        assertNotNull("hh:mm:ss aa time format should parse", timePattern1);
        
        // HH:mm:ss: "15:30:45"
        Date timePattern2 = DateUtil.convertDate("15:30:45", defaultTimeZone, false, allFormats);
        assertNotNull("HH:mm:ss time format should parse", timePattern2);
        
        // hh:mm aa: "03:30 PM"
        Date timePattern3 = DateUtil.convertDate("03:30 PM", defaultTimeZone, false, allFormats);
        assertNotNull("hh:mm aa time format should parse", timePattern3);
        
        // === Verify All December 25 Patterns Produce Consistent Results ===
        
        Date[] december25Dates = {
            datePattern1, datePattern4, datePattern5, datePattern6, datePattern7, 
            datePattern8, datePattern9, datePattern10, datePattern11, datePattern12,
            datePattern13, datePattern14, datePattern15, euPattern1, euPattern2, euPattern3, euPattern4
        };
        
        for (int i = 0; i < december25Dates.length; i++) {
            if (december25Dates[i] != null) {
                assertEquals("All December 25 dates should have same month [" + i + "]", 
                    Calendar.DECEMBER, december25Dates[i].getMonth());
                assertEquals("All December 25 dates should have same day [" + i + "]", 
                    25, december25Dates[i].getDate());
            }
        }
    }

    @Test
    public void test_specific_date_14_05_2027_should_pass() throws ParseException {
        // Test that the date "14-05-2027" parses successfully as May 14, 2027
        // This uses the dd-MM-yyyy format pattern that was added to support European dates
        
        Date result = DateUtil.convertDate("14-05-2027", false, allFormats);
        assertNotNull("14-05-2027 should parse successfully", result);
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        
        assertEquals("Year should be 2027", 2027, cal.get(Calendar.YEAR));
        assertEquals("Month should be May (4)", Calendar.MAY, cal.get(Calendar.MONTH));
        assertEquals("Day should be 14", 14, cal.get(Calendar.DAY_OF_MONTH));
        
        // Additional test cases for dd-MM-yyyy format
        Date result2 = DateUtil.convertDate("01-12-2025", false, allFormats);
        assertNotNull("01-12-2025 should parse successfully", result2);
        
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(result2);
        assertEquals("Year should be 2025", 2025, cal2.get(Calendar.YEAR));
        assertEquals("Month should be December (11)", Calendar.DECEMBER, cal2.get(Calendar.MONTH));
        assertEquals("Day should be 1", 1, cal2.get(Calendar.DAY_OF_MONTH));
    }
}
