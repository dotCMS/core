package com.dotmarketing.business.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotmarketing.business.UserAPIImpl;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;


/**
 */
public class UserWebAPIImpl extends UserAPIImpl implements UserWebAPI {

	public UserWebAPIImpl() {
		
	}

	public User getLoggedInUser(HttpServletRequest request)
			throws DotRuntimeException, PortalException, SystemException {
		User user = PortalUtil.getUser(request);
		if(user == null) {
			//Assuming is a front-end access
			HttpSession session = request.getSession(false);
			if(session != null)
				user = (User)session.getAttribute(WebKeys.CMS_USER);
		}
		return user;
	}

	public boolean isLoggedToBackend(HttpServletRequest request)
			throws DotRuntimeException, PortalException, SystemException {
		return PortalUtil.getUser(request) != null;
	}

	public User getLoggedInFrontendUser(HttpServletRequest request) throws DotRuntimeException, PortalException, SystemException {
		HttpSession session = request.getSession(false);
		if(session != null)
			return (User) session.getAttribute(WebKeys.CMS_USER);
		return null;
	}

	public boolean isLoggedToFrontend(HttpServletRequest req) throws DotRuntimeException, PortalException, SystemException {
		return !isLoggedToBackend(req);
	}


}
