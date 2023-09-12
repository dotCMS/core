package com.dotcms.analytics.bayesian.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

/**
 * Sample data POJO.
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonDeserialize(as = SampleData.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractSampleData {

    @JsonProperty("x")
    double x();

    @JsonProperty("y")
    double y();

}
