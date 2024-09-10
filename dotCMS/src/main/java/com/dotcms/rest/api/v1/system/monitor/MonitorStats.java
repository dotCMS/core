package com.dotcms.rest.api.v1.system.monitor;

import java.util.Map;


class MonitorStats {


    final boolean assetFSHealthy, cacheHealthy, dBHealthy,  esHealthy, localFSHealthy;


    public MonitorStats(boolean assetFSHealthy,
            boolean cacheHealthy,
            boolean dBHealthy,
            boolean esHealthy,
            boolean localFSHealthy) {
        this.assetFSHealthy = assetFSHealthy;
        this.cacheHealthy = cacheHealthy;
        this.dBHealthy = dBHealthy;
        this.esHealthy = esHealthy;
        this.localFSHealthy = localFSHealthy;

    }


    boolean isDotCMSHealthy() {
        return isBackendHealthy() && isFrontendHealthy();
    }

    boolean isBackendHealthy() {
        return this.dBHealthy && this.esHealthy && this.cacheHealthy && this.localFSHealthy
                && this.assetFSHealthy;
    }

    boolean isFrontendHealthy() {
        return this.dBHealthy && this.esHealthy && this.cacheHealthy &&
                this.localFSHealthy && this.assetFSHealthy;
    }


    Map<String, Object> toMap() {

        final Map<String, Object> subsystems = Map.of(
                "dbSelectHealthy", this.dBHealthy,
                "esHealthy", this.esHealthy,
                "cacheHealthy", this.cacheHealthy,
                "localFSHealthy", this.localFSHealthy,
                "assetFSHealthy", this.assetFSHealthy);

        return Map.of(
                "dotCMSHealthy", this.isDotCMSHealthy(),
                "frontendHealthy", this.isFrontendHealthy(),
                "backendHealthy", this.isBackendHealthy(),
                "subsystems", subsystems);

    }


    public static final class Builder {

        private boolean assetFSHealthy;
        private boolean cacheHealthy;
        private boolean dBHealthy;
        private boolean esHealthy;
        private boolean localFSHealthy;


        public Builder() {
        }

        public Builder assetFSHealthy(boolean assetFSHealthy) {
            this.assetFSHealthy = assetFSHealthy;
            return this;
        }

        public Builder cacheHealthy(boolean cacheHealthy) {
            this.cacheHealthy = cacheHealthy;
            return this;
        }

        public Builder dBHealthy(boolean dBHealthy) {
            this.dBHealthy = dBHealthy;
            return this;
        }


        public Builder localFSHealthy(boolean localFSHealthy) {
            this.localFSHealthy = localFSHealthy;
            return this;
        }

        public Builder esHealthy(boolean esHealthy) {
            this.esHealthy = esHealthy;
            return this;
        }

        public MonitorStats build() {
            return new MonitorStats(
                    assetFSHealthy,
                    cacheHealthy,
                    dBHealthy,
                    esHealthy,
                    localFSHealthy);
        }
    }


}
