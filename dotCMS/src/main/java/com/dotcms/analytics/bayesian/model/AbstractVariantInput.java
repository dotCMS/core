package com.dotcms.analytics.bayesian.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

/**
 * Variant successes and failures pair.
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = VariantInput.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractVariantInput {

    @JsonProperty("variant")
    String variant();

    @JsonProperty("isControl")
    boolean isControl();

    @JsonProperty("successes")
    long successes();

    @JsonProperty("failures")
    long failures();

}
