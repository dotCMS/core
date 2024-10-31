package com.dotcms.telemetry.collectors.container;

import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;

/**
 * Total of FILE containers used in LIVE templates
 */
public  class TotalFileContainersInLiveTemplatesDatabaseMetricType extends TotalContainersInLiveTemplatesDatabaseMetricType {

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

