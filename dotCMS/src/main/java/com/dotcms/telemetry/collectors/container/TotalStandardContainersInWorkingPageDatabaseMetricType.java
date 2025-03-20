package com.dotcms.telemetry.collectors.container;

import com.dotcms.telemetry.business.MetricsAPI;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;

import javax.inject.Inject;

/**
 * Total of STANDARD containers used in WORKING pages
 */
public class TotalStandardContainersInWorkingPageDatabaseMetricType extends TotalContainersInWorkingPageDatabaseMetricType {

    @Inject
    public TotalStandardContainersInWorkingPageDatabaseMetricType(final MetricsAPI metricsAPI) {
        super.metricsAPI = metricsAPI;
    }

    @Override
    public String getName() {
        return "COUNT_STANDARD_CONTAINERS_USED_IN_WORKING_PAGES";
    }

    @Override
    public String getDescription() {
        return "Count of STANDARD containers used in WORKING pages";
    }

    @Override
    boolean filterContainer(final String containerId) {
        return !FileAssetContainerUtil.getInstance().isFolderAssetContainerId(containerId);
    }

}
