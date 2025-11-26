package com.dotcms.telemetry.collectors.experiment;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Metric type to count the experiments edited in the past 30 days
 * @author jsanca
 */
public class CountExperimentsEditedInThePast30DaysMetricType implements DBMetricType {

    @Override
    public String getName() {
        return "COUNT_EXPERIMENTS_EDITED_IN_THE_PAST_30_DAYS";
    }

    @Override
    public String getDescription() {
        return "Count of experiments edited in the past 30 days";
    }

    @Override
    public String getSqlQuery() {
        return "select count(*) as Value from experiment where mod_date >= NOW() - INTERVAL '30 days'";
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
