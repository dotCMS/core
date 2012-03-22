package com.liferay.portal.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
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

public class LogoutAsAction extends Action {

	public ActionForward execute(
			ActionMapping mapping, ActionForm form, HttpServletRequest req,
			HttpServletResponse res)
		throws Exception {

		if(req.getSession().getAttribute(WebKeys.PRINCIPAL_USER_ID) != null) {
			User principalUser = APILocator.getUserAPI().loadUserById((String)req.getSession().getAttribute(WebKeys.PRINCIPAL_USER_ID),APILocator.getUserAPI().getSystemUser(),false);
			User currentUser = PortalUtil.getUser(req);
			Logger.info(this, "User " + principalUser.getFullName() + 
					" (" + principalUser.getUserId() + ") has sucessfully logged out as " + currentUser.getFullName() + "("
					+ currentUser.getUserId() + "). Remote IP: " + req.getRemoteAddr());
			req.getSession().setAttribute(WebKeys.USER_ID, req.getSession().getAttribute(WebKeys.PRINCIPAL_USER_ID));
			req.getSession().removeAttribute(WebKeys.PRINCIPAL_USER_ID);
			PrincipalThreadLocal.setName(principalUser.getUserId());

		} else {
			Logger.info(this, "An invalid request to logout as a different user was made by " + PortalUtil.getUser(req).getFullName() + 
					" (" + PortalUtil.getUser(req).getUserId() + "), the user is not logged in as a different user. " +
							"Remote IP: " + req.getRemoteAddr());

		}
		
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
		return mapping.findForward(Constants.COMMON_REFERER);

	}

}
