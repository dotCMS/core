package com.dotcms.telemetry.collectors.experiment;

import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Metric type to count the experiments with exit rate goal
 * @author jsanca
 */
public class CountExperimentsWithExitRateGoalMetricType implements DBMetricType {

    @Override
    public String getName() {
        return "COUNT_EXPERIMENTS_WITH_EXIT_RATE_GOAL";
    }

    @Override
    public String getDescription() {
        return "Count of experiments with exit rate goal";
    }

    @Override
    public String getSqlQuery() {
        return "SELECT COUNT(*) AS Value FROM experiment WHERE goals->'primary'->>'type' = '" + MetricType.EXIT_RATE.name() + "'";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.PAID_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.EXPERIMENTS;
    }
}
