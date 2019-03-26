package com.dotmarketing.business;

import java.io.Serializable;
import java.util.List;

public abstract class RoleCache implements Cachable {

  /**
   * Will add to key and roleid cache
   *
   * @param role
   * @return
   */
  protected abstract Role add(Role role);

  /**
   * Can retrieve by id or key
   *
   * @param key
   * @return
   */
  protected abstract Role get(String key);

  protected abstract List<String> addLayoutsToRole(List<String> layouts, String roleId);

  protected abstract List<UserRoleCacheHelper> addRoleListForUser(
      List<UserRoleCacheHelper> roles, String userId);

  protected abstract List<UserRoleCacheHelper> getRoleIdsForUser(String userId);

  protected abstract List<String> getLayoutsForRole(String roleId);

  protected abstract List<Role> getRootRoles();

  protected abstract List<Role> addRootRoles(List<Role> roles);

  /** FLushes both key and roleid cache */
  public abstract void clearCache();

  public abstract void clearRootRoleCache();

  protected abstract void clearRoleCache();

  protected abstract void clearUserRoleCache();

  protected abstract void remove(String key);

  protected abstract void removeLayoutsOnRole(String roleId);

  protected static class UserRoleCacheHelper implements Serializable {
    private static final long serialVersionUID = 6600085101661951648L;
    private String roleId;
    private boolean inherited;

    protected UserRoleCacheHelper(String roleId, boolean inherited) {
      this.roleId = roleId;
      this.inherited = inherited;
    }

    /** @return the roleId */
    protected String getRoleId() {
      return roleId;
    }
    /** @return the inherited */
    protected boolean isInherited() {
      return inherited;
    }
  }
}
