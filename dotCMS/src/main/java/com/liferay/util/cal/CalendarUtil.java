/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.util.cal;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import com.liferay.util.SimpleCachePool;
import com.liferay.util.Validator;

/**
 * <a href="CalendarUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.15 $
 *
 */
public class CalendarUtil {

	public static int[] MONTH_IDS = new int[] {
		Calendar.JANUARY, Calendar.FEBRUARY, Calendar.MARCH, Calendar.APRIL,
		Calendar.MAY, Calendar.JUNE, Calendar.JULY, Calendar.AUGUST,
		Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER,
		Calendar.DECEMBER
	};

	public static boolean afterByDay(Date date1, Date date2) {
		long millis1 = date1.getTime() / 86400000;
		long millis2 = date2.getTime() / 86400000;

		if (millis1 > millis2) {
			return true;
		}
		else {
			return false;
		}
	}

	public static boolean beforeByDay(Date date1, Date date2) {
		long millis1 = date1.getTime() / 86400000;
		long millis2 = date2.getTime() / 86400000;

		if (millis1 < millis2) {
			return true;
		}
		else {
			return false;
		}
	}

	public static boolean equalsByDay(Date date1, Date date2) {
		long millis1 = date1.getTime() / 86400000;
		long millis2 = date2.getTime() / 86400000;

		if (millis1 == millis2) {
			return true;
		}
		else {
			return false;
		}
	}

	public static int getAge(Date date, TimeZone tz) {
		return getAge(date, new GregorianCalendar(tz));
	}

	public static int getAge(Date date, Calendar today) {
		Calendar birthday = new GregorianCalendar();
		birthday.setTime(date);

		int yearDiff = today.get(Calendar.YEAR) - birthday.get(Calendar.YEAR);

		if (today.get(Calendar.MONTH) < birthday.get(Calendar.MONTH)) {
			yearDiff--;
		}
		else if (today.get(Calendar.MONTH) == birthday.get(Calendar.MONTH) &&
				 today.get(Calendar.DATE) < birthday.get(Calendar.DATE)) {

			yearDiff--;
		}

		return yearDiff;
	}

	public static Date getGTDate(Calendar cal) {
		Calendar gtCal = (Calendar)cal.clone();

		gtCal.set(Calendar.HOUR_OF_DAY, 0);
		gtCal.set(Calendar.MINUTE, 0);
		gtCal.set(Calendar.SECOND, 0);
		gtCal.set(Calendar.MILLISECOND, 0);

		return gtCal.getTime();
	}

	public static Date getLTDate(Calendar cal) {
		Calendar ltCal = (Calendar)cal.clone();

		ltCal.set(Calendar.HOUR_OF_DAY, 23);
		ltCal.set(Calendar.MINUTE, 59);
		ltCal.set(Calendar.SECOND, 59);
		ltCal.set(Calendar.MILLISECOND, 999);

		return ltCal.getTime();
	}

	public static int getDaysInMonth(Calendar cal) {
		return getDaysInMonth(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR));
	}

	public static int getDaysInMonth(int month, int year) {
		month++;

		if ((month == 1) || (month == 3) || (month == 5) || (month == 7) ||
			(month == 8) || (month == 10) || (month == 12)) {

			return 31;
		}
		else if ((month == 4) || (month == 6) || (month == 9) ||
				 (month == 11)) {

			return 30;
		}
		else {
			if (((year % 4) == 0) &&
				((year % 100) != 0) || ((year % 400) == 0)) {

				return 29;
			}
			else {
				return 28;
			}
		}
	}

	public static int getLastDayOfWeek(Calendar cal) {
		int firstDayOfWeek = cal.getFirstDayOfWeek();

		if (firstDayOfWeek == Calendar.SUNDAY) {
			return Calendar.SATURDAY;
		}
		else if (firstDayOfWeek == Calendar.MONDAY) {
			return Calendar.SUNDAY;
		}
		else if (firstDayOfWeek == Calendar.TUESDAY) {
			return Calendar.MONDAY;
		}
		else if (firstDayOfWeek == Calendar.WEDNESDAY) {
			return Calendar.TUESDAY;
		}
		else if (firstDayOfWeek == Calendar.THURSDAY) {
			return Calendar.WEDNESDAY;
		}
		else if (firstDayOfWeek == Calendar.FRIDAY) {
			return Calendar.THURSDAY;
		}

		return Calendar.FRIDAY;
	}

	public static String[] getDays(Locale locale) {
		return getDays(locale, null);
	}

	public static String[] getDays(Locale locale, String pattern) {
		if (Validator.isNull(pattern)) {
			pattern = "EEEE";
		}

		StringBuffer sb = new StringBuffer();
		sb.append("util-java.days_");
		sb.append(pattern);
		sb.append("_");
		sb.append(locale.getLanguage());
		sb.append("_");
		sb.append(locale.getCountry());

		String id = sb.toString();

		String[] days = (String[])SimpleCachePool.get(id);

		if (days == null) {
			days = new String[7];

			DateFormat dayFormat = new SimpleDateFormat(pattern, locale);

			Calendar cal = new GregorianCalendar();
			cal.set(Calendar.DATE, 1);

			for (int i = 0; i < 7; i++) {
				cal.set(Calendar.DAY_OF_WEEK, i + 1);
				days[i] = dayFormat.format(cal.getTime());
			}

			SimpleCachePool.put(id, days);
		}

		return days;
	}

	public static int getGregorianDay(Calendar cal) {
		int year = cal.get(Calendar.YEAR) - 1600;

		int month = cal.get(Calendar.MONTH) + 1;
		if (month < 3) {
			month += 12;
		}

		int day = cal.get(Calendar.DATE);

		int gregorianDay =
			(int)(6286 + (year * 365.25) - (year / 100) + (year / 400) +
				(30.6 * month) + 0.2 + day);

		return gregorianDay;
	}

	public static int[] getMonthIds() {
		return MONTH_IDS;
	}

	public static String[] getMonths(Locale locale) {
		return getMonths(locale, null);
	}

	public static String[] getMonths(Locale locale, String pattern) {
		if (Validator.isNull(pattern)) {
			pattern = "MMMM";
		}

		StringBuffer sb = new StringBuffer();
		sb.append("util-java.months_");
		sb.append(pattern);
		sb.append("_");
		sb.append(locale.getLanguage());
		sb.append("_");
		sb.append(locale.getCountry());

		String id = sb.toString();

		String[] months = (String[])SimpleCachePool.get(id);

		if (months == null) {
			months = new String[12];

			DateFormat monthFormat = new SimpleDateFormat(pattern, locale);

			Calendar cal = new GregorianCalendar();
			cal.set(Calendar.DATE, 1);

			for (int i = 0; i < 12; i++) {
				cal.set(Calendar.MONTH, i);
				months[i] = monthFormat.format(cal.getTime());
			}

			SimpleCachePool.put(id, months);
		}

		return months;
	}

	public static boolean isAfter(int month1, int day1, int year1,
								  int hour1, int minute1, int amPm1,
								  int month2, int day2, int year2,
								  int hour2, int minute2, int amPm2,
								  TimeZone timeZone, Locale locale) {

		Calendar cal1 = new GregorianCalendar(timeZone, locale);
		cal1.set(Calendar.MONTH, month1);
		cal1.set(Calendar.DATE, day1);
		cal1.set(Calendar.YEAR, year1);
		cal1.set(Calendar.HOUR, hour1);
		cal1.set(Calendar.MINUTE, minute1);
		cal1.set(Calendar.AM_PM, amPm1);

		Calendar cal2 = new GregorianCalendar(timeZone, locale);
		cal2.set(Calendar.MONTH, month2);
		cal2.set(Calendar.DATE, day2);
		cal2.set(Calendar.YEAR, year2);
		cal2.set(Calendar.HOUR, hour2);
		cal2.set(Calendar.MINUTE, minute2);
		cal2.set(Calendar.AM_PM, amPm2);

		return cal1.after(cal2);
	}

	public static boolean isBroadcastDate(int month, int day, int year) {
		if (!isDate(month, day, year)) {
			return false;
		}

		Calendar cal1 = new GregorianCalendar();
		cal1.setFirstDayOfWeek(Calendar.MONDAY);
		cal1.set(Calendar.MONTH, month);
		cal1.set(Calendar.DATE, day);
		cal1.set(Calendar.YEAR, year);

		Calendar cal2 = new GregorianCalendar();
		cal2.setFirstDayOfWeek(Calendar.MONDAY);
		cal2.set(Calendar.MONTH, month + 1);
		cal2.set(Calendar.DATE, 1);
		cal2.set(Calendar.YEAR, year);

		if ((cal2.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) &&
			(cal2.get(Calendar.WEEK_OF_YEAR) == cal1.get(
				Calendar.WEEK_OF_YEAR))) {
			return false;
		}

		return true;
	}

	public static boolean isDate(int month, int day, int year) {
		return isGregorianDate(month, day, year);
	}

	public static boolean isFuture(int month, int year) {
		return isFuture(
			month, year, TimeZone.getDefault(), Locale.getDefault());
	}

	public static boolean isFuture(int month, int year, TimeZone timeZone,
								   Locale locale) {

		Calendar curCal = new GregorianCalendar(timeZone, locale);
		curCal.set(Calendar.DATE, 1);

		Calendar cal = (Calendar)curCal.clone();
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.YEAR, year);

		return cal.after(curCal);
	}

	public static boolean isFuture(int month, int day, int year) {
		return isFuture(
			month, day, year, TimeZone.getDefault(), Locale.getDefault());
	}

	public static boolean isFuture(int month, int day, int year,
								   TimeZone timeZone, Locale locale) {

		Calendar curCal = new GregorianCalendar(timeZone, locale);

		Calendar cal = (Calendar)curCal.clone();
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DATE, day);
		cal.set(Calendar.YEAR, year);

		return cal.after(curCal);
	}

	public static boolean isFuture(int month, int day, int year, int hour,
								   int minute, int amPm) {

		return isFuture(
			month, day, year, hour, minute, amPm, TimeZone.getDefault(),
			Locale.getDefault());
	}

	public static boolean isFuture(int month, int day, int year,
								   int hour, int minute, int amPm,
								   TimeZone timeZone, Locale locale) {

		Calendar curCal = new GregorianCalendar(timeZone, locale);

		Calendar cal = (Calendar)curCal.clone();
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DATE, day);
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.HOUR, hour);
		cal.set(Calendar.MINUTE, minute);
		cal.set(Calendar.AM_PM, amPm);

		return cal.after(curCal);
	}

	public static boolean isGregorianDate(int month, int day, int year) {
		if ((month < 0) || (month > 11)) {
			return false;
		}

		int[] months = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

		if (month == 1) {
			int febMax = 28;

			if (((year % 4) == 0) && ((year % 100) != 0) ||
				((year % 400) == 0)) {

				febMax = 29;
			}

			if ((day < 0) || (day > febMax)) {
				return false;
			}
		}
		else if ((day < 0) || (day > months[month])) {
			return false;
		}

		return true;
	}

	public static boolean isJulianDate(int month, int day, int year) {
		if ((month < 0) || (month > 11)) {
			return false;
		}

		int[] months = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

		if (month == 1) {
			int febMax = 28;

			if ((year % 4) == 0) {
				febMax = 29;
			}

			if ((day < 0) || (day > febMax)) {
				return false;
			}
		}
		else if ((day < 0) || (day > months[month])) {
			return false;
		}

		return true;
	}

	public static Calendar roundByMinutes(Calendar cal, int interval) {
		int minutes = cal.get(Calendar.MINUTE);

		int delta = 0;

		if (minutes < interval) {
			delta = interval - minutes;
		}
		else {
			delta = interval - (minutes % interval);
		}

		if (delta == interval) {
			delta = 0;
		}

		cal.add(Calendar.MINUTE, delta);

		return cal;
	}

}