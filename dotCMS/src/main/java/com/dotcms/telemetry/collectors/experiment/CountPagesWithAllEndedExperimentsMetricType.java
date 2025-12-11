package com.dotcms.telemetry.collectors.experiment;

import com.dotcms.experiments.model.AbstractExperiment;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;
import javax.enterprise.context.ApplicationScoped;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;

/**
 * Metric type to count pages with ended experiments
 * @author jsanca
 */
@MetricsProfile(ProfileType.FULL)
@ApplicationScoped
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
    public String getDisplayLabel() {
        return "Pages with ended experiments";
    }

    @Override
    public String getSqlQuery() {
        return "select count(*) as Value from experiment where status = '" + AbstractExperiment.Status.ENDED.name() + "'";
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
