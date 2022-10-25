package com.dotcms.analytics.bayesian.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

/**
 * Sample data POJO.
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
public interface AbstractSampleData {

    @JsonProperty("x")
    double x();

    @JsonProperty("y")
    double y();

}
