/*
 * Copyright 2003-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package org.apache.velocity.tools.generic;


import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;


/**
 * Tool for working with {@link Date} and {@link Calendar}
 * in Velocity templates.  It is useful for accessing and
 * formatting the "current" date as well as for formatting
 * arbitrary {@link Date} and {@link Calendar} objects. Also
 * the tool can be used to retrieve {@link DateFormat} instances
 * or make conversions to and from various date types.
 * <p><pre>
 * Example uses:
 *  $date                         -> Oct 19, 2003 9:54:50 PM
 *  $date.long                    -> October 19, 2003 9:54:50 PM PDT
 *  $date.medium_time             -> 9:54:50 PM
 *  $date.full_date               -> Sunday, October 19, 2003
 *  $date.get('default','short')  -> Oct 19, 2003 9:54 PM
 *  $date.get('yyyy-M-d H:m:s')   -> 2003-10-19 21:54:50
 * 
 *  $myDate                        -> Tue Oct 07 03:14:50 PDT 2003
 *  $date.format('medium',$myDate) -> Oct 7, 2003 3:14:50 AM 
 *
 * Example toolbox.xml config (if you want to use this with VelocityView):
 * &lt;tool&gt;
 *   &lt;key&gt;date&lt;/key&gt;
 *   &lt;scope&gt;application&lt;/scope&gt;
 *   &lt;class&gt;org.apache.velocity.tools.generic.DateTool&lt;/class&gt;
 * &lt;/tool&gt;
 * </pre></p>
 *
 * <p>This tool is entirely threadsafe, and has no instance members.
 * It may be used in any scope (request, session, or application).
 * As such, the methods are highly interconnected, and overriding 
 * key methods provides an easy way to create subclasses that use 
 * a non-default format, calendar, locale, or timezone.</p>
 *
 * @author <a href="mailto:nathan@esha.com">Nathan Bubna</a>
 * @since VelocityTools 1.0
 * @version $Revision: 154105 $ $Date: 2005-02-16 16:56:54 -0800 (Wed, 16 Feb 2005) $
 */
public class DateTool
{

    /** 
     * The default format to be used when none is specified. 
     *
     * @since VelocityTools 1.1
     */
    public static final String DEFAULT_FORMAT = "default";

    /**
     * Default constructor.
     */
    public DateTool()
    {
        // do nothing
    }


    // ------------------------- system date access ------------------

    /**
     * @return the system's current time as a {@link Date}
     */
    public static final Date getSystemDate()
    {
        return getSystemCalendar().getTime();
    }


    /**
     * @return the system's current time as a {@link Calendar}
     */
    public static final Calendar getSystemCalendar()
    {
        return Calendar.getInstance();
    }


    // ------------------------- default parameter access ----------------

    /**
     * This implementation returns the default locale. Subclasses
     * may override this to return alternate locales. Please note that
     * doing so will affect all formatting methods where no locale is
     * specified in the parameters.
     *
     * @return the default {@link Locale}
     */
    public Locale getLocale()
    {
        return Locale.getDefault();
    }

    /**
     * This implementation returns the default TimeZone. Subclasses
     * may override this to return alternate timezones. Please note that
     * doing so will affect all formatting methods where no timezone is
     * specified in the parameters.
     *
     * @return the default {@link TimeZone}
     */
    public TimeZone getTimeZone()
    {
        return TimeZone.getDefault();
    }

    /**
     * Returns a {@link Date} derived from the result of {@link #getCalendar}
     *
     * @return a {@link Date} derived from the result of {@link #getCalendar}
     */
    public Date getDate()
    {
        return getCalendar().getTime();
    }

    /**
     * Returns a {@link Calendar} instance created using the timezone and
     * locale returned by getTimeZone() and getLocale().  This allows subclasses
     * to easily override the default locale and timezone used by this tool.
     *
     * <p>Sub-classes may override this method to return a Calendar instance
     * not based on the system date.
     * Doing so will also cause the getDate(), get(String), get(String,String), 
     * and toString() methods to return dates equivalent to the Calendar 
     * returned by this method, because those methods return values derived 
     * from the result of this method.</p>
     *
     * @return a {@link Calendar} instance created using the results of 
     *         {@link #getTimeZone()} and {@link #getLocale()}.
     * @see Calendar#getInstance(TimeZone zone, Locale aLocale)
     */
    public Calendar getCalendar()
    {
        return Calendar.getInstance(getTimeZone(), getLocale());
    }

    /**
     * Return the pattern or style to be used for formatting dates when none
     * is specified. This implementation gives a 'default' date-time format. 
     * Subclasses may override this to provide a different default format.
     *
     * <p>NOTE: At some point in the future it may be feasible to configure
     * this value via the toolbox definition, but at present, it is not possible
     * to specify custom tool configurations there.  For now you should just 
     * override this in a subclass to have a different default.</p>
     *
     * @since VelocityTools 1.1
     */
    public String getFormat()
    {
        return DEFAULT_FORMAT;
    }


    // ------------------------- date value access ---------------------------

    /**
     * Returns the year value of the date returned by {@link #getCalendar()}.
     *
     * @since VelocityTools 1.2
     */
    public Integer getYear()
    {
        return getYear(getCalendar());
    }

    /**
     * Returns the year value of the specified date.
     *
     * @since VelocityTools 1.2
     */
    public Integer getYear(Object date)
    {
        return getValue(Calendar.YEAR, date);
    }

    /**
     * Returns the month value of the date returned by {@link #getCalendar()}.
     *
     * @since VelocityTools 1.2
     */
    public Integer getMonth()
    {
        return getMonth(getCalendar());
    }

    /**
     * Returns the month value of the specified date.
     *
     * @since VelocityTools 1.2
     */
    public Integer getMonth(Object date)
    {
        return getValue(Calendar.MONTH, date);
    }

    /**
     * Returns the day (of the month) value of the date 
     * returned by {@link #getCalendar()}.
     * <br><br>
     * NOTE: Unlike java.util.Date, this returns the day of the month.
     * It is equivalent to Date.getDate() and 
     * Calendar.get(Calendar.DAY_OF_MONTH).  We could not call this method
     * getDate() because that already exists in this class with a different
     * function.
     *
     * @since VelocityTools 1.2
     */
    public Integer getDay()
    {
        return getDay(getCalendar());
    }

    /**
     * Returns the day (of the month) value for the specified date.
     * <br><br>
     * NOTE: Unlike java.util.Date, this returns the day of the month.
     * It is equivalent to Date.getDate() and 
     * Calendar.get(Calendar.DAY_OF_MONTH).  We could not call this method
     * getDate() because that already exists in this class with a different
     * function.
     *
     * @since VelocityTools 1.2
     */
    public Integer getDay(Object date)
    {
        return getValue(Calendar.DAY_OF_MONTH, date);
    }

    /**
     * Return the specified value of the date returned by 
     * {@link #getCalendar()} or null if the field is invalid.
     *
     * @since VelocityTools 1.2
     */
    public Integer getValue(Object field)
    {
        return getValue(field, getCalendar());
    }

    /**
     * Returns the specified value of the specified date,
     * or null if the field or date is invalid.  The field may be
     * an Integer or it may be the name of the field as a String.
     *
     * @param field the corresponding Integer value or String name of the desired value
     * @param date the date/calendar from which the field value will be taken
     * @since VelocityTools 1.2
     */
    public Integer getValue(Object field, Object date)
    {
        if (field == null)
        {
            return null;
        }

        int fieldValue;
        if (field instanceof Integer)
        {
            fieldValue = ((Integer)field).intValue();
        }
        // all the public static field names are upper case
        String fstr = field.toString().toUpperCase();
        try
        {
            Field clsf = Calendar.class.getField(fstr);
            fieldValue = clsf.getInt(Calendar.getInstance());
        }
        catch (Exception e)
        {
            return null;
        }
        return getValue(fieldValue, date);
    }

    /**
     * Returns the specified value of the specified date,
     * or null if the field or date is invalid.
     *
     * @param field the int for the desired field (e.g. Calendar.MONTH)
     * @param date the date/calendar from which the field value will be taken
     * @since VelocityTools 1.2
     */
    public Integer getValue(int field, Object date)
    {
        Calendar cal = toCalendar(date);
        if (cal == null)
        {
            return null;
        }
        return new Integer(cal.get(field));
    }


    // ------------------------- formatting methods ---------------------------

    /**
     * Returns a formatted string representing the date returned by
     * {@link #getDate()}.  In its default implementation, this method 
     * allows you to retrieve the current date in standard formats by
     * simply doing things like <code>$date.medium</code> or 
     * <code>$date.full</code>.  If you want only the date or time portion
     * you can specify that along with the standard formats. (e.g. 
     * <code>$date.medium_date</code> or <code>$date.short_time</code>)
     * More complex or custom formats can be retrieved
     * by using the full method syntax. (e.g. $date.get('E, MMMM d'))
     *
     * @param format the formatting instructions
     * @return a formatted representation of the date returned by
     *         {@link #getDate()}
     * @see #format(String format, Object obj, Locale locale, TimeZone timezone)
     * @since VelocityTools 1.1
     */
    public String get(String format)
    {
        return format(format, getDate());
    }

    /**
     * Returns a formatted string representing the date and/or time given by
     * {@link #getDate()} in standard, localized patterns.
     *
     * @param dateStyle the style pattern for the date
     * @param timeStyle the style pattern for the time
     * @return a formatted representation of the date returned by
     *         {@link #getDate()}
     * @see DateFormat
     * @see #format(String dateStyle, String timeStyle, Object obj, Locale locale, TimeZone timezone)
     * @since VelocityTools 1.1
     */
    public String get(String dateStyle, String timeStyle)
    {
        return format(dateStyle, timeStyle, getDate(), getLocale());
    }


    /**
     * Converts the specified object to a date and formats it according to
     * the pattern or style returned by {@link #getFormat()}.
     * 
     * @param obj the date object to be formatted
     * @return the specified date formatted as a string
     * @see #format(String format, Object obj, Locale locale, TimeZone timezone)
     * @since VelocityTools 1.1
     */
    public String format(Object obj)
    {
        return format(getFormat(), obj);
    }

    /**
     * Converts the specified object to a date and returns
     * a formatted string representing that date in the locale
     * returned by {@link #getLocale()}.
     *
     * @param format the formatting instructions
     * @param obj the date object to be formatted
     * @return a formatted string for this locale representing the specified
     *         date or <code>null</code> if the parameters are invalid
     * @see #format(String format, Object obj, Locale locale, TimeZone timezone)
     */
    public String format(String format, Object obj)
    {
        return format(format, obj, getLocale());
    }

    /**
     * Converts the specified object to a date and returns
     * a formatted string representing that date in the specified
     * {@link Locale}.
     *
     * @param format the formatting instructions
     * @param obj the date object to be formatted
     * @param locale the locale to be used when formatting
     * @return the given date as a formatted string
     * @see #format(String format, Object obj, Locale locale, TimeZone timezone)
     */
    public String format(String format, Object obj, Locale locale)
    {
        return format(format, obj, locale, getTimeZone());
    }

    /**
     * Returns a formatted string representing the specified date,
     * {@link Locale}, and {@link TimeZone}.
     *
     * <p>
     * The specified format may be a standard style pattern ('full', 'long',
     * 'medium', 'short', or 'default').
     * </p>
     * <p>
     * You may also specify that you want only the date or time portion be
     * appending '_date' or '_time' respectively to the standard style pattern.
     * (e.g. 'full_date' or 'long_time')
     * </p>
     * <p>
     * If the format fits neither of these patterns, then the output 
     * will be formatted according to the symbols defined by 
     * {@link SimpleDateFormat}:
     * <pre>
     *   Symbol   Meaning                 Presentation        Example
     *   ------   -------                 ------------        -------
     *   G        era designator          (Text)              AD
     *   y        year                    (Number)            1996
     *   M        month in year           (Text & Number)     July & 07
     *   d        day in month            (Number)            10
     *   h        hour in am/pm (1~12)    (Number)            12
     *   H        hour in day (0~23)      (Number)            0
     *   m        minute in hour          (Number)            30
     *   s        second in minute        (Number)            55
     *   S        millisecond             (Number)            978
     *   E        day in week             (Text)              Tuesday
     *   D        day in year             (Number)            189
     *   F        day of week in month    (Number)            2 (2nd Wed in July)
     *   w        week in year            (Number)            27
     *   W        week in month           (Number)            2
     *   a        am/pm marker            (Text)              PM
     *   k        hour in day (1~24)      (Number)            24
     *   K        hour in am/pm (0~11)    (Number)            0
     *   z        time zone               (Text)              Pacific Standard Time
     *   '        escape for text         (Delimiter)
     *   ''       single quote            (Literal)           '
     *
     *   Examples: "E, MMMM d" will result in "Tue, July 24"
     *             "EEE, M-d (H:m)" will result in "Tuesday, 7-24 (14:12)"
     * </pre>
     * </p>
     * 
     * @param format the custom or standard pattern to be used
     * @param obj the date to format
     * @param locale the {@link Locale} to format the date for
     * @param timezone the {@link TimeZone} to be used when formatting
     * @return a formatted string representing the specified date or
     *         <code>null</code> if the parameters are invalid
     * @since VelocityTools 1.1
     */
    public String format(String format, Object obj, 
                         Locale locale, TimeZone timezone)
    {
        Date date = toDate(obj);
        DateFormat df = getDateFormat(format, locale, timezone);
        if (date == null || df == null)
        {
            return null;
        }
        return df.format(date);
    }


    /**
     * Returns the specified date as a string formatted according to the
     * specified date and/or time styles.
     *
     * @param dateStyle the style pattern for the date
     * @param timeStyle the style pattern for the time
     * @param obj the date to be formatted
     * @return a formatted representation of the given date 
     * @see #format(String dateStyle, String timeStyle, Object obj, Locale locale, TimeZone timezone)
     * @since VelocityTools 1.1
     */
    public String format(String dateStyle, String timeStyle, Object obj)
    {
        return format(dateStyle, timeStyle, obj, getLocale());
    }

    /**
     * Returns the specified date as a string formatted according to the
     * specified {@link Locale} and date and/or time styles.
     *
     * @param dateStyle the style pattern for the date
     * @param timeStyle the style pattern for the time
     * @param obj the date to be formatted
     * @param locale the {@link Locale} to be used for formatting the date
     * @return a formatted representation of the given date 
     * @see #format(String dateStyle, String timeStyle, Object obj, Locale locale, TimeZone timezone)
     * @since VelocityTools 1.1
     */
    public String format(String dateStyle, String timeStyle,
                         Object obj, Locale locale)
    {
        return format(dateStyle, timeStyle, obj, locale, getTimeZone());
    }

    /**
     * Returns the specified date as a string formatted according to the
     * specified {@link Locale} and date and/or time styles.
     *
     * @param dateStyle the style pattern for the date
     * @param timeStyle the style pattern for the time
     * @param obj the date to be formatted
     * @param locale the {@link Locale} to be used for formatting the date
     * @param timezone the {@link TimeZone} the date should be formatted for
     * @return a formatted representation of the given date 
     * @see java.text.DateFormat
     * @see #format(String dateStyle, String timeStyle, Object obj, Locale locale, TimeZone timezone)
     * @since VelocityTools 1.1
     */
    public String format(String dateStyle, String timeStyle,
                         Object obj, Locale locale, TimeZone timezone)
    {
        Date date = toDate(obj);
        DateFormat df = getDateFormat(dateStyle, timeStyle, locale, timezone);
        if (date == null || df == null)
        {
            return null;
        }
        return df.format(date);
    }


    // -------------------------- DateFormat creation methods --------------

    /**
     * Returns a {@link DateFormat} instance for the specified
     * format, {@link Locale}, and {@link TimeZone}.  If the format
     * specified is a standard style pattern, then a date-time instance
     * will be returned with both the date and time styles set to the 
     * specified style.  If it is a custom format, then a customized
     * {@link SimpleDateFormat} will be returned.
     * 
     * @param format the custom or standard formatting pattern to be used
     * @param locale the {@link Locale} to be used
     * @param timezone the {@link TimeZone} to be used
     * @return an instance of {@link DateFormat}
     * @see SimpleDateFormat
     * @see DateFormat
     * @since VelocityTools 1.1
     */
    public DateFormat getDateFormat(String format, Locale locale, 
                                    TimeZone timezone)
    {
        if (format == null)
        {
            return null;
        }

        DateFormat df = null;
        // do they want a date instance
        if (format.endsWith("_date"))
        {
            String fmt = format.substring(0, format.length() - 5);
            int style = getStyleAsInt(fmt);
            df = getDateFormat(style, -1, locale, timezone);
        }
        // do they want a time instance?
        else if (format.endsWith("_time"))
        {
            String fmt = format.substring(0, format.length() - 5);
            int style = getStyleAsInt(fmt);
            df = getDateFormat(-1, style, locale, timezone);
        }
        // ok, they either want a custom or date-time instance
        else
        {
            int style = getStyleAsInt(format);
            if (style < 0)
            {
                // we have a custom format
                df = new SimpleDateFormat(format, locale);
                df.setTimeZone(timezone);
            }
            else
            {
                // they want a date-time instance
                df = getDateFormat(style, style, locale, timezone);
            }
        }
        return df;
    }

    /**
     * Returns a {@link DateFormat} instance for the specified
     * date style, time style, {@link Locale}, and {@link TimeZone}.
     * 
     * @param dateStyle the date style 
     * @param timeStyle the time style 
     * @param locale the {@link Locale} to be used
     * @param timezone the {@link TimeZone} to be used
     * @return an instance of {@link DateFormat}
     * @see #getDateFormat(int timeStyle, int dateStyle, Locale locale, TimeZone timezone)
     * @since VelocityTools 1.1
     */
    public DateFormat getDateFormat(String dateStyle, String timeStyle,
                                    Locale locale, TimeZone timezone)
    {
        int ds = getStyleAsInt(dateStyle);
        int ts = getStyleAsInt(timeStyle);
        return getDateFormat(ds, ts, locale, timezone);
    }

    /**
     * Returns a {@link DateFormat} instance for the specified
     * time style, date style, {@link Locale}, and {@link TimeZone}.
     * 
     * @param dateStyle the date style (date will be ignored if this is
     *        less than zero and the date style is not)
     * @param timeStyle the time style (time will be ignored if this is
     *        less than zero and the date style is not)
     * @param locale the {@link Locale} to be used
     * @param timezone the {@link TimeZone} to be used
     * @return an instance of {@link DateFormat} or <code>null</code>
     *         if an instance cannot be constructed with the given
     *         parameters
     * @since VelocityTools 1.1
     */
    protected DateFormat getDateFormat(int dateStyle, int timeStyle, 
                                       Locale locale, TimeZone timezone)
    {
        try
        {
            DateFormat df;
            if (dateStyle < 0 && timeStyle < 0)
            {
                // no style was specified, use default instance
                df = DateFormat.getInstance();
            }
            else if (timeStyle < 0)
            {
                // only a date style was specified
                df = DateFormat.getDateInstance(dateStyle, locale);
            }
            else if (dateStyle < 0)
            {
                // only a time style was specified
                df = DateFormat.getTimeInstance(timeStyle, locale);
            }
            else
            {
                df = DateFormat.getDateTimeInstance(dateStyle, timeStyle, 
                                                    locale);
            }
            df.setTimeZone(timezone);
            return df;
        }
        catch (Exception suppressed)
        {
            // let it go...
            return null;
        }
    }

    /**
     * Checks a string to see if it matches one of the standard DateFormat
     * style patterns: FULL, LONG, MEDIUM, SHORT, or DEFAULT.  If it does,
     * it will return the integer constant for that pattern.  If not, it
     * will return -1.
     *
     * @see DateFormat
     * @param style the string to be checked
     * @return the int identifying the style pattern
     * @since VelocityTools 1.1
     */
    protected int getStyleAsInt(String style)
    {
        // avoid needlessly running through all the string comparisons
        if (style == null || style.length() < 4 || style.length() > 7) {
            return -1;
        }
        if (style.equalsIgnoreCase("full"))
        {
            return DateFormat.FULL;
        }
        if (style.equalsIgnoreCase("long"))
        {
            return DateFormat.LONG;
        }
        if (style.equalsIgnoreCase("medium"))
        {
            return DateFormat.MEDIUM;
        }
        if (style.equalsIgnoreCase("short"))
        {
            return DateFormat.SHORT;
        }
        if (style.equalsIgnoreCase("default"))
        {
            return DateFormat.DEFAULT;
        }
        // ok, it's not any of the standard patterns
        return -1;
    }


    // ------------------------- date conversion methods ---------------

    /**
     * Converts an object to an instance of {@link Date} using the
     * format returned by {@link #getFormat()},the {@link Locale} returned 
     * by {@link #getLocale()}, and the {@link TimeZone} returned by
     * {@link #getTimeZone()} if the object is not already an instance
     * of Date, Calendar, or Long.
     *
     * @param obj the date to convert
     * @return the object as a {@link Date} or <code>null</code> if no
     *         conversion is possible
     */
    public Date toDate(Object obj)
    {
        return toDate(getFormat(), obj, getLocale(), getTimeZone());
    }

    /**
     * Converts an object to an instance of {@link Date} using the
     * specified format,the {@link Locale} returned by 
     * {@link #getLocale()}, and the {@link TimeZone} returned by
     * {@link #getTimeZone()} if the object is not already an instance
     * of Date, Calendar, or Long.
     *
     * @param format - the format the date is in
     * @param obj - the date to convert
     * @return the object as a {@link Date} or <code>null</code> if no
     *         conversion is possible
     * @see #toDate(String format, Object obj, Locale locale)
     */
    public Date toDate(String format, Object obj)
    {
        return toDate(format, obj, getLocale(), getTimeZone());
    }

    /**
     * Converts an object to an instance of {@link Date} using the
     * specified format and {@link Locale} if the object is not already
     * an instance of Date, Calendar, or Long.
     *
     * @param format - the format the date is in
     * @param obj - the date to convert
     * @param locale - the {@link Locale}
     * @return the object as a {@link Date} or <code>null</code> if no
     *         conversion is possible
     * @see SimpleDateFormat#parse
     */
    public Date toDate(String format, Object obj, Locale locale)
    {
        return toDate(format, obj, locale, getTimeZone());
    }

    /**
     * Converts an object to an instance of {@link Date} using the
     * specified format, {@link Locale}, and {@link TimeZone} if the 
     * object is not already an instance of Date, Calendar, or Long.
     *
     * @param format - the format the date is in
     * @param obj - the date to convert
     * @param locale - the {@link Locale}
     * @param timezone - the {@link TimeZone}
     * @return the object as a {@link Date} or <code>null</code> if no
     *         conversion is possible
     * @see #getDateFormat
     * @see SimpleDateFormat#parse
     */
    public Date toDate(String format, Object obj, 
                       Locale locale, TimeZone timezone)
    {
        if (obj == null)
        {
            return null;
        }
        if (obj instanceof Date)
        {
            return (Date)obj;
        }
        if (obj instanceof Calendar)
        {
            return ((Calendar)obj).getTime();
        }
        if (obj instanceof Number) 
        {
            Date d = new Date();
            d.setTime(((Number)obj).longValue());
            return d;
        }
        try
        {
            //try parsing w/a customized SimpleDateFormat
            DateFormat parser = getDateFormat(format, locale, timezone);
            return parser.parse(String.valueOf(obj));
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Converts an object to an instance of {@link Calendar} using the
     * locale returned by {@link #getLocale()} if necessary.
     *
     * @param obj the date to convert
     * @return the converted date
     * @see #toCalendar(Object obj, Locale locale)
     */
    public Calendar toCalendar(Object obj)
    {
        return toCalendar(obj, getLocale());
    }

    /**
     * Converts an object to an instance of {@link Calendar} using the
     * locale returned by {@link #getLocale()} if necessary.
     *
     * @param obj the date to convert
     * @param locale the locale used
     * @return the converted date
     * @see #toDate(String format, Object obj, Locale locale)
     * @see Calendar
     */
    public Calendar toCalendar(Object obj, Locale locale)
    {
        if (obj == null)
        {
            return null;
        }
        if (obj instanceof Calendar)
        {
            return (Calendar)obj;
        }
        //try to get a date out of it
        Date date = toDate(obj);
        if (date == null)
        {
            return null;
        }

        //convert the date to a calendar
        Calendar cal = Calendar.getInstance(locale);
        cal.setTime(date);
        // HACK: Force all fields to update. see link for explanation of this.
        //http://java.sun.com/j2se/1.4/docs/api/java/util/Calendar.html
        cal.getTime();
        return cal;
    }


    // ------------------------- default toString() implementation ------------

    /**
     * @return the result of {@link #getDate()} formatted according to the result
     *         of {@link #getFormat()}.
     * @see #format(String format, Object obj)
     */
    public String toString()
    {
        return format(getFormat(), getDate());
    }


}
