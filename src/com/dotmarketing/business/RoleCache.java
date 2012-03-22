package com.dotmarketing.business;

import java.util.List;

public abstract class RoleCache implements Cachable{

	/**
	 * Will add to key and roleid cache
	 * @param role
	 * @return
	 */
	abstract protected Role add(Role role);

	/**
	 * Can retrieve by id or key
	 * @param key
	 * @return
	 */
	abstract protected Role get(String key);

	abstract protected List<String> addRoleToUser(String userId,String roleId);
	
	abstract protected List<String> addLayoutsToRole(List<String> layouts,String roleId);

	abstract protected List<String> addRoleListForUser(List<String> roles, String userId);
	
	abstract protected List<String> getRoleIdsForUser(String userId);
	
	abstract protected List<String> getLayoutsForRole(String roleId);
	
	/**
	 * FLushes both key and roleid cache
	 */
	abstract public void clearCache();
	
	abstract protected void clearRoleCache();
	
	abstract protected void clearUserRoleCache();

	abstract protected void remove(String key);
	
	abstract protected void removeLayoutsOnRole(String roleId);
	
}
