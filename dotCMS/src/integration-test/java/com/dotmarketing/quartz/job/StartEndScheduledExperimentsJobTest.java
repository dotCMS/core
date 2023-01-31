package com.dotmarketing.quartz.job;

import static com.dotcms.analytics.AnalyticsAPI.ANALYTICS_ACCESS_TOKEN_TTL;
import static com.dotmarketing.quartz.job.AccessTokenRenewJob.ANALYTICS_ACCESS_TOKEN_RENEW_JOB;
import static com.dotmarketing.quartz.job.AccessTokenRenewJob.ANALYTICS_ACCESS_TOKEN_RENEW_JOB_CRON_KEY;
import static com.dotmarketing.quartz.job.AccessTokenRenewJob.ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER;
import static com.dotmarketing.quartz.job.AccessTokenRenewJob.ANALYTICS_ACCESS_TOKEN_RENEW_TRIGGER_GROUP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.analytics.AnalyticsTestUtils;
import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.TokenStatus;
import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.experiments.business.ExperimentsAPI;
import com.dotcms.experiments.model.AbstractExperiment;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.init.DotInitScheduler;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Config;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;


/**
 * Test for {@link StartEndScheduledExperimentsJob}
 *
 * @author vico
 */
public class StartEndScheduledExperimentsJobTest extends IntegrationTestBase {

    final ExperimentsAPI experimentsAPI = APILocator.getExperimentsAPI();

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: StartEndScheduledExperimentsJobTest.run
     * Given scenario: Experiments scheduled to be started and ended
     * Expected result: Experiments should be started and ended accordingly
     */
    @Test
    public void testJob()
            throws SchedulerException, InterruptedException, DotDataException, DotSecurityException {

        final ExperimentDataGen experimentDataGen = new ExperimentDataGen();
        final Instant NOW = Instant.now().plus(1, ChronoUnit.MINUTES);
        final Instant NOW_PLUS_ONE_MINUTE = NOW.plus(1, ChronoUnit.MINUTES);

        // create experiment that should have started
        final Experiment scheduledToStartExperiment = experimentDataGen
                .scheduling(Scheduling.builder().startDate(NOW).build()).nextPersisted();

        // create experiment that will end soon
        Experiment scheduledToEndExperiment = experimentDataGen
                .scheduling(Scheduling.builder().endDate(NOW_PLUS_ONE_MINUTE).build())
                .nextPersisted();

        scheduledToEndExperiment = experimentsAPI.start(scheduledToEndExperiment.id().orElseThrow(), APILocator.systemUser());

        assertEquals(Status.DRAFT, scheduledToStartExperiment.status());
        assertEquals(Status.RUNNING, scheduledToEndExperiment.status());
        // wait some minutes
        Thread.sleep(2*60*1000);

        new StartEndScheduledExperimentsJob().run(null);

        assertEquals(Status.RUNNING, experimentsAPI.find(scheduledToStartExperiment.id().orElseThrow()
                , APILocator.systemUser()).orElseThrow().status());
        assertEquals(Status.ENDED, experimentsAPI.find(scheduledToEndExperiment.id().orElseThrow()
                , APILocator.systemUser()).orElseThrow().status());
    }
}
