package com.dotcms.analytics.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.io.Serializable;


/**
 * Access token class to use with analytics stack.
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = AnalyticsKey.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractAnalyticsKey extends Serializable {

    @JsonProperty("jsKey")
    String jsKey();

    @Nullable
    @JsonProperty("m2mKey")
    String m2mKey();

}
