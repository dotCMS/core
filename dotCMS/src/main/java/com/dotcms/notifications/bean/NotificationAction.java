package com.dotcms.notifications.bean;

import com.dotcms.util.I18NMessage;

import java.io.Serializable;
import java.util.Map;

/**
 * A notification created by a system or custom process might need to include
 * additional information. For example, additional links, buttons calling a
 * JavaScript function, an image, etc.
 * <p>
 * This {@code NotificationAction} class allows developers to further customize
 * the look-and-feel and functionality of a notification item. This way, the
 * client rendering the notification information will be able accommodate the
 * new data and/or features accordingly.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 12, 2016
 *
 */
@SuppressWarnings("serial")
public class NotificationAction implements Serializable {

	private final I18NMessage text;
	private final String action;
	private final NotificationActionType actionType;
	private final Map<String, Object> attributes;

	/**
	 * Creates a new Notification Action object.
	 * 
	 * @param text
	 *            - I18NMessage: The message of the action. This can be the label for a
	 *            button or link.
	 * @param action
	 *            - The result of executing this action. This can be the URL of
	 *            a link, the call to a function, etc.
	 * @param actionType
	 *            - The {@link NotificationActionType} class determines the type
	 *            of action represented by this object. This can be a link, a
	 *            button, an image, etc.
	 * @param attributes
	 *            - If more configuration parameters are required, this
	 *            {@code Map} can be used to define them.
	 */
	public NotificationAction(I18NMessage text, String action, NotificationActionType actionType, Map<String, Object> attributes) {
		this.text = text;
		this.action = action;
		this.actionType = actionType;
		this.attributes = attributes;
	}

	/**
	 * Returns the descriptive message of this notification action.
	 * 
	 * @return The text.
	 */
	public I18NMessage getText() {
		return text;
	}

	/**
	 * Returns the action to execute of this notification action.
	 * 
	 * @return The action.
	 */
	public String getAction() {
		return action;
	}

	/**
	 * Returns the type of this notification action.
	 * 
	 * @return The action type.
	 */
	public NotificationActionType getActionType() {
		return actionType;
	}

	/**
	 * Returns the map of attributes of this notification action.
	 * 
	 * @return The attributes map.
	 */
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public String toString() {
		return "NotificationAction [text=" + text + ", action=" + action + ", actionType=" + actionType + ", attributes="
				+ attributes + "]";
	}

}
