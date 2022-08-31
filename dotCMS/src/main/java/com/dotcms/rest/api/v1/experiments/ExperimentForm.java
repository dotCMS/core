package com.dotcms.rest.api.v1.experiments;

import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.experiments.model.TrafficProportion;
import com.dotcms.repackage.javax.validation.constraints.Size;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * From to create/update an {@link com.dotcms.experiments.model.Experiment} from REST
 */

@JsonDeserialize(builder = ExperimentForm.Builder.class)
public class ExperimentForm extends Validated {
    @Size(min=1, max = 255)
    private final String name;
    @Size(max = 255)
    private final String description;
    private final Status status;
    private final String pageId;
    private final float trafficAllocation;
    private final TrafficProportion trafficProportion;
    private final Scheduling scheduling;

    private ExperimentForm(final Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.status = builder.status;
        this.pageId = builder.pageId;
        this.trafficAllocation = builder.trafficAllocation;
        this.trafficProportion = builder.trafficProportion;
        this.scheduling = builder.scheduling;
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

    public Status getStatus() {
        return status;
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

    public static final class Builder {
        private String name;
        private String description;
        private Status status;
        private String pageId;
        private float trafficAllocation=-1;
        private TrafficProportion trafficProportion;
        private Scheduling scheduling;

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

        public ExperimentForm build() {
            return new ExperimentForm(this);
        }
    }
}
