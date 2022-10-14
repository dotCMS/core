package com.dotcms.analytics.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;


/**
 * Access token class to use with analytics stack.
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
public interface AbstractAnalyticsKey {

    @JsonProperty("jsKey")
    String jsKey();

    @JsonProperty("m2mKey")
    String m2mKey();

}
