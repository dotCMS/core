package com.dotcms.telemetry.collectors.workflow;

import com.dotcms.telemetry.DashboardMetric;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;
import com.dotcms.telemetry.collectors.DBMetricType;
import javax.enterprise.context.ApplicationScoped;

/**
 * Collect the count of Content Types that are NOT using 'System Workflow'
 */
@ApplicationScoped
@MetricsProfile({ProfileType.STANDARD, ProfileType.FULL})
@DashboardMetric(category = "content", priority = 4)
public class ContentTypesDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "CONTENT_TYPES_ASSIGNED";
    }

    @Override
    public String getDescription() {
        return "Count content types assigned schemes other than System Workflow";
    }

    @Override
    public String getDisplayLabel() {
        return "With Workflows";
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
        return "SELECT COUNT(distinct structure_id) AS value FROM workflow_scheme_x_structure " +
                "INNER JOIN workflow_scheme ON workflow_scheme.id=workflow_scheme_x_structure.scheme_id " +
                "WHERE name != 'System Workflow'";
    }
}
