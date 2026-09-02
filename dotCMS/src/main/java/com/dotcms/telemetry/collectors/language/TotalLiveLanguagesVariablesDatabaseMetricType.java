package com.dotcms.telemetry.collectors.language;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;
import javax.enterprise.context.ApplicationScoped;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;

/**
 * Collects the count of Language Variables that have a live version.
 */
@MetricsProfile(ProfileType.FULL)
@ApplicationScoped
public class TotalLiveLanguagesVariablesDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "LIVE_LANGUAGE_VARIABLE_COUNT";
    }

    @Override
    public String getDescription() {
        return "Count of Live Language Variables";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.LANGUAGES;
    }

    @Override
    public String getSqlQuery() {
        return "SELECT COUNT(distinct contentlet.identifier) AS value FROM contentlet " +
                "INNER JOIN structure ON contentlet.structure_inode=structure.inode " +
                "INNER JOIN contentlet_version_info ON contentlet.identifier = contentlet_version_info.identifier " +
                "WHERE structuretype = " + BaseContentType.KEY_VALUE.getType() + " AND deleted = false AND live_inode IS NOT null";
    }
}
