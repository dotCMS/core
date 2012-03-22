package com.dotmarketing.portlets.calendar.struts;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletRequest;

import org.apache.struts.action.ActionMapping;

import com.dotmarketing.portlets.contentlet.struts.ContentletForm;
import com.dotmarketing.util.Logger;

/**
 * 
 * @author david
 *
 */
public class EventForm extends ContentletForm {

	private static final long serialVersionUID = 1L;

	private String disconnectedFrom;
	
	private String originalStartDate;
	
	private String originalEndDate;
    
    //Recurrence fields
	private String recurrenceOccurs = "never";

	private int recurrenceInterval = 1;
	private int recurrenceIntervalDaily = 1;
	private int recurrenceIntervalWeekly = 1;
	private int recurrenceIntervalMonthly = 1;
	private int recurrenceIntervalYearly= 1;

	private Date recurrenceStarts = new Date();
	
	private Date recurrenceEnds = new Date();
	
	private String[] daysOfWeekRecurrence = { String.valueOf(Calendar.MONDAY), String.valueOf(Calendar.TUESDAY), 
			String.valueOf(Calendar.WEDNESDAY),	String.valueOf(Calendar.THURSDAY), String.valueOf(Calendar.FRIDAY), 
			String.valueOf(Calendar.SATURDAY),  String.valueOf(Calendar.SUNDAY) };
	
	private String dayOfMonthRecurrence = "";

	private boolean recurrenceChanged = false;
	
	private boolean noEndDate = false;
	
	private int recurrenceWeekOfMonth = 1;
	
	private int recurrenceDayOfWeek = 1;
	
	private int recurrenceMonthOfYear = 1;
	
    private String specificDayOfMonthRecY = "";
	
	private String specificMonthOfYearRecY = "";
	
	
    private boolean isSpecificDate = false;
	
	
	
	public boolean isSpecificDate() {
		return isSpecificDate;
	}

	public void setSpecificDate(boolean isSpecificDate) {
		this.isSpecificDate = isSpecificDate;
	}
	
	public String getSpecificDayOfMonthRecY() {
		return specificDayOfMonthRecY;
	}

	public void setSpecificDayOfMonthRecY(String specificDayOfMonthRecY) {
		this.specificDayOfMonthRecY = specificDayOfMonthRecY;
	}

	public String getSpecificMonthOfYearRecY() {
		return specificMonthOfYearRecY;
	}

	public void setSpecificMonthOfYearRecY(String specificMonthOfYearRecY) {
		this.specificMonthOfYearRecY = specificMonthOfYearRecY;
	}

	
	
	
	public int getRecurrenceMonthOfYear() {
		return recurrenceMonthOfYear;
	}

	public void setRecurrenceMonthOfYear(int recurrenceMonthOfYear) {
		this.recurrenceMonthOfYear = recurrenceMonthOfYear;
	}

	public int getRecurrenceIntervalDaily() {
		return this.recurrenceIntervalDaily;
	}

	public void setRecurrenceIntervalDaily(int recurrenceIntervalDaily) {
		if(recurrenceIntervalDaily != 1 && this.recurrenceIntervalDaily == 1)
		{
			this.recurrenceIntervalDaily = recurrenceIntervalDaily;
		}
	}

	public int getRecurrenceIntervalWeekly() {
		return this.recurrenceIntervalWeekly;
	}

	public int getRecurrenceIntervalYearly() {
		return recurrenceIntervalYearly;
	}

	public void setRecurrenceIntervalYearly(int recurrenceIntervalYearly) {
		this.recurrenceIntervalYearly = recurrenceIntervalYearly;
	}

	public int getRecurrenceWeekOfMonth() {
		return recurrenceWeekOfMonth;
	}

	public void setRecurrenceWeekOfMonth(int recurrenceWeekOfMonth) {
		this.recurrenceWeekOfMonth = recurrenceWeekOfMonth;
	}

	public int getRecurrenceDayOfWeek() {
		return recurrenceDayOfWeek;
	}

	public void setRecurrenceDayOfWeek(int recurrenceDayOfWeek) {
		this.recurrenceDayOfWeek = recurrenceDayOfWeek;
	}

	public void setRecurrenceIntervalWeekly(int recurrenceIntervalWeekly) {
		if(recurrenceIntervalWeekly != 1 && this.recurrenceIntervalWeekly == 1)
		{
			this.recurrenceIntervalWeekly = recurrenceIntervalWeekly;
		}
	}

	public int getRecurrenceIntervalMonthly() {
		return this.recurrenceIntervalMonthly;
	}

	public void setRecurrenceIntervalMonthly(int recurrenceIntervalMonthly) {
		if(recurrenceIntervalMonthly != 1 && this.recurrenceIntervalMonthly == 1)
		{
			this.recurrenceIntervalMonthly = recurrenceIntervalMonthly;
		}
	}

	public String getRecurrenceOccurs() {
		return recurrenceOccurs;
	}

	public void setRecurrenceOccurs(String recurrenceOccurs) {
		this.recurrenceOccurs = recurrenceOccurs;
	}

	public int getRecurrenceInterval() {
		return recurrenceInterval;
	}

	public void setRecurrenceInterval(int interval) {
		this.recurrenceInterval = interval;
	}

	public Date getRecurrenceStartsDate() {
		return recurrenceStarts;
	}

	public void setRecurrenceStartsDate(Date recurrenceStarts) {
		this.recurrenceStarts = recurrenceStarts;
	}

	public String getRecurrenceStarts() {
		return new SimpleDateFormat("yyyy-MM-dd").format(recurrenceStarts);
	}
	
	public void setRecurrenceStarts(String recurrenceStarts) {
		try {
			this.recurrenceStarts = new SimpleDateFormat("yyyy-MM-dd").parse(recurrenceStarts);
		} catch (ParseException e) {
			Logger.error(this, "Error parsing recurrence end date", e);
		}
	}

	public Date getRecurrenceEndsDate() {
		return recurrenceEnds;
	}

	public void setRecurrenceEndsDate(Date recurrenceEnds) {
		this.recurrenceEnds = recurrenceEnds;
	}

	public String getRecurrenceEnds() {
		return new SimpleDateFormat("yyyy-MM-dd").format(recurrenceEnds);
	}

	public void setRecurrenceEnds(String recurrenceEnds) {
		try {
			this.recurrenceEnds = new SimpleDateFormat("yyyy-MM-dd").parse(recurrenceEnds);
		} catch (ParseException e) {
			Logger.error(this, "Error parsing recurrence end date", e);
		}
	}

	public String[] getRecurrenceDaysOfWeek() {
		return daysOfWeekRecurrence;
	}

	public void setRecurrenceDaysOfWeek(String[] daysOfWeek) {
		this.daysOfWeekRecurrence = daysOfWeek;
	}

	public String getRecurrenceDayOfMonth() {
		return dayOfMonthRecurrence;
	}

	public void setRecurrenceDayOfMonth(String dayOfMonth) {
		this.dayOfMonthRecurrence = dayOfMonth;
	}

	public boolean isRecurrenceChanged() {
		return recurrenceChanged;
	}

	public void setRecurrenceChanged(boolean recurrenceChanged) {
		this.recurrenceChanged = recurrenceChanged;
	}
	
	public boolean isNoEndDate() {
		return noEndDate;
	}

	public void setNoEndDate(boolean noEndDate) {
		this.noEndDate = noEndDate;
	}

	@Override
	public void reset(ActionMapping arg0, ServletRequest arg1) {
		super.reset(arg0, arg1);
		this.recurrenceChanged = false;
	}

	public String getDisconnectedFrom() {
		return disconnectedFrom;
	}

	public void setDisconnectedFrom(String disconnectedFrom) {
		this.disconnectedFrom = disconnectedFrom;
	}

	public String getOriginalStartDate() {
		return originalStartDate;
	}

	public void setOriginalStartDate(String originalStartDate) {
		this.originalStartDate = originalStartDate;
	}

	public String getOriginalEndDate() {
		return originalEndDate;
	}

	public void setOriginalEndDate(String originalEndDate) {
		this.originalEndDate = originalEndDate;
	}

	
	
}
