package com.dotcms.telemetry.collectors.workflow;

import com.dotcms.telemetry.DashboardMetric;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;
import javax.enterprise.context.ApplicationScoped;


/**
 * Collect the count of Workflow Schemes
 */
@ApplicationScoped
@DashboardMetric(category = "system", priority = 2)
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
