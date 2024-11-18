package com.dotcms.telemetry.collectors.content;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Collects the count of contentlets which have at least one live version in a non-default Language
 */
public class LiveNotDefaultLanguageContentsDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "LIVE_NOT_DEFAULT_LANGUAGE_COUNT";
    }

    @Override
    public String getDescription() {
        return "Count of Live Content items with non-default Language versions";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.CONTENTLETS;
    }

    @Override
    public String getSqlQuery() {
        return "SELECT COUNT(DISTINCT identifier) AS value " +
                "FROM contentlet_version_info " +
                "WHERE live_inode IS NOT null " +
                    "AND lang NOT IN (SELECT default_language_id FROM company)";
    }
}
