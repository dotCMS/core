package com.dotcms.experiments.model;

import static com.dotcms.experiments.business.ExperimentsAPI.EXPERIMENT_MAX_DURATION;

import com.dotcms.util.DotPreconditions;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * Immutable implementation of Scheduling
 *
 * A Scheduling comprises the start and end dates for an {@link Experiment}
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = Scheduling.class)
@JsonDeserialize(as = Scheduling.class)
public interface AbstractScheduling extends Serializable {
    @JsonProperty("startDate")
    Optional<Instant> startDate();

    @JsonProperty("endDate")
    Optional<Instant> endDate();

    @Value.Check
    default void check() {
        validateScheduling((Scheduling) this);
    }

    static Scheduling validateScheduling(final Scheduling scheduling) {
        Scheduling toReturn = scheduling;
        if(scheduling.startDate().isPresent() && scheduling.endDate().isEmpty()) {
            DotPreconditions.checkState(scheduling.startDate().get().isAfter(Instant.now()),
                    "Experiment cannot be started because the start date is in the past");

            toReturn = scheduling.withEndDate(scheduling.startDate().get()
                    .plus(EXPERIMENT_MAX_DURATION, ChronoUnit.DAYS));
        } else if(scheduling.startDate().isEmpty() && scheduling.endDate().isPresent()) {
            DotPreconditions.checkState(scheduling.endDate().get().isAfter(Instant.now()),
                    "Experiment cannot be started because the end date is in the past");
            DotPreconditions.checkState(
                    Instant.now().plus(EXPERIMENT_MAX_DURATION, ChronoUnit.DAYS)
                            .isAfter(scheduling.endDate().get()),
                    "Experiment duration must be less than "
                            + EXPERIMENT_MAX_DURATION +" days. ");

            toReturn = scheduling.withStartDate(Instant.now());
        } else {
            DotPreconditions.checkState(scheduling.startDate().get().isAfter(Instant.now()),
                    "Experiment cannot be started because the start date is in the past");

            DotPreconditions.checkState(scheduling.endDate().get().isAfter(Instant.now()),
                    "Experiment cannot be started because the end date is in the past");

            DotPreconditions.checkState(scheduling.endDate().get().isAfter(scheduling.startDate().get()),
                    "Experiment cannot be started because the end date must be after the start date");

            DotPreconditions.checkState(Duration.between(scheduling.startDate().get(),
                            scheduling.endDate().get()).toDays() <= EXPERIMENT_MAX_DURATION,
                    "Experiment duration must be less than "
                            + EXPERIMENT_MAX_DURATION +" days. ");
        }
        return toReturn;
    }
}
