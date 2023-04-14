package com.dotmarketing.quartz.job;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.init.DotInitScheduler;
import com.dotmarketing.quartz.QuartzUtils;
import io.vavr.control.Try;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.SchedulerException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class PopulateContentletAsJSONJobTest extends IntegrationTestBase {

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
        DotInitScheduler.start();
        deleteJob();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        deleteJob();
    }

    /**
     * <b>Method to test:</b> {@link PopulateContentletAsJSONJob#fireJob(String)}
     * <p>
     * <b>Given sceneario:</b> This test will register a {@link PopulateContentletAsJSONJob} job and make sure it was
     * scheduled.
     * <p>
     * <b>Expected result:</b> No errors should be thrown and the job should be scheduled.
     */
    @Test
    public void test_fireJob() {

        final var jobName = PopulateContentletAsJSONJob.getJobName();
        final var groupName = PopulateContentletAsJSONJob.getJobGroupName();

        try {
            // Make sure the job can be schedule without errors
            PopulateContentletAsJSONJob.fireJob("Host");
        } catch (Exception e) {
            fail("Unable to fire job: " + e.getMessage());
        }

        // Make sure the job was scheduled
        final var scheduler = QuartzUtils.getScheduler();

        // Checking the job was created
        var jobDetail = Try.of(() -> scheduler.getJobDetail(jobName, groupName))
                .onFailure(e -> {
                    fail(String.format("Error retrieving job detail [%s, %s]: %s", jobName, groupName, e.getMessage()));
                }).getOrNull();
        assertNotNull(jobDetail);
    }

    private static void deleteJob() throws SchedulerException {
        PopulateContentletAsJSONJob.removeJob();
    }
}
