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
    public void test_schedule_delete_job() throws Exception{
        JobDetail job =new JobDetail(TEST_JOB_NAME, TEST_JOB_GROUP, TestJob.class);
        Trigger trigger=new CronTrigger(TEST_JOB_TRIGGER_NAME, TEST_JOB_TRIGGER_GROUP, TEST_JOB_NAME, TEST_JOB_GROUP, new Date(), null, DotInitScheduler.CRON_EXPRESSION_EVERY_5_MINUTES);
        QuartzUtils.getScheduler().addJob(job, true);
        QuartzUtils.getScheduler().scheduleJob(trigger);


        job = QuartzUtils.getScheduler().getJobDetail(TEST_JOB_NAME, TEST_JOB_GROUP);
        trigger=QuartzUtils.getScheduler().getTrigger(TEST_JOB_TRIGGER_NAME,TEST_JOB_TRIGGER_GROUP);


        Assert.assertNotNull(job);
        Assert.assertNotNull(trigger);




        QuartzUtils.deleteJobDB(TEST_JOB_NAME,TEST_JOB_GROUP);

        job   = Try.of(()-> QuartzUtils.getScheduler().getJobDetail(TEST_JOB_NAME, TEST_JOB_GROUP)).getOrNull();
        trigger   = Try.of(()-> QuartzUtils.getScheduler().getTrigger(TEST_JOB_TRIGGER_NAME,TEST_JOB_TRIGGER_GROUP)).getOrNull();



        Assert.assertNull(job);

        Assert.assertNull(trigger);
    }











}
