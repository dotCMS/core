package com.dotcms.analytics.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;


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

    @JsonProperty("analyticsKey")
    AnalyticsKey analyticsKey();

    @JsonProperty("analyticsConfigUrl")
    String analyticsConfigUrl();

    @JsonProperty("analyticsWriteUrl")
    String analyticsWriteUrl();

    @JsonProperty("analyticsReadUrl")
    String analyticsReadUrl();

}
