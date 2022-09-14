package com.dotcms.experiments.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import org.immutables.value.Value;

/**
 * Immutable implementation of TrafficProportion
 *
 * A TrafficProportion represents how the traffic is going to be divided across the different
 * HTML Page Variants.
 *
 * Defaults to {@link AbstractTrafficProportion.Type#SPLIT_EVENLY}
 *
 * It also holds a list of the percentages per variant according to the selected
 * {@link AbstractTrafficProportion.Type}
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = TrafficProportion.class)
@JsonDeserialize(as = TrafficProportion.class)
public interface AbstractTrafficProportion extends Serializable {
    @JsonProperty("type")
    @Value.Default
    default Type type() {
        return Type.SPLIT_EVENLY;
    }

    @JsonProperty("variantsPercentages")
    @Value.Default
    default Map<String, Float> variantsPercentagesMap() {
        return Collections.emptyMap();
    }

    enum Type {
        SPLIT_EVENLY,
        CUSTOM_PERCENTAGES
    }
}
