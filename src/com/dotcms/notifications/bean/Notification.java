package com.dotcms.notifications.bean;

import java.util.Date;

public class Notification {
	private String id;
	private String message;
	private NotificationType type;
	private NotificationLevel level;
	private String userId;
	private Date timeSent;
	private Boolean wasRead;

	public Notification() {}

	public Notification(String message, NotificationLevel level, String userId) {
		this.message = message;
		this.level = level;
		this.userId = userId;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
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
