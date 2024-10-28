package com.dotcms.experience.collectors.site;

import com.dotcms.experience.MetricCategory;
import com.dotcms.experience.MetricFeature;
import com.dotcms.experience.collectors.DBMetricType;

/**
 * Collects the count of sites With non-default value on the tagStorage attribute
 */
public class SitesWithNoDefaultTagStorageDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "SITES_NON_DEFAULT_TAG_STORAGE_COUNT";
    }

    @Override
    public String getDescription() {
        return "Count of Sites With Not Default value on the tagStorage attribute";
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
        return "SELECT COUNT(distinct c.identifier) AS value\n" +
                "FROM contentlet c\n" +
                "         JOIN structure s ON c.structure_inode = s.inode\n" +
                "         JOIN contentlet_version_info cvi on (c.inode = cvi.working_inode OR c.inode = cvi.live_inode)\n" +
                " WHERE s.name = 'Host'\n" +
                "  AND c.identifier <> 'SYSTEM_HOST'\n" +
                "  AND c.contentlet_as_json -> 'fields' -> 'tagStorage' ->> 'value' <>\n" +
                "    -- DEFAULT_HOST identifier\n" +
                "      (SELECT distinct c.identifier\n" +
                "       FROM contentlet c\n" +
                "                JOIN structure s ON c.structure_inode = s.inode\n" +
                "                JOIN contentlet_version_info cvi\n" +
                "                     on (c.inode = cvi.working_inode OR c.inode = cvi.live_inode)\n" +
                "       WHERE s.name = 'Host'\n" +
                "         AND c.contentlet_as_json -> 'fields' -> 'isDefault' ->> 'value' = 'true')";
    }
}
