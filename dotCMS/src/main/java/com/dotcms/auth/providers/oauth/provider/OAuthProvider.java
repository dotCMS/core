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

    /**
     * Build the authorization URL the browser should redirect to.
     * @param state          CSRF-guard state value
     * @param nonce          OIDC nonce (may be null for plain OAuth2)
     * @param codeChallenge  PKCE S256 code_challenge (may be null if PKCE is disabled)
     * @param callbackUrl    where the IdP will redirect after auth
     * @param scope          requested scopes, space-separated
     */
    String buildAuthorizationUrl(String state,
                                 String nonce,
                                 String codeChallenge,
                                 String callbackUrl,
                                 String scope);

    /**
     * Exchange an authorization code for the provider token response. Returns the
     * parsed JSON (keyed by snake_case field name, e.g. {@code access_token}, {@code id_token}).
     * @param code         authorization code returned by the IdP
     * @param codeVerifier PKCE verifier matching the challenge used in the authorization URL
     * @param callbackUrl  same callbackUrl that was used in the authorization URL
     */
    Map<String, Object> exchangeCodeForToken(String code,
                                             String codeVerifier,
                                             String callbackUrl);

    /**
     * Validate an {@code id_token} issued for this provider and return the verified
     * subject claim. OIDC-only; plain OAuth2 providers do not issue id tokens and
     * the default implementation throws {@link UnsupportedOperationException}.
     * @param idToken        the raw JWT as returned from the token endpoint
     * @param expectedNonce  the nonce that was sent in the authorization URL
     */
    default String validateIdTokenAndExtractSubject(final String idToken,
                                                    final String expectedNonce) {
        throw new UnsupportedOperationException(
                "id_token validation is only supported for OIDC providers");
    }

    /**
     * Validate an {@code id_token} issued for this provider and return the verified
     * claim set as a case-insensitive map, keyed by claim name (e.g. {@code sub},
     * {@code email}, {@code given_name}). OIDC-only; plain OAuth2 providers do not
     * issue id tokens and the default implementation throws
     * {@link UnsupportedOperationException}.
     * <p>
     * Same validation contract as {@link #validateIdTokenAndExtractSubject}:
     * signature (JWKS), {@code iss}, {@code aud}, {@code exp}, and {@code nonce}
     * are all verified before claims are returned. Used by the stateless
     * token-exchange endpoint which needs more than just {@code sub}.
     *
     * @param idToken        the raw JWT as returned from the token endpoint
     * @param expectedNonce  the nonce that was sent in the authorization URL
     */
    default Map<String, Object> validateIdTokenAndExtractClaims(final String idToken,
                                                                final String expectedNonce) {
        throw new UnsupportedOperationException(
                "id_token validation is only supported for OIDC providers");
    }

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
