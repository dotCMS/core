package com.dotcms.notifications.business;

import java.util.List;

import com.dotcms.notifications.bean.Notification;
import com.dotcms.notifications.bean.NotificationLevel;
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
 * @version 3.0
 * @since Feb 3, 2014
 *
 */
public interface NotificationAPI {

	/**
	 * 
	 * @param message
	 * @param userId
	 */
	void info(String message, String userId);

	/**
	 * 
	 * @param message
	 * @param userId
	 */
	void error(String message, String userId);

	/**
	 * 
	 * @param message
	 * @param level
	 * @param userId
	 * @throws DotDataException
	 */
	void generateNotification(String message, NotificationLevel level, String userId) throws DotDataException;

	/**
	 * 
	 * @param notificationId
	 * @return
	 * @throws DotDataException
	 */
	Notification findNotification(String notificationId) throws DotDataException;

	/**
	 * 
	 * @param notificationId
	 * @throws DotDataException
	 */
	void deleteNotification(String notificationId) throws DotDataException;

	/**
	 * 
	 * @param userId
	 * @throws DotDataException
	 */
	void deleteNotifications(String userId) throws DotDataException;

	/**
	 * 
	 * @param offset
	 * @param limit
	 * @return
	 * @throws DotDataException
	 */
	List<Notification> getNotifications(long offset, long limit) throws DotDataException;

	/**
	 * 
	 * @param userId
	 * @return
	 * @throws DotDataException
	 */
	Long getNotificationsCount(String userId) throws DotDataException;

	/**
	 * 
	 * @return
	 * @throws DotDataException
	 */
	Long getNotificationsCount() throws DotDataException;

	/**
	 * 
	 * @param userId
	 * @return
	 * @throws DotDataException
	 */
	List<Notification> getAllNotifications(String userId) throws DotDataException;

	/**
	 * 
	 * @param userId
	 * @param offset
	 * @param limit
	 * @return
	 * @throws DotDataException
	 */
	List<Notification> getNotifications(String userId, long offset, long limit) throws DotDataException;

	/**
	 * 
	 * @param userId
	 * @return
	 * @throws DotDataException
	 */
	Long getNewNotificationsCount(String userId) throws DotDataException;

	/**
	 * 
	 * @param userId
	 * @throws DotDataException
	 */
	void markNotificationsAsRead(String userId) throws DotDataException;

}
