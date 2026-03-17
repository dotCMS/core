package com.dotcms.telemetry.collectors.urlmap;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;
import com.dotcms.telemetry.collectors.DBMetricType;
import javax.enterprise.context.ApplicationScoped;

/**
 * Collect the count of all the Contentlets in content types with URL maps that have LIVE Version
 */
@ApplicationScoped
@MetricsProfile(ProfileType.FULL)
public class LiveContentInUrlMapDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "LIVE_CONTENTLETS_IN_CONTENT_TYPES_WITH_URL_MAP";
    }

    @Override
    public String getDescription() {
        return "Count of Live Contentlets in content types with URL maps";
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
        return "SELECT count(DISTINCT contentlet.identifier) AS value FROM contentlet " +
                    "INNER JOIN contentlet_version_info ON contentlet.identifier = contentlet_version_info.identifier " +
                    "INNER JOIN structure ON contentlet.structure_inode = structure.inode " +
                "WHERE url_map_pattern IS NOT null AND page_detail IS NOT null AND deleted = false " +
                "AND live_inode IS NOT null";
    }
}

