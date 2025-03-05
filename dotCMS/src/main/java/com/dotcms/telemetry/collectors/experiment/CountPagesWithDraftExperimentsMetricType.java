package com.dotcms.telemetry.collectors.experiment;

import com.dotcms.experiments.model.AbstractExperiment;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Metric type to count pages with draft experiments
 * @author jsanca
 */
public class CountPagesWithDraftExperimentsMetricType  implements DBMetricType {
    @Override
    public String getName() {
        return "COUNT_PAGES_WITH_DRAFT_EXPERIMENTS";
    }

    @Override
    public String getDescription() {
        return "Count of pages with draft experiments";
    }

    @Override
    public String getSqlQuery() {
        return "select count(*) as Value from experiment where status = '" + AbstractExperiment.Status.DRAFT.name()  +"'";
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
