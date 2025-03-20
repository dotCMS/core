package com.dotmarketing.servlets;

import com.dotcms.rest.WebResource;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Utility class for servlets
 */
public class ServletUtils {

    private ServletUtils() {
        // Utility class
    }

    /**
     * Get the current user from the request and authenticate if
     * an authentication method is included in the request (Basic, Bearer (JWT), etc.)
     *
     * @param webResource the web resource
     * @param req the request
     * @param resp the response
     * @return the current user (authenticated if a method is included in the request)
     */
    static User getUserAndAuthenticateIfRequired(
            final WebResource webResource,
            final HttpServletRequest req, final HttpServletResponse resp) {

        return new WebResource.InitBuilder(webResource)
            .requestAndResponse(req, resp)
            .params(UtilMethods.isSet(req.getPathInfo()) ?
                req.getPathInfo().substring(1) : StringPool.BLANK)
            .authCheckOptions(WebResource.AuthCheckOptions.SKIP_CHECK_FORCE_SSL,
                WebResource.AuthCheckOptions.SKIP_CHECK_ANONYMOUS_PERMISSIONS)
            .init().getUser();

    }
}
