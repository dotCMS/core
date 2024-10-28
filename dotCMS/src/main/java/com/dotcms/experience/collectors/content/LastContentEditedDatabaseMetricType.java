package com.dotcms.experience.collectors.content;

import com.dotcms.experience.MetricCategory;
import com.dotcms.experience.MetricFeature;
import com.dotcms.experience.collectors.DBMetricType;

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
