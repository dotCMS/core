package com.dotcms.experience.collectors.workflow;

import com.dotcms.experience.MetricCategory;
import com.dotcms.experience.MetricFeature;
import com.dotcms.experience.collectors.DBMetricType;


/**
 * Collect the count of Unique Workflow SubActions, it means if the same Sub Action is use by several
 * Workflow Action then it count as 1
 */
public class UniqueSubActionsDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "UNIQUE_SUBACTIONS_COUNT";
    }

    @Override
    public String getDescription() {
        return "Count of unique workflow subactions in all Workflow actions";
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
        return "SELECT COUNT(distinct workflow_action_class.name) AS value " +
                "FROM workflow_action_class " +
                    "INNER JOIN workflow_action ON workflow_action.id=workflow_action_class.action_id " +
                    "INNER JOIN workflow_scheme ON workflow_scheme.id=workflow_action.scheme_id " +
                "WHERE archived = false";
    }
}
