package com.dotmarketing.quartz.job;

import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;
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

    private static class JobContextTest extends JobExecutionContext {

        private Structure st;
        private User user;
        private Field field;
        private JobDetailTest jobDetail;

        public JobContextTest(Map<String, Object> jobMap) {
            super(null, new TriggerFiredBundleTest(new JobDetailTest(jobMap)), null);

            this.st = st;
            this.user = user;
            this.field = field;
        }
    }

    private static class JobDetailTest    extends JobDetail {


        public JobDetailTest(Map<String, Object> jobMap) {
            this.setJobDataMap( new JobDataMap(jobMap) );
        }
    }

    public static class TriggerFiredBundleTest extends TriggerFiredBundle {
        TriggerFiredBundleTest(JobDetail jobDetail){
            super(jobDetail , new TriggerTest(), null, false,null, null, null, null);
        }
    }

    private static class TriggerTest extends SimpleTrigger {


    }
}
