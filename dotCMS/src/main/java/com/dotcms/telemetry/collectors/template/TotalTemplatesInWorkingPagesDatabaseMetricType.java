package com.dotcms.telemetry.collectors.template;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;
import javax.enterprise.context.ApplicationScoped;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;

/**
 * Collects the total all templates used in Working pages
 */
@MetricsProfile(ProfileType.FULL)
@ApplicationScoped
public class TotalTemplatesInWorkingPagesDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "COUNT_OF_TEMPLATES_USED_IN_WORKING_PAGES";
    }

    @Override
    public String getDescription() {
        return "Count of all templates used in WORKING pages";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.LAYOUT;
    }

    @Override
    public String getSqlQuery() {
        return "SELECT count(distinct contentlet.contentlet_as_json->'fields'->'template'->'value') as value " +
                "FROM contentlet INNER JOIN contentlet_version_info ON contentlet_version_info.identifier = contentlet.identifier " +
                "WHERE contentlet_version_info.live_inode is null AND " +
                "deleted = false AND structure_inode in (SELECT inode FROM structure WHERE name = 'Page')";
    }
}
