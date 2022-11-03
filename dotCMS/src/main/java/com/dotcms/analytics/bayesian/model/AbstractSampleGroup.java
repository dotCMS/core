package com.dotcms.analytics.bayesian.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;


/**
 * Sample data map.
 *
 * @author vico
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
public interface AbstractSampleGroup {

    @JsonProperty("samples")
    Map<String, List<SampleData>> samples();

}
