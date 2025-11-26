package com.dotcms.telemetry.collectors.experiment;

import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Metric type to count the experiments with reach page goal
 * @author jsanca
 */
public class CountExperimentsWithReachPageGoalMetricType implements DBMetricType {

    @Override
    public String getName() {
        return "COUNT_EXPERIMENTS_WITH_REACH_PAGE_GOAL";
    }

    @Override
    public String getDescription() {
        return "Count of experiments with Reach Page Goal";
    }

    @Override
    public String getSqlQuery() {
        return "SELECT COUNT(*) AS Value FROM experiment WHERE goals->'primary'->>'type' = '" + MetricType.REACH_PAGE.name() + "'";
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
