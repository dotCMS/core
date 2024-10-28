package com.dotcms.experience.collectors.workflow;

import com.dotcms.experience.MetricCategory;
import com.dotcms.experience.MetricFeature;
import com.dotcms.experience.collectors.DBMetricType;

/**
 * Collect the count of Workflow Actions
 */
public class ActionsDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "ACTIONS_COUNT";
    }

    @Override
    public String getDescription() {
        return "Count of workflow actions in all schemes";
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
        return "SELECT COUNT(*) AS value FROM workflow_action " +
                "INNER JOIN workflow_scheme ON workflow_scheme.id=workflow_action.scheme_id " +
                "WHERE archived = false";
    }
}
