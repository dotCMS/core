package com.dotcms.telemetry.collectors.content;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

public class ImportContentletsJobTriggeredMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "IMPORT_CONTENTLETS_JOB_TRIGGERED";
    }

    @Override
    public String getDescription() {
        return "Customer have used the Import Contentlets Job";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.CONTENTLETS;
    }

    @Override
    public String getSqlQuery() {
        return "select count(*) from job where queue_name='importContentlets'";
    }
}
