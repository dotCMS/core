package com.dotmarketing.quartz;

import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import java.io.Serializable;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.SimpleScheduleBuilder;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public abstract class DotStatefulJob extends DotJob {

    public static final String TRIGGER_JOB_DETAIL = "trigger_job_detail";
    public static final String EXECUTION_DATA = "execution_data";

    static String getJobName(final Class<? extends DotStatefulJob> jobClass) {
        return jobClass.getSimpleName();
    }

    static String getJobGroupName(final Class<? extends DotStatefulJob> jobClass) {
        return getJobName(jobClass) + "_Group";
    }

    static String getJobDescription(final Class<? extends DotStatefulJob> jobClass) {
        return getJobName(jobClass) + " instance.";
    }

    static String nextTriggerName(final Class<? extends DotStatefulJob> jobClass) {
        final String randomID = UUID.randomUUID().toString();
        return jobClass.getSimpleName() + "_Trigger_" + randomID;
    }

    static String getTriggerGroupName(final Class<? extends DotStatefulJob> jobClass) {
        return jobClass.getSimpleName() + "_Trigger_Group";
    }

    private static Optional<Map<String, Object>> getTriggerJobDetail(final String jobName, final String groupName) {
        final JobDetail jobDetail = Try.of(() -> QuartzUtils.getScheduler().getJobDetail(new JobKey(jobName, groupName)))
                .getOrNull();
        if (null == jobDetail) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        final Map<String, Object> dataMap = (Map<String, Object>) jobDetail.getJobDataMap().get(TRIGGER_JOB_DETAIL);
        if (null == dataMap) {
            return Optional.empty();
        }
        return Optional.of(dataMap);
    }

    protected static Optional<Map<String, Object>> getTriggerJobDetail(final Class<? extends DotStatefulJob> jobClass) {
        return getTriggerJobDetail(getJobName(jobClass), getJobGroupName(jobClass));
    }

    protected Map<String, Serializable> getExecutionData(final Trigger trigger, final Class<? extends DotStatefulJob> jobClass) {

        final Optional<Map<String, Object>> triggerJobDetailOptional = getTriggerJobDetail(jobClass);
        if (triggerJobDetailOptional.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Unable to get job detail data `%s`. ", trigger.getKey().getName()));
        }
        final Map<String, Object> triggerJobDetail = triggerJobDetailOptional.get();
        @SuppressWarnings("unchecked")
        final Map<String, Serializable> executionData = (Map<String, Serializable>) triggerJobDetail.get(trigger.getKey().getName());
        if (null == executionData) {
            throw new IllegalArgumentException(
                    String.format("Unable to get trigger execution data for trigger `%s`. ", trigger.getKey().getName()));
        }
        return executionData;
    }

    protected static void enqueueTrigger(final Map<String, Serializable> nextExecutionData,
            final Class<? extends DotStatefulJob> jobClass)
            throws ParseException, SchedulerException, ClassNotFoundException {
        synchronized (DotStatefulJob.class) {
            final String jobName = getJobName(jobClass);
            final String groupName = getJobGroupName(jobClass);
            final String description = getJobDescription(jobClass);
            final String nextTriggerName = nextTriggerName(jobClass);
            final String triggerGroup = getTriggerGroupName(jobClass);
            final Map<String, Object> triggersData = new HashMap<>();

            final Map<String, Object> jobProperties = new HashMap<>();
            final Optional<Map<String, Object>> jobDetailOption = getTriggerJobDetail(jobName, groupName);
            jobDetailOption.ifPresent(triggersData::putAll);
            triggersData.put(nextTriggerName, nextExecutionData);
            jobProperties.put(TRIGGER_JOB_DETAIL, triggersData);

            final JobDetail jobDetail = JobBuilder.newJob(jobClass)
                    .withIdentity(jobName, groupName)
                    .withDescription(description)
                    .storeDurably()
                    .setJobData(new JobDataMap(jobProperties))
                    .build();

            final Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(nextTriggerName, triggerGroup)
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                    .build();

            QuartzUtils.getScheduler().scheduleJob(jobDetail, trigger);
            Logger.info(DotStatefulJob.class, String.format("New Task for job `%s` scheduled`.", jobName));
        }
    }
}