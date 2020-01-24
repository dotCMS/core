/**
 * 
 */
package com.dotmarketing.business.web;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPIImpl;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.RenderRequestImpl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Optional;

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
    public Host getCurrentHost(final HttpServletRequest request)
            throws DotDataException, DotSecurityException {

        return this.getCurrentHost(request, null);
    }

    @Override
    @CloseDBIfOpened
    public Host getCurrentHost(final HttpServletRequest request, final User userParam)
            throws DotDataException, DotSecurityException {

        final UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
        final User user       = userParam != null ? userParam : userWebAPI.getSystemUser();
        final boolean respectAnonPerms = user == null || !user.isBackendUser();

        Optional<Host> optionalHost = this.getCurrentHostFromRequest(request, user, respectAnonPerms);

        if (!optionalHost.isPresent() && user.isBackendUser()){
            optionalHost = this.getCurrentHostFromSession(request, user, respectAnonPerms);
        }

        final Host host = optionalHost.isPresent() ? optionalHost.get() : resolveHostName(request.getServerName(),
                user, respectAnonPerms);

        checkHostPermission(user, respectAnonPerms, host);
        storeCurrentHost(request, user, host);

        return host;
    }

    private void storeCurrentHost(final HttpServletRequest request, final User user, final Host host) {
        final HttpSession session = request.getSession(false);

        request.setAttribute(WebKeys.CURRENT_HOST, host);

        if (session != null && user.isBackendUser()) {
            session.setAttribute(WebKeys.CURRENT_HOST, host);
        }
    }

    private void checkHostPermission(final User user, boolean respectAnonPerms, final Host host)
            throws DotDataException, DotSecurityException {

        if(!APILocator.getPermissionAPI().doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, respectAnonPerms)){
            final String userId = (user != null) ? user.getUserId() : null;

            final String message = "User " + userId + " does not have permission to host:" + host.getHostname();
            Logger.error(HostAPIImpl.class, message);
            throw new DotSecurityException(message);
        }
    }

    private Optional<Host> getCurrentHostFromSession(final HttpServletRequest request, final User user, final boolean respectAnonPerms)
            throws DotSecurityException, DotDataException {

        final HttpSession session = request.getSession(false);

        if (session == null){
            return Optional.empty();
        }

        Host host = null;
        if (session.getAttribute(WebKeys.CURRENT_HOST) != null) {
            final Host hostFromSession = (Host) session.getAttribute(WebKeys.CURRENT_HOST);
            final Object hostId = session.getAttribute(WebKeys.CMS_SELECTED_HOST_ID);
            host = hostFromSession.getIdentifier().equals(hostId) ? hostFromSession : null;
        }

        if (host == null && session.getAttribute(WebKeys.CMS_SELECTED_HOST_ID) != null) {
            final String hostId = (String) session.getAttribute(WebKeys.CMS_SELECTED_HOST_ID);
            host = find(hostId, user, respectAnonPerms);
        }

        return Optional.ofNullable(host);
    }

    private Optional<Host> getCurrentHostFromRequest(final HttpServletRequest request, final User user, final boolean respectAnonPerms)
            throws DotDataException, DotSecurityException {

	    final String hostId = request.getParameter("host_id");

	    if (hostId != null && user.isBackendUser()) {
	        return Optional.ofNullable(find(hostId, user, respectAnonPerms));
        } else if (request.getParameter(Host.HOST_VELOCITY_VAR_NAME) != null) {
            final String hostName = request.getParameter(Host.HOST_VELOCITY_VAR_NAME);
            return this.resolveHostNameWithoutDefault(hostName, user, respectAnonPerms);
        } else if (request.getAttribute(WebKeys.CURRENT_HOST) != null) {
	        return Optional.of((Host) request.getAttribute(WebKeys.CURRENT_HOST));
        } else {
	        return Optional.empty();
        }
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
