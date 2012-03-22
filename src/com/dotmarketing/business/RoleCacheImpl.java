package com.dotmarketing.business;

import java.util.List;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class RoleCacheImpl extends RoleCache {

	private DotCacheAdministrator cache;

	private String primaryGroup = "dotCMSRoleCache";
	
	private String keyGroup = "dotCMSRoleKeyCache";

	private String userGroup = "dotCMSUserRoleCache";

	private String layoutGroup = "dotCMSLayoutCache";

	// region's name for the cache
	private String[] groupNames = {primaryGroup,userGroup,layoutGroup};

	public RoleCacheImpl() {
		cache = CacheLocator.getCacheAdministrator();
	}

	@Override
	protected Role add(Role role) {
		String key = primaryGroup + role.getId();

		// Add the key to the cache
		cache.put(key, role,primaryGroup);
		
		if(UtilMethods.isSet(role.getRoleKey())){
			cache.put(keyGroup + role.getRoleKey() , role,keyGroup);
		}


		return role;
		
	}

	@Override
	protected Role get(String key) {
		Role role = null;
		try{
			role = (Role)cache.get(primaryGroup + key,primaryGroup);
		}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
		if(role == null){
			try{
				role = (Role)cache.get(keyGroup + key,keyGroup);
			}catch (DotCacheException e) {
				Logger.debug(this, "Cache Entry not found", e);
			}
		}
		return role;	
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#clearCache()
	 */
	public void clearCache() {
		// clear the cache
		cache.flushGroup(primaryGroup);
		cache.flushGroup(userGroup);
		cache.flushGroup(keyGroup);
		cache.flushGroup(layoutGroup);
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#remove(java.lang.String)
	 */
	protected void remove(String key){
		try{
			cache.remove(primaryGroup + key,primaryGroup);
			cache.remove(userGroup + key,userGroup);
			cache.remove(keyGroup + key,keyGroup);
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

	@Override
	protected List<String> addRoleToUser(String userId, String roleId) {
		String key = userGroup + userId;
		try {
		    cache.remove(key, userGroup);
			return (List<String>)cache.get(key,primaryGroup);
		} catch (DotCacheException e) {
			Logger.warn(this,"Cache Entry not found after adding", e);
			return null;
		}
	}

	@Override
	protected void clearRoleCache() {
		cache.flushGroup(primaryGroup);
		cache.flushGroup(keyGroup);
	}

	@Override
	protected void clearUserRoleCache() {
		cache.flushGroup(userGroup);
	}

	@Override
	protected List<String> getRoleIdsForUser(String userId) {
		String key = userGroup + userId;
		List<String> l = null;
		try {
			l = (List<String>)cache.get(key, userGroup);
		} catch (DotCacheException e) {
			Logger.debug(this, "Cache not find roleIds for user in cache", e);
		}
		return l;
	}

	protected List<String> addRoleListForUser(List<String> roles, String userId){
		String key = userGroup + userId;
		cache.put(key, roles, userGroup);		
		List<String> l = null;
		try {
			l = (List<String>)cache.get(key, userGroup);
		} catch (DotCacheException e) {
			Logger.warn(this, "Cache not find roleIds for user in cache after adding", e);
		}
		return l;
	}

	@Override
	protected List<String> addLayoutsToRole(List<String> layouts, String roleId) {
		String key = layoutGroup + roleId;
		cache.put(key, layouts, layoutGroup);		
		List<String> l = null;
		try {
			l = (List<String>)cache.get(key, userGroup);
		} catch (DotCacheException e) {
			Logger.warn(this, "Cache not find roleIds for user in cache after adding", e);
		}
		return l;
	}

	@Override
	protected List<String> getLayoutsForRole(String roleId) {
		String key = layoutGroup + roleId;
		List<String> l = null;
		try {
			l = (List<String>)cache.get(key, layoutGroup);
		} catch (DotCacheException e) {
			Logger.debug(this, "Cache not find roleIds for user in cache", e);
		}
		return l;
	}

	@Override
	protected void removeLayoutsOnRole(String roleId) {
		String key = layoutGroup + roleId;
		try{
			cache.remove(key, layoutGroup);
		}catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed", e);
		}
	}
}
