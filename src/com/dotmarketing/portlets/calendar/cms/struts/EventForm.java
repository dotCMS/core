package com.dotmarketing.portlets.calendar.cms.struts;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.util.Logger;
import com.liferay.util.servlet.UploadServletRequest;

/**
 * Used to manage the data comming doem the submit event frontend page
 * @author David Torres
 * @version 1.6
 * @since 1.6
 */
public class EventForm extends ValidatorForm
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
    //Recurrence fields
	private String recurrenceOccurs = "never";

	private int recurrenceIntervalDaily = 1;

	private int recurrenceIntervalWeekly = 1;

	private int recurrenceIntervalMonthly = 1;
	
	private int recurrenceIntervalYearly = 1;

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
	
	private String disconnectedFrom;
	
	private String originalStartDate;
	
	private String originalEndDate;
	
	private String specificDayOfMonthRecY = "";
	
	private String specificMonthOfYearRecY = "";
	
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

	public int getRecurrenceIntervalDaily() {
		return recurrenceIntervalDaily;
	}

	public void setRecurrenceIntervalDaily(int interval) {
		recurrenceIntervalDaily = interval;
	}

	public int getRecurrenceIntervalWeekly() {
		return recurrenceIntervalWeekly;
	}

	public void setRecurrenceIntervalWeekly(int interval) {
		this.recurrenceIntervalWeekly = interval;
	}

	public int getRecurrenceIntervalMonthly() {
		return recurrenceIntervalMonthly;
	}

	public void setRecurrenceIntervalMonthly(int interval) {
		this.recurrenceIntervalMonthly = interval;
	}

	public String getRecurrenceOccurs() {
		return recurrenceOccurs;
	}

	public void setRecurrenceOccurs(String recurrenceOccurs) {
		this.recurrenceOccurs = recurrenceOccurs;
	}

	public Date getRecurrenceStartsDate() {
		return recurrenceStarts;
	}

	public void setRecurrenceStartsDate(Date recurrenceStarts) {
		this.recurrenceStarts = recurrenceStarts;
	}

	public String getRecurrenceStarts() {
		return new SimpleDateFormat("MM/dd/yyyy").format(recurrenceStarts);
	}
	
	public void setRecurrenceStarts(String recurrenceStarts) {
		try {
			this.recurrenceStarts = new SimpleDateFormat("MM/dd/yyyy").parse(recurrenceStarts);
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
		return new SimpleDateFormat("MM/dd/yyyy").format(recurrenceEnds);
	}

	public void setRecurrenceEnds(String recurrenceEnds) {
		try {
			this.recurrenceEnds = new SimpleDateFormat("MM/dd/yyyy").parse(recurrenceEnds);
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


	@SuppressWarnings("deprecation")
	public ActionErrors validate(ActionMapping arg0, HttpServletRequest request) 
	{
		ActionErrors errors = new ActionErrors(); 
		String title = request.getParameter("title") ;
		if(title == null || title.equals("")){
			errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.title.required"));
		}
		if (request instanceof UploadServletRequest)
		{  
			//Validate that the uploaded image corresponds to a valid image file type
			UploadServletRequest uploadReq = (UploadServletRequest) request;
			java.io.File image = uploadReq.getFile("image");
			if(image != null && image.length() > 0) {
				MimetypesFileTypeMap mimeType = new MimetypesFileTypeMap();
				if(!mimeType.getContentType(image).equals("image/jpeg")
						&& !mimeType.getContentType(image).equals("image/pjpeg")
						&& !mimeType.getContentType(image).equals("image/gif")
						&& !mimeType.getContentType(image).equals("image/png")
						&& !mimeType.getContentType(image).equals("image/x-png")){
					errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.image.required"));
				}					
			}		
		}			

		return errors;
	}

	public int getRecurrenceIntervalYearly() {
		return recurrenceIntervalYearly;
	}

	public void setRecurrenceIntervalYearly(int recurrenceIntervalYearly) {
		this.recurrenceIntervalYearly = recurrenceIntervalYearly;
	}

	public boolean isNoEndDate() {
		return noEndDate;
	}

	public void setNoEndDate(boolean noEndDate) {
		this.noEndDate = noEndDate;
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

	public int getRecurrenceMonthOfYear() {
		return recurrenceMonthOfYear;
	}

	public void setRecurrenceMonthOfYear(int recurrenceMonthOfYear) {
		this.recurrenceMonthOfYear = recurrenceMonthOfYear;
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
