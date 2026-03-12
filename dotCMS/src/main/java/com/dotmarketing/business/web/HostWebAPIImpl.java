/**
 *
 */
package com.dotmarketing.business.web;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
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
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.contentlet.business.HostAPIImpl;
import com.dotmarketing.portlets.contentlet.business.HostResolutionResult;
import com.dotmarketing.portlets.contentlet.business.HostResolver;
import com.dotmarketing.portlets.contentlet.business.NestedHostPatternCache;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.RenderRequestImpl;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Optional;

/**
 * 
 * @author david torres
 *
 */
@ApplicationScoped
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

    public Host getCurrentHost() throws DotDataException, DotSecurityException {
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();

        if (request == null) {
            return APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), true);
        }

        return this.getCurrentHost(request);
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
        final boolean respectAnonPerms = user == null || user.isFrontendUser() || !user.isBackendUser();

        Optional<Host> optionalHost = this.getCurrentHostFromRequest(request, user, respectAnonPerms);

        if (optionalHost.isEmpty() && user.isBackendUser()){
            optionalHost = this.getCurrentHostFromSession(request, user, respectAnonPerms);
        }

        // Resolve host from server name when no explicit override is present
        Host host = optionalHost.isPresent()
                ? optionalHost.get()
                : resolveHostName(request.getServerName(), user, respectAnonPerms);

        // For requests resolved via the HTTP Host header (not via a session or request-param
        // override), check whether the URI belongs to a more-specific nested host.  Backend users
        // with an explicit session-based host selection are not subject to nested-host routing so
        // that the admin UI always addresses the selected site directly.
        if (optionalHost.isEmpty()) {
            host = applyNestedHostResolution(request, host, user, respectAnonPerms);
        }

        checkHostPermission(user, respectAnonPerms, host);
        storeCurrentHost(request, user, host);

        return host;
    }

    /**
     * Checks the {@link NestedHostPatternCache} to see whether the incoming request URI matches a
     * nested host that lives under {@code topLevelHost}.
     *
     * <p>When a nested host <em>is</em> matched:</p>
     * <ol>
     *   <li>The remaining URI (the original URI with the nested-host path prefix stripped) is stored
     *       as the {@link Constants#CMS_FILTER_URI_OVERRIDE} request attribute so that
     *       {@link com.dotmarketing.filters.CMSFilter} and every downstream component that calls
     *       {@link com.dotmarketing.filters.CMSUrlUtil#getURIFromRequest} transparently sees the
     *       content-relative path of the nested site instead of the full, prefixed URL.</li>
     *   <li>The nested {@link Host} object is returned so that content resolution, permission
     *       checks, and session storage all operate against the correct site.</li>
     * </ol>
     *
     * <p>If no nested host pattern matches, or if any error occurs while loading the nested host
     * object, {@code topLevelHost} is returned unchanged and no request attribute is modified.</p>
     *
     * @param request          the current HTTP servlet request
     * @param topLevelHost     the top-level host resolved from the HTTP {@code Host} header
     * @param user             the current user (used for permission-checked host lookup)
     * @param respectAnonPerms whether anonymous front-end permissions should be respected
     * @return the nested {@link Host} when a pattern matched; otherwise {@code topLevelHost}
     */
    private Host applyNestedHostResolution(final HttpServletRequest request,
                                           final Host topLevelHost,
                                           final User user,
                                           final boolean respectAnonPerms) {

        if (topLevelHost == null || !UtilMethods.isSet(topLevelHost.getIdentifier())) {
            return topLevelHost;
        }

        final String requestUri = request.getRequestURI();
        if (!UtilMethods.isSet(requestUri)) {
            return topLevelHost;
        }

        final HostResolutionResult resolution = HostResolver.getInstance()
                .resolve(topLevelHost.getIdentifier(), requestUri);

        if (!resolution.isNested()) {
            return topLevelHost;
        }

        try {
            final Host nestedHost = find(resolution.getResolvedHostId(), user, respectAnonPerms);
            if (nestedHost != null && UtilMethods.isSet(nestedHost.getIdentifier())) {
                // Override the URI so that CMSFilter's getURIFromRequest returns the stripped path
                request.setAttribute(Constants.CMS_FILTER_URI_OVERRIDE, resolution.getRemainingUri());
                Logger.debug(HostWebAPIImpl.class,
                        () -> "Nested host resolved: uri=" + requestUri
                                + " topLevelHost=" + topLevelHost.getHostname()
                                + " nestedHost=" + nestedHost.getHostname()
                                + " remainingUri=" + resolution.getRemainingUri());
                return nestedHost;
            }
        } catch (final Exception e) {
            Logger.warn(HostWebAPIImpl.class,
                    "Could not load nested host '" + resolution.getResolvedHostId()
                            + "' for URI '" + requestUri + "': " + e.getMessage());
        }

        return topLevelHost;
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
            Logger.error(HostWebAPIImpl.class, message);
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

        // AC10: prefer the nested-host resolved by NestedHostResolutionFilter (set as CMS_RESOLVED_HOST)
        if (request.getAttribute(WebKeys.CMS_RESOLVED_HOST) != null) {
            return Optional.of((Host) request.getAttribute(WebKeys.CMS_RESOLVED_HOST));
        }

	    final String hostId = request.getParameter("host_id");

	    if (hostId != null && user.isBackendUser()) {
	        return Optional.ofNullable(find(hostId, user, respectAnonPerms));
        } else if (UtilMethods.isSet(request.getParameter(Host.HOST_VELOCITY_VAR_NAME))
            || UtilMethods.isSet(request.getAttribute(Host.HOST_VELOCITY_VAR_NAME))) {

            final String hostVelocityVarName = request.getParameter(Host.HOST_VELOCITY_VAR_NAME);

	        final String hostIdOrName = UtilMethods.isSet(hostVelocityVarName)
                            ? hostVelocityVarName
                            : (String) request.getAttribute(Host.HOST_VELOCITY_VAR_NAME);

            Host host = find(hostIdOrName, user, respectAnonPerms);

            if(host!=null) return Optional.of(host);

            return this.resolveHostNameWithoutDefault(hostIdOrName, user, respectAnonPerms);
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
