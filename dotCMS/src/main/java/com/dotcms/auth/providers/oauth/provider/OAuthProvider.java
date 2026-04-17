package com.dotcms.auth.providers.oauth.provider;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Generic OAuth 2.0 / OpenID Connect provider abstraction.
 * Implementations translate between dotCMS and an identity provider without
 * depending on a specific provider library (no Scribe).
 */
public interface OAuthProvider {

    /** Build the authorization URL the browser should redirect to. */
    String buildAuthorizationUrl(String state, String callbackUrl, String scope);

    /** Exchange an authorization code for an access token. Returns the raw token string. */
    String exchangeCodeForToken(String code, String callbackUrl);

    /** Call the userinfo endpoint with the token. Returns a case-insensitive map of claims. */
    Map<String, Object> getUserInfo(String accessToken);

    /** Optional: fetch groups from a separate endpoint when groups are not in userinfo. */
    default Collection<String> getGroups(String accessToken, Map<String, Object> userInfo) {
        return java.util.Collections.emptyList();
    }

    /** Optional: revoke the access token server-side. No-op if unsupported. */
    default void revokeToken(String accessToken) {}

    /** Optional: provider-side logout redirect URL (RP-initiated logout for OIDC). */
    default Optional<String> getLogoutUrl(String idToken, String postLogoutRedirectUri) {
        return Optional.empty();
    }

    /** Short identifier used for logging and session attribute tagging. */
    String getProviderType();
}
