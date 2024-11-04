package com.dotcms.telemetry.collectors.container;

import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;

/**
 * Total of STANDARD containers used in LIVE pages
 */
public  class TotalStandardContainersInLivePageDatabaseMetricType extends TotalContainersInLivePageDatabaseMetricType {

    @Override
    public String getName() {
        return "COUNT_STANDARD_CONTAINERS_USED_IN_LIVE_PAGES";
    }

    @Override
    public String getDescription() {
        return "Count of STANDARD containers used in LIVE pages";
    }

    @Override
    boolean filterContainer(final String containerId) {
        return !FileAssetContainerUtil.getInstance().isFolderAssetContainerId(containerId);
    }
}

