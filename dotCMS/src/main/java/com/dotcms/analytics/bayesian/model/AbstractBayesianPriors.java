package com.dotcms.analytics.bayesian.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

/**
 * Single Bayesian Prior data class to store provided prior (or known) data.
 * This data is required to make some more calculations regarding the quantiles, eventual histogram rendering, etc.
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = BayesianPriors.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractBayesianPriors {

    @JsonProperty("alpha")
    double alpha();

    @JsonProperty("beta")
    double beta();

}
