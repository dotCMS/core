package com.dotmarketing.quartz.job;

import org.quartz.*;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.JobExecutionContextImpl;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredBundle;

import java.util.Date;
import java.util.Map;

public class TestJobExecutor {

    private TestJobExecutor() {}

    public static void execute(Job job, Map<String, Object> jobMap) {
        try {
            job.execute(new JobContextTest(jobMap));
        } catch (JobExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static class JobContextTest extends JobExecutionContextImpl {

        public JobContextTest(Map<String, Object> jobMap) {
            super(null, new TriggerFiredBundleTest(new JobDetailTest(jobMap)), null);
        }
    }

    private static class JobDetailTest extends JobDetailImpl {

        public JobDetailTest(Map<String, Object> jobMap) {
            this.setJobDataMap(new JobDataMap(jobMap));
        }
    }

    public static class TriggerFiredBundleTest extends TriggerFiredBundle {
        TriggerFiredBundleTest(JobDetail jobDetail) {
            super(jobDetail, new TriggerTest(), null, false, null, null, null, null);
        }
    }

    private static class TriggerTest implements OperableTrigger {

        private Date startTime = new Date();
        private Date endTime;
        private Date nextFireTime = new Date();
        private Date previousFireTime;
        private String fireInstanceId;
        private int priority;
        private String calendarName;
        private int misfireInstruction;

        @Override
        public void updateAfterMisfire(Calendar cal) {
            // Implement this method if needed for tests
        }

        @Override
        public void triggered(Calendar cal) {
            // Implement this method if needed for tests
        }

        @Override
        public Date computeFirstFireTime(Calendar cal) {
            return new Date();
        }

        @Override
        public CompletedExecutionInstruction executionComplete(JobExecutionContext context, JobExecutionException result) {
            return CompletedExecutionInstruction.NOOP;
        }

        @Override
        public void updateWithNewCalendar(Calendar cal, long misfireThreshold) {
            // Implement this method if needed for tests
        }

        @Override
        public void setNextFireTime(Date nextFireTime) {
            this.nextFireTime = nextFireTime;
        }

        @Override
        public void setPreviousFireTime(Date previousFireTime) {
            this.previousFireTime = previousFireTime;
        }

        @Override
        public Date getNextFireTime() {
            return nextFireTime;
        }

        @Override
        public Date getPreviousFireTime() {
            return previousFireTime;
        }

        @Override
        public Date getFireTimeAfter(Date afterTime) {
            return nextFireTime;
        }

        @Override
        public void setFireInstanceId(String id) {
            this.fireInstanceId = id;
        }

        @Override
        public String getFireInstanceId() {
            return fireInstanceId;
        }

        @Override
        public void setMisfireInstruction(int misfireInstruction) {
            this.misfireInstruction = misfireInstruction;
        }


        @Override
        public int getMisfireInstruction() {
            return misfireInstruction;
        }

        @Override
        public void setPriority(int priority) {
            this.priority = priority;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public void setEndTime(Date endTime) {
            this.endTime = endTime;
        }

        @Override
        public Date getEndTime() {
            return endTime;
        }

        @Override
        public void setStartTime(Date startTime) {
            this.startTime = startTime;
        }

        @Override
        public Date getStartTime() {
            return startTime;
        }

        @Override
        public void setCalendarName(String calName) {
            this.calendarName = calName;
        }

        @Override
        public String getCalendarName() {
            return calendarName;
        }

        @Override
        public void setDescription(String description) {
            // Implement this method if needed for tests
        }

        @Override
        public String getDescription() {
            return null;
        }


        @Override
        public void setJobKey(JobKey key) {
            // Implement this method if needed for tests
        }

        @Override
        public JobKey getJobKey() {
            return null;
        }

        @Override
        public void setKey(TriggerKey key) {
            // Implement this method if needed for tests
        }

        @Override
        public TriggerKey getKey() {
            return null;
        }

        @Override
        public TriggerBuilder<? extends Trigger> getTriggerBuilder() {
            return TriggerBuilder.newTrigger();
        }

        @Override
        public boolean mayFireAgain() {
            return true;
        }

        @Override
        public ScheduleBuilder<? extends Trigger> getScheduleBuilder() {
            return SimpleScheduleBuilder.simpleSchedule();
        }



        @Override
        public int compareTo(Trigger other) {
            return 0;
        }

        @Override
        public void validate() {
            // Implement if necessary
        }

        @Override
        public Date getFinalFireTime() {
            return null;
        }

        @Override
        public void setJobDataMap(JobDataMap jobDataMap) {
            // Implement if necessary
        }

        @Override
        public JobDataMap getJobDataMap() {
            return null;
        }

        @Override
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(); // Can't happen
            }
        }
    }
}