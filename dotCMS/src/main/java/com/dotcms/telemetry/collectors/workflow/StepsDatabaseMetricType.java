package com.dotcms.telemetry.collectors.workflow;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Collect the count of Workflow Steps
 */
public class StepsDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "STEPS_COUNT";
    }

    @Override
    public String getDescription() {
        return "Count of steps in all schemes";
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
        return "SELECT COUNT(*) AS value FROM workflow_step " +
                "INNER JOIN workflow_scheme ON workflow_scheme.id=workflow_step.scheme_id " +
                "WHERE archived = false";
    }
}
