package com.dotcms.telemetry.collectors.content;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Collects the count of Contentlets that has at least one working version on any non-default language and that
 * also don't have live version on any non-default language
 */
public class WorkingNotDefaultLanguageContentsDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "WORKING_NOT_DEFAULT_LANGUAGE_COUNT";
    }

    @Override
    public String getDescription() {
        return "Count of Working Content items with non-default Language versions";
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
                "FROM contentlet_version_info AS cvi1 " +
                "WHERE working_inode is not null and live_inode is null " +
                    "AND lang NOT IN (SELECT default_language_id FROM company)  " +
                    "AND (SELECT COUNT(DISTINCT identifier) " +
                            "FROM contentlet_version_info AS cvi2 " +
                            "WHERE live_inode IS NOT null " +
                                "AND cvi1.identifier = cvi2.identifier " +
                                "AND lang NOT IN (SELECT default_language_id FROM company) ) = 0";
    }
}
