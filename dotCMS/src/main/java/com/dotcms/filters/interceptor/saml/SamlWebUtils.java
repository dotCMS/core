package com.dotcms.filters.interceptor.saml;

import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates Saml util methods for Web.
 * @author jsanca
 */
public class SamlWebUtils {

    public static final String BY_PASS_KEY   = "native";
    public static final String BY_PASS_VALUE = "true";

    protected     final UserWebAPI userWebAPI;

    public SamlWebUtils() {
        this(WebAPILocator.getUserWebAPI());
    }

    @VisibleForTesting
    public SamlWebUtils(final UserWebAPI userWebAPI) {
        this.userWebAPI = userWebAPI;
    }

    protected boolean isByPass(final HttpServletRequest request, final HttpSession session) {

        String byPass = request.getParameter(BY_PASS_KEY);

        if (null != session) {
            if (null != byPass) {

                session.setAttribute(BY_PASS_KEY, byPass);
            } else {

                if (this.isNotLogged(request)) {

                    byPass = (String) session.getAttribute(BY_PASS_KEY);
                } else if (null != session.getAttribute(BY_PASS_KEY)) {

                    session.removeAttribute(BY_PASS_KEY);
                }
            }
        }

        return BY_PASS_VALUE.equalsIgnoreCase(byPass);
    }

    /**
     * Return true if the user is not logged. Work for FE and BE
     *
     * @param request
     *            {@link HttpServletRequest}
     * @return boolean
     */
    protected boolean isNotLogged(final HttpServletRequest request) {

        boolean isNotLogged     = true;
        final boolean isBackend = this.isBackEndAdmin(request, request.getRequestURI());

        try {

            isNotLogged = isBackend?
                    !this.userWebAPI.isLoggedToBackend(request):
                    null == this.userWebAPI.getLoggedInFrontendUser(request);

            Logger.debug(this, "Trying to go to back-end login? " + isBackend
                    + ", Is user NOT logged in? " + isNotLogged);
        } catch (PortalException | SystemException e) {

            Logger.error(this, e.getMessage(), e);
            isNotLogged = true;
        }

        return isNotLogged;
    }

    /**
     * Determines whether the user in the {@link HttpServletRequest} object or the incoming URI belong to the
     * dotCMS back-end login mechanism or not.
     *
     * @param request The {@link HttpServletRequest} request
     * @param uri     The incoming URI for login.
     *
     * @return If the user or its URI can be associated to the dotCMS back-end login, returns {@code true}. Otherwise,
     * returns {@code false}.
     */
    protected boolean isBackEndAdmin(final HttpServletRequest request, final String uri) {

        return PageMode.get(request).isAdmin || this.isBackEndLoginPage(uri);
    }

    /**
     * Analyzes the incoming URI and determines whether it belongs to dotCMS back-end login or logout URIs or not.
     *
     * @param uri The incoming URI.
     *
     * @return If the URI can be associated to the dotCMS back-end login or logout, returns {@code true}. Otherwise,
     * returns {@code false}.
     */
    protected boolean isBackEndLoginPage(final String uri) {

        return uri.startsWith("/dotAdmin") || uri.startsWith("/html/portal/login") || uri.startsWith("/c/public/login")
                || uri.startsWith("/c/portal_public/login") || uri.startsWith("/c/portal/logout");
    }

    protected boolean isFrontEndLoginPage(final String uri){

        return uri.startsWith("/dotCMS/login") || uri.startsWith("/application/login");
    }

    public boolean isLogoutRequest(final String requestURI, final String... logoutPathArray) {

        Logger.debug(this, ()-> "----------------------------- isLogoutRequest --------------------------------");
        Logger.debug(this, ()-> "- requestURI = " + requestURI);
        Logger.debug(this, ()-> "- logoutPathArray = " + Arrays.asList(logoutPathArray));

        boolean isLogoutRequest = false;

        if (null != logoutPathArray) {
            for (final String logoutPath : logoutPathArray) {

                if (requestURI.startsWith(logoutPath) || requestURI.equals(logoutPath)) {

                    isLogoutRequest = true;
                    break;
                }
            }
        }

        Logger.debug(this, "- isLogoutRequest = " + isLogoutRequest);

        return isLogoutRequest;
    }

    protected HttpSession renewSession(final HttpServletRequest request, final HttpSession currentSession) {

        String attributeName                        = null;
        Object attributeValue                       = null;
        Enumeration<String> attributesNames         = null;
        HttpSession renewSession                    = currentSession;
        final Map<String, Object> sessionAttributes = new HashMap<>();

        if (null != currentSession && !currentSession.isNew()) {

            Logger.debug(this, ()-> "Renewing the HTTP session");

            attributesNames = currentSession.getAttributeNames();

            while (attributesNames.hasMoreElements()) {

                attributeName  = attributesNames.nextElement();
                attributeValue = currentSession.getAttribute(attributeName);
                Logger.debug(this, "Copying attribute '" + attributeName + "' to the new session.");
                sessionAttributes.put(attributeName, attributeValue);
            }

            Logger.debug(this, ()-> "Killing the current session");
            currentSession.invalidate(); // kill the previous session

            Logger.debug(this, ()-> "Creating a new one");
            renewSession = request.getSession(true);

            for (final Map.Entry<String, Object> sessionEntry : sessionAttributes.entrySet()) {

                Logger.debug(this, ()->"Adding attribute '" + sessionEntry.getKey() + "' to the new session.");
                renewSession.setAttribute(sessionEntry.getKey(), sessionEntry.getValue());
            }

        }

        return renewSession;
    }


} // E:O:F:SamlWebUtils.
