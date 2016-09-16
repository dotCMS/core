package com.dotcms.notifications.business;

import com.dotcms.notifications.bean.Notification;
import static  com.dotcms.util.CollectionsUtils.*;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;

import java.io.Serializable;
import java.util.List;

/**
 * New Notification Cache.
 * @author jsanca
 */
public class NewNotificationCacheImpl extends NewNotificationCache implements Serializable {

	private static final String OFFSET = "offset";
	private static final String LIMIT = "limit";
	private static final String SINGLE_NOTIFICATION = "singleNotification";
	private static final String COUNT_ALL = "All";
	private static final String COUNT_USER = "User";
	private final static String PRIMARY_GROUP = "NewNotificationCache";
	private final static String COUNT_PREFIX = "Count";
	private final static String NOTIFICATION_PREFIX = "Notification";
	// region's name for the cache
	private final static String[] GROUP_NAMES = { PRIMARY_GROUP };

	private final DotCacheAdministrator cache;

	public NewNotificationCacheImpl() {

		this.cache = CacheLocator.getCacheAdministrator();
	}

	@Override
	protected Long addCount(final String key,
					   final Long newNotifications) {

		final String primaryKey =  PRIMARY_GROUP + COUNT_PREFIX + key;

        // Add the key to the cache
        this.cache.put(primaryKey, newNotifications, PRIMARY_GROUP);

		return newNotifications;
	} // add.

	@Override
	protected Long getCount(final String key) {

		final String primaryKey =  PRIMARY_GROUP + COUNT_PREFIX + key;
		Long newNotificationsCount = null;

    	try {

    		newNotificationsCount = (Long)this.cache.get(primaryKey, PRIMARY_GROUP);
    	} catch (DotCacheException e) {

			Logger.debug(this, "Cache Entry not found", e);
		}

        return newNotificationsCount;
	} // get.

	/* (non-Javadoc)
     * @see com.dotmarketing.business.PermissionCache#clearCache()
     */
	@Override
	public void clearCache() {
        // clear the cache
        this.cache.flushGroup(PRIMARY_GROUP);
    } // clearCache/

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#remove(java.lang.String)
	 */
	@Override
    public void remove(final String key) {

		final String primaryKey =  PRIMARY_GROUP + COUNT_PREFIX + key;
    	try {

			this.cache.remove(primaryKey, PRIMARY_GROUP);
    	} catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed", e);
		}
    } // remove.

	@Override
	protected void addNotification(final Notification notification) {

		final String primaryKey   = PRIMARY_GROUP + NOTIFICATION_PREFIX
				+ SINGLE_NOTIFICATION + notification.getId();

		this.cache.put(primaryKey, notification, PRIMARY_GROUP);
	} // addNotification.

	@Override
	protected Notification getNotification(final String notificationId) {

		Notification notification = null;
		final String primaryKey   = PRIMARY_GROUP + NOTIFICATION_PREFIX
				+ SINGLE_NOTIFICATION + notificationId;

		try {

			notification = (Notification)this.cache.get(primaryKey, PRIMARY_GROUP);
		} catch (DotCacheException e) {

			Logger.debug(this, "Cache Entry not found: " + primaryKey, e);
		}

		return notification;
	} // getNotification.

	@Override
	protected void removeNotification(final String notificationId) {

		final String primaryKey   = PRIMARY_GROUP + NOTIFICATION_PREFIX
				+ SINGLE_NOTIFICATION + notificationId;

		try {

			this.cache.remove(primaryKey, PRIMARY_GROUP);
		} catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed: " + primaryKey, e);
		}
	} // removeNotification.

	@Override
	protected Long getAllCount() {

		Long count = null;
		final String primaryKey   = PRIMARY_GROUP + COUNT_PREFIX
				+ COUNT_ALL;

		try {

			count = (Long)this.cache.get(primaryKey, PRIMARY_GROUP);
		} catch (DotCacheException e) {

			Logger.debug(this, "Cache Entry not found: " + primaryKey, e);
		}

		return count;
	} // getAllCount.

	@Override
	protected void addAllCount(final Long count) {

		final String primaryKey   = PRIMARY_GROUP + COUNT_PREFIX
				+ COUNT_ALL;

		this.cache.put(primaryKey, count, PRIMARY_GROUP);
	} // addAllCount.

	@Override
	protected Long getUserCount(final String userId) {

		Long count = null;
		final String primaryKey   = PRIMARY_GROUP + COUNT_PREFIX
				+ COUNT_USER + userId;

		try {

			count = (Long)this.cache.get(primaryKey, PRIMARY_GROUP);
		} catch (DotCacheException e) {

			Logger.debug(this, "Cache Entry not found: " + primaryKey, e);
		}

		return count;
	} // getUserCount.

	@Override
	protected void addUserCount(final String userId, final Long count) {

		final String primaryKey   = PRIMARY_GROUP + COUNT_PREFIX
				+ COUNT_USER + userId;

		this.cache.put(primaryKey, count, PRIMARY_GROUP);
	} // addUserCount.


	@Override
	protected List<Notification> getNotifications(final long offset,
												  final long limit) {

		List<Notification> notifications = null;
		final String primaryKey   = PRIMARY_GROUP + NOTIFICATION_PREFIX
				+ OFFSET + offset + LIMIT + limit;

		try {

			notifications = (List<Notification>)this.cache.get(primaryKey, PRIMARY_GROUP);
		} catch (DotCacheException e) {

			Logger.debug(this, "Cache Entry not found", e);
		}

		return notifications;
	} // getNotifications.



	@Override
	public void addNotifications(final long offset, final long limit,
								 final List<Notification> notifications) {

		final String primaryKey   = PRIMARY_GROUP + NOTIFICATION_PREFIX
				+ OFFSET + offset + LIMIT + limit;
		final String referenceKey = PRIMARY_GROUP + NOTIFICATION_PREFIX;
		List<String> referenceKeyList = null;

		try {

			referenceKeyList = (List<String>) this.cache.
					get(referenceKey, PRIMARY_GROUP);
		} catch (DotCacheException e) {

			referenceKeyList = null;
		}

		referenceKeyList =
				(null == referenceKeyList)? list(): referenceKeyList;

		// Add the primary key to the reference list for future iteration and removes
		referenceKeyList.add(primaryKey);

		// Add the reference list to the cache
		this.cache.put(referenceKey, referenceKeyList, PRIMARY_GROUP);

		// Add the key to the cache
		this.cache.put(primaryKey, notifications, PRIMARY_GROUP);
	} // addNotifications.



	@Override
    public String[] getGroups() {

    	return GROUP_NAMES;
    } // getGroups.

	@Override
    public String getPrimaryGroup() {

    	return PRIMARY_GROUP;
    } // getPrimaryGroup.

} // E:O:F:NewNotificationCacheImpl.
