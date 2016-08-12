package com.dotcms.rest.api.v1.system.role;

import java.io.Serializable;
import java.util.List;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.user.ajax.UserAjax;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Provides utility methods to interact with dotCMS roles.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Aug 9, 2016
 *
 */
@SuppressWarnings("serial")
public class RoleResourceHelper implements Serializable {

	public static final RoleResourceHelper INSTANCE = new RoleResourceHelper();

	private final UserAPI userAPI;
	private final RoleAPI roleAPI;

	/**
	 * Default class constructor.
	 */
	private RoleResourceHelper() {
		this(APILocator.getUserAPI(), APILocator.getRoleAPI());
	}

	@VisibleForTesting
	public RoleResourceHelper(UserAPI userAPI, RoleAPI roleAPI) {
		this.userAPI = userAPI;
		this.roleAPI = roleAPI;
	}

	/**
	 * Verifies that a user is assigned to one of the specified role IDs. It is
	 * not guaranteed that this method will traverse the full list of roles.
	 * Once it finds a role that is associated to the user, it will return.
	 * 
	 * @param userId
	 *            - The ID of the user going through role verification.
	 * @param roleIds
	 *            - A list of role IDs to check the user.
	 * @return If the user is associated to at least one role ID, returns
	 *         {@code true}. Otherwise, returns {@code false}.
	 */
	public boolean userHasRoles(String userId, List<String> roleIds) {
		if (!UtilMethods.isSet(userId) || roleIds == null || roleIds.size() == 0) {
			return false;
		}
		User user;
		try {
			user = this.userAPI.loadUserById(userId, this.userAPI.getSystemUser(), false);
		} catch (Exception e) {
			Logger.error(this, "An error occurred when retrieving information of user ID [" + userId + "]", e);
			return false;
		}
		String currentRoleId = null;
		for (String roleId : roleIds) {
			if (UtilMethods.isSet(roleId.trim())) {
				currentRoleId = roleId;
				try {
					if (this.roleAPI.doesUserHaveRole(user, roleId)) {
						return true;
					}
				} catch (DotDataException e) {
					Logger.error(UserAjax.class, "An error occurred when checking role [" + currentRoleId + "] on user ID ["
							+ userId + "]", e);
					return false;
				}
			}
		}
		return false;
	}

}
