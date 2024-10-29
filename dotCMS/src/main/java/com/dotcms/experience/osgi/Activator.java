package com.dotcms.experience.osgi;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.lock.ClusterLockManager;
import com.dotcms.experience.TelemetryResource;
import com.dotcms.experience.collectors.api.ApiMetricAPI;
import com.dotcms.experience.collectors.api.ApiMetricFactorySubmitter;
import com.dotcms.experience.collectors.api.ApiMetricWebInterceptor;
import com.dotcms.experience.job.MetricsStatsJob;
import com.dotcms.filters.interceptor.FilterWebInterceptorProvider;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.filters.interceptor.WebInterceptorDelegate;
import com.dotcms.rest.config.RestServiceUtil;
import com.dotmarketing.filters.InterceptorFilter;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;
import org.apache.logging.log4j.core.util.CronExpression;
import org.osgi.framework.BundleContext;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Activator extends GenericBundleActivator {
    public static String version;

    private final WebInterceptorDelegate delegate = FilterWebInterceptorProvider
                    .getInstance(Config.CONTEXT)
                    .getDelegate(InterceptorFilter.class);

    private final WebInterceptor apiCallWebInterceptor = new ApiMetricWebInterceptor();
    private final MetricsStatsJob metricsStatsJob = new MetricsStatsJob();

    private static final String METRICS_JOB_LOCK_KEY = "metrics_job_lock";
    private ScheduledFuture<?> scheduledFuture;
    private final Lazy<Boolean> enableTelemetry = Lazy.of(() ->
            Config.getBooleanProperty("FEATURE_FLAG_TELEMETRY", false));

    private final Lazy<Boolean> enableAPIMetrics = Lazy.of(() ->
            Config.getBooleanProperty("TELEMETRY_API_METRICS_ENABLED", false));

    public static final ApiMetricAPI apiStatAPI = new ApiMetricAPI();

    @Override
    public void start(final BundleContext context) {

        PluginVersionUtil.init(context);

        if(Boolean.TRUE.equals(enableTelemetry.get())) {
            Logger.debug(Activator.class.getName(), "Starting the Telemetry plugin");

            RestServiceUtil.addResource(TelemetryResource.class);

            try {
                apiStatAPI.dropTemporaryTable();
                apiStatAPI.createTemporaryTable();

                if(Boolean.TRUE.equals(enableAPIMetrics.get())) {
                    Logger.debug(Activator.class.getName(), "API metrics enabled");
                    delegate.addFirst(apiCallWebInterceptor);
                    ApiMetricFactorySubmitter.INSTANCE.start();
                }
                Logger.debug(Activator.class.getName(), "Scheduling Telemetry Job");
                scheduleMetricsJob();
                Logger.debug(Activator.class.getName(), "The Telemetry plugin was started");
            } catch (Throwable t) {
                Logger.debug(this, "Error starting the Telemetry plugin.", t);
            }
        }
    }

    private void scheduleMetricsJob() throws ParseException {
        final ClusterLockManager<String> lockManager = DotConcurrentFactory.getInstance()
                .getClusterLockManager(METRICS_JOB_LOCK_KEY);

        CronExpression cron = new CronExpression(Config
                .getStringProperty("TELEMETRY_SAVE_SCHEDULE", "0 0 22 * * ?")) ;

        final Instant now = Instant.now();
        final Instant previousRun = cron.getPrevFireTime(Date.from(now)).toInstant();
        final Instant nextRun = cron.getNextValidTimeAfter(Date.from(previousRun)).toInstant();
        final Duration delay = Duration.between(now, nextRun);
        final Duration runEvery = Duration.between(previousRun, nextRun);

        scheduledFuture = DotConcurrentFactory.getScheduledThreadPoolExecutor().scheduleAtFixedRate(
                () -> metricsStatsJob.run(lockManager)
                , delay.get(ChronoUnit.SECONDS),
                runEvery.get(ChronoUnit.SECONDS),
                TimeUnit.SECONDS);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if(Boolean.TRUE.equals(enableTelemetry.get())) {
            RestServiceUtil.removeResource(TelemetryResource.class);
            scheduledFuture.cancel(false);
            apiStatAPI.dropTemporaryTable();

            if(Boolean.TRUE.equals(enableAPIMetrics.get())) {
                delegate.remove(apiCallWebInterceptor.getName(), true);
                ApiMetricFactorySubmitter.INSTANCE.shutdownNow();
            }
        }
    }
}
