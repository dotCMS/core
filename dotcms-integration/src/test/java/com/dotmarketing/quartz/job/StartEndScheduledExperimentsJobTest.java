package com.dotmarketing.quartz.job;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.experiments.business.ExperimentsAPI;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


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
        // Short windows: validateScheduling only requires dates after now-1min, so seconds
        // are enough — the old 1/2-minute windows forced a 2-minute Thread.sleep
        final Instant NOW_PLUS_TWENTY_SECONDS = Instant.now().plus(20, ChronoUnit.SECONDS);

        // create experiment that will end soon
        Experiment scheduledToEndExperiment = new ExperimentDataGen()
                .scheduling(Scheduling.builder().endDate(NOW_PLUS_TWENTY_SECONDS).build())
                .status(Status.RUNNING)
                .nextPersisted();

        Experiment scheduledToStartExperiment = null;

        try {

            assertEquals(Status.RUNNING, scheduledToEndExperiment.status());

            // create experiment that should have started
            final Instant NOW_PLUS_TEN_SECONDS = Instant.now().plus(10, ChronoUnit.SECONDS);

            scheduledToStartExperiment = new ExperimentDataGen()
                    .scheduling(Scheduling.builder().startDate(NOW_PLUS_TEN_SECONDS).build())
                    .nextPersisted();

            scheduledToStartExperiment = experimentsAPI.start(scheduledToStartExperiment.id().orElseThrow(),
                    APILocator.systemUser());

            // wait for both the start date and the end date to be reached
            Thread.sleep(25 * 1000);

            assertEquals(Status.SCHEDULED, scheduledToStartExperiment.status());

            new StartEndScheduledExperimentsJob().run(null);

            assertEquals(Status.RUNNING,
                    experimentsAPI.find(scheduledToStartExperiment.id().orElseThrow()
                            , APILocator.systemUser()).orElseThrow().status());
            assertEquals(Status.ENDED,
                    experimentsAPI.find(scheduledToEndExperiment.id().orElseThrow()
                            , APILocator.systemUser()).orElseThrow().status());
        } finally {
            final Experiment shouldBeRunning = experimentsAPI.find(scheduledToStartExperiment.id().orElseThrow()
                    , APILocator.systemUser()).orElseThrow();
            final Experiment shouldBeEnded = experimentsAPI.find(scheduledToEndExperiment.id().orElseThrow()
                    , APILocator.systemUser()).orElseThrow();

            if(shouldBeRunning.status()==Status.RUNNING) {
                experimentsAPI.end(shouldBeRunning.id().orElseThrow(), APILocator.systemUser());
            }

            if(shouldBeEnded.status()==Status.RUNNING) {
                experimentsAPI.end(shouldBeEnded.id().orElseThrow(), APILocator.systemUser());
            }

        }
    }



}
