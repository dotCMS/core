package com.dotcms.util;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotmarketing.util.DateUtil;
import org.junit.BeforeClass;
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
        mockStatic(DateUtil.class);
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
}