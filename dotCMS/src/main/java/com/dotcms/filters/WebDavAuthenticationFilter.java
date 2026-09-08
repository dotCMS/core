package com.dotcms.filters;

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
 * <p>Answering here removes that variation: a request with no credentials gets the RFC 7235
 * challenge it should have got, for every method and whatever the body, and the servlet only ever
 * sees requests that at least claim to be from somebody. Whether that claim is any good, and what
 * the caller may then do, stays where it belongs -- with the library's authentication handlers and
 * the resources' own permission checks.
 *
 * <p>This deliberately does not exempt OPTIONS. A client sends it to discover what the server
 * supports, and being challenged is the normal answer to that: every WebDAV client already handles
 * a challenge on its first request, since that is how it learns to send credentials at all.
 */
public class WebDavAuthenticationFilter implements Filter {

    /**
     * Basic is what the library's own challenge offers, and what dotCMS authenticates WebDAV with:
     * the resources verify a username and password, so there is nothing else to offer here. The
     * realm is a label a client shows and keys saved credentials by, not a decision input.
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

    @Override
    public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain)
            throws IOException, ServletException {

        if (!(req instanceof HttpServletRequest) || !(res instanceof HttpServletResponse)) {
            chain.doFilter(req, res);
            return;
        }
        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        // Credentials reach WebDAV only in this header: the resources authenticate a username and
        // password, and the library's handlers read nothing else. A present but blank header is no
        // more a caller than an absent one.
        if (!UtilMethods.isSet(request.getHeader("Authorization"))) {
            // A refused request on an interface that serves nobody anonymously belongs in the
            // security log, the same as a back-end URL requested without a session. The volume is
            // bounded: a client is challenged once and then sends credentials up front, so what
            // lands here past the first request of a handshake is something that never had any.
            SecurityLogger.logInfo(WebDavAuthenticationFilter.class,
                    () -> String.format("Refused WebDAV %s %s from %s: no credentials sent",
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
