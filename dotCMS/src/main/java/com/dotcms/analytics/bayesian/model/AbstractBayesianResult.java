package com.dotcms.analytics.bayesian.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
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

    @JsonProperty("inFavorOf")
    String inFavorOf();

    @Nullable
    @JsonProperty("distributionPdfs")
    SampleGroup distributionPdfs();

    @Nullable
    @JsonProperty("differenceData")
    DifferenceData differenceData();

    @Nullable
    @JsonProperty("quantiles")
    Map<Double, QuantilePair> quantiles();

}
