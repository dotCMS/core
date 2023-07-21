package com.dotcms.experiments.model;

import com.dotcms.analytics.metrics.Metric;
import com.dotcms.experiments.model.Goal.GoalType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UrlParameterGoal extends Goal {
    @JsonCreator
    public UrlParameterGoal(final @JsonProperty("metric") Metric metric) {
        super(metric);
    }

    @Override
    public GoalType type() {
        return GoalType.MAXIMIZE;
    }
}
