package com.dotcms.notifications.business;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;

public class NewNotificationCacheImpl extends NewNotificationCache {

	private DotCacheAdministrator cache;

	private static String primaryGroup = "NewNotificationCache";
    // region's name for the cache
    private static String[] groupNames = {primaryGroup};

	public NewNotificationCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
	}

	@Override
	protected Long add(String key, Long newNotifications) {
		key = primaryGroup + key;

        // Add the key to the cache
        cache.put(key, newNotifications, primaryGroup);


		return newNotifications;

	}

	@Override
	protected Long get(String key) {
		key = primaryGroup + key;
		Long newNotificationsCount = null;
    	try{
    		newNotificationsCount = (Long)cache.get(key,primaryGroup);
    	}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
        return newNotificationsCount;
	}

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#clearCache()
	 */
    public void clearCache() {
        // clear the cache
        cache.flushGroup(primaryGroup);
    }

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#remove(java.lang.String)
	 */
    public void remove(String key){
    	key = primaryGroup + key;
    	try{
    		cache.remove(key,primaryGroup);
    	}catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed", e);
		}
    }
    public String[] getGroups() {
    	return groupNames;
    }
    public String getPrimaryGroup() {
    	return primaryGroup;
    }
}
