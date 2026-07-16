package com.dotcms.rest.api.v1.experiments;

import com.dotcms.experiments.model.Goals;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.experiments.model.TargetingCondition;
import com.dotcms.experiments.model.TrafficProportion;
import org.hibernate.validator.constraints.Length;
import com.dotcms.rest.api.Validated;
import com.dotmarketing.business.APILocator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;

/**
 * From to create/update an {@link com.dotcms.experiments.model.Experiment} from REST
 */

@JsonDeserialize(builder = ExperimentForm.Builder.class)
public class ExperimentForm extends Validated {
    @Length(min=1, max = 255)
    private final String name;
    @Length(max = 255)
    private final String description;
    private final String pageId;
    private final float trafficAllocation;
    private final TrafficProportion trafficProportion;
    private final Scheduling scheduling;
    private final Goals goals;
    private final List<TargetingCondition> targetingConditions;
    private final int lookbackWindow;

    private ExperimentForm(final Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.pageId = builder.pageId;
        this.trafficAllocation = builder.trafficAllocation;
        this.trafficProportion = builder.trafficProportion;
        this.scheduling = builder.scheduling;
        this.goals = builder.goals;
        this.targetingConditions = builder.targetingConditions;
        this.lookbackWindow = builder.lookbackWindow;
        checkValid();
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

    public List<TargetingCondition> getTargetingConditions() {
        return targetingConditions;
    }

    public int getLookbackWindow() {
        return lookbackWindow;
    }

    public static final class Builder {
        private String name;
        private String description;
        private String pageId;
        private float trafficAllocation=-1;
        private TrafficProportion trafficProportion;
        private Scheduling scheduling;
        private Goals goals;
        private List<TargetingCondition> targetingConditions = new ArrayList<>();
        private int lookbackWindow =-1;

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

        public Builder withLookbackWindow(int lookbackWindow) {
            this.lookbackWindow = lookbackWindow;
            return this;
        }

        public Builder withTargetingConditions(List<TargetingCondition> targetingConditions) {
            this.targetingConditions = targetingConditions;
            return this;
        }

        public Builder withGoals(final Goals goals) {
            this.goals = goals;
            return this;
        }

        public ExperimentForm build() {
            return new ExperimentForm(this);
        }
    }

    private void validateScheduling(final Scheduling scheduling) {
        APILocator.getExperimentsAPI().validateScheduling(scheduling);
    }


}
