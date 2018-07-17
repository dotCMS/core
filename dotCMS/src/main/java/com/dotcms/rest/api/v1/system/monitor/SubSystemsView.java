package com.dotcms.rest.api.v1.system.monitor;

public class SubSystemsView {

    private final  boolean dbSelectHealthy;
    private final  boolean indexLiveHealthy;
    private final  boolean indexWorkingHealthy;
    private final  boolean cacheHealthy;
    private final  boolean localFSHealthy;
    private final  boolean assetFSHealthy;

    public SubSystemsView(boolean dbSelectHealthy, boolean indexLiveHealthy, boolean indexWorkingHealthy, boolean cacheHealthy, boolean localFSHealthy, boolean assetFSHealthy) {
        this.dbSelectHealthy = dbSelectHealthy;
        this.indexLiveHealthy = indexLiveHealthy;
        this.indexWorkingHealthy = indexWorkingHealthy;
        this.cacheHealthy = cacheHealthy;
        this.localFSHealthy = localFSHealthy;
        this.assetFSHealthy = assetFSHealthy;
    }

    public boolean isDbSelectHealthy() {
        return dbSelectHealthy;
    }

    public boolean isIndexLiveHealthy() {
        return indexLiveHealthy;
    }

    public boolean isIndexWorkingHealthy() {
        return indexWorkingHealthy;
    }

    public boolean isCacheHealthy() {
        return cacheHealthy;
    }

    public boolean isLocalFSHealthy() {
        return localFSHealthy;
    }

    public boolean isAssetFSHealthy() {
        return assetFSHealthy;
    }
}
