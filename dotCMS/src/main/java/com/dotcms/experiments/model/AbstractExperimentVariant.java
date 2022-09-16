package com.dotcms.experiments.model;

import com.dotcms.variant.VariantAPI;
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
public interface AbstractExperimentVariant extends Serializable, Comparable<ExperimentVariant> {
    String EXPERIMENT_VARIANT_NAME_PREFIX = "dotexperiment-";
    String EXPERIMENT_VARIANT_NAME_SUFFIX = "-variant-";
    String EXPERIMENT_VARIANT_DESCRIPTION = "Variant ";

    @JsonProperty("id")
    String id();
    @JsonProperty("name")
    String description();
    float weight();

    default int compareTo(final ExperimentVariant o) {
        if(id().equals(VariantAPI.DEFAULT_VARIANT.name())) {
            return -1;
        }

        if(o.id().equals(VariantAPI.DEFAULT_VARIANT.name())) {
            return 1;
        }

        return id().compareTo(o.id());
    }
}
