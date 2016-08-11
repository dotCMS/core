package com.dotcms.rest.api.v1.user;

import static com.dotcms.util.CollectionsUtils.map;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.dotcms.api.system.user.UserService;
import com.dotcms.api.system.user.UserServiceFactory;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;

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

	public static final UserResourceHelper INSTANCE = new UserResourceHelper();

	/**
	 * Private constructor that initializes all APIs and services.
	 */
	private UserResourceHelper() {
		this.userService = UserServiceFactory.getInstance().getUserService();
		this.roleAPI = APILocator.getRoleAPI();
		this.userAPI = APILocator.getUserAPI();
		this.layoutAPI = APILocator.getLayoutAPI();
		this.hostWebAPI = WebAPILocator.getHostWebAPI();
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
			throw new DotDataException("The 'Login As' user ID is required.");
		}
		if (loginAsUserId.equalsIgnoreCase(currentUser.getUserId())) {
			throw new DotDataException("Current user [" + currentUser.getUserId() + "] trying to log in as himself.");
		}
		final Role loginAsRole = roleAPI.findRoleByFQN(Role.SYSTEM + " --> " + Role.LOGIN_AS);
		if (!roleAPI.doesUserHaveRole(currentUser, loginAsRole)) {
			// Potential hacking attempt
			SecurityUtils.delayRequest(10, "time-sec");
			throw new DotDataException("Current user [" + currentUser.getUserId()
					+ "] does not have the proper 'Login As' role.");
		}
		User systemUser = this.userAPI.getSystemUser();
		final User loginAsUser = this.userAPI.loadUserById(loginAsUserId, systemUser, false);
		final List<Layout> layouts = this.layoutAPI.loadLayoutsForUser(loginAsUser);
		if ((layouts == null) || (layouts.size() == 0) || !UtilMethods.isSet(layouts.get(0).getId())) {
			throw new DotDataException("User [" + loginAsUser.getUserId() + "] does not have any layouts.");
		}
		final Role administratorRole = roleAPI.findRoleByFQN(Role.SYSTEM + " --> " + Role.ADMINISTRATOR);
		if (roleAPI.doesUserHaveRole(loginAsUser, administratorRole)
				|| roleAPI.doesUserHaveRole(loginAsUser, com.dotmarketing.business.APILocator.getRoleAPI()
						.loadCMSAdminRole())) {
			if (!UtilMethods.isSet(loginAsUserPwd)) {
				throw new DotDataException("The 'Login As' user password is required.");
			} else if (LoginFactory.passwordMatch(loginAsUserPwd, currentUser) == false) {
				throw new DotDataException("The 'Login As' user password is invalid.");
			}
		}
		Host host = null;
		if (UtilMethods.isSet(serverName)) {
			host = hostWebAPI.findByName(serverName, systemUser, false);
			if (host == null) {
				host = hostWebAPI.findByAlias(serverName, systemUser, false);
			}
			if (host == null) {
				host = hostWebAPI.findDefaultHost(systemUser, false);
			}
		} else {
			host = hostWebAPI.findDefaultHost(systemUser, false);
		}
		Map<String, Object> sessionData = map(WebKeys.PRINCIPAL_USER_ID, currentUser.getUserId(), WebKeys.USER_ID,
				loginAsUserId, com.dotmarketing.util.WebKeys.CURRENT_HOST, host);
		return sessionData;
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
		User systemUser = this.userAPI.getSystemUser();
		if (!UtilMethods.isSet(principalUserId)) {
			throw new DotDataException("Current user [" + currentLoginAsUser.getUserId()
					+ "] is not logged in as a different user.");
		}
		Host host = null;
		if (UtilMethods.isSet(serverName)) {
			host = hostWebAPI.findByName(serverName, systemUser, false);
			if (host == null) {
				host = hostWebAPI.findByAlias(serverName, systemUser, false);
			}
			if (host == null) {
				host = hostWebAPI.findDefaultHost(systemUser, false);
			}
		} else {
			host = hostWebAPI.findDefaultHost(systemUser, false);
		}
		Map<String, Object> sessionData = map(com.dotmarketing.util.WebKeys.CURRENT_HOST, host);
		return sessionData;
	}

}
