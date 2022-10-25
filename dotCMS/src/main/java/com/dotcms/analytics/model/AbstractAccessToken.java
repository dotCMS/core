package com.dotcms.analytics.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.time.Instant;


/**
 * Access token class to use with analytics stack.
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
public interface AbstractAccessToken {

    @JsonProperty("accessToken")
    String accessToken();

    @JsonProperty("tokenType")
    String tokenType();

    @Nullable
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss.SSS")
    @JsonProperty("issueDate")
    Instant issueDate();

    @JsonProperty("expiresIn")
    Integer expiresIn();

    @Nullable
    @JsonProperty("refreshToken")
    String refreshToken();

    @JsonProperty("scope")
    String scope();

    @Nullable
    @JsonProperty("clientId")
    String clientId();

    @Nullable
    @JsonProperty("aud")
    String aud();

}
