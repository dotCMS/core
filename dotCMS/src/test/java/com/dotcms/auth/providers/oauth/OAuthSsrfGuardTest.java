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

    /** Stub OAUTH_ALLOW_INSECURE_URLS to a known value for the duration of a test. */
    private static MockedStatic<Config> configWithInsecure(final boolean allowInsecure) {
        final MockedStatic<Config> config = Mockito.mockStatic(Config.class);
        config.when(() -> Config.getBooleanProperty(eq("OAUTH_ALLOW_INSECURE_URLS"), anyBoolean()))
                .thenReturn(allowInsecure);
        return config;
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
        // With the toggle on, http is allowed AND the internal-host check is bypassed,
        // so a public literal passes. (This conflation is itself a known low-sev finding;
        // the test pins the current contract.)
        try (MockedStatic<Config> ignored = configWithInsecure(true)) {
            assertNull(OAuthSsrfGuard.validateUrl("http://8.8.8.8/.well-known"));
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
}
