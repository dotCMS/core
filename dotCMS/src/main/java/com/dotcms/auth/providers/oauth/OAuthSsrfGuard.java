package com.dotcms.auth.providers.oauth;

import com.dotmarketing.util.Logger;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Shared SSRF guard for OAuth/OIDC URL validation.
 * Checks whether a hostname resolves to a private, loopback,
 * link-local, site-local, or multicast address.
 */
public final class OAuthSsrfGuard {

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
}
