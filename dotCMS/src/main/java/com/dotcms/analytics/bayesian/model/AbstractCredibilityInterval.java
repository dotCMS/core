package com.dotcms.analytics.bayesian.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

/**
 * Credibility interval class
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = CredibilityInterval.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractCredibilityInterval {

    @JsonProperty("lower")
    double lower();

    @JsonProperty("upper")
    double upper();

}
