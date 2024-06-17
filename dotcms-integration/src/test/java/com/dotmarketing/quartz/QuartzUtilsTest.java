package com.dotmarketing.quartz;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.init.DotInitScheduler;
import com.dotmarketing.quartz.job.UsersToDeleteThread;
import com.dotmarketing.util.Config;
import io.vavr.control.Try;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.*;

import java.util.Calendar;
import java.util.Date;
import org.quartz.impl.StdSchedulerFactory;

public class QuartzUtilsTest {


    private static final String TEST_JOB_NAME="TestingJobName";
    private static final String TEST_JOB_GROUP="TestingJobGroup";
    private static final String TEST_JOB_TRIGGER_NAME="TestingJobTriggerName";
    private static final String TEST_JOB_TRIGGER_GROUP="TestingJobTriggerGroup";


    class TestJob implements StatefulJob{

        @Override
        public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
            System.out.println("This is a test job");
        }
    }

    ;
    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        QuartzUtils.startSchedulers();

    }



    @Test
    public void test_schedule_delete_job() throws Exception {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();

        JobDetail job = JobBuilder.newJob(TestJob.class)
                .withIdentity(TEST_JOB_NAME, TEST_JOB_GROUP)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(TEST_JOB_TRIGGER_NAME, TEST_JOB_TRIGGER_GROUP)
                .forJob(job)
                .withSchedule(CronScheduleBuilder.cronSchedule(DotInitScheduler.CRON_EXPRESSION_EVERY_5_MINUTES))
                .startAt(new Date())
                .build();

        scheduler.addJob(job, true);
        scheduler.scheduleJob(trigger);

        JobKey jobKey = new JobKey(TEST_JOB_NAME, TEST_JOB_GROUP);
        TriggerKey triggerKey = new TriggerKey(TEST_JOB_TRIGGER_NAME, TEST_JOB_TRIGGER_GROUP);

        job = scheduler.getJobDetail(jobKey);
        trigger = scheduler.getTrigger(triggerKey);

        Assert.assertNotNull(job);
        Assert.assertNotNull(trigger);

        scheduler.deleteJob(jobKey);

        job = Try.of(() -> scheduler.getJobDetail(jobKey)).getOrNull();
        trigger = Try.of(() -> scheduler.getTrigger(triggerKey)).getOrNull();

        Assert.assertNull(job);
        Assert.assertNull(trigger);

        scheduler.shutdown();
    }


}
