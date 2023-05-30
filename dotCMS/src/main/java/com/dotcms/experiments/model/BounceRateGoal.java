package com.dotcms.experiments.model;

import com.dotcms.analytics.metrics.Metric;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represent a {@link Goal} that is using a Bounce Rate {@link Metric}
 *
 * @see Metric
 * @see Goal
 */
public class BounceRateGoal extends Goal {

    @JsonCreator
    public BounceRateGoal(final @JsonProperty("metric")  Metric metric) {
        super(metric);
    }

    @Override
    public GoalType type() {
        return GoalType.MINIMIZE;
    }
}
