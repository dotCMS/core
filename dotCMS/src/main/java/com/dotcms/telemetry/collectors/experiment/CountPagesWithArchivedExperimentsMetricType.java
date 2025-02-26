package com.dotcms.telemetry.collectors.experiment;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Metric type to count pages with archived experiments
 * @author jsanca
 */
public class CountPagesWithArchivedExperimentsMetricType implements DBMetricType {

    @Override
    public String getName() {
        return "COUNT_PAGES_WITH_ARCHIVED_EXPERIMENTS";
    }

    @Override
    public String getDescription() {
        return "Count of pages with archived experiments";
    }

    @Override
    public String getSqlQuery() {
        return "select count(*) as Value from experiment where status = 'ARCHIVED'";
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
