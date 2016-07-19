package com.dotcms.notifications.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A notification is a simple information unit that a process can use to let the
 * currently logged in user know about it. Notifications can be programmatically
 * sent to inform users about the status of a routine, for example, when it
 * starts, finishes, or if an error occurred.
 * 
 * @author Daniel Silva
 * @version 3.0
 * @since Feb 3, 2014
 *
 */
@SuppressWarnings("serial")
public class Notification implements Serializable {

	private String id;
	private String title;
	private String message;
	private NotificationType type;
	private NotificationLevel level;
	private String userId;
	private Date timeSent;
	private Boolean wasRead;
	private List<NotificationAction> actions;

	/**
	 * Default constructor.
	 */
	public Notification() {

	}

	/**
	 * Creates a Notification object.
	 * 
	 * @param message
	 *            - The piece of information that will be displayed to the user.
	 * @param level
	 *            - The urgency level or category according to the
	 *            {@link NotificationLevel} class.
	 * @param userId
	 *            - The ID of the user that this notification is going to be
	 *            sent to.
	 */
	public Notification(String message, NotificationLevel level, String userId) {
		this("", message, level, userId);
	}

	/**
	 * Creates a Notification object.
	 * 
	 * @param title
	 *            - The title of the notification. Usually a small introduction
	 *            for the main message.
	 * @param message
	 *            - The piece of information that will be displayed to the user.
	 * @param level
	 *            - The urgency level or category according to the
	 *            {@link NotificationLevel} class.
	 * @param userId
	 *            - The ID of the user that this notification is going to be
	 *            sent to.
	 */
	public Notification(String title, String message, NotificationLevel level, String userId) {
		this(title, message, level, userId, null);
	}

	/**
	 * Creates a Notification object.
	 * 
	 * @param title
	 *            - The title of the notification. Usually a small introduction
	 *            for the main message.
	 * @param message
	 *            - The piece of information that will be displayed to the user.
	 * @param level
	 *            - The urgency level or category according to the
	 *            {@link NotificationLevel} class.
	 * @param userId
	 *            - The ID of the user that this notification is going to be
	 *            sent to.
	 * @param actions
	 *            - A list of {@link NotificationAction} objects that can be
	 *            used to extend the functionality of a notification item. For
	 *            example, adding a link, a button that calls a function, an
	 *            image, etc.
	 */
	public Notification(String title, String message, NotificationLevel level, String userId,
			List<NotificationAction> actions) {
		this.title = title;
		this.message = message;
		this.level = level;
		this.userId = userId;
		this.actions = actions;
	}

	/**
	 * Returns the ID of this notification.
	 * 
	 * @return The notification ID.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the ID of this notification.
	 * 
	 * @param id
	 *            - The notification ID.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Returns the title of this notification.
	 * 
	 * @return The notification title.
	 */
	public String getTitle() {
		return this.title;
	}

	/**
	 * Sets the title of this notification.
	 * 
	 * @param title
	 *            - The notification title.
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Returns the message of this notification.
	 * 
	 * @return The message title.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Sets the message of this notification.
	 * 
	 * @param message
	 *            - The notification message.
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Returns the type of this notification.
	 * 
	 * @return The notification type.
	 */
	public NotificationType getType() {
		return type;
	}

	/**
	 * Sets the type of this notification according to the
	 * {@link NotificationType} class.
	 * 
	 * @param type
	 *            - The notification type.
	 */
	public void setType(NotificationType type) {
		this.type = type;
	}

	/**
	 * Returns the level of this notification.
	 * 
	 * @return The notification level.
	 */
	public NotificationLevel getLevel() {
		return level;
	}

	/**
	 * Sets the level of this notification according to the
	 * {@link NotificationLevel} class.
	 * 
	 * @param level
	 *            - The notification type.
	 */
	public void setLevel(NotificationLevel level) {
		this.level = level;
	}

	/**
	 * Returns the user ID of this notification.
	 * 
	 * @return The notification user ID.
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * Sets the user ID of this notification.
	 * 
	 * @param userId
	 *            - The notification user ID.
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * Returns the date that this notification was sent.
	 * 
	 * @return The notification sent date.
	 */
	public Date getTimeSent() {
		return timeSent;
	}

	/**
	 * Sets the date of this notification was sent.
	 * 
	 * @param timeSent
	 *            - The notification sent date.
	 */
	public void setTimeSent(Date timeSent) {
		this.timeSent = timeSent;
	}

	/**
	 * Returns the status of this notification, indicating if it has been read
	 * or not.
	 * 
	 * @return The notification status.
	 */
	public Boolean getWasRead() {
		return wasRead;
	}

	/**
	 * Sets the status of this notification, indicating if it has been read or
	 * not.
	 * 
	 * @param wasRead
	 *            - The notification status.
	 */
	public void setWasRead(Boolean wasRead) {
		this.wasRead = wasRead;
	}

	/**
	 * Returns the list of {@link NotificationAction} objects for this
	 * notification.
	 * 
	 * @return The notification action list.
	 */
	public List<NotificationAction> getActions() {
		return this.actions;
	}

	/**
	 * Sets the list of {@link NotificationAction} objects for this
	 * notification.
	 * 
	 * @param actions
	 *            - The notification action list.
	 */
	public void setActions(List<NotificationAction> actions) {
		this.actions = actions;
	}

	/**
	 * Adds a new {@link NotificationAction} object to the list of actions for
	 * this notification.
	 * 
	 * @param action
	 *            - The notification action.
	 */
	public void addAction(NotificationAction action) {
		if (this.actions == null) {
			this.actions = new ArrayList<>();
		}
		this.actions.add(action);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Notification that = (Notification) o;

		if (id != null ? !id.equals(that.id) : that.id != null) return false;
		if (title != null ? !title.equals(that.title) : that.title != null) return false;
		if (message != null ? !message.equals(that.message) : that.message != null) return false;
		if (type != that.type) return false;
		if (level != that.level) return false;
		if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
		if (timeSent != null ? !timeSent.equals(that.timeSent) : that.timeSent != null) return false;
		return wasRead != null ? wasRead.equals(that.wasRead) : that.wasRead == null;

	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (title != null ? title.hashCode() : 0);
		result = 31 * result + (message != null ? message.hashCode() : 0);
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + (level != null ? level.hashCode() : 0);
		result = 31 * result + (userId != null ? userId.hashCode() : 0);
		result = 31 * result + (timeSent != null ? timeSent.hashCode() : 0);
		result = 31 * result + (wasRead != null ? wasRead.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Notification [id=" + id + ", title=" + title + ", message=" + message + ", type=" + type + ", level="
				+ level + ", userId=" + userId + ", timeSent=" + timeSent + ", wasRead=" + wasRead + ", actions=" + actions
				+ "]";
	}

}
