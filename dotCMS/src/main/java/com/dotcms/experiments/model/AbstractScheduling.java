package com.dotcms.experiments.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import java.time.Instant;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = Scheduling.class)
@JsonDeserialize(as = Scheduling.class)
public interface AbstractScheduling extends Serializable {
    @JsonProperty("startDate")
    Instant startDate();

    @JsonProperty("endDate")
    Instant endDate();
}
