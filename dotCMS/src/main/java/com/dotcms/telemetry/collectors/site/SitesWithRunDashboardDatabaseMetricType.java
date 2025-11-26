package com.dotcms.telemetry.collectors.site;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Collects the count of sites with 'Run Dashboard' enabled
 */
public class SitesWithRunDashboardDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "SITES_RUN_DASHBOARD_TRUE_COUNT";
    }

    @Override
    public String getDescription() {
        return "Count of sites with 'Run Dashboard' enabled";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.SITES;
    }

    public String getSqlQuery() {
        return "SELECT COUNT(c.identifier)\n" +
                " AS value FROM contentlet c\n" +
                "         JOIN structure s ON c.structure_inode = s.inode\n" +
                "         JOIN contentlet_version_info cvi on (c.inode = cvi.working_inode OR c.inode = cvi.live_inode)\n" +
                " WHERE s.name = 'Host'\n" +
                "  AND c.identifier <> 'SYSTEM_HOST'\n" +
                "  AND c.contentlet_as_json -> 'fields' -> 'runDashboard' ->> 'value' = 'true'";
    }
}
