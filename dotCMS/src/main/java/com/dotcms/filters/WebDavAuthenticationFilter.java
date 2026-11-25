package com.dotcms.filters;

import com.dotcms.auth.BasicAuthentication;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Requires credentials on the WebDAV endpoints.
 *
 * <p>WebDAV in dotCMS is an authenticated interface throughout: every resource is resolved against
 * a user, and the resource implementations answer {@code authorise(...)} with false when there is
 * no user to check. The library that serves these endpoints does not apply that answer uniformly,
 * because a few of its method handlers run their own sequence instead of the shared one that
 * consults it. A request carrying no credentials at all is therefore handled by those handlers,
 * with the resource's own answer never consulted or, for a PROPPATCH naming no property, consulted
 * and then discarded.
 *
 * <p>Answering here removes that variation: disabled Basic authentication or missing credentials
 * receives a direct RFC 7235 challenge, for every method and whatever the body. When the shared
 * policy allows credentials, the helper validates the username/password or username/API-JWT pair;
 * the resources' own permission checks still decide what the authenticated caller may do.
 *
 * <p>This deliberately does not exempt OPTIONS. A client sends it to discover what the server
 * supports, and being challenged is the normal answer to that: every WebDAV client already handles
 * a challenge on its first request, since that is how it learns to send credentials at all.
 */
public class WebDavAuthenticationFilter implements Filter {

    /**
     * Basic is what the library's own challenge offers, and what dotCMS authenticates WebDAV with:
     * the resources verify a username paired with a password or API JWT. The realm is a label a
     * client shows and keys saved credentials by, not a decision input.
     */
    private static final String CHALLENGE = "Basic realm=\"dotCMS WebDAV\"";

    @Override
    public void init(final FilterConfig filterConfig) {
        // Nothing to configure.
    }

    @Override
    public void destroy() {
        // Nothing to release.
    }

    /** Reject disabled or missing credentials without entering the servlet's method handlers. */
    @Override
    public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain)
            throws IOException, ServletException {

        if (!(req instanceof HttpServletRequest) || !(res instanceof HttpServletResponse)) {
            chain.doFilter(req, res);
            return;
        }
        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        // DAV uses a Basic username/password or username/API-JWT pair in this header. A blank
        // header is no more a caller than an absent one, and NONE disables either pair.
        if (BasicAuthentication.mode() == BasicAuthentication.Mode.NONE
                || !UtilMethods.isSet(request.getHeader("Authorization"))) {
            // Record the refusal without including the credential-bearing header.
            SecurityLogger.logInfo(WebDavAuthenticationFilter.class,
                    () -> String.format("Refused WebDAV %s %s from %s: Basic authentication disabled or credentials missing",
                            request.getMethod(), request.getRequestURI(), request.getRemoteAddr()));
            // setStatus, not sendError: web.xml maps 401 to /html/error/custom-error-page.jsp,
            // which redirects, and a WebDAV client handed a 302 to a login page has nothing it can
            // do with it. Setting the status keeps the challenge as the answer to the request.
            response.setHeader("WWW-Authenticate", CHALLENGE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        chain.doFilter(req, res);
    }
}
