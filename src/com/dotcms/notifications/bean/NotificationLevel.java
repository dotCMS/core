package com.dotcms.notifications.bean;

/**
 * A level or "urgency" or importance can be set to a new Notification pushed by
 * a process or routine. This {@code NotificationLevel} class provides different
 * alert levels that services can set to their notifications in order to mark
 * their importance. For example, this value can be handled by the client to
 * provide visual hints that indicate the priority of an event to users.
 * 
 * @author Daniel Silva
 * @version 3.0
 * @since Feb 3, 2014
 *
 */
public enum NotificationLevel {

	INFO, WARNING, ERROR

}
