package com.dotcms.telemetry.collectors.container;

import com.dotcms.telemetry.business.MetricsAPI;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;

import javax.inject.Inject;
import javax.enterprise.context.ApplicationScoped;

/**
 * Total of FILE containers used in LIVE pages
 */
@ApplicationScoped
public class TotalFileContainersInLivePageDatabaseMetricType extends TotalContainersInLivePageDatabaseMetricType {

    @Inject
    public TotalFileContainersInLivePageDatabaseMetricType(final MetricsAPI metricsAPI) {
        super.metricsAPI = metricsAPI;
    }

    @Override
    public String getName() {
        return "COUNT_FILE_CONTAINERS_USED_IN_LIVE_PAGES";
    }

    @Override
    public String getDescription() {
        return "Count of FILE containers used in LIVE pages";
    }

    @Override
    boolean filterContainer(final String containerId) {
        return FileAssetContainerUtil.getInstance().isFolderAssetContainerId(containerId);
    }

}
