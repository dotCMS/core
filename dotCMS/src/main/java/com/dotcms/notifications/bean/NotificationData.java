package com.dotcms.notifications.bean;

import com.dotcms.util.I18NMessage;

import java.io.Serializable;
import java.util.List;

/**
 * This class allows developers to add more custom information to a
 * notification. Depending on the message sent by a process, more information
 * might be necessary to display to the users or for them to take action on what
 * they receive. This class can be used to add more data to a notification and
 * add more flexibility to the client receiving this information.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 14, 2016
 *
 */
@SuppressWarnings("serial")
public class NotificationData implements Serializable {

	private I18NMessage title;
	private I18NMessage message;
	private List<NotificationAction> actions;

	/**
	 * Creates a new Notification Data object.
	 * 
	 * @param title
	 *            - I18NMessage: The title of the notification.
	 * @param message
	 *            - I18NMessage: The message that will be displayed to the user.
	 * @param actions
	 *            - A list of {@link NotificationAction} objects that provide
	 *            more features to the notification.
	 */
	public NotificationData(I18NMessage title, I18NMessage message, List<NotificationAction> actions) {
		this.title = title;
		this.message = message;
		this.actions = actions;
	}

	/**
	 * Returns the title of this notification.
	 * 
	 * @return The notification title.
	 */
	public I18NMessage getTitle() {
		return title;
	}

	/**
	 * Sets the title of this notification.
	 * 
	 * @param title
	 *            - The notification title.
	 */
	public void setTitle(I18NMessage title) {
		this.title = title;
	}

	/**
	 * Returns the message of this notification.
	 * 
	 * @return The notification message.
	 */
	public I18NMessage getMessage() {
		return message;
	}

	/**
	 * Sets the message of this notification.
	 * 
	 * @param message
	 *            - The notification message.
	 */
	public void setMessage(I18NMessage message) {
		this.message = message;
	}

	/**
	 * Returns the list of custom actions for this notification.
	 * 
	 * @return The notification action list.
	 */
	public List<NotificationAction> getActions() {
		return actions;
	}

	/**
	 * Sets the list of {@link NotificationAction} objects for this
	 * notification.
	 * 
	 * @param actions
	 *            - The notification actions.
	 */
	public void setActions(List<NotificationAction> actions) {
		this.actions = actions;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((actions == null) ? 0 : actions.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NotificationData other = (NotificationData) obj;
		if (actions == null) {
			if (other.actions != null)
				return false;
		} else if (!actions.equals(other.actions))
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "NotificationData [title=" + title + ", message=" + message + ", actions=" + actions + "]";
	}

}
