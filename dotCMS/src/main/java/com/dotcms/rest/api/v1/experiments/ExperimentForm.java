package com.dotcms.rest.api.v1.experiments;

import static com.dotcms.experiments.business.ExperimentsAPI.EXPERIMENT_MAX_DURATION;

import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Goals;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.experiments.model.TrafficProportion;
import com.dotcms.repackage.javax.validation.constraints.Size;
import com.dotcms.rest.api.Validated;
import com.dotcms.util.DotPreconditions;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * From to create/update an {@link com.dotcms.experiments.model.Experiment} from REST
 */

@JsonDeserialize(builder = ExperimentForm.Builder.class)
public class ExperimentForm extends Validated {
    @Size(min=1, max = 255)
    private final String name;
    @Size(max = 255)
    private final String description;
    private final String pageId;
    private final float trafficAllocation;
    private final TrafficProportion trafficProportion;
    private final Scheduling scheduling;
    private final Goals goals;

    private ExperimentForm(final Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.pageId = builder.pageId;
        this.trafficAllocation = builder.trafficAllocation;
        this.trafficProportion = builder.trafficProportion;
        this.scheduling = builder.scheduling;
        this.goals = builder.goals;
        checkValid();
    }

    public void checkValid() {
        super.checkValid();
        validateScheduling(scheduling);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPageId() {
        return pageId;
    }

    public float getTrafficAllocation() {
        return trafficAllocation;
    }

    public TrafficProportion getTrafficProportion() {
        return trafficProportion;
    }

    public Scheduling getScheduling() {
        return scheduling;
    }

    public Goals getGoals() {
        return goals;
    }

    public static final class Builder {
        private String name;
        private String description;
        private Status status;
        private String pageId;
        private float trafficAllocation=-1;
        private TrafficProportion trafficProportion;
        private Scheduling scheduling;
        private Goals goals;

        private Builder() {
        }

        public static Builder anExperimentForm() {
            return new Builder();
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withStatus(Status status) {
            this.status = status;
            return this;
        }

        public Builder withPageId(String pageId) {
            this.pageId = pageId;
            return this;
        }

        public Builder withTrafficAllocation(float trafficAllocation) {
            this.trafficAllocation = trafficAllocation;
            return this;
        }

        public Builder withTrafficProportion(TrafficProportion trafficProportion) {
            this.trafficProportion = trafficProportion;
            return this;
        }

        public Builder withScheduling(Scheduling scheduling) {
            this.scheduling = scheduling;
            return this;
        }

        public Builder withGoals(Goals goals) {
            this.goals = goals;
            return this;
        }

        public ExperimentForm build() {
            return new ExperimentForm(this);
        }
    }

    private void validateScheduling(final Scheduling scheduling) {
        if(scheduling==null) return;

        final Instant NOW = Instant.now().plus(1, ChronoUnit.MINUTES);

        if(scheduling.startDate().isPresent() && scheduling.endDate().isEmpty()) {
            DotPreconditions.checkState(scheduling.startDate().get().isAfter(NOW),
                    "Invalid Scheduling. Start date is in the past");

        } else if(scheduling.startDate().isEmpty() && scheduling.endDate().isPresent()) {
            DotPreconditions.checkState(scheduling.endDate().get().isAfter(NOW),
                    "Invalid Scheduling. End date is in the past");
            DotPreconditions.checkState(
                    Instant.now().plus(EXPERIMENT_MAX_DURATION, ChronoUnit.DAYS)
                            .isAfter(scheduling.endDate().get()),
                    "Experiment duration must be less than "
                            + EXPERIMENT_MAX_DURATION +" days. ");

        } else {
            DotPreconditions.checkState(scheduling.startDate().get().isAfter(NOW),
                    "Invalid Scheduling. Start date is in the past");

            DotPreconditions.checkState(scheduling.endDate().get().isAfter(NOW),
                    "Invalid Scheduling. End date is in the past");

            DotPreconditions.checkState(scheduling.endDate().get().isAfter(scheduling.startDate().get()),
                    "Invalid Scheduling. End date must be after the start date");

            DotPreconditions.checkState(Duration.between(scheduling.startDate().get(),
                            scheduling.endDate().get()).toDays() <= EXPERIMENT_MAX_DURATION,
                    "Experiment duration must be less than "
                            + EXPERIMENT_MAX_DURATION +" days. ");
        }
    }
}
