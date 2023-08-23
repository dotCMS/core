package com.dotcms.analytics.bayesian.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

/**
 * Bean to hold control and test data and its difference.
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = DifferenceData.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractDifferenceData {

    @JsonProperty("controlData")
    double[] controlData();

    @JsonProperty("testData")
    double[] testData();

    @JsonProperty("differences")
    double[] differences();

    @JsonProperty("relativeDifference")
    double relativeDifference();

}
