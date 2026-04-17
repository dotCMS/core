package com.dotcms.auth.providers.oauth.rest;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Request body for {@link OAuthTokenResource#getToken}. Clients supply an OAuth
 * access token obtained from the provider; the resource exchanges it for a
 * dotCMS JWT bound to the resolved user.
 */
@JsonDeserialize(builder = TokenForm.Builder.class)
public class TokenForm extends Validated {

    private final String oauthToken;
    private final int    expirationDays;

    public String getOauthToken()    { return oauthToken; }
    public int    getExpirationDays(){ return expirationDays; }

    private TokenForm(final Builder b) {
        this.oauthToken     = b.oauthToken;
        this.expirationDays = b.expirationDays;
        checkValid();
    }

    public static final class Builder {
        @JsonProperty private String oauthToken;
        @JsonProperty private int    expirationDays = -1;

        public Builder oauthToken(final String v)     { this.oauthToken = v;     return this; }
        public Builder expirationDays(final int v)    { this.expirationDays = v; return this; }
        public TokenForm build()                      { return new TokenForm(this); }
    }
}
