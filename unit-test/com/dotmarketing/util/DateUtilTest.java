package com.dotmarketing.util;

import com.dotcms.rest.RestUtilTest;
import org.junit.Test;

import javax.servlet.ServletContext;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DateUtil}
 * @author jsanca
 */
public class DateUtilTest extends BaseMessageResources {

    @Test
    public void testaddDate () throws ParseException {

        final SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");

        format.setLenient(true);
        format.setTimeZone(TimeZone.getTimeZone("America/New_York"));

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
        format.setTimeZone(TimeZone.getTimeZone("America/New_York"));

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
        assertTrue(resultMap.get(DateUtil.DIFF_MONTHS) == 13L);
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

        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);

        final SimpleDateFormat format = new SimpleDateFormat("yyyy/MMM/dd");

        format.setLenient(true);
        format.setTimeZone(TimeZone.getTimeZone("America/New_York"));

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

    }
}
