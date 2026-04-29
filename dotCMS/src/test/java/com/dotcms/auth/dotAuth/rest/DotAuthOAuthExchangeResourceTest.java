package com.dotcms.auth.dotAuth.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.auth.providers.oauth.OAuthAppConfig;
import com.dotmarketing.util.Config;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Unit tests for the guard paths in {@link DotAuthOAuthExchangeResource#exchange}:
 * feature flag, request shape, and site-level OAuth configuration. The deeper
 * happy-path (token validation + JIT provisioning + session-ref minting) is
 * exercised by integration tests against a real IdP; this class pins the
 * short-circuit branches that gate the endpoint before any IdP work happens.
 */
class DotAuthOAuthExchangeResourceTest {

    private static final String ENABLED_PROP = "DOTAUTH_OAUTH_EXCHANGE_ENABLED";

    private final DotAuthOAuthExchangeResource resource = new DotAuthOAuthExchangeResource();

    /**
     * Runs the body with the feature flag stubbed to the supplied value. All other
     * Config calls that happen after the flag check are short-circuited by the
     * pre-IdP guard paths under test, so a default-returning static mock is sufficient.
     */
    private static void withFlag(final boolean enabled, final Runnable body) {
        try (MockedStatic<Config> cfg = Mockito.mockStatic(Config.class)) {
            cfg.when(() -> Config.getBooleanProperty(ENABLED_PROP, false)).thenReturn(enabled);
            body.run();
        }
    }

    @Test
    void flagOff_returns404_soEndpointIsInvisibleWhenDisabled() {
        withFlag(false, () -> {
            final Response resp = resource.exchange(
                    mock(HttpServletRequest.class),
                    mock(HttpServletResponse.class),
                    new OAuthExchangeForm("anything", "any-nonce", 7));
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resp.getStatus());
        });
    }

    @Test
    void nullForm_returns400() {
        withFlag(true, () -> {
            final Response resp = resource.exchange(
                    mock(HttpServletRequest.class),
                    mock(HttpServletResponse.class),
                    null);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        });
    }

    @Test
    void missingIdToken_returns400() {
        withFlag(true, () -> {
            final Response resp = resource.exchange(
                    mock(HttpServletRequest.class),
                    mock(HttpServletResponse.class),
                    new OAuthExchangeForm(null, "nonce", 7));
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        });
    }

    @Test
    void missingNonce_returns400() {
        withFlag(true, () -> {
            final Response resp = resource.exchange(
                    mock(HttpServletRequest.class),
                    mock(HttpServletResponse.class),
                    new OAuthExchangeForm("token", null, 7));
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        });
    }

    @Test
    void noOAuthConfigForSite_returns503() {
        withFlag(true, () -> {
            try (MockedStatic<OAuthAppConfig> appCfg = Mockito.mockStatic(OAuthAppConfig.class)) {
                appCfg.when(() -> OAuthAppConfig.exchangeConfig(any(HttpServletRequest.class)))
                        .thenReturn(Optional.empty());

                final Response resp = resource.exchange(
                        mock(HttpServletRequest.class),
                        mock(HttpServletResponse.class),
                        new OAuthExchangeForm("token", "nonce", 7));
                assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), resp.getStatus());
            }
        });
    }

    @Test
    void nonOidcProvider_returns400() {
        withFlag(true, () -> {
            try (MockedStatic<OAuthAppConfig> appCfg = Mockito.mockStatic(OAuthAppConfig.class)) {
                final OAuthAppConfig config = mock(OAuthAppConfig.class);
                when(config.isOidc()).thenReturn(false);
                // Non-final instance field — set via reflection-free assignment is not possible;
                // downstream code only reads config.providerType for the error message.
                // Mockito doesn't help with public final fields, so we accept the mock's default.
                appCfg.when(() -> OAuthAppConfig.exchangeConfig(any(HttpServletRequest.class)))
                        .thenReturn(Optional.of(config));

                final HttpServletRequest req = mock(HttpServletRequest.class);
                when(req.getRemoteAddr()).thenReturn("127.0.0.1");

                final Response resp = resource.exchange(
                        req,
                        mock(HttpServletResponse.class),
                        new OAuthExchangeForm("token", "nonce", 7));
                assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
            }
        });
    }
}
