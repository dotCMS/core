package com.dotcms.telemetry.collectors.experiment;

import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;
import javax.enterprise.context.ApplicationScoped;

/**
 * Metric type to count the experiments with url parameter goal
 * @author jsanca
 */
@ApplicationScoped
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
        return "SELECT COUNT(*) AS Value FROM experiment WHERE goals->'primary'->>'type' = '" + MetricType.URL_PARAMETER.name() + "'";
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
