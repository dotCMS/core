package com.dotmarketing.util;

import java.text.AttributedCharacterIterator;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class ThreadSafeSimpleDateFormat extends DateFormat {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7876073678435717012L;
	private DateFormat df = new SimpleDateFormat();

	@Override
	public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
		synchronized (this)  {
			return df.format(date, toAppendTo, fieldPosition);
		}
	}

	@Override
	public Date parse(String source, ParsePosition pos) {
		synchronized (this)  {
			return df.parse(source, pos);
		}
	}

	public ThreadSafeSimpleDateFormat() {
		synchronized (this) {
			df = new SimpleDateFormat();
		}
	}

	public ThreadSafeSimpleDateFormat(String format) {
		synchronized (this) {
			this.df = new SimpleDateFormat(format);
		}
	}

	@Override
	public Object clone() {

		return df.clone();
	}

	@Override
	public boolean equals(Object obj) {
		return df.equals(obj);
	}

	@Override
	public Calendar getCalendar() {

		return df.getCalendar();
	}

	@Override
	public NumberFormat getNumberFormat() {
		return df.getNumberFormat();
	}

	@Override
	public TimeZone getTimeZone() {

		return df.getTimeZone();
	}

	@Override
	public int hashCode() {

		return df.hashCode();
	}

	@Override
	public boolean isLenient() {

		return df.isLenient();
	}

	@Override
	public Date parse(String source) throws ParseException {
		synchronized (this)  {
			return df.parse(source);
		}
	}

	@Override
	public Object parseObject(String source, ParsePosition pos) {
		synchronized (this)  {
			return df.parseObject(source, pos);
		}
	}

	@Override
	public void setCalendar(Calendar newCalendar) {
		synchronized (this)  {
			df.setCalendar(newCalendar);
		}
	}

	@Override
	public void setLenient(boolean lenient) {
		synchronized (this)  {
			df.setLenient(lenient);
		}
	}

	@Override
	public void setNumberFormat(NumberFormat newNumberFormat) {
		synchronized (this)  {
			df.setNumberFormat(newNumberFormat);
		}
	}

	@Override
	public void setTimeZone(TimeZone zone) {
		synchronized (this)  {
			df.setTimeZone(zone);
		}
	}

	@Override
	public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
		synchronized (this)  {
			return df.formatToCharacterIterator(obj);
		}
	}

	@Override
	public Object parseObject(String source) throws ParseException {
		synchronized (this)  {
			return df.parseObject(source);
		}
	}

	@Override
	public String toString() {
		return df.toString();
	}

}
