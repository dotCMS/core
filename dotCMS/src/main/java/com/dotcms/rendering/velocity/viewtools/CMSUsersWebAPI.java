package com.dotcms.rendering.velocity.viewtools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;
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
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;

public class CMSUsersWebAPI implements ViewTool {

	private HttpServletRequest request;
	Context ctx;
	User user = null;

	private CategoryAPI categoryAPI = APILocator.getCategoryAPI();

	public CategoryAPI getCategoryAPI() {
		return categoryAPI;
	}

	public void setCategoryAPI(CategoryAPI categoryAPI) {
		this.categoryAPI = categoryAPI;
	}

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
	 * @param email
	 * @return
	 */

	public User getUserByEmail(String email) {
		try {
			return APILocator.getUserAPI().loadByUserByEmail(email, APILocator.getUserAPI().getSystemUser(), false);
		} catch (Exception e) {
			Logger.error(CMSUsersWebAPI.class,e.getMessage(), e);
			return new User();
		}
	}









	/**
	 * get User by user id
	 * 
	 * @param userId
	 *            userid of the user to be obtained
	 * @return User
	 */
	public User getUserByUserId(String userId) {
		if (UtilMethods.isSet(userId)) {
			try{
				return APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
			} catch (Exception e1) {
				Logger.error(CMSUsersWebAPI.class,e1.getMessage(), e1);
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * Retrieves the logged user from session
	 * 
	 * @param request
	 * @return
	 */
	public User getLoggedInUser(HttpServletRequest request) {
		User loggedInUser = PortalUtil.getUser(request);//back-end user

		if(!UtilMethods.isSet(loggedInUser)){
			HttpSession session = request.getSession(false);
			if (session != null) {
				loggedInUser = (User) session.getAttribute(WebKeys.CMS_USER);
			}
		}
		return loggedInUser;
	}





	/**
	 * This method return a list of users with the specified group
	 * @param groupId compare string
	 * @return List<User>
	 * @deprecated will now return a list of roles
	 */
	public List<User> getAllUsersInGroup(String groupId) {
		List<String> uids;
		List<User> ret = new ArrayList<User>();
		try {
			uids = APILocator.getRoleAPI().findUserIdsForRole(APILocator.getRoleAPI().loadRoleById(groupId));
		} catch (Exception e1) {
			Logger.error(this, e1.getMessage(), e1);
			return ret;
		}	
		
		for (String userId : uids) {
			try {
				ret.add(APILocator.getUserAPI().loadUserById(userId, APILocator.getUserAPI().getSystemUser(), true));
			} catch (Exception e1) {
				Logger.error(this, e1.getMessage(), e1);
			}	
		}
		return ret;
	}


	/**
	 * This method return all the groups joined by the specified user
	 * @param user User
	 * @return List<Group>
	 */
	public List<Role> getUserGroups(User user) {
		return getUserRoles(user);
	}



	/**
	 * This method return all the roles joined by the specified user
	 * @param user User
	 * @return List<Role>
	 */
	public List<Role> getUserRoles(User user) {
		try {
			return APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		}
	}


	/**
	 * This method return true|false if the user has
	 * a specific role
	 * @param user User
	 * @param roleName String the name of the role
	 * @return List<Role>
	 */
	public boolean isUserRole(User user, String roleName) {
		boolean retVal = false;
		if(roleName == null){
			return retVal;
		}

		List<Role> roles;
		try {
			roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
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
	 * This method return true|false if the user has
	 * a CMSAdmin role
	 * @param user User
	 * 
	 */
	public boolean isCMSAdmin(User user) {
		if(user==null) {
		    return false;
		}
		return user.isAdmin();
	}	
	
	
	
}