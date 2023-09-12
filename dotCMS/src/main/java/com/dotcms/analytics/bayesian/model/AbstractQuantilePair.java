package com.dotcms.analytics.bayesian.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;


/**
 * Holder for quantile value and its rounded-formatted value.
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = QuantilePair.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractQuantilePair {

    @JsonProperty("quantile")
    double quantile();

    @JsonProperty("formatted")
    double formatted();

}
