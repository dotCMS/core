package com.dotcms.telemetry.collectors.workflow;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;
import javax.enterprise.context.ApplicationScoped;


/**
 * Collect the count of Workflow SubActions, no matter if the same Sub Action is use for more than one Action
 * in this case it count several times.
 */
@ApplicationScoped
public class SubActionsDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "SUBACTIONS_COUNT";
    }

    @Override
    public String getDescription() {
        return "Count of workflow subactions in all Workflow actions";
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
        return "SELECT COUNT(*) AS value " +
                "FROM workflow_action_class " +
                    "INNER JOIN workflow_action ON workflow_action.id=workflow_action_class.action_id " +
                    "INNER JOIN workflow_scheme ON workflow_scheme.id=workflow_action.scheme_id " +
                "WHERE archived = false";
    }
}
