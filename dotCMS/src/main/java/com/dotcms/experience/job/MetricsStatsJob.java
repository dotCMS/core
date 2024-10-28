package com.dotcms.experience.job;

import com.dotcms.concurrent.lock.ClusterLockManager;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.experience.MetricsSnapshot;
import com.dotcms.experience.business.MetricsAPI;
import com.dotcms.experience.collectors.MetricStatsCollector;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

/**
 * Stateful job used to collect and persist a snapshot of the Metrics stats. Frequency set to once a
 * day.
 */
public class MetricsStatsJob implements StatefulJob {

    public static final String JOB_NAME = "MetricsStatsJob";
    public static final String JOB_GROUP = "MetricsStatsJobGroup";
    public static final String ENABLED_PROP = "TELEMETRY_SAVE_SCHEDULE_JOB_ENABLED";
    public static final String CRON_EXPR_PROP = "TELEMETRY_SAVE_SCHEDULE";
    private static final String CRON_EXPRESSION_DEFAULT = "0 0 22 * * ?";

    public static final Lazy<Boolean> ENABLED =
            Lazy.of(() -> Config.getBooleanProperty(ENABLED_PROP, true));
    public static final Lazy<String> CRON_EXPRESSION =
            Lazy.of(() -> Config.getStringProperty(CRON_EXPR_PROP, CRON_EXPRESSION_DEFAULT));

    @Override
    public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Logger.info(this.getClass(), "--------------------------------------");
        Logger.info(this.getClass(), "MetricsStatsJob has started");
        final MetricsSnapshot metricsSnapshot;
        try {
            metricsSnapshot = MetricStatsCollector.getStatsAndCleanUp();
            MetricsAPI.INSTANCE.persistMetricsSnapshot(metricsSnapshot);
            Logger.info(this.getClass(), "MetricsStatsJob has finished!");
        } catch (final Throwable e) {
            Logger.debug(this, String.format("Error occurred during job execution: %s",
                    ExceptionUtil.getErrorMessage(e)), e);
        }
    }

}
