package com.dotcms.analytics.bayesian.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;


/**
 * Single Bayesian Prior data class
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = BayesianPriors.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractBayesianPriors {

    @JsonProperty("alpha")
    @Nullable
    Double alpha();

    @JsonProperty("beta")
    @Nullable
    Double beta();

}
