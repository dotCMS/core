package com.dotcms.telemetry.collectors.experiment;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Metric type to count the experiments with url parameter goal
 * @author jsanca
 */
public class CountExperimentsWithURLParameterGoalMetricType implements DBMetricType {

    @Override
    public String getName() {
        return "COUNT_EXPERIMENTS_WITH_URL_PARAMETER_GOAL";
    }

    @Override
    public String getDescription() {
        return "Count of experiments with url parameter goal";
    }

    @Override
    public String getSqlQuery() {
        return "SELECT COUNT(*) AS experiment_count FROM experiment WHERE goals->'primary'->'metric'->>'type' = 'URL_PARAMETER'";
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
