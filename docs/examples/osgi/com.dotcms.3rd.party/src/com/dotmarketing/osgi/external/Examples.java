package com.dotmarketing.osgi.external;

import hirondelle.date4j.DateTime;

import java.util.Locale;
import java.util.TimeZone;

/**
 * Run the examples, and output to the console.
 * <p/>
 * <P>Example output when you run this class:
 * <PRE>
 * Current date-time in default time zone : 2011-10-24 08:05:59
 * Current date-time in Cairo : 2011-10-24 13:06:00 (Monday)
 * Age of someone born May 16, 1995 is : 16
 * The 3rd Friday of this month is : 2011-10-21
 * Number of days till Christmas : 62
 * 90 days from today is : 2012-01-22
 * 3 months and 5 days from today is : 2012-01-29
 * Numbers of hours difference between Paris and Perth : 6
 * The number of weeks since Sep 6, 2010 : 59
 * This many seconds till midnight : 57240
 * Output using an ISO format: 2011-10-24T08:06:00
 * The first day of this week is : 2011-10-23
 * The number of years the JDK date-time API has been suctorial : 15
 * </PRE>
 *
 * @see <a href="http://www.date4j.net">http://www.date4j.net</a>
 */
public class Examples {

    public static void log ( Object aMsg ) {
        System.out.println( String.valueOf( aMsg ) );
    }

    /**
     * What is the current date-time in the JRE's default time zone?
     */
    public static void currentDateTime () {
        DateTime now = DateTime.now( TimeZone.getDefault() );
        String result = now.format( "YYYY-MM-DD hh:mm:ss" );
        log( "Current date-time in default time zone : " + result );
    }

    /**
     * What is the current date-time in Cairo (include weekday)?
     */
    public static void currentDateTimeInCairo () {
        DateTime now = DateTime.now( TimeZone.getTimeZone( "Africa/Cairo" ) );
        String result = now.format( "YYYY-MM-DD hh:mm:ss (WWWW)", Locale.getDefault() );
        log( "Current date-time in Cairo : " + result );
    }

    /**
     * What's the age of someone born May 16, 1995?
     */
    public static void ageIfBornOnCertainDate () {
        DateTime today = DateTime.today( TimeZone.getDefault() );
        DateTime birthdate = DateTime.forDateOnly( 1995, 5, 16 );
        int age = today.getYear() - birthdate.getYear();
        if ( today.getDayOfYear() < birthdate.getDayOfYear() ) {
            age = age - 1;
        }
        log( "Age of someone born May 16, 1995 is : " + age );
    }

    /**
     * Stock options expire on the 3rd Friday of this month. What day of the month is that?
     */
    public static void optionsExpiry () {
        DateTime today = DateTime.today( TimeZone.getDefault() );
        DateTime firstOfMonth = today.getStartOfMonth();
        int result = 0;
        if ( firstOfMonth.getWeekDay() == 7 ) {
            result = 21;
        } else {
            result = 21 - firstOfMonth.getWeekDay();
        }
        DateTime thirdFriday = DateTime.forDateOnly( firstOfMonth.getYear(), firstOfMonth.getMonth(), result );
        log( "The 3rd Friday of this month is : " + thirdFriday.format( "YYYY-MM-DD" ) );
    }

    /**
     * How many days till the next December 25?
     */
    public static void daysTillChristmas () {
        DateTime today = DateTime.today( TimeZone.getDefault() );
        DateTime christmas = DateTime.forDateOnly( today.getYear(), 12, 25 );
        int result = 0;
        if ( today.isSameDayAs( christmas ) ) {
            // do nothing
        } else if ( today.lt( christmas ) ) {
            result = today.numDaysFrom( christmas );
        } else if ( today.gt( christmas ) ) {
            DateTime christmasNextYear = DateTime.forDateOnly( today.getYear() + 1, 12, 25 );
            result = today.numDaysFrom( christmasNextYear );
        }
        log( "Number of days till Christmas : " + result );
    }

    /**
     * What day is 90 days from today?
     */
    public static void whenIs90DaysFromToday () {
        DateTime today = DateTime.today( TimeZone.getDefault() );
        log( "90 days from today is : " + today.plusDays( 90 ).format( "YYYY-MM-DD" ) );
    }

    /**
     * What day is 3 months and 5 days from today?
     */
    public static void whenIs3Months5DaysFromToday () {
        DateTime today = DateTime.today( TimeZone.getDefault() );
        DateTime result = today.plus( 0, 3, 5, 0, 0, 0, 0, DateTime.DayOverflow.FirstDay );
        log( "3 months and 5 days from today is : " + result.format( "YYYY-MM-DD" ) );
    }

    /**
     * Current number of hours difference between Paris, France and Perth, Australia.
     */
    public static void hoursDifferenceBetweenParisAndPerth () {
        //this assumes the time diff is a whole number of hours; other styles are possible
        DateTime paris = DateTime.now( TimeZone.getTimeZone( "Europe/Paris" ) );
        DateTime perth = DateTime.now( TimeZone.getTimeZone( "Australia/Perth" ) );
        int result = perth.getHour() - paris.getHour();
        if ( result < 0 ) {
            result = result + 24;
        }
        log( "Numbers of hours difference between Paris and Perth : " + result );
    }

    /**
     * How many weeks is it since Sep 6, 2010?
     */
    public static void weeksSinceStart () {
        DateTime today = DateTime.today( TimeZone.getDefault() );
        DateTime startOfProject = DateTime.forDateOnly( 2010, 9, 6 );
        int result = today.getWeekIndex() - startOfProject.getWeekIndex();
        log( "The number of weeks since Sep 6, 2010 : " + result );
    }

    /**
     * How much time till midnight?
     */
    public static void timeTillMidnight () {
        DateTime now = DateTime.now( TimeZone.getDefault() );
        DateTime midnight = now.plusDays( 1 ).getStartOfDay();
        long result = now.numSecondsFrom( midnight );
        log( "This many seconds till midnight : " + result );
    }

    /**
     * Format using ISO style.
     */
    public static void imitateISOFormat () {
        DateTime now = DateTime.now( TimeZone.getDefault() );
        log( "Output using an ISO format: " + now.format( "YYYY-MM-DDThh:mm:ss" ) );
    }

    public static void firstDayOfThisWeek () {
        DateTime today = DateTime.today( TimeZone.getDefault() );
        DateTime firstDayThisWeek = today; //start value
        int todaysWeekday = today.getWeekDay();
        int SUNDAY = 1;
        if ( todaysWeekday > SUNDAY ) {
            int numDaysFromSunday = todaysWeekday - SUNDAY;
            firstDayThisWeek = today.minusDays( numDaysFromSunday );
        }
        log( "The first day of this week is : " + firstDayThisWeek );
    }

    /**
     * For how many years has the JDK date-time API been suctorial?
     */
    public static void jdkDatesSuctorial () {
        DateTime today = DateTime.today( TimeZone.getDefault() );
        DateTime jdkFirstPublished = DateTime.forDateOnly( 1996, 1, 23 );
        int result = today.getYear() - jdkFirstPublished.getYear();
        log( "The number of years the JDK date-time API has been suctorial : " + result );
    }

}