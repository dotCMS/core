package com.dotcms.rest.api.v1.system.monitor;

import java.io.Serializable;

/**
 * Encapsulates the monitor stats
 * @author jsanca
 */
public class MonitorResultView implements Serializable {

    private final  boolean dotCMSHealthy;
    private final  boolean frontendHealthy;
    private final  boolean backendHealthy;
    private final SubSystemsView subSystemView;
    private final  String  serverID;
    private final  String  clusterID;


    private MonitorResultView(final Builder builder) {
        this.dotCMSHealthy = builder.dotCMSHealthy;
        this.frontendHealthy = builder.frontendHealthy;
        this.backendHealthy = builder.backendHealthy;
        this.subSystemView = new SubSystemsView(builder.dbSelectHealthy,
                builder.indexLiveHealthy,
                builder.indexWorkingHealthy,
                builder.cacheHealthy,
                builder.localFSHealthy,
                builder.assetFSHealthy);
        this.serverID = builder.serverID;
        this.clusterID = builder.clusterID;
    }



    public boolean isDotCMSHealthy() {
        return dotCMSHealthy;
    }

    public boolean isFrontendHealthy() {
        return frontendHealthy;
    }

    public boolean isBackendHealthy() {
        return backendHealthy;
    }

    public SubSystemsView getSubSystemView() {
        return subSystemView;
    }

    public String getServerID() {
        return serverID;
    }

    public String getClusterID() {
        return clusterID;
    }

    public static final class Builder {

        private boolean dotCMSHealthy;
        private boolean frontendHealthy;
        private boolean backendHealthy;
        private boolean dbSelectHealthy;
        private boolean indexLiveHealthy;
        private boolean indexWorkingHealthy;
        private boolean cacheHealthy;
        private boolean localFSHealthy;
        private boolean assetFSHealthy;
        private String  serverID;
        private String  clusterID;

        public Builder serverID(String serverID) {
            this.serverID = serverID;
            return this;
        }

        public Builder clusterID(String clusterID) {
            this.clusterID = clusterID;
            return this;
        }


        public Builder dotCMSHealthy(boolean dotCMSHealthy) {
            this.dotCMSHealthy = dotCMSHealthy;
            return this;
        }

        public Builder frontendHealthy(boolean frontendHealthy) {
            this.frontendHealthy = frontendHealthy;
            return this;
        }

        public Builder backendHealthy(boolean backendHealthy) {
            this.backendHealthy = backendHealthy;
            return this;
        }

        public Builder dbSelectHealthy(boolean dbSelectHealthy) {
            this.dbSelectHealthy = dbSelectHealthy;
            return this;
        }

        public Builder indexLiveHealthy(boolean indexLiveHealthy) {
            this.indexLiveHealthy = indexLiveHealthy;
            return this;
        }

        public Builder indexWorkingHealthy(boolean indexWorkingHealthy) {
            this.indexWorkingHealthy = indexWorkingHealthy;
            return this;
        }

        public Builder cacheHealthy(boolean cacheHealthy) {
            this.cacheHealthy = cacheHealthy;
            return this;
        }

        public Builder localFSHealthy(boolean localFSHealthy) {
            this.localFSHealthy = localFSHealthy;
            return this;
        }

        public Builder assetFSHealthy(boolean assetFSHealthy) {
            this.assetFSHealthy = assetFSHealthy;
            return this;
        }

        public MonitorResultView build() {
            return new MonitorResultView(this);
        }
    }

}
