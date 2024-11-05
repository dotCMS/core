package com.dotcms.telemetry.collectors.theme;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Collects the total count of themes used by LIVE templates
 */
public class TotalThemeUsedInLiveTemplatesMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "TOTAL_USED_THEMES_IN_LIVE_TEMPLATES";
    }

    @Override
    public String getDescription() {
        return "Count of themes used by templates";
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
        return "SELECT  count(distinct folder.identifier) value FROM folder " +
                "WHERE inode in ( " +
                    "SELECT DISTINCT theme FROM template INNER JOIN template_version_info ON template_version_info.live_inode = template.inode " +
                    "WHERE title NOT LIKE 'anonymous_layout_%' AND deleted = false)";
    }
}
