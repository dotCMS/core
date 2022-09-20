package com.dotcms.analytics.bayesian.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

import java.util.Map;


/**
 * Bayesian calculation result wrapper.
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
public interface AbstractBayesianResult {

    @JsonProperty("result")
    double result();

    @JsonProperty("distributionPdfs")
    AbstractSampleGroup distributionPdfs();

    @JsonProperty("differenceData")
    AbstractDifferenceData differenceData();

    @JsonProperty("quantiles")
    Map<Double, AbstractQuantilePair> quantiles();

}
