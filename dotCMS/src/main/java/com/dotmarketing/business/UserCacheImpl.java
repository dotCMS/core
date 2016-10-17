/**
 * 
 */
package com.dotmarketing.business;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * @author jasontesser
 *
 */
public class UserCacheImpl extends UserCache {

	private DotCacheAdministrator cache;
	private String primaryGroup = "UserDotCMSCache";
	private String emailGroup = "UserEmailDotCMSCache";
    // region's name for the cache
    private String[] groupNames = {primaryGroup, emailGroup};
	
	
	public UserCacheImpl() {
		cache = CacheLocator.getCacheAdministrator();
	}
	
	/* (non-Javadoc)
	 * @see com.dotmarketing.business.UserCache#add(java.lang.String, com.liferay.portal.model.User)
	 */
	@Override
	public User add(String key, User user) {
		key = primaryGroup + key;
        // Add the key to the cache
        cache.put(key, user,primaryGroup);
        if(UtilMethods.isSet(user.getEmailAddress())){
        	cache.put(user.getEmailAddress(), user, emailGroup);
        }

		return user;
		
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.UserCache#clearCache()
	 */
	@Override
	public void clearCache() {
		// clear the cache
        cache.flushGroup(primaryGroup);
        cache.flushGroup(emailGroup);
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.UserCache#get(java.lang.String)
	 */
	@Override
	public User get(String key) {
		key = primaryGroup + key;
    	User user = null;
    	try{
    		user = (User)cache.get(key,primaryGroup);
    	}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
    	if(!UtilMethods.isSet(user)){
    		try{
    			user = (User)cache.get(key,emailGroup);
    		}catch (DotCacheException e) {
    			Logger.debug(this, "Cache Entry not found", e);
    		}
    	}
        return user;	
	}
	
	/* (non-Javadoc)
	 * @see com.dotmarketing.business.UserCache#remove(java.lang.String)
	 */
	@Override
	public void remove(String key) {
		User u = get(key);
		key = primaryGroup + key;
    	try{
    		cache.remove(key,primaryGroup);
    		cache.remove(key,emailGroup);
    		if(u != null){
    			cache.remove(primaryGroup + u.getUserId(),primaryGroup);
    			cache.remove(primaryGroup + u.getEmailAddress(),emailGroup);
    		}
    	}catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed", e);
		} 
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.Cachable#getGroups()
	 */
	public String[] getGroups() {
		return groupNames;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.Cachable#getPrimaryGroup()
	 */
	public String getPrimaryGroup() {
		return primaryGroup;
	}

}
