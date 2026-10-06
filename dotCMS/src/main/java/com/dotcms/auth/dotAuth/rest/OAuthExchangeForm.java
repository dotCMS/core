package com.dotcms.auth.dotAuth.rest;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;

/**
 * Request body for {@code POST /v1/dotauth/oauth/exchange}. The SPA posts the
 * OIDC {@code id_token} it received from the IdP's token endpoint along with the
 * {@code nonce} it originated for that authentication request.
 *
 * <p>The server validates the id_token (signature via JWKS, {@code iss},
 * {@code aud} == configured client_id, {@code exp}, and {@code nonce}) before
 * provisioning or issuing anything — see
 * {@link com.dotcms.auth.providers.oauth.provider.OIDCProvider#validateIdTokenAndExtractClaims}.
 */
public class OAuthExchangeForm extends Validated {

    @NotNull
    private final String idToken;

    @NotNull
    private final String nonce;

    /** Requested JWT lifetime in days; clamped by server to the configured max. */
    private final int expirationDays;

    @JsonCreator
    public OAuthExchangeForm(@JsonProperty("idToken")        final String idToken,
                             @JsonProperty("nonce")          final String nonce,
                             @JsonProperty("expirationDays") final Integer expirationDays) {
        this.idToken        = idToken;
        this.nonce          = nonce;
        this.expirationDays = expirationDays == null ? 0 : expirationDays;
    }

    public String getIdToken()   { return idToken; }
    public String getNonce()     { return nonce; }
    public int getExpirationDays() { return expirationDays; }
}
