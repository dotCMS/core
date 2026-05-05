package com.dotcms.auth.providers.oauth;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * Shared SSRF guard for OAuth/OIDC URL validation.
 * Checks whether a hostname resolves to a private, loopback,
 * link-local, site-local, or multicast address.
 */
public final class OAuthSsrfGuard {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("https", "http");

    private OAuthSsrfGuard() {}

    public static boolean isInternalHost(final String host) {
        try {
            final InetAddress[] addresses = InetAddress.getAllByName(host);
            for (final InetAddress addr : addresses) {
                if (addr.isAnyLocalAddress()
                        || addr.isLoopbackAddress()
                        || addr.isLinkLocalAddress()
                        || addr.isSiteLocalAddress()
                        || addr.isMulticastAddress()) {
                    return true;
                }
            }
            return false;
        } catch (final UnknownHostException e) {
            Logger.debug(OAuthSsrfGuard.class,
                    "SSRF guard skipped for unresolvable host '" + host + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Validate a URL for safe server-side fetching: scheme must be HTTP(S),
     * HTTPS is required unless {@code OAUTH_ALLOW_INSECURE_URLS=true}, and
     * the host must not resolve to a private/internal address.
     *
     * @return {@code null} if the URL is safe; a rejection reason string otherwise.
     */
    public static String validateUrl(final String url) {
        if (!UtilMethods.isSet(url)) {
            return "URL is required";
        }
        final URI uri;
        try {
            uri = new URI(url);
        } catch (final URISyntaxException e) {
            return "not a valid URI";
        }
        final String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!ALLOWED_SCHEMES.contains(scheme)) {
            return "scheme '" + scheme + "' not allowed";
        }
        final boolean allowInsecure = Config.getBooleanProperty("OAUTH_ALLOW_INSECURE_URLS", false);
        if ("http".equals(scheme) && !allowInsecure) {
            return "http:// URLs require OAUTH_ALLOW_INSECURE_URLS=true";
        }
        final String host = uri.getHost();
        if (!UtilMethods.isSet(host)) {
            return "URL is missing a host";
        }
        if (!allowInsecure && isInternalHost(host)) {
            SecurityLogger.logInfo(OAuthSsrfGuard.class,
                    "URL rejected: host '" + host + "' resolves to an internal address");
            return "host resolves to an internal/private address";
        }
        return null;
    }
}
