package com.dotcms.notifications.bean;

import com.dotcms.util.I18NMessage;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * A notification is a simple information unit that a process can use to let the
 * currently logged in user know about it. Notifications can be programmatically
 * sent to inform users about the status of a routine, for example, when it
 * starts, finishes, or if an error occurred.
 * 
 * @author Daniel Silva
 * @version 3.0, 3.7
 * @since Feb 3, 2014
 *
 */
@SuppressWarnings("serial")
public class Notification implements Serializable, Cloneable {

	private String id;
	private NotificationData notificationData;
	private NotificationType type;
	private NotificationLevel level;
	private String userId;
	private Date timeSent;
	private Boolean wasRead;
	private String prettyDate;

	/**
	 * Default constructor.
	 */
	public Notification() {

	}

	/**
	 * Creates a Notification object.
	 * 
	 * @param level
	 *            - The urgency level or category according to the
	 *            {@link NotificationLevel} class.
	 * @param userId
	 *            - The ID of the user that this notification is going to be
	 *            sent to.
	 * @param notificationData
	 *            - The additional information that make up this notification.
	 */
	public Notification(NotificationLevel level, String userId, NotificationData notificationData) {
		this(null, level, userId, notificationData);
	}

	/**
	 * Creates a Notification object.
	 * 
	 * @param type
	 *            - The type or notification according to the
	 *            {@link NotificationType} class.
	 * @param level
	 *            - The urgency level or category according to the
	 *            {@link NotificationLevel} class.
	 * @param userId
	 *            - The ID of the user that this notification is going to be
	 *            sent to.
	 * @param notificationData
	 *            - The additional information that make up this notification.
	 */
	public Notification(NotificationType type, NotificationLevel level, String userId, NotificationData notificationData) {
		this(type, level, userId, false, notificationData);
	}

	/**
	 * Creates a Notification object.
	 * 
	 * @param type
	 *            - The type or notification according to the
	 *            {@link NotificationType} class.
	 * @param level
	 *            - The urgency level or category according to the
	 *            {@link NotificationLevel} class.
	 * @param userId
	 *            - The ID of the user that this notification is going to be
	 *            sent to.
	 * @param wasRead
	 *            - If set to {@code true}, this notification will be marked as
	 *            "read" by the user. Otherwise, set to {@code false}.
	 * @param notificationData
	 *            - The additional information that make up this notification.
	 */
	public Notification(NotificationType type, NotificationLevel level, String userId, Boolean wasRead,
			NotificationData notificationData) {
		this("", type, level, userId, null, false, notificationData);
	}

	/**
	 * Creates a Notification object.
	 * 
	 * @param id
	 *            - The ID of this notification. If a new object is being
	 *            created, leave this parameter as {@code null} so the system
	 *            generates an appropriate ID.
	 * @param type
	 *            - The type or notification according to the
	 *            {@link NotificationType} class.
	 * @param level
	 *            - The urgency level or category according to the
	 *            {@link NotificationLevel} class.
	 * @param userId
	 *            - The ID of the user that this notification is going to be
	 *            sent to.
	 * @param timeSent
	 *            - The creation date of this notification.
	 * @param wasRead
	 *            - If set to {@code true}, this notification will be marked as
	 *            "read" by the user. Otherwise, set to {@code false}.
	 * @param notificationData
	 *            - The additional information that make up this notification.
	 */
	public Notification(String id, NotificationType type, NotificationLevel level, String userId, Date timeSent,
			Boolean wasRead, NotificationData notificationData) {
		this.id = id;
		this.userId = userId;
		this.notificationData = notificationData;
		this.type = type;
		this.level = level;
		this.timeSent = timeSent;
		this.wasRead = wasRead;
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
	public I18NMessage getTitle() {
		return this.notificationData.getTitle();
	}

	/**
	 * Sets the title of this notification.
	 * 
	 * @param title
	 *            - The notification title.
	 */
	public void setTitle(I18NMessage title) {
		this.notificationData.setTitle(title);
	}

	/**
	 * Returns the message of this notification.
	 * 
	 * @return The message title.
	 */
	public I18NMessage getMessage() {
		return this.notificationData.getMessage();
	}

	/**
	 * Sets the message of this notification.
	 * 
	 * @param message
	 *            - The notification message.
	 */
	public void setMessage(I18NMessage message) {
		this.notificationData.setMessage(message);
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
		return this.notificationData.getActions();
	}

	/**
	 * Sets the list of {@link NotificationAction} objects for this
	 * notification.
	 * 
	 * @param actions
	 *            - The notification action list.
	 */
	public void setActions(List<NotificationAction> actions) {
		this.notificationData.setActions(actions);
	}

	/**
	 * Returns the {@link NotificationData} object of this notification.
	 * 
	 * @return The NotificationData object.
	 */
	public NotificationData getNotificationData() {
		return this.notificationData;
	}

	public String getPrettyDate() {
		return prettyDate;
	}

	public void setPrettyDate(String prettyDate) {
		this.prettyDate = prettyDate;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Notification that = (Notification) o;

		if (id != null ? !id.equals(that.id) : that.id != null) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		I18NMessage title   = this.notificationData.getTitle();
		I18NMessage message = this.notificationData.getMessage();
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
		return "Notification [id=" + id + ", title=" + this.getTitle() + ", message=" + this.getMessage() + ", type=" + type
				+ ", level=" + level + ", userId=" + userId + ", timeSent=" + timeSent + ", wasRead=" + wasRead
				+ ", actions=" + this.getActions() + "]";
	}

}
