package com.dotcms.telemetry.collectors.site;

import com.dotcms.telemetry.DashboardMetric;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;
import javax.enterprise.context.ApplicationScoped;

/**
 * Collects the count of aliases on all sites
 */
@ApplicationScoped
@DashboardMetric(category = "site", priority = 3)
public class TotalAliasesAllSitesDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "ALIASES_SITES_COUNT";
    }

    @Override
    public String getDescription() {
        return "Count of aliases on all sites";
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
        return  "SELECT coalesce(SUM(array_length(regexp_split_to_array(trim(result.aliases), '[\\n\\r\\t]'), 1)),0) AS value\n" +
                "FROM (SELECT c.contentlet_as_json -> 'fields' -> 'aliases' ->> 'value' AS aliases\n" +
                "      FROM contentlet c\n" +
                "               JOIN structure s on c.structure_inode = s.inode JOIN contentlet_version_info\n" +
                "      cvi on (c.inode = cvi.working_inode OR  c.inode = cvi.live_inode)\n" +
                "      WHERE s.name = 'Host' \n" +
                "        AND c.contentlet_as_json -> 'fields' -> 'hostName' ->> 'value' <> 'System Host')\n" +
                "         AS result\n" +
                "WHERE result.aliases <> ''\n";
    }
}
