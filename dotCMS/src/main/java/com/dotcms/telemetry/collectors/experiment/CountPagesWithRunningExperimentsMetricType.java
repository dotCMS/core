package com.dotcms.telemetry.collectors.experiment;

import com.dotcms.experiments.model.AbstractExperiment;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Metric type to count pages with running experiments
 * @author jsanca
 */
public class CountPagesWithRunningExperimentsMetricType  implements DBMetricType {

    @Override
    public String getName() {
        return "COUNT_PAGES_WITH_RUNNING_EXPERIMENTS";
    }

    @Override
    public String getDescription() {
        return "Count of pages with running experiments";
    }

    @Override
    public String getSqlQuery() {
        return "select count(*) as Value from experiment where status = '"+ AbstractExperiment.Status.RUNNING.name() + "'";
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
