package com.dotcms.util;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotmarketing.util.DateUtil;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TimeMachineUtilTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        //Let's NOT Mock static Code its quite problematic as code remains mocked and can cause issues in other tests
    }

    /**
     * Method to Test: {@link TimeMachineUtil#getTimeMachineDate()}
     * When: When Time Machine is not running
     * Should: Return a {@link Optional#empty()}
     */
    @Test
    public void shouldReturnOptionalEmptyWhenTimeMachineIsNotRunning(){
        final HttpSession session = mock(HttpSession.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(anyBoolean())).thenReturn(session);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);


        final Optional<String> timeMachineDate = TimeMachineUtil.getTimeMachineDate();
        assertFalse(timeMachineDate.isPresent());
    }

    /**
     * Method to Test: {@link TimeMachineUtil#getTimeMachineDate()}
     * When: When Time Machine is running
     * Should: return the date in a {@link Optional}
     */
    @Test
    public void shouldReturnOptionalWithValueWhenTimeMachineIsRunning(){
        final HttpSession session = mock(HttpSession.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(anyBoolean())).thenReturn(session);

        final String time = Long.toString(new Date().getTime());

        when(session.getAttribute("tm_date")).thenReturn(time);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final Optional<String> timeMachineDate = TimeMachineUtil.getTimeMachineDate();
        assertTrue(timeMachineDate.isPresent());
        assertEquals(time, timeMachineDate.get());
    }

    /**
     * Method to Test: {@link TimeMachineUtil#isRunning()}
     * When: When Time Machine is not running
     * Should: Return a false
     */
    @Test
    public void whenTimeMachineIsNotRunningShouldReturnFalse(){
        final HttpSession session = mock(HttpSession.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(anyBoolean())).thenReturn(session);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        assertFalse(TimeMachineUtil.isRunning());
    }

    /**
     * Method to Test: {@link TimeMachineUtil#getTimeMachineDate()}
     * When: When Time Machine is running
     * Should: return true
     */
    @Test
    public void whenTimeMachineIsNotRunningShouldReturnTrue(){
        final HttpSession session = mock(HttpSession.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(anyBoolean())).thenReturn(session);

        final String time = Long.toString(new Date().getTime());

        when(session.getAttribute("tm_date")).thenReturn(time);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        assertTrue(TimeMachineUtil.isRunning());
    }

    /**
     * Method to Test: {@link TimeMachineUtil#parseTimeMachineDate(String)}
     * When: When the input date is null
     * Should: Return an empty {@link Optional}
     */
    @Test
    public void testParseTimeMachineDate_NullDate() {
        Optional<Instant> result = TimeMachineUtil.parseTimeMachineDate(null);
        assertFalse(result.isPresent());
    }

    /**
     * Method to Test: {@link TimeMachineUtil#parseTimeMachineDate(String)}
     * When: When the input date is invalid
     * Should: Throw an {@link IllegalArgumentException}
     */
    @Test(expected = IllegalArgumentException.class)
    public void testParseTimeMachineDate_InvalidDate() {
        TimeMachineUtil.parseTimeMachineDate("invalid-date");
    }

    /**
     * Method to Test: {@link TimeMachineUtil#parseTimeMachineDate(String)}
     * When: When the input date is valid and within the grace window
     * Should: Return an empty {@link Optional}
     */
    @Ignore("Lets not mock static code, once mocked the class remains mocked")
    @Test
    public void testParseTimeMachineDate_ValidDateWithinGraceWindow() throws ParseException {
        Instant now = Instant.now().plus(Duration.ofMinutes(3));

        // Format the Instant to a string that DateUtil.convertDate can parse
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        String dateWithinGraceWindow = formatter.format(now);

        // Convert Instant to Date
        Date dateObject = Date.from(now);
        when(DateUtil.convertDate(dateWithinGraceWindow)).thenReturn(dateObject);

        Optional<Instant> result = TimeMachineUtil.parseTimeMachineDate(dateWithinGraceWindow);
        assertTrue(result.isEmpty());
    }

    /**
     * Method to Test: {@link TimeMachineUtil#parseTimeMachineDate(String)}
     * When: When the input date is valid and outside the grace window
     * Should: Return a present {@link Optional} with the parsed date
     */
    @Ignore("Lets not mock static code, once mocked the class remains mocked")
    @Test
    public void testParseTimeMachineDate_ValidDateOutsideGraceWindow() throws ParseException {
        Instant now = Instant.now().plus(Duration.ofMinutes(10));

        // Format the Instant to a string that DateUtil.convertDate can parse
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        String dateWithinGraceWindow = formatter.format(now);

        Date dateObject = Date.from(now);
        when(DateUtil.convertDate(dateWithinGraceWindow)).thenReturn(dateObject);

        Optional<Instant> result = TimeMachineUtil.parseTimeMachineDate(dateWithinGraceWindow);
        assertTrue(result.isPresent());
        assertEquals(now.truncatedTo(ChronoUnit.SECONDS), result.get().truncatedTo(ChronoUnit.SECONDS));
    }

    /**
     * Method to Test: {@link TimeMachineUtil#isOlderThanGraceWindow(Instant)}
     * When: When the input date is within the grace window
     * Should: Return false
     */
    @Test
    public void testIsOlderThanGraceWindow_shouldReturnTrue() {
        Instant now = Instant.now();
        Instant futureDate= now.plus(Duration.ofMinutes(5)); // 5 minutes in the future
        assertFalse(TimeMachineUtil.isOlderThanGraceWindow(futureDate));
    }

    /**
     * Method to Test: {@link TimeMachineUtil#isOlderThanGraceWindow(Instant)}
     * When: When the input date is outside the grace window
     * Should: Return true
     */
    @Test
    public void testIsOlderThanGraceWindow_shouldReturnFalse() {
        Instant now = Instant.now();
        Instant futureDate = now.plus(Duration.ofMinutes(6));   // 6 minutes in the future
        assertTrue(TimeMachineUtil.isOlderThanGraceWindow(futureDate));
    }

    /**
     * Method to Test: {@link TimeMachineUtil#parseTimeMachineDate(String)}
     * When: Test parsing a valid ISO date string without milliseconds
     * Should: Return a present {@link Optional} with the parsed date
     * @throws ParseException
     */
    @Test
    public void testParseTimeMachineDateWithoutMillis() throws ParseException {
        // Given: An ISO 8601 date string without milliseconds
        String dateAsISO8601 = "2085-03-21T13:18:00Z";

        // When: Converting using DateUtil
        final Date date = DateUtil.convertDate(dateAsISO8601);

        // Then: The conversion should succeed
        assertNotNull("DateUtil should convert ISO string to Date", date);

        // When: Parsing using TimeMachineUtil
        Optional<Instant> result = TimeMachineUtil.parseTimeMachineDate(dateAsISO8601);

        // Then: The result should exist
        assertTrue("TimeMachineUtil should return a non-empty Optional", result.isPresent());

        // And: The Instant should contain the correct date/time values
        Instant instant = result.get();
        ZonedDateTime zdt = instant.atZone(ZoneOffset.UTC);

        assertDateTime(zdt, 2085, 3, 21, 13, 18, 0, 0);

        // And: The string representation should match the input
        assertEquals("String representation should match input",
                dateAsISO8601, instant.toString().replace(".000Z", "Z"));
    }

    /**
     * Method to Test: {@link TimeMachineUtil#parseTimeMachineDate(String)}
     * When: Test parsing a valid ISO date string with milliseconds
     * Should: Return a present {@link Optional} with the parsed date
     * @throws ParseException
     */
    @Test
    public void testParseTimeMachineDateWithMillis() throws ParseException {
        // Given: An ISO 8601 date string with milliseconds
        String dateAsISO8601 = "2085-03-21T19:45:57.746Z";

        // When: Converting using DateUtil
        final Date date = DateUtil.convertDate(dateAsISO8601);

        // Then: The conversion should succeed
        assertNotNull("DateUtil should convert ISO string to Date", date);

        // When: Parsing using TimeMachineUtil
        Optional<Instant> result = TimeMachineUtil.parseTimeMachineDate(dateAsISO8601);

        // Then: The result should exist
        assertTrue("TimeMachineUtil should return a non-empty Optional", result.isPresent());

        // And: The Instant should contain the correct date/time values
        Instant instant = result.get();
        ZonedDateTime zdt = instant.atZone(ZoneOffset.UTC);

        assertDateTime(zdt, 2085, 3, 21, 19, 45, 57, 746);

        // And: The string representation should match the input
        assertEquals("String representation should match input",
                dateAsISO8601, instant.toString());
    }

    /**
     * Helper method to assert all date/time components match expected values.
     */
    private void assertDateTime(ZonedDateTime zdt, int year, int month, int day,
            int hour, int minute, int second, int millis) {
        assertEquals("Year should match", year, zdt.getYear());
        assertEquals("Month should match", month, zdt.getMonthValue());
        assertEquals("Day should match", day, zdt.getDayOfMonth());
        assertEquals("Hour should match", hour, zdt.getHour());
        assertEquals("Minute should match", minute, zdt.getMinute());
        assertEquals("Second should match", second, zdt.getSecond());
        assertEquals("Milliseconds should match", millis * 1_000_000, zdt.getNano());
    }
}