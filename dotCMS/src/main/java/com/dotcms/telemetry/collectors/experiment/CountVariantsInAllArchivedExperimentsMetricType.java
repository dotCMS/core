package com.dotcms.telemetry.collectors.experiment;

import com.dotcms.experiments.model.AbstractExperiment;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Metric type to count the variants on all archived experiments
 * @author jsanca
 */
public class CountVariantsInAllArchivedExperimentsMetricType implements DBMetricType {

    @Override
    public String getName() {
        return "COUNT_VARIANTS_IN_ALL_ARCHIVED_EXPERIMENTS";
    }

    @Override
    public String getDescription() {
        return "Count of pages with archived experiments";
    }

    @Override
    public String getSqlQuery() {
        return "SELECT COALESCE(SUM(jsonb_array_length(traffic_proportion->'variants')),0) AS Value FROM experiment where experiment.status = '"+ AbstractExperiment.Status.ARCHIVED.name() +"'";
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
