package com.dotcms.notifications.view;

import com.dotcms.notifications.bean.NotificationAction;
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
public class NotificationDataView implements Serializable {

	private final String title;
	private final String message;
	private final List<NotificationActionView> actions;

	/**
	 * Creates a new Notification Data object.
	 *
	 * @param title
	 *            -  The title of the notification.
	 * @param message
	 *            - The message that will be displayed to the user.
	 * @param actions
	 *            - A list of {@link NotificationAction} objects that provide
	 *            more features to the notification.
	 */
	public NotificationDataView(String title, String message, List<NotificationActionView> actions) {
		this.title = title;
		this.message = message;
		this.actions = actions;
	}

	/**
	 * Returns the title of this notification.
	 * 
	 * @return The notification title.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Returns the message of this notification.
	 * 
	 * @return The notification message.
	 */
	public String getMessage() {
		return message;
	}


	/**
	 * Returns the list of custom actions for this notification.
	 * 
	 * @return The notification action list.
	 */
	public List<NotificationActionView> getActions() {
		return actions;
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
		NotificationDataView other = (NotificationDataView) obj;
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
