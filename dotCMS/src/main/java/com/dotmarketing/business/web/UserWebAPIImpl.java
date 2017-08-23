package com.dotmarketing.business.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
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

	@CloseDBIfOpened
    @Override
    public User getUser(HttpServletRequest request) {
      try{
        User user = PortalUtil.getUser(request);
        if(user == null){
          user = this.getLoggedInUser(request.getSession(false));
        }
        if(user==null){
          user =  APILocator.getUserAPI().getAnonymousUser();
        }
        return user;
      }catch(Exception e){
        throw new DotStateException(e);
      }
    }
    
	@Override
	public User getLoggedInUser(HttpServletRequest request)
			throws DotRuntimeException, PortalException, SystemException {
		User user = PortalUtil.getUser(request);
		return (user == null)?
				//Assuming is a front-end access
				this.getLoggedInUser(request.getSession(false)):user;
	}

	@Override
	public User getLoggedInUser(final HttpSession session) {

		return  (session != null)?
					(User)session.getAttribute(WebKeys.CMS_USER):null;
	}

	@Override
	public boolean isLoggedToBackend(HttpServletRequest request)
			throws DotRuntimeException, PortalException, SystemException {
		return PortalUtil.getUser(request) != null;
	}

	@Override
	public User getLoggedInFrontendUser(HttpServletRequest request) throws DotRuntimeException, PortalException, SystemException {
		HttpSession session = request.getSession(false);
		if(session != null)
			return (User) session.getAttribute(WebKeys.CMS_USER);
		return null;
	}

	@Override
	public boolean isLoggedToFrontend(HttpServletRequest req) throws DotRuntimeException, PortalException, SystemException {
		return !isLoggedToBackend(req);
	}


}
