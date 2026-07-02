package com.dotcms.auth.providers.oauth.provider;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotmarketing.exception.DotRuntimeException;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link GenericOAuth2Provider}. Focuses on the pure URL-building
 * logic that doesn't require HTTP mocks.
 */
class GenericOAuth2ProviderTest {

    private GenericOAuth2Provider newProvider() {
        return new GenericOAuth2Provider(
                "my-client-id",
                "top-secret".toCharArray(),
                "https://idp.example.com/authorize",
                "https://idp.example.com/token",
                "https://idp.example.com/userinfo",
                null,
                null,
                null,
                null);
    }

    @Test
    void buildAuthorizationUrl_includesClientIdStateAndEncodedScope() {
        final GenericOAuth2Provider p = newProvider();
        final String url = p.buildAuthorizationUrl(
                "the-state", null, null,
                "https://callback/api/v1/oauth/callback", "email profile");

        assertTrue(url.startsWith("https://idp.example.com/authorize?"), "URL should start with the configured authorization endpoint: " + url);
        assertTrue(url.contains("response_type=code"),           "missing response_type=code");
        assertTrue(url.contains("response_mode=query"),          "missing response_mode=query pin");
        assertTrue(url.contains("client_id=my-client-id"),       "missing client_id");
        assertTrue(url.contains("state=the-state"),              "missing state");
        // scope "email profile" must be URL-encoded
        assertTrue(url.contains("scope=email+profile") || url.contains("scope=email%20profile"),
                "scope not URL-encoded in: " + url);
        // redirect_uri is URL-encoded
        assertTrue(url.contains("redirect_uri=https%3A%2F%2Fcallback%2Fapi%2Fv1%2Foauth%2Fcallback"),
                "redirect_uri not URL-encoded in: " + url);
    }

    @Test
    void buildAuthorizationUrl_omitsScopeParamWhenScopeEmpty() {
        final GenericOAuth2Provider p = newProvider();
        final String url = p.buildAuthorizationUrl("state", null, null, "https://cb", "");
        assertTrue(url.contains("state=state"));
        // No scope param at all when empty
        assertTrue(!url.contains("&scope="), "scope param should be omitted when empty: " + url);
    }

    @Test
    void constructor_rejectsMissingRequiredUrls() {
        assertThrows(DotRuntimeException.class, () -> new GenericOAuth2Provider(
                "id", "secret".toCharArray(),
                null,                           // missing authorization URL
                "https://idp.example.com/token",
                "https://idp.example.com/userinfo",
                null, null, null, null));
        assertThrows(DotRuntimeException.class, () -> new GenericOAuth2Provider(
                "id", "secret".toCharArray(),
                "https://idp.example.com/authorize",
                null,                           // missing token URL
                "https://idp.example.com/userinfo",
                null, null, null, null));
    }

    @Test
    void getProviderType_isOAuth2() {
        assertTrue("OAuth2".equals(newProvider().getProviderType()));
    }
}
