package com.dotmarketing.quartz.job;

import com.dotcms.analytics.AccessTokenRenewRunnable;
import com.dotcms.analytics.AnalyticsAPI;
import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.AnalyticsAppWithStatus;
import com.dotcms.analytics.model.TokenStatus;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.init.DotInitScheduler;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.StatefulJob;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * This job is in charge of maintaining fresh access token so dotCMS can consume infrastructure services right away
 * without the need of requesting a new.
 * Job is supposed to run every minute to verify the stored access tokens that are about to expire to be renewed with
 * new ones.
 * This is the current criteria to renew:
 * <pre>
 *   - Is Auth in an unrecoverable error state, e.g. invalid secure secret, missing details
 *     - Return exception, need to direct to configuration page to update security.
 *   - Is token in 1 min (configurable) window of expiry and update request thread (should not need a dedicated thread for this just a background job in executor but should not run in parallel) not already running
 *     - Yes
 *       - Spawn thread to update token
 *       - On success
 *         - Update cached token
 *       - On failure
 *         - If temporary error, e.g. network connection error, recoverable retry max 2 (delay ?) ?
 *         - If a permanent error, e.g. secret is wrong report error notification exception, set status, do not keep retrying block further requests.
 *       - Return old token immediately.
 *     - Not Expired
 *       - Spawn thread to update token if thread not already running
 *       - Block for new token ( timeout auth exception, timeout must take into account any retry and delay, and preferably not fully block entire system, maybe add spinner until this timeout )
 *     - No
 *       - Return existing token from cache no block.
 * </pre>
 *
 * @author vico
 */
public class AccessTokenRenewJob implements StatefulJob {

    public static final String ANALYTICS_ACCESS_TOKEN_RENEW_JOB_CRON_KEY = "analytics.accesstoken.renewjob.cron";
    public static final String ANALYTICS_ACCESS_TOKEN_RENEW_JOB_CRON_DEFAULT = "0 0/1 * * * ?";
    public static final String ANALYTICS_ACCESS_TOKEN_RENEW_JOB = "AnalyticsAccessTokenRenewJob";
    public static final String ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER = "trigger31";
    public static final String ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER_GROUP = "group31";

    private final AnalyticsAPI analyticsAPI;
    private final HostAPI hostAPI;
    private final DotSubmitter submitter;
    private boolean renewRunning;

    public AccessTokenRenewJob() {
        analyticsAPI = APILocator.getAnalyticsAPI();
        hostAPI = APILocator.getHostAPI();
        final DotConcurrentFactory.SubmitterConfig config = new DotConcurrentFactory.SubmitterConfigBuilder()
            .poolSize(1)
            .maxPoolSize(1)
            .keepAliveMillis(1000)
            .queueCapacity(1)
            .rejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy())
            .build();
        submitter = DotConcurrentFactory.getInstance().getSubmitter(
            AnalyticsAPI.ANALYTICS_ACCESS_TOKEN_THREAD_NAME,
            config);
        renewRunning = false;
    }

    public boolean isRenewRunning() {
        return renewRunning;
    }

    public void setRenewRunning(boolean renewRunning) {
        this.renewRunning = renewRunning;
    }

    /**
     * Executes logic renew and maintain fresh tokens to be used when consuming analytics infrastructure.
     *
     * @param jobExecutionContext job execution context
     * @throws JobExecutionException when execution fails
     */
    @Override
    public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
        final Set<AnalyticsAppWithStatus> apps = Try.of(
            () -> hostAPI.findAll(APILocator.systemUser(), 0, 0, null, false)
                .stream()
                .filter(Objects::nonNull)
                .map(host -> Try.of(() -> AnalyticsHelper.get().appFromHost(host)).getOrElse((AnalyticsApp) null))
                .filter(Objects::nonNull)
                .filter(AnalyticsApp::isConfigValid)
                .map(this::withStatus)
                .peek(this::logAccessTokenStatus)
                .filter(this::needsRenew)
                .collect(Collectors.toSet()))
            .getOrElseGet(e -> {
                Logger.error(this, "Error renewing access tokens", e);
                return Collections.emptySet();
            });

        if (apps.isEmpty()) {
            return;
        }

        renewTokens(apps);
    }

    /**
     * Add a resolved instance of {@link TokenStatus} to provided {@link AnalyticsApp} based on its status
     * nd issueDate to determine expiration.
     *
     * @param analyticsApp analytics app
     * @return tuple with two values
     */
    private AnalyticsAppWithStatus withStatus(final AnalyticsApp analyticsApp) {
        final AccessToken accessToken = analyticsAPI.getCachedAccessToken(analyticsApp);
        final TokenStatus tokenStatus = AnalyticsHelper.get().resolveTokenStatus(accessToken);
        return new AnalyticsAppWithStatus(analyticsApp, tokenStatus);
    }

    /**
     * Log an appropriate message based on what {@link TokenStatus} is.
     *
     * @param analyticsAppWithStatus analytics app and token status instance
     */
    private void logAccessTokenStatus(final AnalyticsAppWithStatus analyticsAppWithStatus) {
        final AnalyticsApp analyticsApp = analyticsAppWithStatus.getAnalyticsApp();
        final TokenStatus tokenStatus = analyticsAppWithStatus.getTokenStatus();

        final String message;
        switch (tokenStatus) {
            case NONE:
                message = String.format(
                    "ACCESS_TOKEN for clientId %s is null or has no status, interpreting this as it needs to renew",
                    analyticsApp.getAnalyticsProperties().clientId());
                break;
            case NOOP:
                message = String.format(
                    "ACCESS_TOKEN for clientId %s is NOOP it cannot be used due to a permanent error",
                    analyticsApp.getAnalyticsProperties().clientId());
                break;
            case BLOCKED:
                message = String.format(
                    "ACCESS_TOKEN for clientId %s is BLOCKED due to renew process",
                    analyticsApp.getAnalyticsProperties().clientId());
                break;
            case EXPIRED:
            case IN_WINDOW:
                message = String.format(
                    "ACCESS_TOKEN for clientId %s needs to be renewed",
                    analyticsApp.getAnalyticsProperties().clientId());
                break;
            default:
                message = null;
        }

        Optional.ofNullable(message).ifPresent(msg -> Logger.debug(this, message));
    }

    /**
     * Evaluates if the {@link AccessToken} associated to the provided {@link AnalyticsApp} is subject to renew.
     * That is if token is present and if it's expired or in window.
     *
     * @param appWithStatus provided analytics app with status tuple
     * @return true when it's expired or in window
     */
    private boolean needsRenew(final AnalyticsAppWithStatus appWithStatus) {
        final TokenStatus tokenStatus = appWithStatus.getTokenStatus();

        if (tokenStatus == TokenStatus.OK || tokenStatus == TokenStatus.NOOP || tokenStatus == TokenStatus.BLOCKED) {
            return false;
        }

        return tokenStatus == TokenStatus.EXPIRED
            || tokenStatus == TokenStatus.IN_WINDOW
            || tokenStatus == TokenStatus.NONE;
    }

    /**
     * For a filtered provided list of {@link AnalyticsApp}s if the token renew thread is not running then try to
     * renew the token associated to the provided applications.
     *
     * @param analyticsApps analytics applications
     */
    private void renewTokens(final Set<AnalyticsAppWithStatus> analyticsApps) {
        submitter.execute(new AccessTokenRenewRunnable(this, analyticsApps));
    }

    /**
     * Job scheduler class through {@link DotInitScheduler}.
     * Rather to have this logic here than in DotInitScheduler.
     *
     * @author vico
     */
    public static class AccessTokensRenewJobScheduler {

        private AccessTokensRenewJobScheduler() {}

        /**
         * Emulates what {@link DotInitScheduler} class does when scheduling jobs.
         *
         * @throws SchedulerException when scheduling goes wrong
         */
        public static void schedule() throws SchedulerException {
            final String tokenRenewCron = Config.getStringProperty(
                ANALYTICS_ACCESS_TOKEN_RENEW_JOB_CRON_KEY,
                ANALYTICS_ACCESS_TOKEN_RENEW_JOB_CRON_DEFAULT);
            final Scheduler scheduler = QuartzUtils.getScheduler();
            JobDetail job;

            if (StringUtils.isNotBlank(tokenRenewCron)) {
                boolean isNew = false;

                try {
                    try {
                        job = getJob(scheduler);
                        if (Objects.isNull(job)) {
                            job = createJob();
                            isNew = true;
                        }
                    } catch (SchedulerException se) {
                        scheduler.deleteJob(ANALYTICS_ACCESS_TOKEN_RENEW_JOB, DotInitScheduler.DOTCMS_JOB_GROUP_NAME);
                        job = createJob();
                        isNew = true;
                    }

                    final CronTrigger trigger = new CronTrigger(
                        ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER,
                        ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER_GROUP,
                        ANALYTICS_ACCESS_TOKEN_RENEW_JOB,
                        DotInitScheduler.DOTCMS_JOB_GROUP_NAME,
                        Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()),
                        null,
                        tokenRenewCron);
                    trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
                    scheduler.addJob(job, true);

                    final Date jobDate = isNew
                            ? scheduler.scheduleJob(trigger)
                            : scheduler.rescheduleJob(
                                ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER,
                                ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER_GROUP,
                                trigger);
                    Logger.info(
                        AccessTokensRenewJobScheduler.class,
                        String.format(
                            "Stateful %s (%s:%s) job was schedule to run at %s",
                            ANALYTICS_ACCESS_TOKEN_RENEW_JOB,
                            ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER,
                            ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER_GROUP,
                            Objects.requireNonNullElse(jobDate, "unknown")));
                } catch (Exception e) {
                    Logger.error(DotInitScheduler.class, e.getMessage(), e);
                }
            } else {
                Logger.info(
                    DotInitScheduler.class,
                    String.format("%s Cron Job schedule disabled on this server", ANALYTICS_ACCESS_TOKEN_RENEW_JOB));
                Logger.info(
                    AccessTokensRenewJobScheduler.class,
                    String.format("Deleting %s Job", ANALYTICS_ACCESS_TOKEN_RENEW_JOB));

                job = getJob(scheduler);
                if (Objects.nonNull(job)) {
                    scheduler.deleteJob(ANALYTICS_ACCESS_TOKEN_RENEW_JOB, DotInitScheduler.DOTCMS_JOB_GROUP_NAME);
                }
            }
        }

        /**
         * Retrieves {@link JobDetail} instance from provided {@link Scheduler}/
         *
         * @param scheduler provider scheduler
         * @return scheduled job detail
         * @throws SchedulerException when scheduling fails
         */
        private static JobDetail getJob(final Scheduler scheduler) throws SchedulerException {
            return scheduler.getJobDetail(ANALYTICS_ACCESS_TOKEN_RENEW_JOB, DotInitScheduler.DOTCMS_JOB_GROUP_NAME);
        }

        /**
         * @return a {@link JobDetail} instance configured for this job
         */
        private static JobDetail createJob() {
            return new JobDetail(
                ANALYTICS_ACCESS_TOKEN_RENEW_JOB,
                DotInitScheduler.DOTCMS_JOB_GROUP_NAME,
                AccessTokenRenewJob.class);
        }

    }

    /**
     * Encapsulates Scheduler to use.
     *
     * @return a scheduler
     *  @throws SchedulerException when getting scheduler
     */
    public static Scheduler getJobScheduler() throws SchedulerException {
        return QuartzUtils.getScheduler();
    }

}
