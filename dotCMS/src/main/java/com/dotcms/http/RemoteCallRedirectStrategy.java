package com.dotcms.http;

import com.dotcms.util.network.IPUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.SecurityLogger;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.Lazy;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;

/**
 * Redirect policy for outbound calls made on behalf of a user-supplied URL. The target of the
 * original request is checked before the call is made; this strategy applies the same rule to
 * every redirect the remote server answers with, so a public URL cannot bounce the server to
 * a loopback, link-local or private address. {@code ALLOW_ACCESS_TO_PRIVATE_SUBNETS} opens
 * those ranges for redirects as it does for direct calls.
 *
 * <p>A redirect must also stay on http or https, must not go from https to http, and does not
 * carry the caller's credential headers to a different host or port.</p>
 */
public class RemoteCallRedirectStrategy extends DefaultRedirectStrategy {

    private static final Lazy<Boolean> allowAccessToPrivateSubnets =
            Lazy.of(() -> Config.getBooleanProperty("ALLOW_ACCESS_TO_PRIVATE_SUBNETS", false));

    /**
     * Headers that carry the caller's identity. They are not sent to a host other than the one
     * the caller addressed.
     */
    private static final List<String> CREDENTIAL_HEADERS =
            List.of("Authorization", "Cookie", "Proxy-Authorization");

    /**
     * Resolves the redirect location and refuses it when it must not be followed. The check
     * lives here rather than in {@code isRedirected} because the parent's
     * {@code getLocationURI} records each resolved location in the client context to detect
     * circular redirects, so it must run exactly once per response, which the client does
     * when it builds the redirected request.
     *
     * @param request  the request that was sent
     * @param response the redirect response received
     * @param context  the execution context
     * @return the location to follow
     * @throws ProtocolException when the location cannot be resolved or must not be followed
     */
    @Override
    public URI getLocationURI(final HttpRequest request, final HttpResponse response,
            final HttpContext context) throws ProtocolException {

        final URI location = super.getLocationURI(request, response, context);
        final HttpHost origin = context == null ? null : HttpCoreContext.adapt(context).getTargetHost();
        checkRedirectTarget(location, allowAccessToPrivateSubnets.get(),
                origin == null ? null : origin.getSchemeName());
        return location;
    }

    /**
     * Refuses a redirect location that is not http or https, that goes from https to http, or
     * whose host resolves to an internal address while private subnets are not allowed.
     *
     * @param location            the resolved redirect location
     * @param allowPrivateSubnets whether internal addresses may be reached
     * @param originScheme        the scheme of the request that was redirected, or {@code null}
     *                            when it is unknown, which skips the https to http check
     * @throws ProtocolException when the location must not be followed
     */
    @VisibleForTesting
    static void checkRedirectTarget(final URI location, final boolean allowPrivateSubnets,
            final String originScheme) throws ProtocolException {

        final String scheme = location.getScheme() == null ? "" : location.getScheme().toLowerCase();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new ProtocolException("Redirect to scheme '" + scheme + "' refused");
        }
        if ("https".equalsIgnoreCase(originScheme) && "http".equals(scheme)) {
            throw new ProtocolException("Redirect from https to http refused: " + location.getHost());
        }
        final String host = location.getHost();
        if (!allowPrivateSubnets && IPUtils.isIpPrivateSubnet(host)) {
            SecurityLogger.logInfo(RemoteCallRedirectStrategy.class, "Refused to follow a redirect to host '"
                    + host + "': it resolves to a private or internal address");
            throw new ProtocolException("Redirect to host '" + host
                    + "' refused: it resolves to a private or internal address");
        }
    }

    /**
     * Builds the redirect and sets its headers explicitly. HttpClient copies the original
     * request's headers onto a redirect only when the redirect has none, and a plain GET redirect
     * has none, so removing headers after the fact would be undone by that copy.
     *
     * <p>A redirect to another host or port keeps every header except the credential headers.
     * If the request carried nothing but credentials, nothing is left to send, and the redirect
     * is refused rather than letting HttpClient copy the credentials back.</p>
     *
     * @param request  the request that was sent
     * @param response the redirect response received
     * @param context  the execution context
     * @return the redirected request with its headers set
     * @throws ProtocolException when the location must not be followed, or the request carried
     *                           only credentials and the redirect leaves its origin
     */
    @Override
    public HttpUriRequest getRedirect(final HttpRequest request, final HttpResponse response,
            final HttpContext context) throws ProtocolException {

        final HttpUriRequest redirect = super.getRedirect(request, response, context);
        final Header[] headers = request.getAllHeaders();
        final HttpHost previous = context == null ? null : HttpCoreContext.adapt(context).getTargetHost();
        if (sameOrigin(previous, URIUtils.extractHost(redirect.getURI()))) {
            redirect.setHeaders(headers);
            return redirect;
        }
        final Header[] kept = Arrays.stream(headers)
                .filter(header -> CREDENTIAL_HEADERS.stream().noneMatch(header.getName()::equalsIgnoreCase))
                .toArray(Header[]::new);
        if (kept.length == 0 && headers.length > 0) {
            throw new ProtocolException("Redirect to " + redirect.getURI().getHost()
                    + " is not followed: the request carries only credentials");
        }
        redirect.setHeaders(kept);
        return redirect;
    }

    /**
     * Whether two hosts are the same origin for header purposes: the host names match ignoring
     * case, and the ports match once a missing port is replaced by the default for the scheme.
     *
     * @param previous the host the redirected request was sent to
     * @param target   the host the redirect points at
     * @return {@code true} when both are known and the same origin
     */
    @VisibleForTesting
    static boolean sameOrigin(final HttpHost previous, final HttpHost target) {
        return previous != null && target != null
                && previous.getHostName().equalsIgnoreCase(target.getHostName())
                && defaultPort(previous) == defaultPort(target);
    }

    /**
     * The host's port, or the default port for its scheme when none is given.
     *
     * @param host the host
     * @return the effective port
     */
    private static int defaultPort(final HttpHost host) {
        if (host.getPort() != -1) {
            return host.getPort();
        }
        return "https".equalsIgnoreCase(host.getSchemeName()) ? 443 : 80;
    }
}
