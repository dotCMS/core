package com.dotcms.analytics.bayesian.model;

import com.dotcms.analytics.model.AnalyticsKey;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Map;


/**
 * Bayesian calculation result wrapper.
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = BayesianResult.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractBayesianResult {

    @JsonProperty("result")
    double result();

    @JsonProperty("distributionPdfs")
    SampleGroup distributionPdfs();

    @JsonProperty("differenceData")
    DifferenceData differenceData();

    @JsonProperty("quantiles")
    Map<Double, QuantilePair> quantiles();

}
