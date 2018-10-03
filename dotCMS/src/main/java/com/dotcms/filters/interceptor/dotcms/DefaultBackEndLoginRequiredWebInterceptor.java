package com.dotcms.filters.interceptor.dotcms;

import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletURLUtil;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.WebKeys;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Interceptor created mainly to intercept requests to the internal <i>/html</i> folder but can be use
 * to verify if any internal folder requires authentication just changing the <i>getFilters()</i> method.
 *
 * <p>Open access to internal web folders opens the door for XSS attacks.</p>
 * @author Jonathan Gamba 9/12/18
 */
public class DefaultBackEndLoginRequiredWebInterceptor implements WebInterceptor {

    public static final String ALLOWED_HTML_PATHS_WITHOUT_AUTHENTICATION = "ALLOWED_HTML_PATHS_WITHOUT_AUTHENTICATION";

    private static final String LOGIN_URL = String
            .format("/%s/#/public/login", PortletURLUtil.URL_ADMIN_PREFIX);

    private static final String DEFAULT_ALLOWED_URLS = "/html/js/dojo,"
            + "/html/images/backgrounds,/html/images/persona";
    private static String[] ALLOWED_URLS;

    private UserWebAPI userWebAPI;

    public DefaultBackEndLoginRequiredWebInterceptor() {
        this(WebAPILocator.getUserWebAPI());
    }

    @VisibleForTesting
    protected DefaultBackEndLoginRequiredWebInterceptor(final UserWebAPI userWebAPI) {
        this.userWebAPI = userWebAPI;
    }

    @Override
    public String[] getFilters() {
        return new String[]{"\\A/html/"};
    }

    @Override
    public void init() {

        //Set the list of allowed paths without authentication
        final String allowedPaths = Config
                .getStringProperty(ALLOWED_HTML_PATHS_WITHOUT_AUTHENTICATION, DEFAULT_ALLOWED_URLS);
        ALLOWED_URLS = allowedPaths.split(",");
    }

    @Override
    public Result intercept(final HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Result result = Result.NEXT;

        boolean requiresAuthentication = true;

        //Verify if the requested url requires authentication
        final String requestedURI = request.getRequestURI();
        if (null != requestedURI) {
            for (final String allowedURL : ALLOWED_URLS) {

                if (requestedURI.startsWith(allowedURL.toLowerCase())) {
                    requiresAuthentication = false;
                    break;
                }
            }
        }

        if (requiresAuthentication) {

            boolean isLoggedToBackend = false;
            try {
                isLoggedToBackend = this.userWebAPI.isLoggedToBackend(request);
            } catch (Exception e) {
                //Do nothing...
                Logger.warn(this.getClass(), e.getMessage(), e);
            }

            // if we are not logged in...
            if (!isLoggedToBackend) {

                final String queryStringToAppend =
                        null != request.getQueryString() ? "?" + request.getQueryString() : "";
                final String completeRequestedURL = requestedURI + queryStringToAppend;

                SecurityLogger.logInfo(this.getClass(),
                        "LoginRequiredFilter for requested url: " + completeRequestedURL);

                final HttpSession session = request.getSession(false);
                if (null != session) {
                    session.setAttribute(WebKeys.REDIRECT_AFTER_LOGIN, SecurityUtils
                            .stripReferer(request, completeRequestedURL));
                }
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.sendRedirect(LOGIN_URL);

                result = Result.SKIP_NO_CHAIN; // needs to stop the filter chain.
            }
        }

        return result; // if it is log in, continue!
    }

}