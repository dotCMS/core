package com.dotcms.telemetry.collectors.site;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Collects the total count of sites
 */
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
