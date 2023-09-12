package com.dotcms.analytics.bayesian.model;

import com.dotcms.analytics.bayesian.beta.BetaDistributionWrapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;


/**
 * Bayesian intermediate data class to store all the data required to make the calculations.
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = BayesianProcessingData.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractBayesianProcessingData {

    @JsonProperty("priors")
    BayesianPriors priors();

    @JsonProperty("input")
    VariantInput input();

    @JsonProperty("distribution")
    BetaDistributionWrapper distribution();

}
