package com.dotcms.rest.api.v1.user;

import com.dotcms.api.system.user.UserService;
import com.dotcms.api.system.user.UserServiceFactory;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.authentication.IncorrectPasswordException;
import com.dotcms.util.SecurityUtils;
import com.dotcms.util.SecurityUtils.DelayStrategy;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.*;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.StringPool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;

/**
 * Provides utility methods to interact with information of dotCMS users and
 * their capabilities.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Aug 10, 2016
 *
 */
@SuppressWarnings("serial")
public class UserResourceHelper implements Serializable {

	private final UserService userService;
	private final RoleAPI roleAPI;
	private final UserAPI userAPI;
	private final LayoutAPI layoutAPI;
	private final HostWebAPI hostWebAPI;
	private final UserWebAPI userWebAPI;
	private final PermissionAPI permissionAPI;
	private final UserProxyAPI userProxyAPI;
	private final LoginServiceAPI loginService;

	@VisibleForTesting
	public UserResourceHelper (	final UserService userService,
			final RoleAPI roleAPI,
			final UserAPI userAPI,
			final LayoutAPI layoutAPI,
			final HostWebAPI hostWebAPI,
			final UserWebAPI userWebAPI,
			final PermissionAPI permissionAPI,
			final UserProxyAPI userProxyAPI,
			final LoginServiceAPI loginService) {

		this.userService = userService;
		this.roleAPI = roleAPI;
		this.userAPI = userAPI;
		this.layoutAPI = layoutAPI;
		this.hostWebAPI = hostWebAPI;
		this.userWebAPI = userWebAPI;
		this.permissionAPI = permissionAPI;
		this.userProxyAPI = userProxyAPI;
		this.loginService = loginService;
	}

	private static class SingletonHolder {
		private static final UserResourceHelper INSTANCE = new UserResourceHelper();
	}

	public static UserResourceHelper getInstance() {

		return UserResourceHelper.SingletonHolder.INSTANCE;
	}

	/**
	 * Private constructor that initializes all APIs and services.
	 */
	private UserResourceHelper() {
		this.userService = UserServiceFactory.getInstance().getUserService();
		this.roleAPI = APILocator.getRoleAPI();
		this.userAPI = APILocator.getUserAPI();
		this.layoutAPI = APILocator.getLayoutAPI();
		this.hostWebAPI = WebAPILocator.getHostWebAPI();
		this.userWebAPI = WebAPILocator.getUserWebAPI();
		this.permissionAPI = APILocator.getPermissionAPI();
		this.userProxyAPI = APILocator.getUserProxyAPI();
		this.loginService = APILocator.getLoginServiceAPI();
	}

	/**
	 * 
	 * @param action
	 * @param message
	 */
	public void log(final String action, String message) {
		ActivityLogger.logInfo(UserResource.class, action, message);
		AdminLogger.log(UserResource.class, action, message);
	}

	/**
	 * Returns a list of dotCMS users based on the specified search criteria.
	 * Two types of result can be obtained by calling this method:
	 * <ul>
	 * <li>If both the {@code assetInode} and the {@code permission} values
	 * <b>are set</b>, this method will return the list of users that have the
	 * specified permission type on the specified Inode.</li>
	 * <li>If the {@code assetInode} or the {@code permission} value <b>is NOT
	 * set</b>, this method will return a list of users based on the criteria
	 * specified in the {@code params} Map:
	 * <ul>
	 * <li>{@code query}: The String or characters that can match the first
	 * name, last name, or e-mail of a user. This is the same value that would
	 * be passed to the {@code LIKE} keyword in SQL. This value will be
	 * automatically sanitized to strip off malicious code.</li>
	 * <li>{@code start}: For pagination purposes. The bottom range of records
	 * to include in the result.</li>
	 * <li>{@code end}: For pagination purposes. The top range of records to
	 * include in the result.</li>
	 * <li>{@code includeAnonymous}: Set to {@code true} if anonymous users will
	 * be included in the result list. Otherwise, set to {@code false}.</li>
	 * <li>{@code includeDefault}: Set to {@code true} if the default user will
	 * be included in the result list. Otherwise, set to {@code false}.</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * 
	 * @param assetInode
	 *            - (Optional) The Inode of the asset that one or more users
	 *            have permission on.
	 * @param permission
	 *            - (Optional) The type of permission assigned to the specified
	 *            asset.
	 * @param params
	 *            - Additional parameters for more specific queries.
	 * @return A {@code Map} containing the dotCMS users that match the filter
	 *         criteria.
	 * @throws Exception
	 *             An error occurred when retrieving the user list.
	 */
	public Map<String, Object> getUserList(final String assetInode, final String permission, final Map<String, String> params)
			throws Exception {
		return this.userService.getUsersList(assetInode, permission, params);
	}

	/**
	 * Gathers all the required parameters that must be changed in order to
	 * perform the 'Login As' functionality. Only users with specific roles can
	 * accomplish this.
	 * 
	 * @param currentUser
	 *            - The currently logged-in user that wants to temporarily login
	 *            as other user.
	 * @param loginAsUserId
	 *            - The ID of the user that will be "imitated" by the logged in
	 *            user.
	 * @param loginAsUserPwd
	 *            - If the user to imitate is an Administrator or CMS
	 *            Administrator, that user's password is required (for security
	 *            reasons).
	 * @param serverName
	 *            - The server name obtained via the request URL to try to
	 *            figure out the Site that will be displayed.
	 * @return A {@link Map} containing all the parameters that must be changed
	 *         in order to perform the temporary 'Login As'.
	 * @throws DotDataException
	 *             An error occurred with the input parameters, or when
	 *             retrieving roles and Site information.
	 * @throws NoSuchUserException
	 *             The ID of the user to login as does not exist.
	 * @throws DotSecurityException
	 *             The operation is not supported by the user.
	 */
	public Map<String, Object> doLoginAs(final User currentUser, final String loginAsUserId, final String loginAsUserPwd,
			final String serverName) throws DotDataException, NoSuchUserException, DotSecurityException {
		if (!UtilMethods.isSet(loginAsUserId)) {
			throw new DotDataException("The 'Login As' user ID is required.", "loginas.error.requireduserid");
		}
		if (loginAsUserId.equalsIgnoreCase(currentUser.getUserId())) {
			throw new DotDataException("Current user [" + currentUser.getUserId() + "] trying to log in as himself.",
					"loginas.error.selfloginas");
		}
		checkLoginAsRole(currentUser);
		final User systemUser = this.userAPI.getSystemUser();
		final User loginAsUser = this.userAPI.loadUserById(loginAsUserId, systemUser, false);
		final List<Layout> layouts = this.layoutAPI.loadLayoutsForUser(loginAsUser);
		if ((layouts == null) || (layouts.size() == 0) || !UtilMethods.isSet(layouts.get(0).getId())) {
			throw new DotDataException("User [" + loginAsUser.getUserId() + "] does not have any layouts.",
					"loginas.error.nolayouts");
		}
		final Role administratorRole = roleAPI.findRoleByFQN(Role.SYSTEM + " --> " + Role.ADMINISTRATOR);
		if (this.roleAPI.doesUserHaveRole(loginAsUser, administratorRole) || this.roleAPI.doesUserHaveRole(loginAsUser,
				com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole())) {
			if (!UtilMethods.isSet(loginAsUserPwd)) {
				throw new DotDataException("The 'Login As' user password is required.", "loginas.error.missingloginaspwd");
			} else if (LoginFactory.passwordMatch(loginAsUserPwd, currentUser) == false) {
				throw new DotDataException("The 'Login As' user password is invalid.",
						"loginas.error.invalidloginascredentials");
			}
		}
		Host host = null;
		if (UtilMethods.isSet(serverName)) {
			host = this.hostWebAPI.findByName(serverName, systemUser, false);
			if (host == null) {
				host = this.hostWebAPI.findByAlias(serverName, systemUser, false);
			}
			if (host == null) {
				host = this.hostWebAPI.findDefaultHost(systemUser, false);
			}
		} else {
			host = this.hostWebAPI.findDefaultHost(systemUser, false);
		}
		final Map<String, Object> sessionData = map(WebKeys.PRINCIPAL_USER_ID, currentUser.getUserId(), WebKeys.USER_ID,
				loginAsUserId, com.dotmarketing.util.WebKeys.CURRENT_HOST, host);
		return sessionData;
	}

	/**
	 * Check if the user has the LOGIN_AS role.
	 *
	 * @param user
	 * @throws DotDataException if the user doesn't have the Login_AS Role.
	 */
	private void checkLoginAsRole(User user) throws DotDataException {
		final Role loginAsRole = this.roleAPI.findRoleByFQN(Role.SYSTEM + " --> " + Role.LOGIN_AS);
		if (!this.roleAPI.doesUserHaveRole(user, loginAsRole)) {
			// Potential hacking attempt
			SecurityUtils.delayRequest(10, DelayStrategy.TIME_SEC);
			throw new DotDataException(
					"Current user [" + user.getUserId() + "] does not have the proper 'Login As' role.",
					"loginas.error.missingloginasrole");
		}
	}

	/**
	 * Gathers all the required parameters that must be changed in order to
	 * perform the logout process of the 'Login As' functionality.
	 * 
	 * @param principalUserId
	 *            - The ID of the user that performed the 'Login As'
	 *            functionality and will be restored.
	 * @param currentLoginAsUser
	 *            - The temporary {@link User} that was imitated by the
	 *            principal user.
	 * @param serverName
	 *            - The server name obtained via the request URL to try to
	 *            figure out the Site that will be displayed.
	 * @return A {@link Map} containing all the parameters that must be changed
	 *         in order to logout form the 'Login As'.
	 * @throws DotDataException
	 *             An error occurred with the input parameters, or when
	 *             retrieving Site information.
	 * @throws DotSecurityException
	 *             The operation is not supported by the user.
	 */
	public Map<String, Object> doLogoutAs(final String principalUserId, final User currentLoginAsUser,
			final String serverName) throws DotDataException, DotSecurityException {
		if (!UtilMethods.isSet(principalUserId)) {
			throw new DotDataException("Current user [" + currentLoginAsUser.getUserId()
			+ "] is not logged in as a different user.");
		}
		final User systemUser = this.userAPI.getSystemUser();
		Host host = null;
		if (UtilMethods.isSet(serverName)) {
			host = this.hostWebAPI.findByName(serverName, systemUser, false);
			if (host == null) {
				host = this.hostWebAPI.findByAlias(serverName, systemUser, false);
			}
			if (host == null) {
				host = this.hostWebAPI.findDefaultHost(systemUser, false);
			}
		} else {
			host = this.hostWebAPI.findDefaultHost(systemUser, false);
		}
		final Map<String, Object> sessionData = map(com.dotmarketing.util.WebKeys.CURRENT_HOST, host);
		return sessionData;
	}

	/**
	 * Return all the user without the anonymous and default users, also add extra login as information.<br>
	 * If you are a Admin user you can use the "Login As" feature, with this you can login as a another user,
	 * but if that user has the "Admin" role or the "Login As" then you would need the another user password to login
	 * as that user.<br>
	 *
	 * Each user is represent by a Map&lt;String, String&gt; with the follow keys:<br>
	 *     <lu>
	 *         <li>name: User's name</li>
	 *         <li>emailaddress: User's email</li>
	 *         <li>id: User's ID</li>
	 *         <li>type:</li>
	 *         <li>requestPassword: if you need password to login as this user</li>
	 *     </lu>
	 *
	 * @return A list of Map, each Map represent a {@link User}
	 * @throws Exception if anything if wrong
	 */
	public ResponseEntityView getLoginAsUsers(User currentUser, String filter, boolean includeUsersCount) throws Exception {
		checkLoginAsRole(currentUser);
		List<User> users = userAPI.getUsersByName(filter, 1, 100, currentUser, false);

		List<Map<String, Object>> userList = new ArrayList<>();
		List<String> rolesId = list( roleAPI.loadRoleByKey(Role.ADMINISTRATOR).getId(), roleAPI.loadCMSAdminRole().getId() );

		String currentUserId = currentUser != null ? currentUser.getUserId() : StringUtils.EMPTY;

		for (User user : users) {
			if (!currentUserId.equalsIgnoreCase(user.getUserId())) {
				Map<String, Object> userMap = user.toMap();
				String id = user.getUserId();
				boolean hasPermissions = roleAPI.doesUserHaveRoles(id, rolesId);

				if (hasPermissions) {
					userMap.put("requestPassword", true);
				}
				userList.add(userMap);
			}
		}

		Map<String, Object> mapResponse = map("users", userList);

		if (includeUsersCount) {
			long countUsersByNameOrEmail = userAPI.getCountUsersByNameOrEmail(StringPool.BLANK);
			mapResponse.put("nUsers", countUsersByNameOrEmail);
		}

		return new ResponseEntityView(mapResponse );
	}

	/**
	 * Update a user
	 *
	 * @param updateUserForm data to update the user, the {@link UpdateUserForm#getUserId()} is the if of the user to update,
	 *                       {@link UpdateUserForm#getCurrentPassword()} is the current password
	 * @param modUser User who is updating the user, if modUser.getUserId() is equals to {@link UpdateUserForm#getUserId()},
	 *                then the current password is need
	 * @param request
	 * @param locale
	 * @return User updated
	 * @throws DotSecurityException if modUser doesn't has permission to update the user
	 * @throws DotDataException
	 * @throws IncorrectPasswordException if modUser is equals to {@link UpdateUserForm#getUserId()} and
	 * 									  {@link UpdateUserForm#getCurrentPassword()} is incorrect
     */
	public User updateUser(final UpdateUserForm updateUserForm, final User modUser,
						   final HttpServletRequest request, Locale locale)
			throws DotSecurityException, DotDataException, IncorrectPasswordException {

		final HttpSession session = request.getSession();
		boolean validatePassword = false;

		User userToSave = null;

		try {
			userToSave = (User)this.userAPI.loadUserById
                    (updateUserForm.getUserId(), this.userAPI.getSystemUser(), false).clone();


			userToSave.setModified(false);
			userToSave.setFirstName(updateUserForm.getGivenName());
			userToSave.setLastName(updateUserForm.getSurname());

			if (null != updateUserForm.getEmail()) {
				userToSave.setEmailAddress(updateUserForm.getEmail());
			}

			if (null != updateUserForm.getNewPassword()) {
				// Password has changed, so it has to be validated
				userToSave.setPassword(updateUserForm.getNewPassword());
				// And re-authentication might be required
				validatePassword = true;
			}

			if (userToSave.getUserId().equalsIgnoreCase(modUser.getUserId())) {

				boolean passwordMatch = loginService.passwordMatch(updateUserForm.getCurrentPassword(), modUser);

				if (!passwordMatch){
					throw new IncorrectPasswordException();
				}

				this.userAPI.save(userToSave, this.userAPI.getSystemUser(), validatePassword, false);
				// if the user logged is the same of the user to save, we need to set the new user changes to the session.
				session.setAttribute(com.dotmarketing.util.WebKeys.CMS_USER, userToSave);
			} else if (this.permissionAPI.doesUserHavePermission
					(this.userProxyAPI.getUserProxy(userToSave, modUser, false),
							PermissionAPI.PERMISSION_EDIT, modUser, false)) {

				this.userAPI.save(userToSave, modUser, validatePassword, !userWebAPI.isLoggedToBackend(request));
			} else {
				throw new DotSecurityException(LanguageUtil.get(locale, "User-Doesnot-Have-Permission"));
			}
		}  catch (SystemException|PortalException e) {
			throw new RuntimeException(e);
		}

		return userToSave;
	}
}
