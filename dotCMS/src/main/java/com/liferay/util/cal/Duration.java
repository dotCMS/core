/*
 * Copyright (c) 2000, Columbia University.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS
 * IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.liferay.util.cal;

import java.io.Serializable;

/**
 * <a href="Duration.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Jonathan Lennox
 * @version $Revision: 1.2 $
 *
 */
public class Duration implements Cloneable, Serializable {

	/**
	 * Field weeks
	 */
	private int weeks;

	/**
	 * Field days
	 */
	private int days;

	/**
	 * Field hours
	 */
	private int hours;

	/**
	 * Field minutes
	 */
	private int minutes;

	/**
	 * Field seconds
	 */
	private int seconds;

	/**
	 * Field SECONDS_PER_MINUTE
	 */
	private final static int SECONDS_PER_MINUTE = 60;

	/**
	 * Field MINUTES_PER_HOUR
	 */
	private final static int MINUTES_PER_HOUR = 60;

	/**
	 * Field HOURS_PER_DAY
	 */
	private final static int HOURS_PER_DAY = 24;

	/**
	 * Field DAYS_PER_WEEK
	 */
	private final static int DAYS_PER_WEEK = 7;

	/**
	 * Field MILLIS_PER_SECOND
	 */
	private final static int MILLIS_PER_SECOND = 1000;

	/**
	 * Field MILLIS_PER_MINUTE
	 */
	private final static int MILLIS_PER_MINUTE = SECONDS_PER_MINUTE
												 * MILLIS_PER_SECOND;

	/**
	 * Field MILLIS_PER_HOUR
	 */
	private final static int MILLIS_PER_HOUR = MINUTES_PER_HOUR
											   * MILLIS_PER_MINUTE;

	/**
	 * Field MILLIS_PER_DAY
	 */
	private final static int MILLIS_PER_DAY = HOURS_PER_DAY * MILLIS_PER_HOUR;

	/**
	 * Field MILLIS_PER_WEEK
	 */
	private final static int MILLIS_PER_WEEK = DAYS_PER_WEEK * MILLIS_PER_DAY;

	/**
	 * Constructor Duration
	 *
	 *
	 */
	public Duration() {

		/* Zero-initialization of all fields happens by default */

	}

	/**
	 * Constructor Duration
	 *
	 *
	 * @param	d
	 * @param	h
	 * @param	m
	 * @param	s
	 *
	 */
	public Duration(int d, int h, int m, int s) {
		days = d;
		hours = h;
		minutes = m;
		seconds = s;
	}

	/**
	 * Constructor Duration
	 *
	 *
	 * @param	h
	 * @param	m
	 * @param	s
	 *
	 */
	public Duration(int h, int m, int s) {
		this(0, h, m, s);
	}

	/**
	 * Constructor Duration
	 *
	 *
	 * @param	w
	 *
	 */
	public Duration(int w) {
		weeks = w;
	}

	/**
	 * Method clear
	 *
	 *
	 */
	public void clear() {
		weeks = 0;
		days = 0;
		hours = 0;
		minutes = 0;
		seconds = 0;
	}
	;

	/**
	 * Method getWeeks
	 *
	 *
	 * @return	int
	 *
	 */
	public int getWeeks() {
		return weeks;
	}

	/**
	 * Method setWeeks
	 *
	 *
	 * @param	w
	 *
	 */
	public void setWeeks(int w) {
		if (w < 0) {
			throw new IllegalArgumentException("Week value out of range");
		}

		checkWeeksOkay(w);

		weeks = w;
	}

	/**
	 * Method getDays
	 *
	 *
	 * @return	int
	 *
	 */
	public int getDays() {
		return days;
	}

	/**
	 * Method setDays
	 *
	 *
	 * @param	d
	 *
	 */
	public void setDays(int d) {
		if (d < 0) {
			throw new IllegalArgumentException("Day value out of range");
		}

		checkNonWeeksOkay(d);

		days = d;

		normalize();
	}

	/**
	 * Method getHours
	 *
	 *
	 * @return	int
	 *
	 */
	public int getHours() {
		return hours;
	}

	/**
	 * Method setHours
	 *
	 *
	 * @param	h
	 *
	 */
	public void setHours(int h) {
		if (h < 0) {
			throw new IllegalArgumentException("Hour value out of range");
		}

		checkNonWeeksOkay(h);

		hours = h;

		normalize();
	}

	/**
	 * Method getMinutes
	 *
	 *
	 * @return	int
	 *
	 */
	public int getMinutes() {
		return minutes;
	}

	/**
	 * Method setMinutes
	 *
	 *
	 * @param	m
	 *
	 */
	public void setMinutes(int m) {
		if (m < 0) {
			throw new IllegalArgumentException("Minute value out of range");
		}

		checkNonWeeksOkay(m);

		minutes = m;

		normalize();
	}

	/**
	 * Method getSeconds
	 *
	 *
	 * @return	int
	 *
	 */
	public int getSeconds() {
		return seconds;
	}

	/**
	 * Method setSeconds
	 *
	 *
	 * @param	s
	 *
	 */
	public void setSeconds(int s) {
		if (s < 0) {
			throw new IllegalArgumentException("Second value out of range");
		}

		checkNonWeeksOkay(s);

		seconds = s;

		normalize();
	}

	/**
	 * Method getInterval
	 *
	 *
	 * @return	long
	 *
	 */
	public long getInterval() {
		return seconds * MILLIS_PER_SECOND + minutes * MILLIS_PER_MINUTE
			   + hours * MILLIS_PER_HOUR + days * MILLIS_PER_DAY
			   + weeks * MILLIS_PER_WEEK;
	}

	/**
	 * Method setInterval
	 *
	 *
	 * @param	millis
	 *
	 */
	public void setInterval(long millis) {
		if (millis < 0) {
			throw new IllegalArgumentException("Negative-length interval");
		}

		clear();

		days = (int)(millis / MILLIS_PER_DAY);
		seconds = (int)((millis % MILLIS_PER_DAY) / MILLIS_PER_SECOND);

		normalize();
	}

	/**
	 * Method normalize
	 *
	 *
	 */
	protected void normalize() {
		minutes += seconds / SECONDS_PER_MINUTE;
		seconds %= SECONDS_PER_MINUTE;
		hours += minutes / MINUTES_PER_HOUR;
		minutes %= MINUTES_PER_HOUR;
		days += hours / HOURS_PER_DAY;
		hours %= HOURS_PER_DAY;
	}

	/**
	 * Method checkWeeksOkay
	 *
	 *
	 * @param	f
	 *
	 */
	protected void checkWeeksOkay(int f) {
		if ((f != 0)
			&& ((days != 0) || (hours != 0) || (minutes != 0)
				|| (seconds != 0))) {
			throw new IllegalStateException(
				"Weeks and non-weeks are incompatible");
		}
	}

	/**
	 * Method checkNonWeeksOkay
	 *
	 *
	 * @param	f
	 *
	 */
	protected void checkNonWeeksOkay(int f) {
		if ((f != 0) && (weeks != 0)) {
			throw new IllegalStateException(
				"Weeks and non-weeks are incompatible");
		}
	}

	/**
	 * Method clone
	 *
	 *
	 * @return	Object
	 *
	 */
	public Object clone() {
		try {
			Duration other = (Duration)super.clone();

			other.weeks = weeks;
			other.days = days;
			other.hours = hours;
			other.minutes = minutes;
			other.seconds = seconds;

			return other;
		}
		catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}

	/**
	 * Method toString
	 *
	 *
	 * @return	String
	 *
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();

		buffer.append(getClass().getName());
		buffer.append("[weeks=");
		buffer.append(weeks);
		buffer.append(",days=");
		buffer.append(days);
		buffer.append(",hours=");
		buffer.append(hours);
		buffer.append(",minutes=");
		buffer.append(minutes);
		buffer.append(",seconds=");
		buffer.append(seconds);
		buffer.append("]");

		return buffer.toString();
	}

}