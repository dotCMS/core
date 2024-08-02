package com.dotcms.rest.api.v1.system.monitor;

import com.liferay.util.StringPool;
import java.util.Map;


class MonitorStats {

    final MonitorSubSystemStats subSystemStats = new MonitorSubSystemStats();
    String clusterId = StringPool.BLANK;
    String serverId = StringPool.BLANK;

    boolean isDotCMSHealthy() {
        return isBackendHealthy() && isFrontendHealthy();
    }

    boolean isBackendHealthy() {
        return subSystemStats.isDBHealthy && subSystemStats.isLiveIndexHealthy && subSystemStats.isWorkingIndexHealthy
                &&
                subSystemStats.isCacheHealthy && subSystemStats.isLocalFileSystemHealthy
                && subSystemStats.isAssetFileSystemHealthy;
    }

    boolean isFrontendHealthy() {
        return subSystemStats.isDBHealthy && subSystemStats.isLiveIndexHealthy && subSystemStats.isCacheHealthy &&
                subSystemStats.isLocalFileSystemHealthy && subSystemStats.isAssetFileSystemHealthy;
    }


    Map<String, Object> toMap() {

        final Map<String, Object> subsystems = Map.of(
                "dbSelectHealthy", subSystemStats.isDBHealthy,
                "indexLiveHealthy", subSystemStats.isLiveIndexHealthy,
                "indexWorkingHealthy", subSystemStats.isWorkingIndexHealthy,
                "cacheHealthy", subSystemStats.isCacheHealthy,
                "localFSHealthy", subSystemStats.isLocalFileSystemHealthy,
                "assetFSHealthy", subSystemStats.isAssetFileSystemHealthy);

        return Map.of(
                "serverID", this.serverId,
                "clusterID", this.clusterId,
                "dotCMSHealthy", this.isDotCMSHealthy(),
                "frontendHealthy", this.isFrontendHealthy(),
                "backendHealthy", this.isBackendHealthy(),
                "subsystems", subsystems);




    }

}
