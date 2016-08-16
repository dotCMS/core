package com.dotcms.notifications.dto;

import java.io.Serializable;
import java.util.Date;

import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;

/**
 * This class is the physical representation of a Notification in the database.
 * The data access layer interacts with this class to represent each row of the
 * {@code notification} database table.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 14, 2016
 *
 */
@SuppressWarnings("serial")
public class NotificationDTO implements Serializable {

	private String id;
	private String message;
	private String type;
	private String level;
	private String userId;
	private Date timeSent;
	private Boolean wasRead;

	/**
	 * Creates a Notification object.
	 * 
	 * @param id
	 *            - The ID of this notification. If a new object is being
	 *            created, leave this parameter as {@code null} so the system
	 *            generates an appropriate ID.
	 * @param message
	 *            - The contents of this notification. This column can contain a
	 *            simple String, or a String representation of a more complex
	 *            Java object containing more pieces of information.
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
	 */
	public NotificationDTO(String id, String message, String type, String level, String userId, Date timeSent,
			Boolean wasRead) {
		this.id = id;
		this.message = message;
		this.type = type;
		this.level = level;
		this.userId = userId;
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
	 * Returns the message of this notification. This message can be a simple
	 * String, or a String representation of a more complex Java object
	 * containing more pieces of information.
	 * 
	 * @return The message message.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Sets the message of this notification. This message can be a simple
	 * String, or a String representation of a more complex Java object
	 * containing more pieces of information.
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
	public String getType() {
		return type;
	}

	/**
	 * Sets the type of this notification according to the
	 * {@link NotificationType} class.
	 * 
	 * @param type
	 *            - The notification type.
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Returns the level of this notification.
	 * 
	 * @return The notification level.
	 */
	public String getLevel() {
		return level;
	}

	/**
	 * Sets the level of this notification according to the
	 * {@link NotificationLevel} class.
	 * 
	 * @param level
	 *            - The notification level.
	 */
	public void setLevel(String level) {
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

	@Override
	public String toString() {
		return "NotificationDTO [id=" + id + ", message=" + message + ", type=" + type + ", level=" + level + ", userId="
				+ userId + ", timeSent=" + timeSent + ", wasRead=" + wasRead + "]";
	}

}
