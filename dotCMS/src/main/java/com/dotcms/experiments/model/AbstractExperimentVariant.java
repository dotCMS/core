package com.dotcms.experiments.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import org.immutables.value.Value;

/**
 * Immutable implementation of ExperimentVariant
 *
 * Represents a {@link com.dotcms.variant.model.Variant} related to an {@link Experiment}
 *
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = ExperimentVariant.class)
@JsonDeserialize(as = ExperimentVariant.class)
public interface AbstractExperimentVariant extends Serializable {
    String EXPERIMENT_VARIANT_NAME = "experiment-variant-";
    String EXPERIMENT_VARIANT_DESCRIPTION = "Variant ";

    @JsonProperty("id")
    String id();
    @JsonProperty("description")
    String description();
    float weight();
}
