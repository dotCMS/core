package com.dotcms.rendering.velocity.viewtools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.rendering.velocity.viewtools.bean.UserProfile;
import com.dotcms.rendering.velocity.viewtools.util.UserCopies;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.factories.PublicAddressFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;

/**
 * The {@code $cmsuser} Velocity tool: user lookups and role checks for templates.
 *
 * <p>Looked-up accounts are returned as read-only {@link UserProfile} objects while the
 * {@link FeatureFlagName#FEATURE_FLAG_CMSUSER_READ_ONLY_PROFILES} feature flag is on (the default).
 * Installations whose templates pass looked-up accounts to methods that require a {@link User}
 * can turn the flag off; lookups then return detached {@link User} copies with the credential
 * fields cleared. Only turn it off when templates need it, because those copies are accepted
 * wherever a {@link User} is.
 *
 * <p>{@link #getLoggedInUser(HttpServletRequest)} always returns a {@link User}, because templates
 * pass the requester to methods that require one, but as a detached copy without the credential
 * fields rather than the object held by the session.
 */
public class CMSUsersWebAPI implements ViewTool {

	private HttpServletRequest request;
	Context ctx;
	User user = null;

	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		this.request = context.getRequest();
		ctx = context.getVelocityContext();

		try {
			user = WebAPILocator.getUserWebAPI().getLoggedInUser(this.request);
		} catch (Exception e) {
			Logger.debug(CMSUsersWebAPI.class,e.getMessage(),e);
		}


	}

	/**
	 * Looks up an account by email address.
	 *
	 * @param email the email address of the account
	 * @return a read-only {@link UserProfile} of the account (a detached {@link User} copy
	 *         without credential fields when the read-only profiles flag is off), or
	 *         {@code null} if no account matches
	 */
	public Object getUserByEmail(final String email) {
		try {
			return forTemplate(APILocator.getUserAPI().loadByUserByEmail(email, APILocator.getUserAPI().getSystemUser(), false));
		} catch (Exception e) {
			Logger.error(CMSUsersWebAPI.class,e.getMessage(), e);
			return null;
		}
	}









	/**
	 * Looks up an account by user id.
	 *
	 * @param userId the id of the account
	 * @return a read-only {@link UserProfile} of the account (a detached {@link User} copy
	 *         without credential fields when the read-only profiles flag is off), or
	 *         {@code null} if no account matches
	 */
	public Object getUserByUserId(final String userId) {
		if (UtilMethods.isSet(userId)) {
			try{
				return forTemplate(APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false));
			} catch (Exception e1) {
				Logger.error(CMSUsersWebAPI.class,e1.getMessage(), e1);
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * Returns the user making the request: the back-end user, or else the front-end user held by
	 * the session.
	 *
	 * @param request the current request
	 * @return a detached copy of the requester without the password, password-reset and
	 *         remember-me tokens, or {@code null} if nobody is logged in
	 */
	public User getLoggedInUser(final HttpServletRequest request) {
		User loggedInUser = PortalUtil.getUser(request);//back-end user

		if(!UtilMethods.isSet(loggedInUser)){
			HttpSession session = request.getSession(false);
			if (session != null) {
				loggedInUser = (User) session.getAttribute(WebKeys.CMS_USER);
			}
		}
		return UserCopies.withoutCredentials(loggedInUser);
	}





	/**
	 * Lists the accounts that hold the given role.
	 *
	 * @param groupId the id of the role
	 * @return read-only {@link UserProfile} objects for the accounts (detached {@link User}
	 *         copies without credential fields when the read-only profiles flag is off)
	 * @deprecated will now return a list of roles
	 */
	public List<Object> getAllUsersInGroup(final String groupId) {
		List<String> uids;
		List<Object> ret = new ArrayList<>();
		try {
			uids = APILocator.getRoleAPI().findUserIdsForRole(APILocator.getRoleAPI().loadRoleById(groupId));
		} catch (Exception e1) {
			Logger.error(this, e1.getMessage(), e1);
			return ret;
		}	
		
		for (String userId : uids) {
			try {
				ret.add(forTemplate(APILocator.getUserAPI().loadUserById(userId, APILocator.getUserAPI().getSystemUser(), true)));
			} catch (Exception e1) {
				Logger.error(this, e1.getMessage(), e1);
			}	
		}
		return ret;
	}


	/**
	 * Returns the roles held by the given account.
	 *
	 * @param user a {@link User} or a {@link UserProfile} returned by this tool's lookups
	 * @return the roles of the account
	 */
	public List<Role> getUserGroups(final Object user) {
		return getUserRoles(user);
	}



	/**
	 * Returns the roles held by the given account.
	 *
	 * @param user a {@link User} or a {@link UserProfile} returned by this tool's lookups
	 * @return the roles of the account, an empty list if no account is given, or {@code null}
	 *         if the roles cannot be loaded
	 */
	public List<Role> getUserRoles(final Object user) {
		final String userId = userIdOf(user);
		if (userId == null) {
			return Collections.emptyList();
		}
		try {
			return APILocator.getRoleAPI().loadRolesForUser(userId);
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		}
	}


	/**
	 * Checks whether the given account holds the role with the given name.
	 *
	 * @param user a {@link User} or a {@link UserProfile} returned by this tool's lookups
	 * @param roleName the name of the role
	 * @return {@code true} if the account holds the role
	 */
	public boolean isUserRole(final Object user, final String roleName) {
		boolean retVal = false;
		final String userId = userIdOf(user);
		if(roleName == null || userId == null){
			return retVal;
		}

		List<Role> roles;
		try {
			roles = APILocator.getRoleAPI().loadRolesForUser(userId);
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
			return retVal;
		}
		for(Role r : roles){
			if(roleName.equals(r.getName())){
				retVal = true;
			}
		}
		return retVal;
	}


	/**
	 * This method return true|false if the user has
	 * a specific role (by key)
	 * @param roleKey String the key of the role
	 * @return boolean
	 * @throws DotDataException 
	 */
	public boolean hasRole(String roleKey) throws DotDataException {

		return APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadRoleByKey(roleKey));

	}
	
	
	/**
	 * Checks whether the given account holds the CMS Administrator role.
	 *
	 * @param user a {@link User} or a {@link UserProfile} returned by this tool's lookups
	 * @return {@code true} if the account is a CMS Administrator
	 */
	public boolean isCMSAdmin(final Object user) {
		if (user instanceof UserProfile) {
			return ((UserProfile) user).isAdmin();
		}
		if (user instanceof User) {
			return ((User) user).isAdmin();
		}
		return false;
	}

	/**
	 * Resolves the user id of an account passed in by a template.
	 *
	 * @param user a {@link User}, a {@link UserProfile}, or anything else
	 * @return the user id, or {@code null} if {@code user} is not an account
	 */
	private static String userIdOf(final Object user) {
		if (user instanceof UserProfile) {
			return ((UserProfile) user).getUserId();
		}
		if (user instanceof User) {
			return ((User) user).getUserId();
		}
		return null;
	}

	/**
	 * Prepares a looked-up account for a template: a read-only {@link UserProfile} while
	 * {@link FeatureFlagName#FEATURE_FLAG_CMSUSER_READ_ONLY_PROFILES} is on (the default), or a
	 * detached {@link User} copy without credential fields when it is off.
	 *
	 * @param user the looked-up account, may be {@code null}
	 * @return the object handed to the template, or {@code null}
	 */
	private static Object forTemplate(final User user) {
		if (user == null) {
			return null;
		}
		if (Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_CMSUSER_READ_ONLY_PROFILES, true)) {
			return new UserProfile(user);
		}
		// another account's login addresses are not needed by templates
		final User copy = UserCopies.withoutCredentials(user);
		copy.setLoginIP(null);
		copy.setLastLoginIP(null);
		return copy;
	}

}