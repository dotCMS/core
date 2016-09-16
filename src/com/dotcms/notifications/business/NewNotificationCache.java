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
	 * Remove a cache by using a key.
	 * @param key {@link String}
     */
	abstract public void remove(String key);

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
} // E:O:F:NewNotificationCache.