package com.dotcms.filters.interceptor.saml;

import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * This interceptor encapsulates the logic for Saml
 * Basically if there is any configuration set on apps and c
 * @author jsanca
 */
public class SamlWebInterceptor implements WebInterceptor {

    protected static final String BY_PASS_KEY   = "native";
    protected static final String BY_PASS_VALUE = "true";

    private final LoginServiceAPI loginServiceAPI;
    protected final UserWebAPI    userWebAPI;


    public SamlWebInterceptor() {
        this(APILocator.getLoginServiceAPI());
    }

    public SamlWebInterceptor(final LoginServiceAPI loginServiceAPI) {
        this.loginServiceAPI = loginServiceAPI;
    }

    @Override
    public Result intercept(final HttpServletRequest request,
                            final HttpServletResponse response) throws IOException {

        final HttpSession session  = request.getSession(false);
        final boolean     useSaml  = this.isAnySamlConfigurated(request, session);
        Result result              = Result.NEXT;

        if (useSaml && null != session) {

            if (this.isByPass(request, session)) {

                Logger.info(this, ()->"Using SAML by pass");
                return Result.NEXT;
            }

            // todo: continue here the saml login logic
        }

        return result;
    } // intercept.

    protected boolean isByPass(final HttpServletRequest request, final HttpSession session) {

        String byPass = request.getParameter(BY_PASS_KEY);

        if (null != session) {
            if (null != byPass) {

                session.setAttribute(BY_PASS_KEY, byPass);
            } else {
                if (this.isNotLogged(request, session)) {

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
     * @param session
     *            {@link HttpSession}
     * @return boolean
     */
    protected boolean isNotLogged(final HttpServletRequest request, final HttpSession session) {
        boolean isNotLogged = true;
        boolean isBackend   = this.isBackEndAdmin(session, request.getRequestURI());
        try {
            isNotLogged = isBackend? !this.userWebAPI.isLoggedToBackend(request)
                    : null == this.userWebAPI.getLoggedInFrontendUser(request);

            Logger.debug(this, "Trying to go to back-end login? " + isBackend + ", Is user NOT logged in? " + isNotLogged);
        } catch (PortalException | SystemException e) {

            Logger.error(this, e.getMessage(), e);
            isNotLogged = true;
        }

        return isNotLogged;
    }

    /**
     * Determines whether the user in the {@link HttpSession} object or the incoming URI belong to the
     * dotCMS back-end login mechanism or not.
     *
     * @param session The {@link HttpSession} object containing user information.
     * @param uri     The incoming URI for login.
     *
     * @return If the user or its URI can be associated to the dotCMS back-end login, returns {@code true}. Otherwise,
     * returns {@code false}.
     */
    protected boolean isBackEndAdmin(final HttpSession session, final String uri) {

        return PageMode.get(session).isAdmin || this.isBackEndLoginPage(uri);
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
} // E:O:F:SamlWebInterceptor.
