package com.dotcms.http;

import com.dotcms.util.network.IPUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.SecurityLogger;
import io.vavr.Lazy;
import java.net.URI;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;

/**
 * Redirect policy for outbound calls made on behalf of a user-supplied URL. The target of the
 * original request is checked before the call is made; this strategy applies the same rule to
 * every redirect the remote server answers with, so a public URL cannot bounce the server to
 * a loopback, link-local or private address. {@code ALLOW_ACCESS_TO_PRIVATE_SUBNETS} opens
 * those ranges for redirects as it does for direct calls.
 */
public class RemoteCallRedirectStrategy extends DefaultRedirectStrategy {

    private static final Lazy<Boolean> allowAccessToPrivateSubnets =
            Lazy.of(() -> Config.getBooleanProperty("ALLOW_ACCESS_TO_PRIVATE_SUBNETS", false));

    /**
     * Resolves the redirect location and refuses it when it points at an internal address.
     * The check lives here rather than in {@code isRedirected} because the parent's
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
        final String host = location.getHost();
        if (IPUtils.isIpPrivateSubnet(host) && !allowAccessToPrivateSubnets.get()) {
            SecurityLogger.logInfo(this.getClass(), "Refused to follow a redirect to host '" + host
                    + "': it resolves to a private or internal address");
            throw new ProtocolException("Redirect to host '" + host
                    + "' refused: it resolves to a private or internal address");
        }
        return location;
    }
}
