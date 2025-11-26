package com.dotcms.telemetry.collectors.container;

import com.dotcms.telemetry.business.MetricsAPI;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;

import javax.inject.Inject;

/**
 * Total of FILE containers used in LIVE pages
 */
public class TotalFileContainersInWorkingPageDatabaseMetricType extends TotalContainersInWorkingPageDatabaseMetricType {

    @Inject
    public TotalFileContainersInWorkingPageDatabaseMetricType(final MetricsAPI metricsAPI) {
        super.metricsAPI = metricsAPI;
    }

    @Override
    public String getName() {
        return "COUNT_FILE_CONTAINERS_USED_IN_WORKING_PAGES";
    }

    @Override
    public String getDescription() {
        return "Count of FILE containers used in WORKING pages";
    }

    @Override
    boolean filterContainer(final String containerId) {
        return FileAssetContainerUtil.getInstance().isFolderAssetContainerId(containerId);
    }

}
