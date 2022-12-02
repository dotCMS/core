package com.dotmarketing.quartz.job;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.analytics.AnalyticsAPI;
import com.dotcms.analytics.AnalyticsTestUtils;
import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.cache.AnalyticsCache;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.exception.AnalyticsException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.init.DotInitScheduler;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Config;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;

import java.util.Optional;

import static com.dotmarketing.quartz.job.AccessTokenRenewJob.ANALYTICS_ACCESS_TOKEN_RENEW_JOB;
import static com.dotmarketing.quartz.job.AccessTokenRenewJob.ANALYTICS_ACCESS_TOKEN_RENEW_JOB_CRON_KEY;
import static com.dotmarketing.quartz.job.AccessTokenRenewJob.ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER;
import static com.dotmarketing.quartz.job.AccessTokenRenewJob.ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER_GROUP;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


/**
 * Access Token renew integration test.
 *
 * @author vico
 */
public class AccessTokenRenewJobTest extends IntegrationTestBase {

    private static AnalyticsAPI analyticsAPI;
    private static AnalyticsCache analyticsCache;
    private AnalyticsApp analyticsApp;
    private AccessTokenRenewJob accessTokenRenewJob;

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();

        analyticsAPI = APILocator.getAnalyticsAPI();
        analyticsCache = CacheLocator.getAnalyticsCache();
        Config.setProperty("ALLOW_ACCESS_TO_PRIVATE_SUBNETS", true);
        Config.setProperty(ANALYTICS_ACCESS_TOKEN_RENEW_JOB_CRON_KEY, "0/15 0 0 * * ?");

        deleteJob();
        DotInitScheduler.start();
    }

    @Before
    public void before() throws DotDataException, DotSecurityException {
        accessTokenRenewJob = new AccessTokenRenewJob();
        final Host host = new SiteDataGen().nextPersisted(false);
        analyticsApp = AnalyticsTestUtils.prepareAnalyticsApp(host);
    }

    /**
     * When main scheduler has initiated
     * Then make sure {@link org.quartz.JobDetail} is scheduled in the Quartz scheduler object
     */
    @Test
    public void test_schedule() throws SchedulerException, DotDataException {
        /*AccessTokenRenewJob.AccessTokensRenewJobScheduler.schedule(
            ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER,
            ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER_GROUP);*/
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
    public void test_accessTokenRenew_happyPath() throws SchedulerException, AnalyticsException {
        AccessToken accessToken = analyticsAPI.getAccessToken(analyticsApp);
        assertNotNull(accessToken);
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
