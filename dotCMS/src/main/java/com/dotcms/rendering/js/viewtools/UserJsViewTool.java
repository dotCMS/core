package com.dotcms.rendering.js.viewtools;

import com.dotcms.rendering.js.proxy.JsRole;
import com.dotcms.rendering.js.proxy.JsUser;
import com.dotcms.rendering.js.JsViewContextAware;
import com.dotcms.rendering.js.JsViewTool;
import com.dotcms.rendering.velocity.viewtools.CMSUsersWebAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.apache.velocity.tools.view.context.ViewContext;
import org.graalvm.polyglot.HostAccess;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

/**
 * Wraps the CMSUsersWebAPI (cmsuser) into the JS context.
 * @author jsanca
 */
public class UserJsViewTool implements JsViewTool, JsViewContextAware {

	private CMSUsersWebAPI cmsUsersWebAPI = new CMSUsersWebAPI();

	@HostAccess.Export
	/**
	 * Get the user by email
	 * @param email
	 * @return User
	 */
	public JsUser getUserByEmail(final String email) {
		final User user = cmsUsersWebAPI.getUserByEmail(email);
		return null != user? new JsUser(user): null;
	}

	@HostAccess.Export
	/**
	 * Get User by user id
	 * 
	 * @param userId
	 *            userid of the user to be obtained
	 * @return User
	 */
	public JsUser getUserByUserId(final String userId) {
		final User user = cmsUsersWebAPI.getUserByUserId(userId);
		return null != user? new JsUser(user): null;
	}

	@HostAccess.Export
	/**
	 * Retrieves the logged user from session
	 * 
	 * @param request
	 * @return
	 */
	public JsUser getLoggedInUser(final HttpServletRequest request) {
		final User user = this.cmsUsersWebAPI.getLoggedInUser(request);
		return null != user? new JsUser(user): null;
	}

	@HostAccess.Export
	/**
	 * This method return a list of users with the specified group
	 * @param groupId compare string
	 * @return List<User>
	 * @deprecated will now return a list of roles
	 */
	public List<JsUser> getAllUsersInGroup(final String groupId) {

		final List<User> users = this.cmsUsersWebAPI.getAllUsersInGroup(groupId);
		return UtilMethods.isSet(users)?
				users.stream().map(JsUser::new).collect(java.util.stream.Collectors.toList()): Collections.emptyList();
	}

	@HostAccess.Export
	/**
	 * This method return all the groups joined by the specified user
	 * @param user User
	 * @return List<Group>
	 */
	public List<JsRole> getUserGroups(final User user) {

		final List<Role> roles = this.cmsUsersWebAPI.getUserGroups(user);
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

		final List<Role> roles = this.cmsUsersWebAPI.getUserRoles(user);
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

		return this.cmsUsersWebAPI.isUserRole(user, roleName);
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

		return this.cmsUsersWebAPI.hasRole(roleKey);
	}

	@HostAccess.Export
	/**
	 * This method return true|false if the user has
	 * a CMSAdmin role
	 * @param user User
	 * 
	 */
	public boolean isCMSAdmin(final User user) {

		return this.cmsUsersWebAPI.isCMSAdmin(user);
	}

	@Override
	public void setViewContext(final ViewContext viewContext) {

		cmsUsersWebAPI.init(viewContext);
	}

	@Override
	public String getName() {
		return "cmsuser";
	}
}
