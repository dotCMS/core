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

	abstract protected List<String> addLayoutsToRole(List<String> layouts,String roleId);

	abstract protected List<UserRoleCacheHelper> addRoleListForUser(List<UserRoleCacheHelper> roles, String userId);

	abstract protected List<UserRoleCacheHelper> getRoleIdsForUser(String userId);

	abstract protected List<String> getLayoutsForRole(String roleId);

	abstract protected List<Role> getRootRoles();

	abstract protected List<Role> addRootRoles(List<Role> roles);

	/**
	 * FLushes both key and roleid cache
	 */
	abstract public void clearCache();

	abstract public void clearRootRoleCache();

	abstract protected void clearRoleCache();

	abstract protected void clearUserRoleCache();

	abstract protected void remove(String key);

	abstract protected void removeLayoutsOnRole(String roleId);

	protected static class UserRoleCacheHelper{
		private String roleId;
		private boolean inherited;

		protected UserRoleCacheHelper(String roleId, boolean inherited){
			this.roleId = roleId;
			this.inherited = inherited;
		}

		/**
		 * @return the roleId
		 */
		protected String getRoleId() {
			return roleId;
		}
		/**
		 * @return the inherited
		 */
		protected boolean isInherited() {
			return inherited;
		}
	}
}
