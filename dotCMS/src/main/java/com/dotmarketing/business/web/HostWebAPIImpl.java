/**
 * 
 */
package com.dotmarketing.business.web;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPIImpl;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.RenderRequestImpl;

/**
 * 
 * @author david torres
 *
 */
public class HostWebAPIImpl extends HostAPIImpl implements HostWebAPI {

	public Host getCurrentHost(RenderRequest req) throws DotDataException, DotSecurityException, PortalException, SystemException {
		return getCurrentHost(((RenderRequestImpl)req).getHttpServletRequest());
	}
	
	public Host getCurrentHost(ActionRequest req) throws DotDataException, DotSecurityException, PortalException, SystemException {
		return getCurrentHost(((ActionRequestImpl)req).getHttpServletRequest());
	}
	
	@Override
	public Host getHost(HttpServletRequest request)  {
	     try{
	       return getCurrentHost(request);
	     }catch(Exception e){
	       throw new DotStateException(e);
	     }
	  
	  
	}
	
	
    @CloseDBIfOpened
    public Host getCurrentHost(HttpServletRequest request)
            throws DotDataException, DotSecurityException, PortalException, SystemException {
        Host host = null;
        HttpSession session = request.getSession(false);
        UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
        User systemUser = userWebAPI.getSystemUser();
        boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(request);

        PageMode mode = PageMode.get(request);

        String pageHostId = request.getParameter("host_id");
        if (pageHostId != null && !mode.showLive) {
            host = find(pageHostId, systemUser, respectFrontendRoles);
        } else {
            if (session != null && mode.isAdmin && session.getAttribute(WebKeys.CURRENT_HOST) != null) {
                host = (Host) session.getAttribute(WebKeys.CURRENT_HOST);
            } else if (request.getAttribute(WebKeys.CURRENT_HOST) != null) {
                host = (Host) request.getAttribute(WebKeys.CURRENT_HOST);
            } else {
                String serverName = request.getServerName();
                if (UtilMethods.isSet(serverName)) {
                    host = resolveHostName(serverName, systemUser, respectFrontendRoles);
                }
            }
        }

        request.setAttribute(WebKeys.CURRENT_HOST, host);
        if (session != null && mode.isAdmin) {
            session.setAttribute(WebKeys.CURRENT_HOST, host);
        }
        return host;
    }

	@Override
    public Host getCurrentHostNoThrow(HttpServletRequest request) {
       try {
           return getCurrentHost(request);
       }
       catch(Exception e) {
           throw new DotRuntimeException(e);
       }
    }
}
