package com.dotcms.telemetry.collectors.container;

import com.dotcms.telemetry.business.MetricsAPI;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;

/**
 * Total of STANDARD containers used in LIVE pages
 */
@MetricsProfile(ProfileType.FULL)
@ApplicationScoped
public class TotalStandardContainersInLivePageDatabaseMetricType extends TotalContainersInLivePageDatabaseMetricType {

    @Inject
    public TotalStandardContainersInLivePageDatabaseMetricType(final MetricsAPI metricsAPI) {
        super.metricsAPI = metricsAPI;
    }

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
