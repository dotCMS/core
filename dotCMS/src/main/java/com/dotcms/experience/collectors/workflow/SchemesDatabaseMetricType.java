package com.dotcms.experience.collectors.workflow;

import com.dotcms.experience.MetricCategory;
import com.dotcms.experience.MetricFeature;
import com.dotcms.experience.collectors.DBMetricType;


/**
 * Collect the count of Workflow Schemes
 */
public class SchemesDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "SCHEMES_COUNT";
    }

    @Override
    public String getDescription() {
        return "Count of workflow schemes";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.WORKFLOW;
    }

    @Override
    public String getSqlQuery() {
        return "SELECT count(*) as value FROM workflow_scheme WHERE archived=false";
    }
}
