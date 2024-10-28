package com.dotcms.experience.collectors.urlmap;

import com.dotcms.experience.MetricCategory;
import com.dotcms.experience.MetricFeature;
import com.dotcms.experience.collectors.DBMetricType;

/**
 * Collect the count of all the Contentlets in content types with URL maps that does not have LIVE Version
 */
public class WorkingContentInUrlMapDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "WORKING_CONTENTLETS_IN_CONTENT_TYPES_WITH_URL_MAP";
    }

    @Override
    public String getDescription() {
        return "Count of Working Contentlets in content types with URL maps";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.URL_MAPS;
    }

    @Override
    public String getSqlQuery() {
        return "SELECT COUNT(DISTINCT contentlet.identifier) as value FROM contentlet\n" +
                    "INNER JOIN contentlet_version_info ON contentlet.identifier = contentlet_version_info.identifier\n" +
                    "INNER JOIN structure ON contentlet.structure_inode = structure.inode\n" +
                "WHERE url_map_pattern IS NOT null AND page_detail is not null AND deleted = false " +
                "AND live_inode IS null AND working_inode is not null";
    }
}

