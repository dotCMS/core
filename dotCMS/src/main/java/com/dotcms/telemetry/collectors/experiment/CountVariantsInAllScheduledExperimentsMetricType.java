package com.dotcms.telemetry.collectors.experiment;

import com.dotcms.experiments.model.AbstractExperiment;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;
import javax.enterprise.context.ApplicationScoped;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;

/**
 * Count Variants in All Scheduled Experiments Metric Type
 * @author jsanca
 */
@MetricsProfile(ProfileType.FULL)
@ApplicationScoped
public class CountVariantsInAllScheduledExperimentsMetricType implements DBMetricType {

    @Override
    public String getName() {
        return "COUNT_VARIANTS_IN_ALL_SCHEDULED_EXPERIMENTS";
    }

    @Override
    public String getDescription() {
        return "Count of pages with scheduled experiments";
    }

    @Override
    public String getSqlQuery() {
        return "SELECT COALESCE(SUM(jsonb_array_length(traffic_proportion->'variants')),0) AS Value FROM experiment where experiment.status = '"+ AbstractExperiment.Status.SCHEDULED.name() +"'";
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
