package com.dotcms.rendering.js.viewtools;

import com.dotcms.rendering.js.proxy.JsRole;
import com.dotcms.rendering.js.proxy.JsUser;
import com.dotcms.rendering.js.JsViewContextAware;
import com.dotcms.rendering.js.JsViewTool;
import com.dotcms.rendering.velocity.viewtools.CMSUserTool;
import com.dotcms.rendering.velocity.viewtools.util.UserCopies;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.apache.velocity.tools.view.context.ViewContext;
import org.graalvm.polyglot.HostAccess;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wraps the CMSUserTool (cmsuser) into the JS context.
 * Looked-up accounts are wrapped around detached copies without credential fields, so scripts
 * never hold the cached user record.
 * @author jsanca
 */
public class UserJsViewTool implements JsViewTool, JsViewContextAware {

	private final CMSUserTool cmsUserTool = new CMSUserTool();

	@HostAccess.Export
	/**
	 * Get the user by email
	 * @param email the email address of the account
	 * @return the account wrapped for the JS context, or {@code null} if none matches
	 */
	public JsUser getUserByEmail(final String email) {
		try {
			final User user = APILocator.getUserAPI().loadByUserByEmail(email, APILocator.systemUser(), false);
			return null != user? new JsUser(UserCopies.withoutCredentials(user)): null;
		} catch (final Exception e) {
			Logger.error(UserJsViewTool.class, e.getMessage(), e);
			return null;
		}
	}

	@HostAccess.Export
	/**
	 * Get User by user id
	 *
	 * @param userId
	 *            userid of the user to be obtained
	 * @return the account wrapped for the JS context, or {@code null} if none matches
	 */
	public JsUser getUserByUserId(final String userId) {
		if (!UtilMethods.isSet(userId)) {
			return null;
		}
		try {
			final User user = APILocator.getUserAPI().loadUserById(userId, APILocator.systemUser(), false);
			return null != user? new JsUser(UserCopies.withoutCredentials(user)): null;
		} catch (final Exception e) {
			Logger.error(UserJsViewTool.class, e.getMessage(), e);
			return null;
		}
	}

	@HostAccess.Export
	/**
	 * Retrieves the logged user from session
	 * 
	 * @param request
	 * @return
	 */
	public JsUser getLoggedInUser(final HttpServletRequest request) {
		final User user = this.cmsUserTool.getLoggedInUser(request);
		return null != user? new JsUser(user): null;
	}

	@HostAccess.Export
	/**
	 * This method return a list of users with the specified group
	 * @param groupId the id of the role
	 * @return the accounts holding the role, wrapped for the JS context
	 * @deprecated will now return a list of roles
	 */
	public List<JsUser> getAllUsersInGroup(final String groupId) {

		final List<String> userIds;
		try {
			userIds = APILocator.getRoleAPI().findUserIdsForRole(APILocator.getRoleAPI().loadRoleById(groupId));
		} catch (final Exception e) {
			Logger.error(this, e.getMessage(), e);
			return Collections.emptyList();
		}

		final List<JsUser> users = new ArrayList<>();
		for (final String userId : userIds) {
			try {
				users.add(new JsUser(UserCopies.withoutCredentials(
						APILocator.getUserAPI().loadUserById(userId, APILocator.systemUser(), true))));
			} catch (final Exception e) {
				Logger.error(this, e.getMessage(), e);
			}
		}
		return users;
	}

	@HostAccess.Export
	/**
	 * This method return all the groups joined by the specified user
	 * @param user User
	 * @return List<Group>
	 */
	public List<JsRole> getUserGroups(final User user) {

		final List<Role> roles = this.cmsUserTool.getUserGroups(user);
		return UtilMethods.isSet(roles)?
				roles.stream().map(JsRole::new).collect(java.util.stream.Collectors.toList()): Collections.emptyList();
	}

	@HostAccess.Export
	/**
	 * This method return all the roles joined by the specified user
	 * @param user User
	 * @return List<JsRole>
	 */
	public List<JsRole> getUserRoles(final User user) {

		final List<Role> roles = this.cmsUserTool.getUserRoles(user);
		return UtilMethods.isSet(roles)?
				roles.stream().map(JsRole::new).collect(java.util.stream.Collectors.toList()): Collections.emptyList();
	}

	@HostAccess.Export
	/**
	 * This method return true|false if the user has
	 * a specific role
	 * @param user User
	 * @param roleName String the name of the role
	 * @return List<Role>
	 */
	public boolean isUserRole(final User user, final String roleName) {

		return this.cmsUserTool.isUserRole(user, roleName);
	}

	@HostAccess.Export
	/**
	 * This method return true|false if the user has
	 * a specific role (by key)
	 * @param roleKey String the key of the role
	 * @return boolean
	 * @throws DotDataException 
	 */
	public boolean hasRole(final String roleKey) throws DotDataException {

		return this.cmsUserTool.hasRole(roleKey);
	}

	@HostAccess.Export
	/**
	 * This method return true|false if the user has
	 * a CMSAdmin role
	 * @param user User
	 * 
	 */
	public boolean isCMSAdmin(final User user) {

		return this.cmsUserTool.isCMSAdmin(user);
	}

	@Override
	public void setViewContext(final ViewContext viewContext) {

		cmsUserTool.init(viewContext);
	}

	@Override
	public String getName() {
		return "cmsuser";
	}
}
