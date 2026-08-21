package com.dotcms.auth.providers.oauth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;

import com.dotmarketing.util.Config;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Unit coverage for {@link OAuthSsrfGuard}, the shared SSRF defense that vets every
 * admin-configured and IdP-discovered URL before dotCMS fetches it server-side. The
 * guard is security-critical and reachable from the unauthenticated headless exchange
 * flow, so its branches (scheme allow-list, the {@code OAUTH_ALLOW_INSECURE_URLS}
 * toggle, missing host, and the private/loopback/link-local/site-local resolution)
 * are pinned here against silent regression.
 *
 * <p>Tests use literal IP addresses (loopback, link-local IMDS, RFC1918, public) so the
 * resolution checks are deterministic and do not depend on live DNS.
 */
class OAuthSsrfGuardTest {

    /**
     * Stub OAUTH_ALLOW_INSECURE_URLS / OAUTH_ALLOW_INTERNAL_HOSTS to known values
     * for the duration of a test. Either may be {@code null} to leave that
     * property unstubbed (mock returns {@code false}).
     */
    private static MockedStatic<Config> configWith(final Boolean allowInsecure,
                                                   final Boolean allowInternal) {
        final MockedStatic<Config> config = Mockito.mockStatic(Config.class);
        if (allowInsecure != null) {
            config.when(() -> Config.getBooleanProperty(eq("OAUTH_ALLOW_INSECURE_URLS"), anyBoolean()))
                    .thenReturn(allowInsecure);
        }
        if (allowInternal != null) {
            config.when(() -> Config.getBooleanProperty(eq("OAUTH_ALLOW_INTERNAL_HOSTS"), anyBoolean()))
                    .thenReturn(allowInternal);
        }
        return config;
    }

    /** Back-compat helper for tests that only care about the insecure flag. */
    private static MockedStatic<Config> configWithInsecure(final boolean allowInsecure) {
        return configWith(allowInsecure, null);
    }

    @Test
    void validateUrl_nullOrBlank_rejected() {
        try (MockedStatic<Config> ignored = configWithInsecure(false)) {
            assertNotNull(OAuthSsrfGuard.validateUrl(null));
            assertNotNull(OAuthSsrfGuard.validateUrl("   "));
        }
    }

    @Test
    void validateUrl_disallowedSchemes_rejected() {
        try (MockedStatic<Config> ignored = configWithInsecure(false)) {
            assertNotNull(OAuthSsrfGuard.validateUrl("file:///etc/passwd"));
            assertNotNull(OAuthSsrfGuard.validateUrl("gopher://example.com/"));
            assertNotNull(OAuthSsrfGuard.validateUrl("ftp://example.com/"));
        }
    }

    @Test
    void validateUrl_httpRejectedWhenInsecureDisabled() {
        try (MockedStatic<Config> ignored = configWithInsecure(false)) {
            assertNotNull(OAuthSsrfGuard.validateUrl("http://idp.example.com/.well-known"));
        }
    }

    @Test
    void validateUrl_httpAcceptedWhenInsecureEnabled() {
        // http is allowed by the insecure toggle; the internal-host check is now
        // governed by the separate OAUTH_ALLOW_INTERNAL_HOSTS flag (defaults to
        // the insecure flag). A public literal passes here.
        try (MockedStatic<Config> ignored = configWith(true, null)) {
            assertNull(OAuthSsrfGuard.validateUrl("http://8.8.8.8/.well-known"));
        }
    }

    @Test
    void validateUrl_httpAllowedButInternalStillRejectedWhenInternalFlagExplicitlyOff() {
        // The two flags are decoupled: allowing http must not also open the door
        // to internal addresses when OAUTH_ALLOW_INTERNAL_HOSTS=false.
        try (MockedStatic<Config> ignored = configWith(true, false)) {
            assertNotNull(OAuthSsrfGuard.validateUrl("http://10.0.0.1/jwks"));
        }
    }

    @Test
    void validateUrl_internalHostAcceptedWhenInternalFlagExplicitlyOn() {
        try (MockedStatic<Config> ignored = configWith(false, true)) {
            assertNull(OAuthSsrfGuard.validateUrl("https://10.0.0.1/jwks"));
        }
    }

    @Test
    void validateUrl_missingHost_rejected() {
        try (MockedStatic<Config> ignored = configWithInsecure(false)) {
            assertNotNull(OAuthSsrfGuard.validateUrl("https:///no-host"));
        }
    }

    @Test
    void validateUrl_internalHosts_rejected() {
        try (MockedStatic<Config> ignored = configWithInsecure(false)) {
            assertNotNull(OAuthSsrfGuard.validateUrl("https://127.0.0.1/jwks"), "loopback");
            assertNotNull(OAuthSsrfGuard.validateUrl("https://169.254.169.254/latest/meta-data"),
                    "cloud-metadata link-local");
            assertNotNull(OAuthSsrfGuard.validateUrl("https://10.0.0.1/jwks"), "RFC1918 site-local");
        }
    }

    @Test
    void validateUrl_publicHttpsHost_accepted() {
        try (MockedStatic<Config> ignored = configWithInsecure(false)) {
            assertNull(OAuthSsrfGuard.validateUrl("https://8.8.8.8/jwks"));
        }
    }

    @Test
    void isInternalHost_classifiesLiteralAddresses() {
        assertTrue(OAuthSsrfGuard.isInternalHost("127.0.0.1"), "loopback");
        assertTrue(OAuthSsrfGuard.isInternalHost("169.254.169.254"), "link-local / IMDS");
        assertTrue(OAuthSsrfGuard.isInternalHost("10.0.0.1"), "RFC1918 site-local");
        assertTrue(OAuthSsrfGuard.isInternalHost("::1"), "IPv6 loopback");
        assertFalse(OAuthSsrfGuard.isInternalHost("8.8.8.8"), "public address");
    }

    @Test
    void isInternalHost_detectsIpv6UniqueLocalAddresses() {
        // fc00::/7 is NOT covered by InetAddress.isSiteLocalAddress() (which only
        // matches the deprecated fec0::/10) — the guard must check it explicitly.
        assertTrue(OAuthSsrfGuard.isInternalHost("fd12:3456:789a:1::1"), "fd00::/8 ULA");
        assertTrue(OAuthSsrfGuard.isInternalHost("fc00::1"), "fc00::/8 ULA");
        assertFalse(OAuthSsrfGuard.isInternalHost("2606:4700:4700::1111"), "public IPv6");
    }

    @Test
    void isInternalHost_failsClosedOnUnresolvableHost() {
        // A name that cannot be resolved at validation time may resolve to an
        // internal address at fetch time (DNS rebinding) — treat it as internal.
        assertTrue(OAuthSsrfGuard.isInternalHost("host.invalid.nonexistent-ssrf-probe.example"));
    }

    @Test
    void validateUrl_rejectsUnresolvableHost() {
        try (MockedStatic<Config> ignored = configWithInsecure(false)) {
            assertNotNull(OAuthSsrfGuard.validateUrl(
                    "https://host.invalid.nonexistent-ssrf-probe.example/.well-known"));
        }
    }
}
