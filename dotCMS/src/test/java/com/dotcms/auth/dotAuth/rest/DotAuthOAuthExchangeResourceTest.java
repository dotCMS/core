package com.dotcms.auth.dotAuth.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.auth.providers.oauth.OAuthAppConfig;
import com.dotcms.auth.providers.oauth.OAuthHelper;
import com.dotcms.auth.providers.oauth.OAuthSsrfGuard;
import com.dotcms.auth.providers.oauth.provider.OIDCProvider;
import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.business.DuplicateUserException;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Unit tests for the guard paths in {@link DotAuthOAuthExchangeResource#exchange}:
 * request shape and site-level OAuth configuration. The deeper happy-path (token
 * validation + JIT provisioning + session-ref minting) is exercised by integration
 * tests against a real IdP; this class pins the short-circuit branches that gate
 * the endpoint before any IdP work happens.
 */
class DotAuthOAuthExchangeResourceTest {

    private final DotAuthOAuthExchangeResource resource = new DotAuthOAuthExchangeResource();

    @Test
    void nullForm_returns400() {
        final Response resp = resource.exchange(
                mock(HttpServletRequest.class),
                mock(HttpServletResponse.class),
                null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    }

    @Test
    void missingIdToken_returns400() {
        final Response resp = resource.exchange(
                mock(HttpServletRequest.class),
                mock(HttpServletResponse.class),
                new OAuthExchangeForm(null, "nonce", 7));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    }

    @Test
    void missingNonce_returns400() {
        final Response resp = resource.exchange(
                mock(HttpServletRequest.class),
                mock(HttpServletResponse.class),
                new OAuthExchangeForm("token", null, 7));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    }

    @Test
    void noOAuthConfigForSite_returns503() {
        try (MockedStatic<OAuthAppConfig> appCfg = Mockito.mockStatic(OAuthAppConfig.class)) {
            appCfg.when(() -> OAuthAppConfig.exchangeConfig(any(HttpServletRequest.class)))
                    .thenReturn(Optional.empty());

            final Response resp = resource.exchange(
                    mock(HttpServletRequest.class),
                    mock(HttpServletResponse.class),
                    new OAuthExchangeForm("token", "nonce", 7));
            assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), resp.getStatus());
        }
    }

    @Test
    void nonOidcProvider_returns400() {
        try (MockedStatic<OAuthAppConfig> appCfg = Mockito.mockStatic(OAuthAppConfig.class)) {
            final OAuthAppConfig config = mock(OAuthAppConfig.class);
            when(config.isOidc()).thenReturn(false);
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
    }

    @Test
    void originWithEmptyAllowedOrigins_returns403() {
        try (MockedStatic<OAuthAppConfig> appCfg = Mockito.mockStatic(OAuthAppConfig.class)) {
            // allowedOriginsJson left null on the mock -> parsed as an empty list. A request
            // that carries an Origin header must be denied: the safe default for a
            // token-exchange endpoint is deny, NOT allow-any-origin.
            final OAuthAppConfig config = mock(OAuthAppConfig.class);
            appCfg.when(() -> OAuthAppConfig.exchangeConfig(any(HttpServletRequest.class)))
                    .thenReturn(Optional.of(config));

            final HttpServletRequest req = mock(HttpServletRequest.class);
            when(req.getHeader("Origin")).thenReturn("https://spa.example.com");
            when(req.getRemoteAddr()).thenReturn("127.0.0.1");

            final Response resp = resource.exchange(
                    req, mock(HttpServletResponse.class),
                    new OAuthExchangeForm("token", "nonce", 7));
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), resp.getStatus());
        }
    }

    @Test
    void unlistedOrigin_returns403() throws Exception {
        // Build the config BEFORE opening the static mock: the constructor calls
        // OAuthAppConfig's own private static helpers, which a MockedStatic would otherwise
        // intercept and null out.
        final OAuthAppConfig config = headlessConfig(Map.of(
                "enabled", "true",
                "allowedOrigins", "[\"https://trusted.example.com\"]"));
        try (MockedStatic<OAuthAppConfig> appCfg = Mockito.mockStatic(OAuthAppConfig.class)) {
            appCfg.when(() -> OAuthAppConfig.exchangeConfig(any(HttpServletRequest.class)))
                    .thenReturn(Optional.of(config));

            final HttpServletRequest req = mock(HttpServletRequest.class);
            when(req.getHeader("Origin")).thenReturn("https://evil.example.com");
            when(req.getRemoteAddr()).thenReturn("127.0.0.1");

            final Response resp = resource.exchange(
                    req, mock(HttpServletResponse.class),
                    new OAuthExchangeForm("token", "nonce", 7));
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), resp.getStatus());
        }
    }

    @Test
    void allowedOriginOnErrorResponse_carriesCorsHeaders() throws Exception {
        // A browser that passed the preflight must also be able to read the actual POST's
        // error responses — ACAO/credentials belong on the POST response itself, not only
        // on the preflight. Non-OIDC provider short-circuits with 400 after the origin check.
        final OAuthAppConfig config = headlessConfig(Map.of(
                "enabled", "true",
                "providerType", "GENERIC",
                "allowedOrigins", "[\"https://trusted.example.com\"]"));
        try (MockedStatic<OAuthAppConfig> appCfg = Mockito.mockStatic(OAuthAppConfig.class)) {
            appCfg.when(() -> OAuthAppConfig.exchangeConfig(any(HttpServletRequest.class)))
                    .thenReturn(Optional.of(config));

            final HttpServletRequest req = mock(HttpServletRequest.class);
            when(req.getHeader("Origin")).thenReturn("https://trusted.example.com");
            when(req.getRemoteAddr()).thenReturn("127.0.0.1");

            final Response resp = resource.exchange(
                    req, mock(HttpServletResponse.class),
                    new OAuthExchangeForm("token", "nonce", 7));
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
            assertEquals("https://trusted.example.com",
                    resp.getHeaderString("Access-Control-Allow-Origin"));
            assertEquals("true", resp.getHeaderString("Access-Control-Allow-Credentials"));
            assertEquals("Origin", resp.getHeaderString("Vary"));
        }
    }

    @Test
    void noOriginHeader_noCorsHeadersOnResponse() throws Exception {
        // Non-browser callers (server-to-server) carry no Origin header — the response
        // must not claim a cross-origin policy it does not need.
        final OAuthAppConfig config = headlessConfig(Map.of(
                "enabled", "true",
                "providerType", "GENERIC",
                "allowedOrigins", "[\"https://trusted.example.com\"]"));
        try (MockedStatic<OAuthAppConfig> appCfg = Mockito.mockStatic(OAuthAppConfig.class)) {
            appCfg.when(() -> OAuthAppConfig.exchangeConfig(any(HttpServletRequest.class)))
                    .thenReturn(Optional.of(config));

            final HttpServletRequest req = mock(HttpServletRequest.class);
            when(req.getRemoteAddr()).thenReturn("127.0.0.1");

            final Response resp = resource.exchange(
                    req, mock(HttpServletResponse.class),
                    new OAuthExchangeForm("token", "nonce", 7));
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
            assertNull(resp.getHeaderString("Access-Control-Allow-Origin"));
            assertNull(resp.getHeaderString("Access-Control-Allow-Credentials"));
        }
    }

    @Test
    void untrustedIssuer_returns401() throws Exception {
        final OAuthAppConfig config = headlessConfig(Map.of(
                "enabled", "true",
                "providerType", "OIDC",
                "trustedIdps", "[{\"enabled\":true,\"issuer\":\"https://good-idp.example.com\"}]"));
        try (MockedStatic<OAuthAppConfig> appCfg = Mockito.mockStatic(OAuthAppConfig.class)) {
            appCfg.when(() -> OAuthAppConfig.exchangeConfig(any(HttpServletRequest.class)))
                    .thenReturn(Optional.of(config));

            final HttpServletRequest req = mock(HttpServletRequest.class);
            // No Origin header -> CORS check is skipped, reaching the trusted-IdP allowlist.
            when(req.getRemoteAddr()).thenReturn("127.0.0.1");

            final Response resp = resource.exchange(
                    req, mock(HttpServletResponse.class),
                    new OAuthExchangeForm(jwtWithIssuer("https://evil-idp.example.com"), "nonce", 7));
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), resp.getStatus());
        }
    }

    @Test
    void undecodableIssuerWithTrustedIdps_returns400() throws Exception {
        final OAuthAppConfig config = headlessConfig(Map.of(
                "enabled", "true",
                "providerType", "OIDC",
                "trustedIdps", "[{\"enabled\":true,\"issuer\":\"https://good-idp.example.com\"}]"));
        try (MockedStatic<OAuthAppConfig> appCfg = Mockito.mockStatic(OAuthAppConfig.class)) {
            appCfg.when(() -> OAuthAppConfig.exchangeConfig(any(HttpServletRequest.class)))
                    .thenReturn(Optional.of(config));

            final HttpServletRequest req = mock(HttpServletRequest.class);
            when(req.getRemoteAddr()).thenReturn("127.0.0.1");

            final Response resp = resource.exchange(
                    req, mock(HttpServletResponse.class),
                    new OAuthExchangeForm("not-a-jwt", "nonce", 7));
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        }
    }

    /**
     * Regression for #37691: a duplicate user during JIT provisioning must return 409 with a
     * message naming the cause, not a generic 500, and must not echo the email address.
     */
    @Test
    void duplicateUserDuringProvisioning_returns409WithoutEmail() throws Exception {
        final String issuer = "https://good-idp.example.com";
        final OAuthAppConfig config = headlessConfig(Map.of(
                "enabled", "true",
                "providerType", "OIDC",
                "trustedIdps", "[{\"enabled\":true,\"issuer\":\"" + issuer + "\",\"audience\":\"client-1\"}]"));
        final Map<String, Object> claims = Map.of("iss", issuer, "sub", "subject-1", "email", "alice@example.com");

        try (MockedStatic<OAuthAppConfig> appCfg = Mockito.mockStatic(OAuthAppConfig.class);
             MockedStatic<OAuthSsrfGuard> ssrf = Mockito.mockStatic(OAuthSsrfGuard.class);
             MockedConstruction<OIDCProvider> providers = Mockito.mockConstruction(OIDCProvider.class,
                     (mock, ctx) -> when(mock.validateIdTokenAndExtractClaims(any(), any())).thenReturn(claims));
             MockedConstruction<OAuthHelper> helpers = Mockito.mockConstruction(OAuthHelper.class,
                     (mock, ctx) -> when(mock.resolveOrProvisionUser(any(), any(), any(), any(), any(), anyBoolean()))
                             .thenThrow(new DuplicateUserException("User already exists with this email alice@example.com")))) {
            appCfg.when(() -> OAuthAppConfig.exchangeConfig(any(HttpServletRequest.class)))
                    .thenReturn(Optional.of(config));
            ssrf.when(() -> OAuthSsrfGuard.validateUrl(any())).thenReturn(null);

            // Built inside the construction mock so the resource's OAuthHelper field is the stub.
            final DotAuthOAuthExchangeResource withStubbedHelper = new DotAuthOAuthExchangeResource();
            final HttpServletRequest req = mock(HttpServletRequest.class);
            when(req.getRemoteAddr()).thenReturn("127.0.0.1");

            final Response resp = withStubbedHelper.exchange(
                    req, mock(HttpServletResponse.class),
                    new OAuthExchangeForm(jwtWithIssuer(issuer), "nonce", 7));

            assertEquals(Response.Status.CONFLICT.getStatusCode(), resp.getStatus());
            final String body = String.valueOf(((ResponseEntityView<?>) resp.getEntity()).getEntity());
            assertFalse(body.contains("alice@example.com"), "The email must not reach the response");
        }
    }

    /**
     * Build a real headless {@link OAuthAppConfig} via its private secrets constructor.
     * The config's fields are {@code public final}, so they cannot be stubbed on a mock —
     * we construct a genuine instance from a Secret map carrying the keys under test.
     */
    private static OAuthAppConfig headlessConfig(final Map<String, String> values) throws Exception {
        final Map<String, Secret> secrets = new HashMap<>();
        values.forEach((k, v) -> secrets.put(k,
                Secret.builder().withValue(v).withType(Type.STRING).build()));
        final Constructor<OAuthAppConfig> ctor =
                OAuthAppConfig.class.getDeclaredConstructor(Map.class, boolean.class);
        ctor.setAccessible(true);
        return ctor.newInstance(secrets, true);
    }

    /** Build a structurally-valid (unsigned) JWT carrying only an {@code iss} claim. */
    private static String jwtWithIssuer(final String issuer) {
        final String header = base64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        final String payload = base64Url("{\"iss\":\"" + issuer + "\"}");
        return header + "." + payload + ".sig";
    }

    private static String base64Url(final String json) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
