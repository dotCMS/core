package com.dotmarketing.quartz.job;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.analytics.AccessTokens;
import com.dotcms.analytics.AnalyticsAPI;
import com.dotcms.analytics.AnalyticsAPIImpl;
import com.dotcms.analytics.AnalyticsTestUtils;
import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.AccessTokenFetchMode;
import com.dotcms.analytics.model.TokenStatus;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.init.DotInitScheduler;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Config;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static com.dotmarketing.quartz.job.AccessTokenRenewJob.ANALYTICS_ACCESS_TOKEN_RENEW_JOB;
import static com.dotmarketing.quartz.job.AccessTokenRenewJob.ANALYTICS_ACCESS_TOKEN_RENEW_JOB_CRON_DEFAULT;
import static com.dotmarketing.quartz.job.AccessTokenRenewJob.ANALYTICS_ACCESS_TOKEN_RENEW_JOB_CRON_KEY;
import static com.dotmarketing.quartz.job.AccessTokenRenewJob.ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER;
import static com.dotmarketing.quartz.job.AccessTokenRenewJob.ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER_GROUP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;


/**
 * Access Token renew integration test.
 *
 * @author vico
 */
public class AccessTokenRenewJobTest extends IntegrationTestBase {

    private static AnalyticsAPI analyticsAPI;
    private static int accessTokenTtl;
    private AccessTokenRenewJob accessTokenRenewJob;
    private Host host;
    private AnalyticsApp analyticsApp;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();

        analyticsAPI = APILocator.getAnalyticsAPI();
        Config.setProperty("ALLOW_ACCESS_TO_PRIVATE_SUBNETS", true);
        Config.setProperty(ANALYTICS_ACCESS_TOKEN_RENEW_JOB_CRON_KEY, "0 0 0 * * ?");

        deleteJob();
        DotInitScheduler.start();

        accessTokenTtl = Config.getIntProperty(
                AnalyticsAPI.ANALYTICS_ACCESS_TOKEN_TTL_KEY,
                (int) TimeUnit.HOURS.toSeconds(1));
    }

    @AfterClass
    public static void afterClass() {
        Config.setProperty(ANALYTICS_ACCESS_TOKEN_RENEW_JOB_CRON_KEY, ANALYTICS_ACCESS_TOKEN_RENEW_JOB_CRON_DEFAULT);
        Config.setProperty("ALLOW_ACCESS_TO_PRIVATE_SUBNETS", false);
    }

    @Before
    public void before() throws Exception {
        host = new SiteDataGen().nextPersisted();
        analyticsApp = AnalyticsTestUtils.prepareAnalyticsApp(host);
        accessTokenRenewJob = new AccessTokenRenewJob();
    }

    /**
     * When main scheduler has initiated
     * Then make sure {@link org.quartz.JobDetail} is scheduled in the Quartz scheduler object
     */
    @Test
    public void test_schedule() throws SchedulerException, DotDataException {
        assertNotNull(QuartzUtils.getScheduler().getJobDetail(
            ANALYTICS_ACCESS_TOKEN_RENEW_JOB,
            DotInitScheduler.DOTCMS_JOB_GROUP_NAME));
        deleteJob();
    }

    /**
     * Given an {@link AnalyticsApp}
     * Then try to renew the {@link AccessToken} associated with it
     * And verify it is created
     */
    @Test
    public void test_accessTokenRenew_none() throws Exception {
        analyticsAPI.resetAccessToken(analyticsApp);

        accessTokenRenewJob.execute(getJobContext());
        Thread.sleep(2000);

        AccessToken accessToken = analyticsAPI.getCachedAccessToken(analyticsApp);
        assertNotNull(accessToken);
    }

    /**
     * Given an {@link AnalyticsApp}
     * Then try to renew the expired {@link AccessToken} associated with it
     * And verify it is created
     */
    @Test
    public void test_accessTokenRenew_expired() throws Exception {
        analyticsAPI.resetAccessToken(analyticsApp);

        final Instant issueDate = Instant.now().minusSeconds(accessTokenTtl);
        AccessToken accessToken = analyticsAPI
            .getAccessToken(
                analyticsApp,
                AccessTokenFetchMode.FORCE_RENEW)
            .withIssueDate(issueDate);
        AccessTokens.get().putAccessToken(accessToken);

        accessTokenRenewJob.execute(getJobContext());
        Thread.sleep(2000);

        accessToken = analyticsAPI.getCachedAccessToken(analyticsApp);
        assertNotNull(accessToken);
        assertTrue(accessToken.issueDate().isAfter(issueDate));
    }

    /**
     * Given an {@link AnalyticsApp}
     * Then try to renew soon to be expired {@link AccessToken} associated with it
     * And verify it is created
     */
    @Test
    public void test_accessTokenRenew_inWindow() throws Exception {
        analyticsAPI.resetAccessToken(analyticsApp);

        final Instant issueDate = Instant.now().minusSeconds(accessTokenTtl).plusSeconds(30);
        AccessToken accessToken = analyticsAPI
            .getAccessToken(
                analyticsApp,
                AccessTokenFetchMode.FORCE_RENEW)
            .withIssueDate(issueDate);
        AccessTokens.get().putAccessToken(accessToken);

        accessTokenRenewJob.execute(getJobContext());
        Thread.sleep(2000);

        accessToken = analyticsAPI.getCachedAccessToken(analyticsApp);
        assertNotNull(accessToken);
        assertTrue(accessToken.issueDate().isAfter(issueDate));
    }

    /**
     * Given an {@link AnalyticsApp} and an {@link AccessToken} with a {@link TokenStatus#NOOP}
     * Then try to renew the {@link AccessToken} associated with it
     * And verify it's still the same
     */
    @Test
    public void test_accessTokenRenew_whenNoop() throws Exception {
        Config.setProperty(AnalyticsAPI.ANALYTICS_USE_DUMMY_TOKEN_KEY, "false");
        analyticsAPI = new AnalyticsAPIImpl();

        final String reason = "some-reason";
        final String clientId = analyticsApp.getAnalyticsProperties().clientId();
        AccessTokens.get().putAccessToken(AnalyticsHelper.get().createNoopToken(analyticsApp, reason).withClientId(clientId));

        accessTokenRenewJob.execute(getJobContext());
        Thread.sleep(2000);

        AccessToken accessToken = analyticsAPI.getCachedAccessToken(analyticsApp);
        assertSame(TokenStatus.NOOP, accessToken.status().tokenStatus());
        assertEquals(reason, accessToken.status().reason());

        Config.setProperty(AnalyticsAPI.ANALYTICS_USE_DUMMY_TOKEN_KEY, "true");
        analyticsAPI = APILocator.getAnalyticsAPI();
    }

    /**
     * Given an {@link AnalyticsApp} and an {@link AccessToken} with a {@link TokenStatus#BLOCKED}
     * Then try to renew the {@link AccessToken} associated with it
     * And verify it's still the same
     */
    @Test
    public void test_accessTokenRenew_whenBlocked() throws Exception {
        Config.setProperty(AnalyticsAPI.ANALYTICS_USE_DUMMY_TOKEN_KEY, "false");
        analyticsAPI = new AnalyticsAPIImpl();

        final String reason = "some-reason";
        final String clientId = analyticsApp.getAnalyticsProperties().clientId();
        AccessTokens.get().putAccessToken(AnalyticsHelper.get().createBlockedToken(analyticsApp, reason).withClientId(clientId));

        accessTokenRenewJob.execute(getJobContext());
        Thread.sleep(2000);

        AccessToken accessToken = analyticsAPI.getCachedAccessToken(analyticsApp);
        assertSame(TokenStatus.BLOCKED, accessToken.status().tokenStatus());
        assertEquals(reason, accessToken.status().reason());

        Config.setProperty(AnalyticsAPI.ANALYTICS_USE_DUMMY_TOKEN_KEY, "true");
        analyticsAPI = APILocator.getAnalyticsAPI();
    }

    /**
     * Given an {@link AnalyticsApp} and an {@link AccessToken} with a {@link TokenStatus#OK}
     * Then try to renew the {@link AccessToken} associated with it
     * And verify it's still the same
     */
    @Test
    public void test_accessTokenRenew_whenOk() throws Exception {
        analyticsAPI.resetAccessToken(analyticsApp);
        analyticsAPI.getAccessToken(analyticsApp, AccessTokenFetchMode.FORCE_RENEW);

        accessTokenRenewJob.execute(getJobContext());
        Thread.sleep(2000);

        AccessToken accessToken = analyticsAPI.getCachedAccessToken(analyticsApp);
        assertSame(TokenStatus.OK, accessToken.status().tokenStatus());
    }

    /**
     * Given an {@link AnalyticsApp} instance with wrong clientId
     * When executing the job
     * When verify token is {@link TokenStatus#NOOP}
     */
    @Test
    public void test_accessTokenRenew_fail_wrongClientId() throws Exception {
        Config.setProperty(AnalyticsAPI.ANALYTICS_USE_DUMMY_TOKEN_KEY, "false");
        analyticsAPI = new AnalyticsAPIImpl();

        analyticsApp = AnalyticsTestUtils.prepareAnalyticsApp(host, "some-client-id-xxx");

        accessTokenRenewJob.execute(getJobContext());
        Thread.sleep(2000);

        AccessToken accessToken = analyticsAPI.getCachedAccessToken(analyticsApp);
        assertNull(accessToken);

        Config.setProperty(AnalyticsAPI.ANALYTICS_USE_DUMMY_TOKEN_KEY, "true");
        analyticsAPI = APILocator.getAnalyticsAPI();
    }

    private static void unscheduleJob() throws SchedulerException {
        QuartzUtils.getScheduler().deleteJob(
            ANALYTICS_ACCESS_TOKEN_RENEW_JOB,
            DotInitScheduler.DOTCMS_JOB_GROUP_NAME);
    }

    private JobExecutionContext getJobContext() throws SchedulerException {
        final JobDataMap jobDataMap = new JobDataMap();
        final JobDetail jobDetail = new JobDetail(
            ANALYTICS_ACCESS_TOKEN_RENEW_JOB,
            DotInitScheduler.DOTCMS_JOB_GROUP_NAME,
            AccessTokenRenewJob.class);
        jobDetail.setJobDataMap(jobDataMap);
        jobDetail.setDurability(false);
        jobDetail.setVolatility(false);
        jobDetail.setRequestsRecovery(true);

        return new JobExecutionContext(
            AccessTokenRenewJob.getJobScheduler(),
            new TestJobExecutor.TriggerFiredBundleTest(jobDetail),
            accessTokenRenewJob);
    }

    private static void deleteJob() throws DotDataException, SchedulerException {
        unscheduleJob();

        DotConnect dotConnect = new DotConnect();
        dotConnect
            .setSQL("delete from QRTZ_EXCL_CRON_TRIGGERS where TRIGGER_NAME = ? and TRIGGER_GROUP = ?")
            .addParam(ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER)
            .addParam(ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER_GROUP)
            .loadObjectResults();
        dotConnect
            .setSQL("delete from QRTZ_EXCL_SIMPLE_TRIGGERS where TRIGGER_NAME = ? and TRIGGER_GROUP = ?")
            .addParam(ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER)
            .addParam(ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER_GROUP)
            .loadObjectResults();
        dotConnect
            .setSQL("delete from QRTZ_EXCL_TRIGGERS where TRIGGER_NAME = ? and TRIGGER_GROUP = ?")
            .addParam(ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER)
            .addParam(ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER_GROUP)
            .loadObjectResults();
        dotConnect
            .setSQL("delete from QRTZ_EXCL_JOB_DETAILS where JOB_NAME = ? and JOB_GROUP = ?")
            .addParam(ANALYTICS_ACCESS_TOKEN_RENEW_JOB)
            .addParam(DotInitScheduler.DOTCMS_JOB_GROUP_NAME)
            .loadObjectResults();
    }

}
