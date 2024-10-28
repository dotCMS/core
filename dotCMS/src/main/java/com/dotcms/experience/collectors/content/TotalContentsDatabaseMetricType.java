package com.dotcms.experience.collectors.content;

import com.dotcms.experience.MetricCategory;
import com.dotcms.experience.MetricFeature;
import com.dotcms.experience.collectors.DBMetricType;

/**
 * Collects the total number of contentlets
 */
public class TotalContentsDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "COUNT";
    }

    @Override
    public String getDescription() {
        return "Total number of Contentlets";
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
        return "SELECT COUNT(working_inode) AS value FROM contentlet_version_info";
    }
}
