package com.dotcms.telemetry.collectors.site;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;
import javax.enterprise.context.ApplicationScoped;

/**
 * Collects the count of sites with permissions not inheriting from System Host
 */
@ApplicationScoped
public class CountOfSitesWithIndividualPermissionsMetricType implements DBMetricType {


    @Override
    public String getName() {
        return "SITES_WITH_INDIVIDUAL_PERMISSIONS";
    }

    @Override
    public String getDescription() {
        return "Count of sites with permissions not inheriting from System Host";
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
        return "SELECT COUNT(DISTINCT c.identifier) AS value\n" +
                "FROM contentlet c JOIN structure s\n" +
                "                       ON c.structure_inode = s.inode\n" +
                "                  JOIN contentlet_version_info cvi\n" +
                "                       ON (c.inode = cvi.working_inode OR c.inode = cvi.live_inode)\n" +
                "WHERE s.name = 'Host' AND c.identifier <> 'SYSTEM_HOST'\n" +
                "AND (SELECT COUNT(*) AS cc FROM permission where inode_id=c.identifier) > 0;";
    }
}
