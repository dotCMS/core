package com.dotcms.saml.utils;

import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Makes an SP-initiated login start on the host the IdP posts back to.
 *
 * The AuthnRequest always names the assertion consumer URL on the Service Provider Endpoint Hostname, and the
 * request-binding cookie ({@link AuthnRequestStateCookie}) is set on the host that starts the login. When the
 * user starts on another host (a site alias, or a site using the System Host configuration under a different
 * hostname), the browser would not send that cookie back with the response. So the browser is first sent to
 * the assertion consumer URL on the endpoint host, where a GET starts the login again. Users already end up on
 * the endpoint host after login, so the flow looks the same to them.
 *
 * The target is always the configured assertion consumer URL, never a value from the request. A marker
 * parameter stops a second redirect, so a proxy that rewrites the Host header can't cause a loop. Hosts are
 * compared without ports, because cookies are shared across ports.
 *
 * @author dotCMS
 */
public final class EndpointHostRedirect {

    public static final String MARKER_PARAMETER = "dotsaml_host_redirect";

    private EndpointHostRedirect() {
        // utility class
    }

    /**
     * Redirects the browser to the assertion consumer URL when the login started on another host.
     *
     * @param request                  {@link HttpServletRequest} starting the login
     * @param response                 {@link HttpServletResponse}
     * @param assertionConsumerUrl     the configured assertion consumer URL
     * @return true when the browser was redirected and the login must not continue on this request
     * @throws IOException when the redirect can't be sent
     */
    public static boolean redirectIfNeeded(final HttpServletRequest request, final HttpServletResponse response,
                                           final String assertionConsumerUrl) throws IOException {

        if (null != request.getParameter(MARKER_PARAMETER)) {
            return false;
        }

        final String endpointHost = hostOf(assertionConsumerUrl);
        final String serverName   = StringUtils.trimToNull(request.getServerName());
        final String requestHost  = null == serverName ? null : serverName.toLowerCase(Locale.ROOT);
        if (null == endpointHost || null == requestHost || endpointHost.equals(requestHost)) {
            return false;
        }

        response.sendRedirect(assertionConsumerUrl + (assertionConsumerUrl.contains("?") ? "&" : "?")
                + MARKER_PARAMETER + "=1");
        return true;
    }

    /** The lower-case host of an absolute URL, or null. */
    static String hostOf(final String url) {

        if (StringUtils.isBlank(url)) {
            return null;
        }

        try {
            final String host = new URI(url.trim()).getHost();
            return null == host ? null : host.toLowerCase(Locale.ROOT);
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
