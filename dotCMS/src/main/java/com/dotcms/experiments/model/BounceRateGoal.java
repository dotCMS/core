package com.dotcms.experiments.model;

import com.dotcms.analytics.metrics.Metric;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BounceRateGoal extends Goal {

    public BounceRateGoal(final @JsonProperty("metric")  Metric metric) {
        super(metric);
    }

    @Override
    public GoalType type() {
        return GoalType.MINIMIZE;
    }
}
