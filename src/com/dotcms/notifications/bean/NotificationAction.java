package com.dotcms.notifications.bean;

import java.io.Serializable;
import java.util.Map;

/**
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 12, 2016
 *
 */
public class NotificationAction implements Serializable {

	private final String text;
	private final String action;
	private final NotificationActionType actionType;
	private final Map<String, Object> attributes;

	/**
	 * 
	 * @param text
	 * @param action
	 * @param actionType
	 * @param attributes
	 */
	public NotificationAction(String text, String action, NotificationActionType actionType, Map<String, Object> attributes) {
		this.text = text;
		this.action = action;
		this.actionType = actionType;
		this.attributes = attributes;
	}

	public String getText() {
		return text;
	}

	public String getAction() {
		return action;
	}

	public NotificationActionType getActionType() {
		return actionType;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public String toString() {
		return "NotificationAction [text=" + text + ", action=" + action + ", actionType=" + actionType + ", attributes="
				+ attributes + "]";
	}

}
