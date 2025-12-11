package com.dotcms.telemetry.collectors.site;

import com.dotcms.telemetry.DashboardMetric;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;
import com.dotcms.telemetry.collectors.DBMetricType;
import javax.enterprise.context.ApplicationScoped;

/**
 * Collects the total count of sites
 */
@ApplicationScoped
@MetricsProfile({ProfileType.MINIMAL, ProfileType.STANDARD, ProfileType.FULL})
@DashboardMetric(category = "site", priority = 1)
public class TotalSitesDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "COUNT_OF_SITES";
    }

    @Override
    public String getDescription() {
        return "Total count of sites";
    }

    @Override
    public String getDisplayLabel() {
        return "Total Sites";
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
        return "SELECT COUNT(id) as value FROM identifier WHERE asset_subtype='Host' AND id <> 'SYSTEM_HOST'";
    }
}
