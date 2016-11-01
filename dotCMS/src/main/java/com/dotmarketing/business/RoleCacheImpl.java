package com.dotmarketing.business;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class RoleCacheImpl extends RoleCache {

	private DotCacheAdministrator cache;

	private String primaryGroup = "dotCMSRoleCache";
	
	private String keyGroup = "dotCMSRoleKeyCache";

	private String userGroup = "dotCMSUserRoleCache";

	private String layoutGroup = "dotCMSLayoutCache";
	
	private String rootRolesGroup = "dotCMSRootRolesCache";
	
	private final String rootRoleKey = "ROOT";

	// region's name for the cache
	private String[] groupNames = {primaryGroup,userGroup,layoutGroup, rootRolesGroup};

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
	protected List<Role> getRootRoles() {
		List<Role> ret = null;
		try {
			ret = (List<Role>)cache.get(rootRoleKey, rootRolesGroup);
		} catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
		return ret;
	}
	
	@Override
	protected List<Role> addRootRoles(List<Role> roles) {		
		cache.put(rootRoleKey, roles, rootRolesGroup);		
		List<Role> l = null;
		try {
			l = (List<Role>)cache.get(rootRoleKey, rootRolesGroup);
		} catch (DotCacheException e) {
			Logger.warn(this, "Cache not find root roles in cache after adding", e);
		}
		return l;
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
		cache.flushGroup(rootRolesGroup);
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#remove(java.lang.String)
	 */
	protected void remove(String key){
		try{
			cache.remove(primaryGroup + key,primaryGroup);
			cache.remove(userGroup + key,userGroup);
			cache.remove(keyGroup + key,keyGroup);
			cache.flushGroup(rootRolesGroup);
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
	protected void clearRoleCache() {
		cache.flushGroup(primaryGroup);
		cache.flushGroup(keyGroup);
	}

	@Override
	public void clearRootRoleCache() {
		cache.flushGroup(rootRolesGroup);
	}
	
	@Override
	protected void clearUserRoleCache() {
		cache.flushGroup(userGroup);
	}

	@Override
	protected List<UserRoleCacheHelper> getRoleIdsForUser(String userId) {
		String key = userGroup + userId;
		List<UserRoleCacheHelper> l = null;
		try {
			l = (List<UserRoleCacheHelper>)cache.get(key, userGroup);
		} catch (DotCacheException e) {
			Logger.debug(this, "Cache not find roleIds for user in cache", e);
		}
		return l;
	}

	protected List<UserRoleCacheHelper> addRoleListForUser(List<UserRoleCacheHelper> roles, String userId){
		String key = userGroup + userId;
		cache.put(key, roles, userGroup);		
		List<UserRoleCacheHelper> l = null;
		try {
			l = (List<UserRoleCacheHelper>)cache.get(key, userGroup);
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
			l = (List<String>)cache.get(key, layoutGroup);
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
