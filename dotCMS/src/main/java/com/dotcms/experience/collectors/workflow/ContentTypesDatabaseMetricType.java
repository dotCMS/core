package com.dotcms.experience.collectors.workflow;

import com.dotcms.experience.MetricCategory;
import com.dotcms.experience.MetricFeature;
import com.dotcms.experience.collectors.DBMetricType;

/**
 * Collect the count of Content Types that are NOT using 'System Workflow'
 */
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
