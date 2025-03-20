package com.dotcms.telemetry.collectors.theme;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Collects the total count of Live containers
 */
public class TotalLiveContainerDatabaseMetricType implements DBMetricType {

    @Override
    public String getName() {
        return "COUNT_OF_LIVE_CONTAINERS";
    }

    @Override
    public String getDescription() {
        return "Total count of LIVE containers";
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
        return "SELECT container.count + file_container.count as value " +
                "FROM (SELECT COUNT(DISTINCT cvi.identifier) FROM container_version_info cvi WHERE deleted = false AND live_inode is not null) container, " +
                      "(SELECT COUNT(DISTINCT cvi.identifier) " +
                        "FROM contentlet_version_info cvi INNER JOIN identifier id ON cvi.identifier = id.id " +
                        "WHERE id.parent_path like '/application/containers%' and " +
                        "id.asset_name = 'container.vtl' and cvi.deleted = false AND live_inode is not null) as file_container";
    }
}

