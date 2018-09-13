package com.dotcms.filters.interceptor.dotcms;

import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.util.PortletURLUtil;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.WebKeys;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author Jonathan Gamba 9/12/18
 */
public class DefaultBackEndLoginRequiredWebInterceptor implements WebInterceptor {

    private static final String LOGIN_URL = String
            .format("/%s/#/public/login", PortletURLUtil.URL_ADMIN_PREFIX);

    private static final String[] ALLOWED_URLS =
            new String[]{"/html/js/dojo",
                    "/html/images/backgrounds", "/html/images/persona",
                    "/html/portal/login.jsp"};

    @Override
    public String[] getFilters() {
        return new String[]{"/html"};
    }

    @Override
    public Result intercept(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        final HttpSession session = request.getSession(false);
        Result result = Result.NEXT;

        boolean requiresAuthentication = true;

        //Verify if the requested url requires authentication
        final String requestedURI = request.getRequestURI();
        if (null != requestedURI) {
            for (final String allowedURL : ALLOWED_URLS) {

                if (requestedURI.startsWith(allowedURL)) {
                    requiresAuthentication = false;
                    break;
                }
            }
        }

        if (requiresAuthentication) {

            // if we are not logged in...
            if (null == session || session.getAttribute(WebKeys.CMS_USER) == null) {

                final String queryStringToAppend =
                        null != request.getQueryString() ? "?" + request.getQueryString() : "";
                final String completeRequestedURL = requestedURI + queryStringToAppend;

                SecurityLogger.logInfo(this.getClass(),
                        "LoginRequiredFilter for requested url: " + completeRequestedURL);

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