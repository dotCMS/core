package com.dotmarketing.util;

import static com.dotcms.util.DotPreconditions.checkNotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;

/**
 * Provides utility methods to interact with {@link Date} objects, date formats,
 * transformation of time units, etc.
 * 
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 *
 */
public class DateUtil {

	public static final long SECOND_MILLIS = 1000;
	public static final long MINUTE_MILLIS = 60 * SECOND_MILLIS;
	public static final long HOUR_MILLIS   = 60 * MINUTE_MILLIS;

	public static final String DIFF_YEARS = "diffYears";
	public static final String DIFF_MONTHS = "diffMonths";
	public static final String DIFF_DAYS = "diffDays";
	public static final String DIFF_HOURS = "diffHours";
	public static final String DIFF_MINUTES = "diffMinutes";

	private static Map<String, DateTimeFormatter> formatterMap = new ConcurrentHashMap<>();


	/**
	 * This method allows you to add to a java.util.Date returning a Date
	 * instead of void like the Calendar does
	 *
	 * @param date
	 *            - The date to modify
	 * @param calendarField
	 *            - The static field from java.util.Calendar to add
	 * @param numberToAdd
	 *            - The number to add
	 * @return Date
	 */
	public static Date addDate(final Date date,
							   final int calendarField,
							   final int numberToAdd) {

		final Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(calendarField, numberToAdd);
		return c.getTime();
	}

	/**
	 * This method will set the time on a date to 00:00:00
	 *
	 * @param date - 
	 * @return Date
	 */
	public static Date minTime(final Date date) {
		final Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.set(Calendar.HOUR, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.AM_PM, Calendar.AM);
		return c.getTime();
	}

	/**
	 * This method will set the time on a date to 23:59:59
	 *
	 * @param date - 
	 * @return Date
	 */
	public static Date maxTime(final Date date) {
		final Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.set(Calendar.HOUR, 11);
		c.set(Calendar.MINUTE, 59);
		c.set(Calendar.SECOND, 59);
		c.set(Calendar.AM_PM, Calendar.PM);
		return c.getTime();
	}

	/**
	 * This method will return the diff between to dates
	 *
	 * @param from
	 *            - Date
	 * @param to
	 *            - Date
	 * @return HashMap
	 * 		- {@link DateUtil}.DIFF_YEARS
	 *		- {@link DateUtil}.DIFF_MONTHS
	 *		- {@link DateUtil}.DIFF_DAYS
	 *		- {@link DateUtil}.DIFF_HOURS
	 *		- {@link DateUtil}.DIFF_MINUTES
	 */
	public static HashMap<String, Long> diffDates(final Date from, final Date to) {
		final HashMap<String, Long> result = new HashMap<String, Long>(8);

		try {

			final Calendar toCal = new GregorianCalendar();
			toCal.setTime(to);
			final Calendar fromCal = new GregorianCalendar();
			fromCal.setTime(from);
			long diffYears = 0;
			long diffMonths = -1;
			int fromMonth = fromCal.get(Calendar.MONTH);
			int currentMonth;
			for (; fromCal.before(toCal);) {
				fromCal.add(Calendar.MONTH, 1);
				++diffMonths;
				currentMonth = fromCal.get(Calendar.MONTH);
				if (currentMonth == fromMonth)
					++diffYears;
			}

			result.put(DIFF_YEARS, diffYears);
			result.put(DIFF_MONTHS, diffMonths);

			final long milliSecondDiff = to.getTime() - from.getTime();
			final long diffDays = milliSecondDiff / (24 * 3600 * 1000);
			final long diffHours = milliSecondDiff / (3600 * 1000);
			final long timeLeft = milliSecondDiff % (3600 * 1000);
			final long diffMinutes = timeLeft / (60 * 1000);

			result.put(DIFF_DAYS, diffDays);
			result.put(DIFF_HOURS, diffHours);
			result.put(DIFF_MINUTES, diffMinutes);
		} catch (Exception e) {
			Logger.warn(DateUtil.class, e.toString());
		}
		return result;
	}

	/**
	 * This method try to parse a string into a Date object using an array with
	 * the valid formats
	 *
	 * @param date
	 *            - the string to be parsed
	 * @param formats
	 *            - the valid format to parse the string
	 * @return return the Date object that represent the string
	 * @throws java.text.ParseException
	 */
	public static Date convertDate(final String date,
								   final String[] formats) throws java.text.ParseException {
		Date ret = null;
		for (String pattern : formats) { // todo: usually the formaters are cached and thread-safe
			try {
				ret = new SimpleDateFormat(pattern).parse(date);
				break;
			} catch (java.text.ParseException e) {
			}
		}
		if (ret == null)
			throw new java.text.ParseException(date, 0);

		return ret;
	}

	/**
	 * This method takes a Date and the desired format to convert it to a String
	 */
	public static String formatDate(final Date date,
									final String format) {

		return new SimpleDateFormat(format).format(date);
	}

	/**
	 * Pretty format for a date using as to Date the current Date.
	 * For instance if the locale is english, and the distance between today and the date parameter is more than a year, a message such as will be returned:
	 *
	 * "more than a year ago"
	 *
	 * The same for months, days, etc.
	 *
	 * see {@link DateUtilTest} to check more about it
	 *
	 * @param date
	 * @param locale
	 * @return String
	 */
	public static String prettyDateSince(final Date date, final Locale locale) {

		return prettyDateSince(date, locale, new Date());
	}
	/**
	 * Pretty format for a date using as to date the toDate parameter
	 * For instance if the locale is english, and the distance between today and the date parameter is more than a year, a message such as will be returned:
	 *
	 * "more than a year ago"
	 *
	 * The same for months, days, etc.
	 *
	 * see {@link DateUtilTest} to check more about it
	 *
	 * @param fromDate
	 * @param locale
	 * @param toDate
	 * @return String
	 */
	public static String prettyDateSince(final Date fromDate,
										 Locale locale,
										 final Date toDate) {

		if (locale == null) {
			Company company = PublicCompanyFactory.getDefaultCompany();
			locale = company.getLocale();
		}
		String sinceMessage = null;

		try {

			HashMap<String, Long> diffDates = DateUtil.diffDates(fromDate, toDate);
			if (0 < diffDates.get(DateUtil.DIFF_YEARS)) {
				sinceMessage = LanguageUtil.get(locale, "more-than-a-year-ago");
			} else if (1 < diffDates.get(DateUtil.DIFF_MONTHS)) {
				sinceMessage = LanguageUtil.format(locale, "x-months-ago", diffDates.get(DateUtil.DIFF_MONTHS), false);

			} else if (1 == diffDates.get(DateUtil.DIFF_MONTHS)) {
				sinceMessage = LanguageUtil.get(locale, "last-month");

			} else if (13 < diffDates.get(DateUtil.DIFF_DAYS)) {
				sinceMessage =
						LanguageUtil.format(locale, "x-weeks-ago", new Double(Math.floor(diffDates.get(DateUtil.DIFF_DAYS) / 7)).intValue());

			} else if (6 < diffDates.get(DateUtil.DIFF_DAYS)) {
				sinceMessage = LanguageUtil.get(locale, "last-week");

			} else if (1 < diffDates.get(DateUtil.DIFF_DAYS)) {
				sinceMessage = LanguageUtil.format(locale, "x-days-ago",diffDates.get(DateUtil.DIFF_DAYS) );

			} else if (23 < diffDates.get(DateUtil.DIFF_HOURS)) {
				sinceMessage = LanguageUtil.get(locale, "yesterday");
			} else if (1 < diffDates.get(DateUtil.DIFF_HOURS)) {
				sinceMessage =  LanguageUtil.format(locale, "x-hours-ago", diffDates.get(DateUtil.DIFF_HOURS) );
			} else if (1 == diffDates.get(DateUtil.DIFF_HOURS)) {
				sinceMessage = LanguageUtil.get(locale, "an-hour-ago");
			} else if (diffDates.get(DateUtil.DIFF_MINUTES) > 1) {
				sinceMessage =  LanguageUtil.format(locale, "x-minutes-ago", diffDates.get(DateUtil.DIFF_MINUTES) );
			} else {
				sinceMessage = LanguageUtil.get(locale, "seconds-ago");
			}
		} catch (Exception e) {

		}
		return sinceMessage;
	}

	/**
	 * 
	 * @param date
	 * @return
	 */
	public static String prettyDateSince(Date date) {
		return prettyDateSince(date, null);
	}

	/**
	 * 
	 * @param date
	 * @param locale
	 * @return
	 */
	public static String prettyDateSinceWithDate(Date date, Locale locale) {
		if (locale == null) {
			Company company = PublicCompanyFactory.getDefaultCompany();
			locale = company.getLocale();
		}
		SimpleDateFormat sdf = new SimpleDateFormat("h:mm aa, MMMM d?, yyyy", locale);
		String modDate = sdf.format(date);
		modDate = modDate.replaceAll("\\?", "th");

		return prettyDateSince(date, locale) + " (" + modDate + ")";

	}

	/**
	 * 
	 * @param date
	 * @return
	 */
	public static String prettyDateSinceWithDate(Date date) {
		return prettyDateSinceWithDate(date, null);
	}

	/**
	 * 
	 * @return
	 */
	public static String getCurrentDate() { // todo: this should be configurable for all the app (the default format date)
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date now = Calendar.getInstance().getTime();
		String date = df.format(now);
		return date;
	}

	/**
	 * Converts the specified number of days to milliseconds.
	 * 
	 * @param days
	 *            - The number of days.
	 * @return The number of days transformed to milliseconds.
	 */
	public static long daysToMillis(int days) {
		return days * 24 * 3600 * 1000L;
	}

	/**
	 * Converts millis to seconds
	 * @param time long
	 * @return int
     */
	// todo: test it
	public static int millisToSeconds(final long time) {

		return (int) (time / SECOND_MILLIS);
	}

	/**
	 * Formats a date object to a custom representation using the default locale
	 * @param date date value
	 * @param customFormat format pattern
	 * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html">Valid formats</a>
	 * @return formated text value
	 */
	public static String format(final Date date, final String customFormat){
		checkNotNull(date);
		checkNotNull(customFormat);
		LocalDateTime localDate = Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
		return format(localDate,customFormat);
	}

	/**
	 * Formats a date object to a custom representation and the locale
	 * @param date date value
	 * @param customFormat format pattern
	 * @param locale locale information
	 * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html">Valid formats</a>
	 * @return formated text value
	 */
	public static String format(final Date date, final String customFormat, Locale locale){
		checkNotNull(date);
		checkNotNull(customFormat);
		checkNotNull(locale);
		LocalDateTime localDate = Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
		return format(localDate, customFormat, locale);
	}

	/**
	 * Formats a date object to a custom representation using the default locale
	 * @param date date value
	 * @param customFormat format pattern
	 * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html">Valid formats</a>
	 * @return formated text value
	 */
	public static String format(final LocalDateTime date, final String customFormat) {
		checkNotNull(date);
		checkNotNull(customFormat);
		return format(date,customFormat,Locale.getDefault());
	}

	/**
	 * Formats a date object to a custom representation and the locale
	 * @param date date value
	 * @param customFormat format pattern
	 * @param locale locale information
	 * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html">Valid formats</a>
	 * @return formated text value
	 */
	public static String format(final LocalDateTime date, final String customFormat, Locale locale) {
		checkNotNull(date);
		checkNotNull(customFormat);
		checkNotNull(locale);
		String dateString = null;
		try {
			if (!formatterMap.containsKey(customFormat)) {
				formatterMap.put(customFormat, DateTimeFormatter.ofPattern(customFormat,locale));
			}
			DateTimeFormatter formatter = formatterMap.get(customFormat);
			dateString = formatter.format(date);
		} catch (Exception e) {
			dateString = StringUtils.EMPTY;
		}
		return dateString;
	}
}
