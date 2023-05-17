package com.dotcms.experiments.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import org.immutables.value.Value;

/**
 * Immutable implementation of Goals
 *
 * as v1 Goals represent what is set as the experiment primary goal
 *
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = Goals.class)
@JsonDeserialize(as = Goals.class)
public interface AbstractGoals extends Serializable {
    @JsonProperty("primary")
    Goal primary();
}
