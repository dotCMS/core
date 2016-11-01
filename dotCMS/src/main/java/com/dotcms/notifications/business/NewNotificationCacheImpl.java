package com.dotcms.notifications.business;

import com.dotcms.notifications.bean.Notification;
import static  com.dotcms.util.CollectionsUtils.*;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;

import java.io.Serializable;
import java.util.Collections;
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
	private static final String ALL_NOTIFICATIONS = "AllNotifications";
	// region's name for the cache
	private final static String[] GROUP_NAMES = { PRIMARY_GROUP };


	private final DotCacheAdministrator cache;

	public NewNotificationCacheImpl() {

		this.cache = CacheLocator.getCacheAdministrator();
	}

	//////////////

	private String getCountUserPrimaryKey(final String userId) {

		return PRIMARY_GROUP + COUNT_PREFIX + userId;
	}

	@Override
	protected Long addCount(final String userId,
					   final Long newNotifications) {

		final String primaryKey = this.getCountUserPrimaryKey(userId);

        // Add the key to the cache
        this.cache.put(primaryKey, newNotifications, PRIMARY_GROUP);

		return newNotifications;
	} // add.

	@Override
	protected Long getCount(final String userId) {

		final String primaryKey =  this.getCountUserPrimaryKey(userId);
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
    public void remove(final String userId) {

		final String countPrimaryKey =
				this.getCountUserPrimaryKey(userId);

		final String userCountPrimaryKey =
				this.getUserCountPrimaryKey(userId);

		final String allNotificationsPrimaryKey =
				this.getAllNotificationsPrimaryKey(userId);

		final String referenceKey = PRIMARY_GROUP + NOTIFICATION_PREFIX;
		List<String> referenceKeyList = null;

		try {

			referenceKeyList = (List<String>) this.cache.
					get(referenceKey, PRIMARY_GROUP);
		} catch (DotCacheException e) {

			referenceKeyList = null;
		}

    	try {

			this.cache.remove(countPrimaryKey,     		  PRIMARY_GROUP);
			this.cache.remove(userCountPrimaryKey, 		  PRIMARY_GROUP);
			this.cache.remove(allNotificationsPrimaryKey, PRIMARY_GROUP);

			if (null != referenceKeyList) {

				for (String primaryKey : referenceKeyList) {

					this.cache.remove(primaryKey, PRIMARY_GROUP);
				}
			}

			this.cache.remove(referenceKey, PRIMARY_GROUP);
    	} catch (Exception e) {

			Logger.debug(this, "Cache not able to be removed", e);
		}
    } // remove.

	///////////////////

	protected String getSingleNotificationPrimaryKey (final String notificationId) {

		return PRIMARY_GROUP + NOTIFICATION_PREFIX
				+ SINGLE_NOTIFICATION + notificationId;
	}

	@Override
	protected void addNotification(final Notification notification) {

		final String primaryKey   =
				this.getSingleNotificationPrimaryKey (notification.getId());

		this.cache.put(primaryKey, notification, PRIMARY_GROUP);
	} // addNotification.

	@Override
	protected Notification getNotification(final String notificationId) {

		Notification notification = null;
		final String primaryKey   =
				this.getSingleNotificationPrimaryKey (notificationId);

		try {

			notification = (Notification)this.cache.get(primaryKey, PRIMARY_GROUP);
		} catch (DotCacheException e) {

			Logger.debug(this, "Cache Entry not found: " + primaryKey, e);
		}

		return notification;
	} // getNotification.

	@Override
	protected void removeNotification(final String notificationId) {

		final String primaryKey   =
				this.getSingleNotificationPrimaryKey (notificationId);

		try {

			this.cache.remove(primaryKey, PRIMARY_GROUP);
		} catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed: " + primaryKey, e);
		}
	} // removeNotification.

	/////////////
	protected String getAllCountPrimaryKey () {

		return PRIMARY_GROUP + COUNT_PREFIX
				+ COUNT_ALL;
	}

	@Override
	protected Long getAllCount() {

		Long count = null;
		final String primaryKey   = this.getAllCountPrimaryKey();

		try {

			count = (Long)this.cache.get(primaryKey, PRIMARY_GROUP);
		} catch (DotCacheException e) {

			Logger.debug(this, "Cache Entry not found: " + primaryKey, e);
		}

		return count;
	} // getAllCount.

	@Override
	protected void addAllCount(final Long count) {

		final String primaryKey   = this.getAllCountPrimaryKey();

		this.cache.put(primaryKey, count, PRIMARY_GROUP);
	} // addAllCount.

	///////////////

	protected String getUserCountPrimaryKey(final String userId) {

		return PRIMARY_GROUP + COUNT_PREFIX
				+ COUNT_USER + userId;
	}

	@Override
	protected Long getUserCount(final String userId) {

		Long count = null;
		final String primaryKey   =
				this.getUserCountPrimaryKey(userId);

		try {

			count = (Long)this.cache.get(primaryKey, PRIMARY_GROUP);
		} catch (DotCacheException e) {

			Logger.debug(this, "Cache Entry not found: " + primaryKey, e);
		}

		return count;
	} // getUserCount.

	@Override
	protected void addUserCount(final String userId, final Long count) {

		final String primaryKey   =
				this.getUserCountPrimaryKey(userId);

		this.cache.put(primaryKey, count, PRIMARY_GROUP);
	} // addUserCount.

	////////

	protected String getAllNotificationsPrimaryKey(final String userId) {

		return PRIMARY_GROUP + ALL_NOTIFICATIONS + userId;
	}

	@Override
	public List<Notification> getAllNotifications(final String userId) {

		List<Notification> notifications = null;
		final String primaryKey   = getAllNotificationsPrimaryKey (userId);

		try {

			notifications = (List<Notification>)this.cache.get(primaryKey, PRIMARY_GROUP);
		} catch (DotCacheException e) {

			Logger.debug(this, "Cache Entry not found", e);
		}

		return notifications;
	} // getAllNotifications.

	@Override
	public void addAllNotifications(final String userId,
									final List<Notification> notifications) {

		final String primaryKey   = getAllNotificationsPrimaryKey (userId);
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
	} // addAllNotifications.

	/////

	protected String getNotificationsPrimaryKey(final String userId,
												final long offset,
												final long limit) {

		return PRIMARY_GROUP + ALL_NOTIFICATIONS + userId +
				OFFSET + offset +
				LIMIT  + limit;
	}

	@Override
	public List<Notification> getNotifications(final String userId,
											   final long offset,
											   final long limit) {

		List<Notification> notifications = null;
		final String primaryKey   = getNotificationsPrimaryKey (userId, offset, limit);

		try {

			notifications = (List<Notification>)this.cache.get(primaryKey, PRIMARY_GROUP);
		} catch (DotCacheException e) {

			Logger.debug(this, "Cache Entry not found", e);
		}

		return notifications;
	} // getNotifications.

	@Override
	public void addNotifications(final String userId,
								 final long offset,
								 final long limit,
								 final List<Notification> notifications) {

		final String primaryKey   = getNotificationsPrimaryKey (userId, offset, limit);

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

	/////

	protected String getNotificationsPrimaryKey(final long offset,
												final long limit) {

		return PRIMARY_GROUP + NOTIFICATION_PREFIX
				+ OFFSET + offset + LIMIT + limit;
	}

	@Override
	protected List<Notification> getNotifications(final long offset,
												  final long limit) {

		List<Notification> notifications = null;
		final String primaryKey   = getNotificationsPrimaryKey (offset, limit);

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

		final String primaryKey   = getNotificationsPrimaryKey (offset, limit);

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
