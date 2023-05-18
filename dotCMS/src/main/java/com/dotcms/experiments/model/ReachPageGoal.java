package com.dotcms.experiments.model;

import com.dotcms.analytics.metrics.Metric;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

/**
 * Represent a {@link Goal} that is using a Reach Page {@link Metric}
 *
 * @see Metric
 * @see Goal
 */
public class ReachPageGoal extends Goal {

    ReachPageGoal(final @JsonProperty("metric") Metric metric) {
        super(metric);
    }

    @Override
    public GoalType type() {
        return GoalType.MAXIMIZE;
    }
}
