package com.dotcms.telemetry.collectors.site;

import com.dotcms.telemetry.DashboardMetric;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;
import javax.enterprise.context.ApplicationScoped;

/**
 * Collects the total count of active sites
 */
@ApplicationScoped
@DashboardMetric(category = "site", priority = 2)
public class TotalActiveSitesDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "COUNT_OF_ACTIVE_SITES";
    }

    @Override
    public String getDescription() {
        return "Total count of active sites";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.SITES;
    }

    @Override
    public String getSqlQuery() {
        return "SELECT COUNT(id) as value\n" +
                "FROM identifier i\n" +
                "JOIN contentlet_version_info cvi ON i.id = cvi.identifier\n" +
                "WHERE asset_subtype = 'Host'\n" +
                "  AND id <> 'SYSTEM_HOST'\n" +
                "AND cvi.live_inode is not null";
    }
}
