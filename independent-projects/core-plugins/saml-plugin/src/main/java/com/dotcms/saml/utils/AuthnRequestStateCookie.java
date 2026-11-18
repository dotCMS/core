package com.dotcms.saml.utils;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.service.external.SamlConstants;
import com.dotcms.saml.service.external.SamlException;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/**
 * Remembers, in the user's browser, the authentication requests dotCMS sent to the IdP, so the
 * SAML Response that comes back can be bound to the request that caused it (InResponseTo).
 *
 * The IdP returns the user with a cross-site POST, and the session cookie is SameSite=Lax by default, so
 * the HTTP session is not available on that request. A short-lived, HttpOnly, SameSite=None cookie is sent
 * on that POST instead. There is one cookie per IdP configuration, holding the most recent request IDs
 * (several, so logins started in more than one tab still work), each with the time it was issued. The
 * {@code __Host-} prefix keeps a sibling subdomain from setting it. A response whose InResponseTo isn't in
 * the cookie was not started from this browser and is rejected.
 *
 * @author dotCMS
 */
public final class AuthnRequestStateCookie {

    public static final String COOKIE_PREFIX = "__Host-dotsaml_req_";

    /** How many outstanding requests are kept per IdP configuration. */
    static final int MAX_OUTSTANDING_REQUESTS = 5;

    private static final Pattern SAFE_TOKEN = Pattern.compile("[A-Za-z0-9_-]{1,128}");
    private static final String  ENTRY_SEPARATOR = ".";
    private static final String  TIME_SEPARATOR  = ":";
    private static final int MIN_MAX_AGE_SECONDS = 60;
    private static final int MAX_MAX_AGE_SECONDS = 3600;

    private static final LongSupplier SYSTEM_CLOCK = System::currentTimeMillis;

    private AuthnRequestStateCookie() {
        // utility class
    }

    /**
     * Records an outstanding authentication request for the IdP configuration.
     *
     * @param request                       {@link HttpServletRequest} that starts the login
     * @param response                      {@link HttpServletResponse} that redirects or posts to the IdP
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @param requestId                     the AuthnRequest ID
     */
    public static void remember(final HttpServletRequest request, final HttpServletResponse response,
                                final IdentityProviderConfiguration identityProviderConfiguration,
                                final String requestId) {

        remember(request, response, identityProviderConfiguration, requestId, SYSTEM_CLOCK);
    }

    static void remember(final HttpServletRequest request, final HttpServletResponse response,
                         final IdentityProviderConfiguration identityProviderConfiguration,
                         final String requestId, final LongSupplier clock) {

        if (!isSafeToken(requestId) || !isSafeToken(identityProviderConfiguration.getId())) {

            throw new SamlException("Can not record the authentication request for IdP '"
                    + identityProviderConfiguration.getIdpName() + "'");
        }

        final int maxAgeSeconds = getMaxAgeSeconds(identityProviderConfiguration);
        final List<String> entries = new ArrayList<>();
        entries.add(requestId + TIME_SEPARATOR + nowSeconds(clock));
        for (final String entry : readEntries(request, identityProviderConfiguration, maxAgeSeconds, clock)) {

            if (entries.size() < MAX_OUTSTANDING_REQUESTS && !requestId.equals(idOf(entry))) {
                entries.add(entry);
            }
        }

        writeCookie(response, identityProviderConfiguration, entries, maxAgeSeconds);
    }

    /**
     * Returns true when this browser has an unexpired outstanding authentication request with the given ID
     * for the IdP configuration, and removes it so the same request can not be answered twice.
     *
     * @param request                       {@link HttpServletRequest} carrying the SAML Response
     * @param response                      {@link HttpServletResponse}
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @param requestId                     the InResponseTo value of the SAML Response
     * @return boolean
     */
    public static boolean consume(final HttpServletRequest request, final HttpServletResponse response,
                                  final IdentityProviderConfiguration identityProviderConfiguration,
                                  final String requestId) {

        return consume(request, response, identityProviderConfiguration, requestId, SYSTEM_CLOCK);
    }

    static boolean consume(final HttpServletRequest request, final HttpServletResponse response,
                           final IdentityProviderConfiguration identityProviderConfiguration,
                           final String requestId, final LongSupplier clock) {

        if (!isSafeToken(requestId) || !isSafeToken(identityProviderConfiguration.getId())) {

            return false;
        }

        final int maxAgeSeconds = getMaxAgeSeconds(identityProviderConfiguration);
        final List<String> entries = readEntries(request, identityProviderConfiguration, maxAgeSeconds, clock);
        final boolean found = entries.removeIf(entry -> requestId.equals(idOf(entry)));
        if (found) {

            writeCookie(response, identityProviderConfiguration, entries, maxAgeSeconds);
        }

        return found;
    }

    static String cookieName(final IdentityProviderConfiguration identityProviderConfiguration) {

        return COOKIE_PREFIX + identityProviderConfiguration.getId();
    }

    static int getMaxAgeSeconds(final IdentityProviderConfiguration identityProviderConfiguration) {

        int maxAge = SamlConstants.AUTHN_REQUEST_MAX_AGE_DEFAULT_VALUE;
        if (identityProviderConfiguration.containsOptionalProperty(SamlConstants.AUTHN_REQUEST_MAX_AGE)) {

            try {
                maxAge = Integer.parseInt(String.valueOf(
                        identityProviderConfiguration.getOptionalProperty(SamlConstants.AUTHN_REQUEST_MAX_AGE)).trim());
            } catch (NumberFormatException e) {
                maxAge = SamlConstants.AUTHN_REQUEST_MAX_AGE_DEFAULT_VALUE;
            }
        }

        return Math.max(MIN_MAX_AGE_SECONDS, Math.min(MAX_MAX_AGE_SECONDS, maxAge));
    }

    /** Unexpired, well-formed entries from the cookie, newest first. */
    private static List<String> readEntries(final HttpServletRequest request,
                                            final IdentityProviderConfiguration identityProviderConfiguration,
                                            final int maxAgeSeconds, final LongSupplier clock) {

        final List<String> entries = new ArrayList<>();
        final Cookie[] cookies = request.getCookies();
        if (null == cookies) {
            return entries;
        }

        final String name = cookieName(identityProviderConfiguration);
        final long oldest = nowSeconds(clock) - maxAgeSeconds;
        for (final Cookie cookie : cookies) {

            if (!name.equals(cookie.getName()) || StringUtils.isBlank(cookie.getValue())) {
                continue;
            }

            for (final String entry : StringUtils.split(cookie.getValue(), ENTRY_SEPARATOR)) {

                final String id = idOf(entry);
                final long issuedAt = issuedAtOf(entry);
                if (isSafeToken(id) && issuedAt >= oldest && entries.size() < MAX_OUTSTANDING_REQUESTS) {
                    entries.add(entry);
                }
            }
        }

        return entries;
    }

    private static void writeCookie(final HttpServletResponse response,
                                    final IdentityProviderConfiguration identityProviderConfiguration,
                                    final List<String> entries, final int maxAgeSeconds) {

        final String value = String.join(ENTRY_SEPARATOR, entries);
        response.addHeader("Set-Cookie", cookieName(identityProviderConfiguration) + "=" + value
                + "; Max-Age=" + (entries.isEmpty() ? 0 : maxAgeSeconds)
                + "; Path=/; Secure; HttpOnly; SameSite=None");
    }

    private static String idOf(final String entry) {

        return StringUtils.substringBefore(entry, TIME_SEPARATOR);
    }

    private static long issuedAtOf(final String entry) {

        try {
            return Long.parseLong(StringUtils.substringAfter(entry, TIME_SEPARATOR));
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private static long nowSeconds(final LongSupplier clock) {

        return clock.getAsLong() / 1000L;
    }

    private static boolean isSafeToken(final String value) {

        return null != value && SAFE_TOKEN.matcher(value).matches();
    }
}
