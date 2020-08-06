package com.dotmarketing.quartz.job;

import org.quartz.*;
import org.quartz.spi.TriggerFiredBundle;

import java.util.Map;

public class TestJobExecutor {

    private TestJobExecutor(){}

    public static void execute(Job job, Map<String, Object> jobMap){
        try {
            job.execute(new JobContextTest(jobMap));
        } catch (JobExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static void execute(Job job, final JobDetail jobDetail, final Trigger trigger){
        try {
            job.execute(new JobContextTest(jobDetail, trigger));
        } catch (JobExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static class JobContextTest extends JobExecutionContext {

        public JobContextTest(Map<String, Object> jobMap) {
            super(null, new TriggerFiredBundleTest(new JobDetailTest(jobMap)), null);
        }

        public JobContextTest(final JobDetail jobDetail, final Trigger trigger) {
            super(null, new TriggerFiredBundleTest(jobDetail, trigger), null);
        }
    }

    private static class JobDetailTest    extends JobDetail {


        public JobDetailTest(final Map<String, Object> jobMap) {
            this.setJobDataMap( new JobDataMap(jobMap) );
        }
    }

    private static class TriggerFiredBundleTest extends TriggerFiredBundle {
        TriggerFiredBundleTest(final JobDetail jobDetail){
            super(jobDetail , new TriggerTest(), null, false,null, null, null, null);
        }

        TriggerFiredBundleTest(final JobDetail jobDetail, final Trigger trigger){
            super(jobDetail , trigger, null, false,null, null, null, null);
        }
    }

    private static class TriggerTest extends SimpleTrigger {


    }
}
