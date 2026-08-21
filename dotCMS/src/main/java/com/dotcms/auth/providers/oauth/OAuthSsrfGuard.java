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
 * link-local, site-local, unique-local (IPv6 {@code fc00::/7}) or
 * multicast address.
 *
 * <p><strong>Fail-closed:</strong> a hostname that cannot be resolved at
 * validation time is treated as internal and rejected. A name that fails
 * to resolve during validation but resolves at fetch time is exactly the
 * shape of a DNS-rebinding setup, so letting it through would be
 * fail-open.</p>
 *
 * <p><strong>Known residual risk — DNS rebinding (TOCTOU):</strong> this
 * guard resolves the hostname at validation time, while the actual HTTP
 * fetch re-resolves it. A rebinding DNS server can therefore return a
 * public address during validation and an internal one at fetch time.
 * Callers must invoke {@link #validateUrl} (or {@link #isInternalHost})
 * <em>immediately before</em> the fetch to reduce the window, but full
 * prevention requires pinning the resolved address through the request
 * (custom {@link org.apache.http.conn.DnsResolver}), which the shared
 * {@code CircuitBreakerUrl} does not yet support.</p>
 *
 * <p><strong>Flags:</strong> {@code OAUTH_ALLOW_INSECURE_URLS} only
 * permits {@code http://} schemes. {@code OAUTH_ALLOW_INTERNAL_HOSTS}
 * separately permits internal/private hosts (needed for dev against
 * {@code http://localhost}); when unset it defaults to the value of
 * {@code OAUTH_ALLOW_INSECURE_URLS} so existing single-flag dev setups
 * keep working.</p>
 */
public final class OAuthSsrfGuard {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("https", "http");

    private OAuthSsrfGuard() {}

    /**
     * @return {@code true} when the configuration permits fetching URLs
     *         whose hosts resolve to internal/private addresses. Defaults
     *         to the value of {@code OAUTH_ALLOW_INSECURE_URLS} when the
     *         dedicated flag is unset, preserving dev setups that rely on
     *         the single legacy flag.
     */
    public static boolean internalHostsAllowed() {
        return Config.getBooleanProperty("OAUTH_ALLOW_INTERNAL_HOSTS",
                Config.getBooleanProperty("OAUTH_ALLOW_INSECURE_URLS", false));
    }

    public static boolean isInternalHost(final String host) {
        try {
            final InetAddress[] addresses = InetAddress.getAllByName(host);
            for (final InetAddress addr : addresses) {
                if (addr.isAnyLocalAddress()
                        || addr.isLoopbackAddress()
                        || addr.isLinkLocalAddress()
                        || addr.isSiteLocalAddress()
                        || addr.isMulticastAddress()
                        || isUniqueLocalIpv6(addr)) {
                    return true;
                }
            }
            return false;
        } catch (final UnknownHostException e) {
            // Fail closed: an unresolvable name must not slip past the guard,
            // since it may resolve to an internal address at fetch time.
            Logger.debug(OAuthSsrfGuard.class,
                    "SSRF guard rejecting unresolvable host '" + host + "': " + e.getMessage());
            return true;
        }
    }

    /**
     * IPv6 unique-local addresses ({@code fc00::/7}) are not covered by
     * {@link InetAddress#isSiteLocalAddress()}, which only matches the
     * deprecated {@code fec0::/10} site-local range — check them explicitly.
     */
    private static boolean isUniqueLocalIpv6(final InetAddress addr) {
        final byte[] bytes = addr.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC;
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
        if (!internalHostsAllowed() && isInternalHost(host)) {
            SecurityLogger.logInfo(OAuthSsrfGuard.class,
                    "URL rejected: host '" + host + "' resolves to an internal address");
            return "host resolves to an internal/private address";
        }
        return null;
    }
}
