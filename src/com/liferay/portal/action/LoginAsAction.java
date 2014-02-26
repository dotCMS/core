package com.liferay.portal.action;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotcms.util.SecurityUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.PortletURLImpl;
import com.liferay.util.Encryptor;

public class LoginAsAction extends Action {
	
	RoleAPI roleAPI;

	public LoginAsAction () {
		roleAPI = APILocator.getRoleAPI();
	}
	
	public ActionForward execute(
			ActionMapping mapping, ActionForm form, HttpServletRequest req,
			HttpServletResponse res)
		throws Exception {

		User currentUser = PortalUtil.getUser(req);
		Role loginAsRole = roleAPI.findRoleByFQN(Role.SYSTEM + " --> " + Role.LOGIN_AS);
		Role administratorRole  = roleAPI.findRoleByFQN(Role.SYSTEM + " --> " + Role.ADMINISTRATOR);
		if(!roleAPI.doesUserHaveRole(currentUser, loginAsRole)) {
			Logger.info(this, "An attempt to login as a different user was made by " + currentUser.getFullName() + 
					" (" + currentUser.getUserId() + "), without permission to login as. Remote IP: " + req.getRemoteAddr() + 
					". Hack Attempt?.");
			Thread.sleep(10000);
			throw new Exception ("Unable to login as without the proper Login As role");
		}
		
		String loginAsParameter = req.getParameter("portal_login_as_user");
		String loginAsUserID = null;
		if(!UtilMethods.isSet(loginAsParameter) && !loginAsParameter.startsWith("user-")) {
			Logger.info(this, "An invalid request to login as a different user was made by " + currentUser.getFullName() + 
					" (" + currentUser.getUserId() + "), without the required user id parameter. Remote IP: " + req.getRemoteAddr() + 
					". Hack Attempt?.");
			
			return mapping.findForward(Constants.COMMON_REFERER);
		} 
			
		loginAsUserID = loginAsParameter.split("-")[1];
	
		User loginAsUser = APILocator.getUserAPI().loadUserById(loginAsUserID,APILocator.getUserAPI().getSystemUser(),false);
		List<Layout> layouts = APILocator.getLayoutAPI().loadLayoutsForUser(loginAsUser);
		if ((layouts == null) || (layouts.size() == 0) || !UtilMethods.isSet(layouts.get(0).getId())) {
		   req.getSession().setAttribute("portal_login_as_error", "user-without-portlet");
		   Logger.info(this, "An invalid request to login as a different user was made by " + currentUser.getFullName() + 
		     " (" + currentUser.getUserId() + "), user dont have layouts. Remote IP: " + req.getRemoteAddr());
		   return mapping.findForward(Constants.COMMON_REFERER);
		}
		if(roleAPI.doesUserHaveRole(loginAsUser, administratorRole) || 
				roleAPI.doesUserHaveRole(loginAsUser, com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole())) {
			String passwordParameter = req.getParameter("portal_login_as_password");
			if(!UtilMethods.isSet(passwordParameter)) {
				req.getSession().setAttribute("portal_login_as_error", "please-enter-a-valid-password");
				Logger.info(this, "An invalid request to login as a different user was made by " + currentUser.getFullName() + 
						" (" + currentUser.getUserId() + "), invalid user password submitted. Remote IP: " + req.getRemoteAddr());
				return mapping.findForward(Constants.COMMON_REFERER);
			} else if (currentUser.getPasswordEncrypted() && !currentUser.getPassword().equals(Encryptor.digest(passwordParameter))) {
				req.getSession().setAttribute("portal_login_as_error", "please-enter-a-valid-password");
				Logger.info(this, "An invalid request to login as a different user was made by " + currentUser.getFullName() + 
						" (" + currentUser.getUserId() + "), invalid user password submitted. Remote IP: " + req.getRemoteAddr());
				return mapping.findForward(Constants.COMMON_REFERER);
			} else if (!currentUser.getPasswordEncrypted() && !currentUser.getPassword().equals(passwordParameter)) {
				req.getSession().setAttribute("portal_login_as_error", "please-enter-a-valid-password");
				Logger.info(this, "An invalid request to login as a different user was made by " + currentUser.getFullName() + 
						" (" + currentUser.getUserId() + "), invalid user password submitted. Remote IP: " + req.getRemoteAddr());
				return mapping.findForward(Constants.COMMON_REFERER);
			}
		}
			
		if(loginAsUserID.equals(currentUser.getUserId())) {
			Logger.info(this, "An invalid request to login as a different user was made by " + currentUser.getFullName() + 
					" (" + currentUser.getUserId() + "), trying to login as himself, request ignored. Remote IP: " + req.getRemoteAddr());
			return mapping.findForward(Constants.COMMON_REFERER);
		}
		
		if(req.getSession().getAttribute(WebKeys.PRINCIPAL_USER_ID) == null)
			req.getSession().setAttribute(WebKeys.PRINCIPAL_USER_ID, currentUser.getUserId());
		req.getSession().setAttribute(WebKeys.USER_ID, loginAsUserID);

		PrincipalThreadLocal.setName(loginAsUserID);
		
		Logger.info(this, "User " + currentUser.getFullName() + 
				" (" + currentUser.getUserId() + "), has sucessfully login as " + loginAsUser.getFullName() + 
				" (" + loginAsUserID + "). Remote IP: " + req.getRemoteAddr());

		

		
			try{
				HostWebAPI hostWebAPI  = WebAPILocator.getHostWebAPI();
				UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
				User systemUser = userWebAPI.getSystemUser();
				boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(req);
				String serverName = req.getServerName();
				Host host = null;
				if (UtilMethods.isSet(serverName)) {
				host = hostWebAPI.findByName(serverName, systemUser, respectFrontendRoles);
				if(host == null)
					host = hostWebAPI.findByAlias(serverName, systemUser, respectFrontendRoles);
				//If no host matches then we return the default host
				if(host == null)
					host = hostWebAPI.findDefaultHost(systemUser, respectFrontendRoles);
				} else {
					host = hostWebAPI.findDefaultHost(systemUser, respectFrontendRoles);
				}
				req.getSession().setAttribute(com.dotmarketing.util.WebKeys.CURRENT_HOST, host);
			
				//List<Layout> layouts = APILocator.getLayoutAPI().loadLayoutsForUser(loginAsUser);
				PortletURLImpl portletURLImp = new PortletURLImpl(req, layouts.get(0).getPortletIds().get(0), layouts.get(0).getId(), false);
				res.sendRedirect(SecurityUtils.stripReferer(req, portletURLImp.toString()));
				return null;
				
			}
			catch(Exception e){
				Logger.error(this.getClass(), "LoginAs redirect failed logging in as :" +loginAsUser);
			}

		return mapping.findForward(Constants.COMMON_REFERER);

	}

}
