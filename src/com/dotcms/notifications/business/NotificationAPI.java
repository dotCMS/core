package com.dotcms.notifications.business;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import com.dotcms.notifications.bean.Notification;
import com.dotcms.notifications.bean.NotificationAction;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.exception.DotDataException;

/**
 * Provides access to useful information displayed in the notification section
 * located in the dotCMS back-end site.
 * <p>
 * Events associated to different processes and system routines, such as
 * ElasticSearch Re-index, content creation, SiteSearch indexes creation, custom
 * processes added via plugins, etc., can send messages to logged-in users
 * briefing them about the status of a specific operation.
 * <p>
 * Notifications are specially useful in situations where processes or routines
 * are executed in the back-end. This way, users can continue working on dotCMS
 * as usual and are able to receive alerts indicating, for example, if the
 * process they executed finished correctly or failed to finish due to any
 * error.
 * 
 * @author Daniel Silva
 * @version 3.0, 3.7
 * @since Feb 3, 2014
 *
 */
public interface NotificationAPI extends Serializable {

	/**
	 * Sends an information message to the Notification queue.
	 * 
	 * @param message
	 *            - The message that will be displayed to the user.
	 * @param userId
	 *            - The ID of the user that triggered this notification.
	 */
	void info(String message, String userId);

	/**
	 * Sends an error message to the Notification queue.
	 * 
	 * @param message
	 *            - The message that will be displayed to the user.
	 * @param userId
	 *            - The ID of the user that triggered this notification.
	 */
	void error(String message, String userId);

	/**
	 * Sends a customized message to the Notification queue.
	 * 
	 * @param message
	 *            - The message that will be displayed to the user.
	 * @param level
	 *            - The urgency level of the message according to the
	 *            {@link NotificationLevel} class.
	 * @param userId
	 *            - The ID of the user that triggered this notification.
	 * @throws DotDataException
	 *             An error occurred when saving the message in the database.
	 */
	void generateNotification(String message, NotificationLevel level, String userId) throws DotDataException;

	/**
	 * Sends a customized message to the Notification queue.
	 *
	 * @param message
	 *            - The message that will be displayed to the user.
	 * @param level
	 *            - The urgency level of the message according to the
	 *            {@link NotificationLevel} class.
	 *
	 * @param type
	 * 			  - The type of the notification, by default is gonna be generic
	 * @param userId
	 *            - The ID of the user that triggered this notification.
	 * @throws DotDataException
	 *             An error occurred when saving the message in the database.
	 */
	void generateNotification(String message, NotificationLevel level, NotificationType type, String userId) throws DotDataException;

	/**
	 * Sends a customized message to the Notification queue.
	 * @param title
	 *            - title for the message
	 * @param message
	 *            - The message that will be displayed to the user.
	 * @param actions
	 * 			  - {@link List} of {@link NotificationAction} encapsulate the actions for notifications.
	 * @param level
	 *            - The urgency level of the message according to the
	 *            {@link NotificationLevel} class.
	 *
	 * @param type
	 * 			  - The type of the notification, by default is gonna be generic
	 * @param userId
	 *            - The ID of the user that triggered this notification.
	 * @throws DotDataException
	 *             An error occurred when saving the message in the database.
	 */
	 void generateNotification(String title, String message, List<NotificationAction> actions,
									 NotificationLevel level, NotificationType type, String userId) throws DotDataException;

	/**
	 * Sends a customized message to the Notification queue.
	 * @param title
	 *            - title for the message
	 * @param message
	 *            - The message that will be displayed to the user.
	 * @param actions
	 * 			  - {@link List} of {@link NotificationAction} encapsulate the actions for notifications.
	 * @param level
	 *            - The urgency level of the message according to the
	 *            {@link NotificationLevel} class.
	 *
	 * @param type
	 * 			  - The type of the notification, by default is gonna be generic
	 * @param userId
	 *            - The ID of the user that triggered this notification.
	 * @param locale
	 * 			  - if you send a locale will be used to create the pretty message, otherwise will use the company default.
	 * @throws DotDataException
	 *             An error occurred when saving the message in the database.
	 */
	void generateNotification(I18NMessage title, I18NMessage message, List<NotificationAction> actions,
							  NotificationLevel level, NotificationType type, String userId, Locale locale) throws DotDataException;
	/**
	 * Returns a notification based on its ID.
	 * 
	 * @param notificationId
	 *            - The ID of the notification.
	 * @return The {@link Notification} object.
	 * @throws DotDataException
	 *             An error occurred when finding the notification from the
	 *             database.
	 */
	Notification findNotification(String notificationId) throws DotDataException;

	/**
	 * Deletes a notification based on its ID.
	 * 
	 *
	 * @param userId
	 * @param notificationId
	 *            - The ID of the notification.
	 * @throws DotDataException
	 *             An error occurred when deleting the notification in the
	 *             database.
	 */
	void deleteNotification(String userId, String notificationId) throws DotDataException;

	/**
	 * Deletes a notification based on its ID.
	 *
	 * @param userId String
	 * @param notificationId
	 *            - The ID of the notification.
	 * @throws DotDataException
	 *             An error occurred when deleting the notification in the
	 *             database.
	 */
	void deleteNotifications(String userId, String... notificationId) throws DotDataException;

	/**
	 * Deletes all the notifications associated to a specific user ID.
	 * 
	 * @param userId
	 *            - The ID of the user.
	 * @throws DotDataException
	 *             An error occurred when deleting the notifications in the
	 *             database.
	 */
	void deleteNotifications(String userId) throws DotDataException;

	/**
	 * Returns a paginated result of all the notifications according to the
	 * specified filters.
	 * 
	 * @param offset
	 *            - The row number to read notifications from.
	 * @param limit
	 *            - The limit of rows to include in the result.
	 * @return The list of {@link Notification} objects in the paginated result.
	 * @throws DotDataException
	 *             An error occurred when retrieving the notifications from the
	 *             database.
	 */
	List<Notification> getNotifications(long offset, long limit) throws DotDataException;

	/**
	 * Returns the number of notifications associated to a specific user ID.
	 * 
	 * @param userId
	 *            - The ID of the user.
	 * @return The number of notifications for a user.
	 * @throws DotDataException
	 *             An error occurred when retrieving the number of notifications
	 *             from the database.
	 */
	Long getNotificationsCount(String userId) throws DotDataException;

	/**
	 * Returns the total number of notifications.
	 * 
	 * @return The total number of notifications.
	 * @throws DotDataException
	 *             An error occurred when retrieving the number of all
	 *             notifications from the database.
	 */
	Long getNotificationsCount() throws DotDataException;

	/**
	 * Returns all notifications associated to a user ID.
	 * 
	 * @param userId
	 *            - The ID of the user.
	 * @return The list of {@link Notification} objects associated to a user.
	 * @throws DotDataException
	 *             An error occurred when retrieving the user notifications from
	 *             the database.
	 */
	List<Notification> getAllNotifications(String userId) throws DotDataException;

	/**
	 * Returns a paginated result of all notifications associated to a user ID.
	 * 
	 * @param userId
	 *            - The ID of the user.
	 * @param offset
	 *            - The row number to read notifications from.
	 * @param limit
	 *            - The limit of rows to include in the result.
	 * @return The list of paginated {@link Notification} objects associated to
	 *         a user.
	 * @throws DotDataException
	 *             An error occurred when retrieving the user notifications from
	 *             the database.
	 */
	List<Notification> getNotifications(String userId, long offset, long limit) throws DotDataException;


	/**
	 * Returns the number of new notifications for a specific user ID.
	 * 
	 * @param userId
	 *            - The ID of the user.
	 * @return The number of new notifications.
	 * @throws DotDataException
	 *             An error occurred when retrieving the number of new
	 *             notifications from the database.
	 */
	Long getNewNotificationsCount(String userId) throws DotDataException;

	/**
	 * Marks all the notifications of a user as "read".
	 * 
	 * @param userId
	 *            - The ID of the user.
	 * @throws DotDataException
	 *             An error occurred when updating the user notifications from
	 *             the database.
	 */
	void markNotificationsAsRead(String userId) throws DotDataException;

}
