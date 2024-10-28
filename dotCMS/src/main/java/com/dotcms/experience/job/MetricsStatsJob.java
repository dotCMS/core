package com.dotcms.experience.job;

import com.dotcms.concurrent.lock.ClusterLockManager;
import com.dotcms.experience.MetricsSnapshot;
import com.dotcms.experience.business.MetricsAPI;
import com.dotcms.experience.collectors.MetricStatsCollector;
import com.dotmarketing.util.Logger;

/**
 * Stateful job used to collect and persist a snapshot of the Metrics stats. Frequency set to once a
 * day.
 */
public class MetricsStatsJob {

    public void run(final ClusterLockManager<String> lockManager) {
        try {
            lockManager.tryClusterLock(() -> {
                Logger.debug(this, "MetricsStatsJob is running");
                final MetricsSnapshot metricsSnapshot;

                try {
                    metricsSnapshot = MetricStatsCollector.getStatsAndCleanUp();

                    MetricsAPI.INSTANCE.persistMetricsSnapshot(metricsSnapshot);
                } catch (Throwable e) {
                    Logger.debug(this, "Error occurred during job execution. ", e);
                }
            });
        } catch (Throwable e) {
            Logger.debug(this, "Error trying to acquire the lock. Error: " + e.getMessage(), e);
        }
    }

}
