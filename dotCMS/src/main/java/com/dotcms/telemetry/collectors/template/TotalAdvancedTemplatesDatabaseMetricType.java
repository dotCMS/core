package com.dotcms.telemetry.collectors.template;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Collects the total count of advanced templates
 */
public class TotalAdvancedTemplatesDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "COUNT_OF_ADVANCED_TEMPLATES";
    }

    @Override
    public String getDescription() {
        return "Total count of advanced templates";
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
        return "SELECT COUNT(DISTINCT template.identifier) as value " +
                "FROM template INNER JOIN template_version_info on template.identifier = template_version_info.identifier " +
                "WHERE drawed = false and deleted = false";
    }
}
