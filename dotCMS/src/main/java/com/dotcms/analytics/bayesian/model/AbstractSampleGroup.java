package com.dotcms.analytics.bayesian.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
@JsonDeserialize(as = SampleGroup.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractSampleGroup {

    @JsonProperty("samples")
    Map<String, List<SampleData>> samples();

}
