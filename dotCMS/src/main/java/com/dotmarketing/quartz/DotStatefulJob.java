package com.dotmarketing.quartz;

import java.util.Date;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.quartz.StatefulJob;
import org.quartz.Trigger;

public abstract class DotStatefulJob extends DotJob implements StatefulJob {

    public static final class TriggerBuilder {
        private JobDetail jobDetail;
        private String triggerGroupName;

        public TriggerBuilder triggerGroupName(final String triggerGroupName){
            this.triggerGroupName = triggerGroupName;
            return this;
        }

        public TriggerBuilder jobDetail(final JobDetail jobDetail){
            this.jobDetail = jobDetail;
            return this;
        }

        public Trigger build(){
            return new SimpleTrigger(jobDetail.getJobClass().getSimpleName() + "_trigger",
                    triggerGroupName, new Date(System.currentTimeMillis()));
        }
    }

}
