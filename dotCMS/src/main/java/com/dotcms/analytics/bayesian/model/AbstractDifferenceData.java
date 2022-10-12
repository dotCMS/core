package com.dotcms.analytics.bayesian.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

/**
 * Bean to hold control and test data and its difference.
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
public interface AbstractDifferenceData {

    @JsonProperty("controlData")
    double[] controlData();

    @JsonProperty("testData")
    double[] testData();

    @JsonProperty("differences")
    double[] differences();

}
