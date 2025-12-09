package com.dotcms.telemetry.collectors.experiment;

import com.dotcms.experiments.model.AbstractExperiment;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;
import javax.enterprise.context.ApplicationScoped;

/**
 * Metric type to count the variants on draft experiments
 * @author jsanca
 */
@ApplicationScoped
public class CountVariantsInAllDraftExperimentsMetricType   implements DBMetricType {
    @Override
    public String getName() {
        return "COUNT_VARIANTS_WITH_DRAFT_EXPERIMENTS";
    }

    @Override
    public String getDescription() {
        return "Count of variants with draft experiments";
    }

    @Override
    public String getSqlQuery() {
        return "SELECT COALESCE(SUM(jsonb_array_length(traffic_proportion->'variants')),0) AS Value FROM experiment where experiment.status = '" + AbstractExperiment.Status.DRAFT.name() + "'";
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

