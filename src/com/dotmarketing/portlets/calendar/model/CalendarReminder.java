package com.dotmarketing.portlets.calendar.model;

import java.io.Serializable;
import java.util.Date;

public class CalendarReminder implements Serializable {
	
	private String userId;
	private String eventId;
	private Date sendDate;
	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getEventId() {
		return eventId;
	}
	public void setEventId(String eventId) {
		this.eventId = eventId;
	}
	public Date getSendDate() {
		return sendDate;
	}
	public void setSendDate(Date sendDate) {
		this.sendDate = sendDate;
	}
	
	public boolean equals(Object object)
	{
		boolean returnValue = false;
		if((object instanceof CalendarReminder))
		{
			CalendarReminder cr = (CalendarReminder) object;
			if(this.userId.equals(cr.getUserId()) && 
					this.eventId == cr.getEventId() &&
					this.sendDate.equals(cr.getSendDate()))
			{
				returnValue = true;
			}
		}
		return returnValue;
	}
	
	public int hashCode()
	{
		long returnValue = 1;
		returnValue = this.userId.hashCode() + this.eventId.hashCode() + this.sendDate.getTime();
		return (int) returnValue;
	}
}