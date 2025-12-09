package com.dotcms.telemetry.collectors.template;

import com.dotcms.telemetry.DashboardMetric;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;
import javax.enterprise.context.ApplicationScoped;

/**
 * Collects the total count of builder templates, it means excluding File, Advanced, and Layout templates
 */
@ApplicationScoped
@DashboardMetric(category = "system", priority = 5)
public class TotalBuilderTemplatesDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "COUNT_OF_TEMPLATE_BUILDER_TEMPLATES";
    }

    @Override
    public String getDescription() {
        return "Total count of Builder templates, excluding File, Advanced, and Layout templates";
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
                "WHERE drawed = true AND deleted = false AND title NOT LIKE 'anonymous_layout_%'";
    }
}
