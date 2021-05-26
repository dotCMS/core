package com.dotcms.filters.interceptor.dotcms;

import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.WebKeys;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Interceptor created to intercept requests to internal paths that required authentication.
 *
 * <p>Open access to internal web folders opens the door for XSS attacks.</p>
 * @author Jonathan Gamba 9/12/18
 */
public class DefaultBackEndLoginRequiredWebInterceptor implements WebInterceptor {

    private static final String ALLOWED_SUB_PATHS_WITHOUT_AUTHENTICATION = "ALLOWED_SUB_PATHS_WITHOUT_AUTHENTICATION";
    private static final String AUTHENTICATION_REQUIRED_PATHS = "AUTHENTICATION_REQUIRED_PATHS";

    private static final String DEFAULT_ALLOWED_SUB_PATHS = "/html/js/dojo,"
            + "/html/fonts,"
            + "/html/images/backgrounds,/html/images/persona,"
            + "/html/portal/login.jsp,"
            + "/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/,"
            + "/DotAjaxDirector/com.dotmarketing.sitesearch.ajax.SiteSearchAjaxAction/cmd/";
    private static String[] ALLOWED_SUB_PATHS;

    // \A -> The beginning of the input
    // All paths needs to be in lower case as the URI is lowercase before to be evaluated
    private static final String DEFAULT_REQUIRED_URLS = "\\A/html/,\\A/c/,\\A/servlets/," +
            "\\A/dottaillogservlet,\\A/categoriesservlet/,\\A/dwr/,\\A/dotajaxdirector," +
            "\\A/dotscheduledjobs,\\A/dotadmin/#/c/,\\A/jsontags/,\\A/edit/," +
            "\\A/dotadmin/c/,\\A/servlet/";

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
        final String loginRequiredPaths = Config
                .getStringProperty(AUTHENTICATION_REQUIRED_PATHS, DEFAULT_REQUIRED_URLS);
        return loginRequiredPaths.split(",");
    }

    @Override
    public void init() {

        //Set the list of allowed paths without authentication
        final String allowedPaths = Config
                .getStringProperty(ALLOWED_SUB_PATHS_WITHOUT_AUTHENTICATION,
                        DEFAULT_ALLOWED_SUB_PATHS);
        ALLOWED_SUB_PATHS = allowedPaths.split(",");
    }

    @Override
    public Result intercept(final HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Result result = Result.NEXT;

        boolean requiresAuthentication = true;

        //Verify if the requested url requires authentication
        final String requestedURI = request.getRequestURI();
        if (null != requestedURI) {
            for (final String allowedSubPath : ALLOWED_SUB_PATHS) {

                if (requestedURI.toLowerCase().startsWith(allowedSubPath.toLowerCase())) {
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
                response.setContentType("text/html");
                response.getWriter().print(unauthorizedHtmlResponse());

                result = Result.SKIP_NO_CHAIN; // needs to stop the filter chain.
            }
        }

        return result; // if it is log in, continue!
    }

    /**
     * HTML response that will be mainly use for angular in order to identify we have a 401.
     * Basically from angular there is not a simpler way to identify the status of the requested URL
     * by an iframe but angular can check things like the title of the iframe and handle according
     * to that.
     */
    private String unauthorizedHtmlResponse() {

        return ""
                + "<html>\n"
                + " <head>\n"
                + "     <title>401</title>\n"
                + " </head>\n"
                + " <body>"
                + "     <h1>401 / Unauthorized</h1>\n"
                + "     <p>This server could not verify that you are authorized to access the URL "
                + "     requested. Either you supplied the wrong credentials (e.g., bad password), "
                + "     or your browser doesn't understand how to supply the credentials required.</p>\n"
                + " </body>\n"
                + "</html>";
    }

}
