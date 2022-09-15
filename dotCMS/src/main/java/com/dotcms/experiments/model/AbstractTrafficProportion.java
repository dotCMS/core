package com.dotcms.experiments.model;

import com.dotcms.util.DotPreconditions;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.vavr.control.Try;
import java.io.Serializable;
import java.util.Set;
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

    @JsonProperty("variants")
    @Value.Default
    default Set<ExperimentVariant> variants() {
        return Set.of(ExperimentVariant.builder()
                .id(APILocator.getVariantAPI().DEFAULT_VARIANT.identifier())
                .description("Original").weight(100).build());
    }

    @Value.Check
    default void check() {
        if(UtilMethods.isSet(variants())) {
            DotPreconditions.isTrue(variants().stream()
                    .allMatch((variant)-> Try.of(()-> APILocator.getVariantAPI()
                            .getByName(variant.id()).isPresent()
                    ).getOrElse(false)), IllegalArgumentException.class,
                    ()->"Invalid Variants provided");
        }
    }

    enum Type {
        SPLIT_EVENLY,
        CUSTOM_PERCENTAGES
    }
}
