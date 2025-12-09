package com.dotcms.telemetry.job;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.telemetry.MetricsSnapshot;
import com.dotcms.telemetry.collectors.MetricStatsCollector;
import com.dotmarketing.business.APILocator;
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
    public static final String CRON_EXPRESSION_DEFAULT = "0 0 22 * * ?";

    public static final Lazy<Boolean> ENABLED =
            Lazy.of(() -> Config.getBooleanProperty(ENABLED_PROP, true));
    public static final Lazy<String> CRON_EXPRESSION =
            Lazy.of(() -> Config.getStringProperty(CRON_EXPR_PROP, CRON_EXPRESSION_DEFAULT));

    @Override
    public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
        final MetricsSnapshot metricsSnapshot;
        try {
            final MetricStatsCollector collector = CDIUtils.getBeanThrows(MetricStatsCollector.class);
            metricsSnapshot = collector.getStatsAndCleanUp();
            APILocator.getMetricsAPI().persistMetricsSnapshot(metricsSnapshot);
        } catch (final Throwable e) {
            Logger.debug(this, String.format("An error occurred during job execution: %s",
                    ExceptionUtil.getErrorMessage(e)), e);
        }
    }

}
