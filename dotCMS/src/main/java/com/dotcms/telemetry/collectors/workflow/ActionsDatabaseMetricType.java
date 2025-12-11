package com.dotcms.telemetry.collectors.workflow;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;
import com.dotcms.telemetry.collectors.DBMetricType;
import javax.enterprise.context.ApplicationScoped;

/**
 * Collect the count of Workflow Actions
 */
@ApplicationScoped
@MetricsProfile(ProfileType.FULL)
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
    public String getDisplayLabel() {
        return "Workflow actions in all schemes";
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
