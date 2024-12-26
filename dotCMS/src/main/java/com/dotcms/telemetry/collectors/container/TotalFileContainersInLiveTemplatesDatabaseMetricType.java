package com.dotcms.telemetry.collectors.container;

import com.dotcms.telemetry.business.MetricsAPI;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Total of FILE containers used in LIVE templates
 */
@ApplicationScoped
public class TotalFileContainersInLiveTemplatesDatabaseMetricType extends TotalContainersInLiveTemplatesDatabaseMetricType {

    @Inject
    public TotalFileContainersInLiveTemplatesDatabaseMetricType(final MetricsAPI metricsAPI) {
        super.metricsAPI = metricsAPI;
    }

    @Override
    public String getName() {
        return "COUNT_FILE_CONTAINERS_USED_IN_LIVE_TEMPLATES";
    }

    @Override
    public String getDescription() {
        return "Total of FILE containers used in LIVE templates";
    }

    @Override
    boolean filterContainer(final String containerId) {
        return FileAssetContainerUtil.getInstance().isFolderAssetContainerId(containerId);
    }

}

