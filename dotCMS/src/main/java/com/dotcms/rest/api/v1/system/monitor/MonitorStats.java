package com.dotcms.rest.api.v1.system.monitor;

import com.liferay.util.StringPool;

class MonitorStats {
    String clusterId = StringPool.BLANK;
    String serverId = StringPool.BLANK;
    final MonitorSubSystemStats subSystemStats = new MonitorSubSystemStats();

    boolean isDotCMSHealthy() {
        return isBackendHealthy() && isFrontendHealthy();
    }

    boolean isBackendHealthy() {
        return subSystemStats.isDBHealthy && subSystemStats.isLiveIndexHealthy && subSystemStats.isWorkingIndexHealthy &&
                subSystemStats.isCacheHealthy && subSystemStats.isLocalFileSystemHealthy && subSystemStats.isAssetFileSystemHealthy;
    }

    boolean isFrontendHealthy() {
        return subSystemStats.isDBHealthy && subSystemStats.isLiveIndexHealthy && subSystemStats.isCacheHealthy &&
                subSystemStats.isLocalFileSystemHealthy && subSystemStats.isAssetFileSystemHealthy;
    }
}


