package com.dotcms.analytics.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.Instant;


/**
 * Access token class to use with analytics stack.
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = AccessToken.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractAccessToken extends Serializable {

    @JsonProperty(value = "access_token", access = JsonProperty.Access.WRITE_ONLY)
    String accessToken();

    @JsonProperty("token_type")
    String tokenType();

    @Nullable
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss.SSS")
    @JsonProperty("issueDate")
    Instant issueDate();

    @JsonProperty("expires_in")
    Integer expiresIn();

    @Nullable
    @JsonProperty("refresh_expires_in")
    Integer refreshExpiresIn();

    @Nullable
    @JsonProperty(value = "refresh_token", access = JsonProperty.Access.WRITE_ONLY)
    String refreshToken();

    @Nullable
    @JsonProperty("scope")
    String scope();

    @Nullable
    @JsonProperty("client_id")
    String clientId();

    @Nullable
    @JsonProperty("aud")
    String aud();

    @Nullable
    @JsonProperty("status")
    AccessTokenStatus status();

}
