package com.dotcms.experience.collectors.theme;

import com.dotcms.experience.MetricCategory;
import com.dotcms.experience.MetricFeature;
import com.dotcms.experience.collectors.DBMetricType;

/**
 * Collects the total of themes
 */
public class TotalThemeMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "TOTAL_THEMES";
    }

    @Override
    public String getDescription() {
        return "Count of themes";
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
        return "SELECT COUNT(id.parent_path) as value " +
                "FROM contentlet_version_info cvi INNER JOIN identifier id ON cvi.identifier = id.id " +
                "WHERE id.parent_path LIKE '/application/themes/%' AND id.asset_name = 'template.vtl' " +
                "AND deleted = false";
    }
}
