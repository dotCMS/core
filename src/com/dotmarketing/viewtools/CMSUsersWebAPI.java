package com.dotmarketing.viewtools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.ChallengeQuestion;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.cms.factories.PublicAddressFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.factories.ChallengeQuestionFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.user.factories.UserCommentsFactory;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
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
		HttpSession ses = request.getSession(false);
		if (ses != null)
			user = (User) ses.getAttribute(WebKeys.CMS_USER);

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
	 * getAddresesByUser
	 * 
	 * @param user User
	 * @return List
	 */
	public List getAddresesByUser(User user) {
		try {
			return PublicAddressFactory.getAddressesByUserId(user.getUserId());
		} catch (Exception e) {

			Logger.error(this,e.getMessage(),e);
		}
		return new ArrayList();
	}

	/**
	 * getCommentsByUser
	 * 
	 * @param user User
	 * @return List
	 */
	public List getCommentsByUser(User user) {

		UserProxy up;
		try {
			up = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}	

		return UserCommentsFactory.getUserCommentsByProxyInode(up.getInode());

	}

	/**
	 * Return user challenge question
	 * 
	 * @param email String
	 * @return String with the challenge question
	 */
	public String getUserChallengeQuestionByEmail(String email) {
		String result = "";

		try {
			User user = APILocator.getUserAPI().loadByUserByEmail(email, APILocator.getUserAPI().getSystemUser(), false);
			if (user != null) {
				UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);

				ChallengeQuestion challengeQuestion = ChallengeQuestionFactory.getChallengeQuestionById(Long
						.parseLong(userProxy.getChallengeQuestionId()));

				if (challengeQuestion != null)
					result = challengeQuestion.getChallengeQuestionText();
			} else {
				return null;
			}
		} catch (Exception e) {
			Logger.warn(CMSUsersWebAPI.class, e.toString());
		}

		return result;
	}

	/**
	 * Return user challenge question
	 * 
	 * @param userId String
	 * @return String with the challenge question
	 */
	public String getUserChallengeQuestionByUserId(String userId) {
		String result = "";

		try {
			User user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
			if (user != null) {
				UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);

				ChallengeQuestion challengeQuestion = ChallengeQuestionFactory.getChallengeQuestionById(Long
						.parseLong(userProxy.getChallengeQuestionId()));

				if (challengeQuestion != null)
					result = challengeQuestion.getChallengeQuestionText();
			} else {
				return null;
			}
		} catch (Exception e) {
			Logger.warn(CMSUsersWebAPI.class, e.toString());
		}

		return result;
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
		User loggedInUser = null;
		try {
			loggedInUser = PortalUtil.getUser(request);//back-end user
		} catch (PortalException e) {
			Logger.error(CMSUsersWebAPI.class,e.getMessage(), e);
		} catch (SystemException e) {
			Logger.error(CMSUsersWebAPI.class,e.getMessage(), e);
		}
		if(!UtilMethods.isSet(loggedInUser)){
			HttpSession session = request.getSession(false);
			if (session != null) {
				loggedInUser = (User) session.getAttribute(WebKeys.CMS_USER);
			}
		}
		return loggedInUser;
	}

	public void doLoginMacro(HttpServletRequest request, HttpServletResponse response){
		String referrer = null;
		if (UtilMethods.isSet(request.getSession().getAttribute("referrer")) )
		{
			referrer = (String)request.getSession().getAttribute("referrer");
		}
		if (UtilMethods.isSet(request.getParameter("referrer")) )
		{
			referrer = (String)request.getParameter("referrer");
		}
		if(UtilMethods.isSet(referrer)){
			request.getSession().setAttribute("referrer", referrer);
		}


		String loginAction = request.getParameter("_loginAction");
		if(loginAction == null){
			return;
		}



		if("login".equals(loginAction)){
			boolean _rVal = false;


			boolean rememberMe= (request.getParameter("_loginRememberMe") != null);
			String userName = request.getParameter("_loginUserName");
			String password= request.getParameter("_loginPassword");




			try {
				_rVal = LoginFactory.doLogin(userName, password, rememberMe, request, response);                
			} catch (NoSuchUserException e) {
				_rVal = false;
				Logger.debug(this, "failed login from:" + request.getRemoteHost());
			}
			if(! _rVal){
				ctx.put("_loginMessage", "dotcms.macro.login.failed");
			}

			if( _rVal && UtilMethods.isSet(referrer)){
				try {
					response.sendRedirect(referrer);
					request.getSession().removeAttribute("referrer");
					return;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Logger.error(this,e.getMessage(),e);
				}
			}       
		}
		else if("logout".equals(loginAction)){
			LoginFactory.doLogout(request, response);
			ctx.remove("user");
			ctx.put("_loginMessage", "dotcms.macro.login.loggedOut");
		}
		return;
	}

	/**
	 * This method return a paginated list of the info of user and user proxy that have a firstname, lastname, user proxy title, groups and roles.
	 * @param firstName compare string
	 * @param lastName compare string
	 * @param title compare string
	 * @param groups list of group names
	 * @param showUserGroups boolean. If true then a list of group names will returned in the result. This value will be automatically set to true if the parameter group list is greater than 0 or the order by parameter is ordered by group name.
	 * @param roles list of role names
	 * @param showUserRoles boolean. If true then a list of role names will returned in the result. This value will be automatically set to true if the parameter role list is greater than 0 or the order by parameter is ordered by role name.
	 * @param orderBy how will be ordered the result
	 * @param page page to display
	 * @param pageSize number of element to show in the page
	 * @return HashMap<String, Object>: The list of users found will be associated to the key 'users'. The list of users proxy found will be associated to the key 'usersProxy'. The total number of items returned is associated to the key 'total'. A list of group names can be found and associated to the key 'groupNames' if a group list is passed in. A list of role names can be found and associated to the key 'roleNames' if a role list is passed in.
	 */
	public HashMap<String, Object> searchUsersAndUsersProxy(String firstName, String lastName, String title, List<String> groupNames, boolean showUserGroups, List<String> roleKeys, boolean showUserRoles, String orderBy, int page, int pageSize) {

		List<Role> roles = new ArrayList<Role>();
		if ((roleKeys != null) && (0 < roleKeys.size())) {
			Role role;

			for (String key: roleKeys) {
				try {
					role = APILocator.getRoleAPI().loadRoleByKey(key);
				} catch (DotDataException e) {
					Logger.error(this,e.getMessage(), e);
					return  new HashMap<String, Object>();
				}
				if ((role.getId() != null) && (!role.getId().equals("0")))
					roles.add(role);
				else
					return null;
			}
		}

		try {
			return APILocator.getUserProxyAPI().searchUsersAndUsersProxy(firstName, lastName, title, showUserGroups, roles, showUserRoles, orderBy, page, pageSize);
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(),e);
			return new HashMap<String, Object>();
		}
	}

	/**
	 * This method return all the posible title setted by the users.
	 * @return List<String> of titles.
	 */
	public List<String> getUsersTitle() {
		try {
			return APILocator.getUserProxyAPI().findUsersTitle();
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
			return new ArrayList<String>();
		}
	}

	/**
	 * This method return a list of users and roles which name are like the compared string passed
	 * @param filter compare string
	 * @param start first element to display
	 * @param limit max number of elements to show
	 * @return Map<String, Object>
	 */
	public Map<String, Object> getUsersAnRolesByName(String filter, int start,int limit) {
		try {
			return APILocator.getUserAPI().getUsersAnRolesByName(filter, start, limit);
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(),e);
			return new HashMap<String, Object>();
		}
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
	 * This method return user proxy of the specified user
	 * @param user User
	 * @return UserProxy
	 */
	public UserProxy getUserProxy(User user) {
		try {
			return com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}	
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


}