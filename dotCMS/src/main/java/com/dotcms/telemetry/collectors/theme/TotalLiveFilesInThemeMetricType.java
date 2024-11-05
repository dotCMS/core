package com.dotcms.telemetry.collectors.theme;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Collects the total of Number of LIVE files in themes
 */
public class TotalLiveFilesInThemeMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "TOTAL_LIVE_FILES_IN_THEMES";
    }

    @Override
    public String getDescription() {
        return "Count of Number of LIVE files in themes";
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
        return "SELECT COUNT(distinct CONCAT(id.parent_path, asset_name)) as value " +
                "FROM contentlet_version_info cvi INNER JOIN identifier id ON cvi.identifier = id.id " +
                "WHERE  cvi.live_inode IS NOT NULL AND deleted = false AND " +
                "id.parent_path LIKE ANY (SELECT CONCAT(id.parent_path, '%') " +
                                          "FROM contentlet_version_info cvi INNER JOIN identifier id ON cvi.identifier = id.id " +
                                          "WHERE id.parent_path LIKE '/application/themes/%' AND id.asset_name = 'template.vtl')";

    }
}
