package com.dotcms.telemetry.collectors.content;

import com.dotcms.telemetry.DashboardMetric;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;
import javax.enterprise.context.ApplicationScoped;

/**
 * Collects the total number of contentlets
 */
@ApplicationScoped
@DashboardMetric(category = "content", priority = 1)
public class TotalContentsDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "COUNT";
    }

    @Override
    public String getDescription() {
        return "Total number of Contentlets";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.CONTENTLETS;
    }

    @Override
    public String getSqlQuery() {
        return "SELECT COUNT(working_inode) AS value FROM contentlet_version_info";
    }
}
