package com.dotcms.telemetry.collectors.experiment;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Metric type to count pages with scheduled experiments
 * @author jsanca
 */
public class CountPagesWithScheduledExperimentsMetricType implements DBMetricType {

    @Override
    public String getName() {
        return "COUNT_PAGES_WITH_SCHEDULED_EXPERIMENTS";
    }

    @Override
    public String getDescription() {
        return "Count of pages with scheduled experiments";
    }

    @Override
    public String getSqlQuery() {
        return "select count(*) as Value from experiment where scheduling IS NOT NULL AND status = 'SCHEDULED'";
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
