package com.dotcms.analytics.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

import javax.annotation.Nullable;


/**
 * POJO class to encapsulate analytics app properties.
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
public interface AbstractAnalyticsProperties {

    @JsonProperty("clientId")
    String clientId();

    @JsonProperty("clientSecret")
    String clientSecret();

    @Nullable
    @JsonProperty("analyticsKey")
    String analyticsKey();

    @JsonProperty("analyticsConfigUrl")
    String analyticsConfigUrl();

    @JsonProperty("analyticsWriteUrl")
    String analyticsWriteUrl();

    @JsonProperty("analyticsReadUrl")
    String analyticsReadUrl();

}
