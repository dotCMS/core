package com.dotcms.telemetry.collectors.experiment;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Metric type to count pages with ended experiments
 * @author jsanca
 */
public class CountPagesWithAllEndedExperimentsMetricType implements DBMetricType {

    @Override
    public String getName() {
        return "COUNT_PAGES_WITH_ENDED_EXPERIMENTS";
    }

    @Override
    public String getDescription() {
        return "Count of pages with ended experiments";
    }

    @Override
    public String getSqlQuery() {
        return "select count(*) as Value from experiment where status = 'ENDED'";
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
