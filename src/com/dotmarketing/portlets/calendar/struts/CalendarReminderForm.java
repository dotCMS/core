package com.dotmarketing.portlets.calendar.struts;

import org.apache.struts.action.ActionForm;

public class CalendarReminderForm extends ActionForm{
	
	private String firstName;
	private String lastName;
	private String emailAddress;
	private String eventId;
	private int daysInAdvance = 7;
	
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	public String getEmailAddress() {
		return emailAddress;
	}
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}
	
	public String getEventId() {
		return eventId;
	}
	public void setEventId(String eventId) {
		this.eventId = eventId;
	}
	
	public int getDaysInAdvance() {
		return daysInAdvance;
	}
	public void setDaysInAdvance(int daysInAdvance) {
		this.daysInAdvance = daysInAdvance;
	}
	
	
}
