package com.dotmarketing.portlets.calendar.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.calendar.business.EventAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.util.UtilMethods;


public class Event extends Contentlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public enum Occurrency {

		DAILY("daily"),
		MONTHLY("monthly"),
		WEEKLY("weekly"),
		ANNUALLY("annually");

		private String name;

		Occurrency(String name) {
			this.name = name;
		}

		public String toString() {
			return name;
		}

		public static Occurrency findOcurrency(String occurency) {
			if(occurency.equals("daily"))
				return DAILY;
			if(occurency.equals("weekly"))
				return WEEKLY;
			if(occurency.equals("monthly"))
				return MONTHLY;
			if(occurency.equals("annually"))
				return ANNUALLY;
			return null;
		}
	}
    
    
    /**
     * Parent recurrence of the event
     */


    public Event() throws DotDataException {
    	EventAPI eventAPI = APILocator.getEventAPI();
    	setStructureInode(eventAPI.getEventStructure().getInode());
    }
    
    protected Event (Contentlet c) {
    	
    }
    
	public String getTitle() {
		return getStringProperty("title");
	}

	public void setTitle(String title) {
		setStringProperty("title", title);
	}

	public String getDescription() {
		return getStringProperty("description");
	}

	public void setDescription(String description) {
		setStringProperty("description", description);
	}

	public Date getStartDate() {
		return getDateProperty("startDate");
	}

	public void setStartDate(java.util.Date startDate) {
		setDateProperty("startDate", startDate);
	}

	public java.util.Date getEndDate() {
		return getDateProperty("endDate");
	}

	public void setEndDate(java.util.Date endDate) {
		setDateProperty("endDate", endDate);
	}

	public String getLink() {
		return getStringProperty("link");
	}

	public void setLink(String link) {
		setStringProperty("link", link);
	}

	public String getLocation() {
		return getStringProperty("location");
	}

	public void setLocation(String location) {
		setStringProperty("location", location);
	}

	public String getTags() {
		return (String) getStringProperty("tags");
	}

	public void setTags(String tags) {
		setStringProperty("tags", tags);
	}
	
	public void setRecurs(boolean recurs){
		setBoolProperty("recurs",recurs);
	}
	
	public boolean getRecurs(){
		return getBoolProperty("recurs");
	}
	
	public void setRecurrenceStart(Date recurrenceStart){
		setDateProperty("recurrenceStart",recurrenceStart);
	}
	
	public Date getRecurrenceStart(){
		return getDateProperty("recurrenceStart");
	}
	
	public void setRecurrenceEnd(Date recurrenceEnd){
		setDateProperty("recurrenceEnd",recurrenceEnd);
	}
	
	public Date getRecurrenceEnd(){
		return getDateProperty("recurrenceEnd");
	}
	
	public void setRecurrenceOccurs(String occurs){
		setStringProperty("recurrenceOccurs",occurs);
	}
	
	public String getRecurrenceOccurs(){
		return getStringProperty("recurrenceOccurs");
	}
	
	public void setRecurrenceInterval(int recurrenceInterval){
		setLongProperty("recurrenceInterval",Long.valueOf(recurrenceInterval));
	}
	
	public int getRecurrenceInterval(){
		return (int)getLongProperty("recurrenceInterval");
	}
	
	public void setRecurrenceDaysOfWeek(String recurrenceDaysOfWeek){
		setStringProperty("recurrenceDaysOfWeek",recurrenceDaysOfWeek);
	}
	
	public String getRecurrenceDaysOfWeek(){
		return getStringProperty("recurrenceDaysOfWeek");
	}
	
	public void setRecurrenceDayOfMonth(int recurrenceDayOfMonth){
		setProperty("recurrenceDayOfMonth",Long.valueOf(recurrenceDayOfMonth));
	}
	
	public int getRecurrenceDayOfMonth(){
		return (int)getLongProperty("recurrenceDayOfMonth");
	}
	
	public void setRecurrenceMonthOfYear(int recurrenceMonthOfYear){
		setProperty("recurrenceMonthOfYear",Long.valueOf(recurrenceMonthOfYear));
	}
	
	public int getRecurrenceMonthOfYear(){
		return (int)getLongProperty("recurrenceMonthOfYear");
	}
	
	public void setRecurrenceDatesToIgnore(String recurrenceDatesToIgnore){
		setStringProperty("recurrenceDatesToIgnore",recurrenceDatesToIgnore);
	}
	
	public String getRecurrenceDatesToIgnore(){
		return getStringProperty("recurrenceDatesToIgnore");
	}
	
	public void setOccursEnum(Occurrency occurs) {
		setStringProperty("recurrenceOccurs",occurs.toString());
	}

	public Occurrency getOccursEnum() {
		return Occurrency.findOcurrency(this.getStringProperty("recurrenceOccurs"));
	}
	
	public void setDisconnectedFrom(String baseEventId){
		setStringProperty("disconnectedFrom", baseEventId);
	}
	
	public String getDisconnectedFrom(){
		return getStringProperty("disconnectedFrom");
	}
	
	public void setOriginalStartDate(Date originalStartDate){
		setDateProperty("originalStartDate",originalStartDate);
	}
	
	public Date getOriginalStartDate(){
		return getDateProperty("originalStartDate");
	}
	
	
	public void addDateToIgnore(Date date){
		if(UtilMethods.isSet(date)){
			String dateStr = String.valueOf(date.getTime());
			String recurrenceDatesToIgnore = getStringProperty("recurrenceDatesToIgnore");
			if(UtilMethods.isSet(recurrenceDatesToIgnore)){
				String[] toIgnoreArr = recurrenceDatesToIgnore.split(" ");
				ArrayList<String> toIgnoreList = new ArrayList<String>(Arrays.asList(toIgnoreArr));
				if(!toIgnoreList.contains(dateStr)){
				   recurrenceDatesToIgnore+=" "+dateStr;
				}
			}else{
				recurrenceDatesToIgnore = dateStr;
			}
			setStringProperty("recurrenceDatesToIgnore",recurrenceDatesToIgnore);
		}
	}
	
	public void deleteDateToIgnore(Date date){
		if(UtilMethods.isSet(date)){
			String dateStr = String.valueOf(date.getTime());
			String recurrenceDatesToIgnore = getStringProperty("recurrenceDatesToIgnore");
			if(UtilMethods.isSet(recurrenceDatesToIgnore)){
				if(recurrenceDatesToIgnore.trim().equals(dateStr.trim())){
					setStringProperty("recurrenceDatesToIgnore","");
				}else{
					String[] toIgnoreArr = recurrenceDatesToIgnore.split(" ");
					if(toIgnoreArr.length>0){
						int count =0;
						for(String dateToIgnore : toIgnoreArr){
							if(!dateStr.equals(dateToIgnore)){
								if(count==0){
									recurrenceDatesToIgnore = new String();
								}
								if(count>0){
									recurrenceDatesToIgnore+=" ";
								}
								recurrenceDatesToIgnore+=dateToIgnore;
								count++;
							}
						}
					}
					setStringProperty("recurrenceDatesToIgnore",recurrenceDatesToIgnore);
				}
			}

		}

	}

	public List<Field> getFields() {
		return FieldsCache.getFieldsByStructureVariableName("calendarEvent");
	}
	
	public boolean isRecurrent(){
		return getBoolProperty("recurs");
	}
	
	public void setNoRecurrenceEnd(boolean recurrenceEnds){
		setBoolProperty("noRecurrenceEnd", recurrenceEnds);
	}
	
	public boolean isNoRecurrenceEnd(){
		return getBoolProperty("noRecurrenceEnd");
	}
	
	public void setRecurrenceWeekOfMonth(int recurrenceWeekOfMonth){
		setProperty("recurrenceWeekOfMonth",Long.valueOf(recurrenceWeekOfMonth));
	}
	
	public void setRecurrenceDayOfWeek(int recurrenceDayOfWeek){
		setProperty("recurrenceDayOfWeek",Long.valueOf(recurrenceDayOfWeek));
	}
	
	public int getRecurrenceWeekOfMonth(){
		return (int)getLongProperty("recurrenceWeekOfMonth");
	}
	
	public int getRecurrenceDayOfWeek(){
		return (int)getLongProperty("recurrenceDayOfWeek");
	}


}
