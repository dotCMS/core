package com.dotcms.telemetry.collectors.content;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Collects the modification date of the most recently edited Contentlet
 */
public class LastContentEditedDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "LAST_CONTENT_EDITED";
    }

    @Override
    public String getDescription() {
        return "Mod date of the most recently edited Contentlet";
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
        return "SELECT to_char (max(version_ts),'HH12:MI:SS DD Mon YYYY') AS value FROM contentlet_version_info";
    }
}
