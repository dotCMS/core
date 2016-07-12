package com.dotcms.notifications.bean;

import java.util.Date;

/**
 * 
 * @author Daniel Silva
 * @version 3.0
 * @since Feb 3, 2014
 *
 */
public class Notification {

	private String id;
	private String title;
	private String message;
	private NotificationType type;
	// for the icon
	private NotificationLevel level;
	private String userId;
	private Date timeSent;
	private Boolean wasRead;

	/**
	 * 
	 */
	public Notification() {}

	/**
	 * 
	 * @param message
	 * @param level
	 * @param userId
	 */
	public Notification(String message, NotificationLevel level, String userId) {
		this("", message, level, userId);
	}

	public Notification(String title, String message, NotificationLevel level, String userId) {
		this.title = title;
		this.message = message;
		this.level = level;
		this.userId = userId;
	}

	/**
	 * 
	 * @param id
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * 
	 * @return
	 */
	public String getId() {
		return id;
	}

	/**
	 * 
	 * @return
	 */
	public String getTitle() {
		return this.title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public NotificationType getType() {
		return type;
	}
	public void setType(NotificationType type) {
		this.type = type;
	}
	public NotificationLevel getLevel() {
		return level;
	}
	public void setLevel(NotificationLevel level) {
		this.level = level;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public Date getTimeSent() {
		return timeSent;
	}
	public void setTimeSent(Date timeSent) {
		this.timeSent = timeSent;
	}
	public Boolean getWasRead() {
		return wasRead;
	}
	public void setWasRead(Boolean wasRead) {
		this.wasRead = wasRead;
	}

}
