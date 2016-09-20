package com.dotcms.notifications.business;

import com.dotcms.notifications.bean.Notification;
import com.dotmarketing.business.Cachable;

import java.util.List;

/**
 * Encapsulate the notification cache for counts and notification list
 * @author jsanca
 */
public abstract class NewNotificationCache implements Cachable {

	/**
	 * Add the Count cache
	 * @param key
	 * @param newNotifications
     * @return Long
     */
	abstract protected Long addCount(String key, Long newNotifications);

	/**
	 * Get the Count cache
	 * @param key
	 * @return
     */
	abstract protected Long getCount(String key);

	/**
	 * Get the notification by offset and limit
	 * @param offset
	 * @param limit
     * @return List
     */
	abstract protected List<Notification> getNotifications(long offset, long limit);

	/**
	 * Get a single notification
	 * @param notificationId String
	 * @return Notification
     */
	abstract protected Notification getNotification(String notificationId);
	/**
	 * Adds a notification by offset and limit.
	 * @param offset
	 * @param limit
	 * @param notifications
     */
	public abstract void addNotifications(long offset, long limit, List<Notification> notifications);

	/**
	 * Clear all the cache (count and notifications)
	 */
	abstract public void clearCache();

	/**
	 * Remove a cache by using a userId, count and collections.
	 * @param userId {@link String}
     */
	abstract public void remove(String userId);

	/**
	 * Adds a notification
	 * @param notification Notification
     */
	protected abstract void addNotification(Notification notification);

	/**
	 * Remove a single notification
	 * @param notificationId {@link String}
     */
	protected abstract void removeNotification(String notificationId);

	/**
	 * Get the all count
	 * @return Long
     */
	protected abstract Long getAllCount();

	/**
	 * All the count to all notification
	 * @param count {@link Long}
     */
	protected abstract void addAllCount(Long count);

	/**
	 * Get the User Count
	 * @param userId {@link String}
	 * @return Long
     */
	protected abstract Long getUserCount(String userId);

	/**
	 * Adds User count
	 * @param userId {@link String}
	 * @param count {@link Long}
     */
	protected abstract void addUserCount(String userId, Long count);

	/**
	 * Get all notifications for an user
	 * @param userId {@link String}
	 * @return List
     */
	public abstract List<Notification> getAllNotifications(String userId);

	/**
	 * Add all notification for an user
	 * @param userId {@link String}
	 * @param notifications {@link List}
     */
	public abstract void addAllNotifications(String userId, List<Notification> notifications);

	/**
	 * Get a segment of user notifications
	 * @param userId {@link String}
	 * @param offset {@link Long}
	 * @param limit {@link Long}
     * @return List
     */
	public abstract List<Notification> getNotifications(String userId, long offset, long limit);

	/**
	 * Adds a segment of user notifications
	 * @param userId {@link String}
	 * @param offset {@link Long}
	 * @param limit {@link Long}
	 * @param notifications {@link List}
     */
	public abstract void addNotifications(String userId, long offset, long limit, List<Notification> notifications);
} // E:O:F:NewNotificationCache.