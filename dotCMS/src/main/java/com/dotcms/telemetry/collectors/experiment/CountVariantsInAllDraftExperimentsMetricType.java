package com.dotcms.telemetry.collectors.experiment;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

public class CountVariantsInAllDraftExperimentsMetricType   implements DBMetricType {
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
        return "SELECT COALESCE(SUM(jsonb_array_length(traffic_proportion->'variants')),0) AS Value FROM experiment where experiment.status = 'DRAFT'";
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

