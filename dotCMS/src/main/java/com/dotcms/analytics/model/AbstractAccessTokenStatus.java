package com.dotcms.analytics.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * Access token wrapper class.
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = AccessTokenStatus.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractAccessTokenStatus extends Serializable {

    @JsonProperty("tokenStatus")
    TokenStatus tokenStatus();

    @Nullable
    @JsonProperty("errorType")
    AccessTokenErrorType errorType();

    @Nullable
    @JsonProperty("reason")
    String reason();

}
