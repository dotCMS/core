package com.dotcms.rest.api.v1.system.monitor;

import com.dotcms.http.CircuitBreakerUrlBuilder;

import java.util.Map;

/**
 * This class is used to report on the status of the various subsystems used by dotCMS.
 *
 * @author Brent Griffin
 * @since Jul 18th, 2018
 */
public class MonitorStats {

    final boolean assetFSHealthy;
    final boolean cacheHealthy;
    final boolean dBHealthy;
    final boolean esHealthy;
    final boolean localFSHealthy;
    final String contentAnalytics;
    final boolean telemetry;

    public MonitorStats(boolean assetFSHealthy,
            boolean cacheHealthy,
            boolean dBHealthy,
            boolean esHealthy,
            boolean localFSHealthy,
            String contentAnalytics,
            boolean telemetry) {

        this.assetFSHealthy = assetFSHealthy;
        this.cacheHealthy = cacheHealthy;
        this.dBHealthy = dBHealthy;
        this.esHealthy = esHealthy;
        this.localFSHealthy = localFSHealthy;
        this.contentAnalytics = contentAnalytics;
        this.telemetry = telemetry;
    }

    /**
     * This method checks if the dotCMS instance is healthy. It does this by checking if the backend
     * and frontend are healthy.
     *
     * @return If the dotCMS instance is healthy, returns {@code true}.
     */
    boolean isDotCMSHealthy() {
        return isBackendHealthy() && isFrontendHealthy();
    }

    /**
     * This method checks if the backend is healthy. It does this by checking if the database,
     * elasticsearch, cache, local file system, and asset file system are healthy.
     *
     * @return If the backend is healthy, returns {@code true}.
     */
    boolean isBackendHealthy() {
        return this.dBHealthy && this.esHealthy && this.cacheHealthy && this.localFSHealthy
                && this.assetFSHealthy;
    }

    /**
     * Return the content analytics status: see {@link com.dotcms.experiments.business.ExperimentsAPI.Health}
     * @return String
     */
    public String getContentAnalytics() {
        return contentAnalytics;
    }

    /**
     * This method checks if the frontend is healthy. It does this by checking if the database,
     * elasticsearch, cache, local file system, and asset file system are healthy.
     *
     * @return If the frontend is healthy, returns {@code true}.
     */
    boolean isFrontendHealthy() {
        return this.dBHealthy && this.esHealthy && this.cacheHealthy &&
                this.localFSHealthy && this.assetFSHealthy;
    }

    /**
     * This method converts the monitor stats to a map.
     *
     * @return A map containing the monitor stats.
     */
    Map<String, Object> toMap() {
        final Map<String, Object> subsystems = Map.of(
                "dbSelectHealthy", this.dBHealthy,
                "esHealthy", this.esHealthy,
                "cacheHealthy", this.cacheHealthy,
                "localFSHealthy", this.localFSHealthy,
                "assetFSHealthy", this.assetFSHealthy,
                "contentAnalytics", this.contentAnalytics,
                "telemetry", this.telemetry);

        return Map.of(
                "dotCMSHealthy", this.isDotCMSHealthy(),
                "frontendHealthy", this.isFrontendHealthy(),
                "backendHealthy", this.isBackendHealthy(),
                "subsystems", subsystems);
    }

    /**
     * This class is used to build an instance of {@link MonitorStats}.
     */
    public static final class Builder {

        private boolean assetFSHealthy;
        private boolean cacheHealthy;
        private boolean dBHealthy;
        private boolean esHealthy;
        private boolean localFSHealthy;
        private String contentAnalytics;
        private boolean telemetry;

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

        public Builder contentAnalytics(String contentAnalytics) {
            this.contentAnalytics = contentAnalytics;
            return this;
        }

        public Builder telemetry(boolean telemetry) {
            this.telemetry = telemetry;
            return this;
        }

        public MonitorStats build() {
            return new MonitorStats(
                    assetFSHealthy,
                    cacheHealthy,
                    dBHealthy,
                    esHealthy,
                    localFSHealthy,
                    contentAnalytics,
                    telemetry);
        }


    }

}
