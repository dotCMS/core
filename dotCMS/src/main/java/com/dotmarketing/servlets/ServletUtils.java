package com.dotmarketing.servlets;

import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.WebResource;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.util.Map;
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

        final Map<String, String> paramsMap = WebResource.buildParamsMap(
            UtilMethods.isSet(req.getPathInfo()) ? req.getPathInfo().substring(1) : StringPool.BLANK);

        // Call the servlet-only authenticate path directly with the anonymous-fallback flag set to
        // true. The flag is a plain method argument here and is intentionally NOT exposed on
        // InitBuilder / AuthCheckOptions, so it cannot be enabled from a JAX-RS resource — only the
        // static/binary asset servlets (which delegate to this method) use it. Permission
        // enforcement for the resolved (possibly anonymous) user remains downstream
        // (e.g. BinaryExporterServlet).
        return webResource.getCurrentUser(req, resp, paramsMap, AnonymousAccess.NONE, true,
            WebResource.AuthCheckOptions.SKIP_CHECK_FORCE_SSL,
            WebResource.AuthCheckOptions.SKIP_CHECK_ANONYMOUS_PERMISSIONS);
    }
}
