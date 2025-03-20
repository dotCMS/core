package com.dotcms.telemetry.collectors.template;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Collects the total count of templates
 */
public class TotalTemplatesDatabaseMetricType implements DBMetricType {

    @Override
    public String getName() {
        return "COUNT_OF_TEMPLATES";
    }

    @Override
    public String getDescription() {
        return "Total count of templates";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.LAYOUT;
    }

    @Override
    public String getSqlQuery() {
        return "SELECT template.count + file_template.count as value " +
                "FROM (SELECT COUNT(DISTINCT template.identifier)" +
                        "FROM template_version_info " +
                            "INNER JOIN template ON template_version_info.identifier = template.identifier " +
                        "WHERE title NOT LIKE 'anonymous_layout_%' and deleted = false) template, " +
                      "(SELECT COUNT(DISTINCT cvi.identifier) " +
                        "FROM contentlet_version_info cvi, identifier id " +
                        "WHERE id.parent_path like '/application/templates%' and " +
                            "id.asset_name = 'properties.vtl' and " +
                            "cvi.identifier = id.id and " +
                            "cvi.deleted = false) as file_template";
    }
}

