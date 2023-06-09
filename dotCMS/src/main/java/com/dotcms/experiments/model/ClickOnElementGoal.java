package com.dotcms.experiments.model;

import com.dotcms.analytics.metrics.Metric;
import com.fasterxml.jackson.annotation.JsonCreator;

public class ClickOnElementGoal extends Goal {

    @JsonCreator()
    public ClickOnElementGoal(Metric metric) {
        super(metric);
    }

    @Override
    public GoalType type() {
        return GoalType.MAXIMIZE;
    }
}
