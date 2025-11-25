package com.dotmarketing.quartz;

import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.StatefulJob;
import org.quartz.Trigger;

public abstract class DotStatefulJob extends DotJob implements StatefulJob {

     public static final String TRIGGER_JOB_DETAIL = "trigger_job_detail";
     public static final String EXECUTION_DATA = "execution_data";

    /**
     * This is meant simplify the generation of a Job-name
     * if we have a stateful class and we want it to behave as one single job allowed to run at the time
     * Meaning no parallel instances. The key resides in the name. Quartz guarantees such behavior based on the name.
     * We can still have two instances of a Stateful job if they're scheduled under a different name.
     * @param jobClass
     * @return
     */
    static String getJobName(final Class<? extends StatefulJob> jobClass) {
        return jobClass.getSimpleName();
    }

    /**
     * This is meant simplify the generation of a Job-Group name which also needs to be unique.
     * The Key that makes a job unique is the value pair (job-name,job-group)
     * @param jobClass
     * @return
     */
    static String getJobGroupName(final Class<? extends StatefulJob> jobClass) {
        return getJobName(jobClass) + "_Group";
    }

    /**
     * Description is only informative.
     * @param jobClass
     * @return
     */
    static String getJobDescription(final Class<? extends StatefulJob> jobClass) {
        return getJobName(jobClass) + " instance.";
    }

    /**
     * Trigger names must be unique. A duplicate entry with the same name created in a later date would imply an execution re-schedule rescheduling.
     * Again here the key is a composite of (trigger-name,trigger-group)
     * @param jobClass
     * @return
     */
    static String nextTriggerName(final Class<? extends StatefulJob> jobClass) {
        final String randomID = UUID.randomUUID().toString();
        return jobClass.getSimpleName() + "_Trigger_" + randomID;
    }

    /**
     * Group must be unique to provide meaning to the triggers
     * @param jobClass
     * @return
     */
    static String getTriggerGroupName(final Class<? extends StatefulJob> jobClass) {
        return jobClass.getSimpleName() + "_Trigger_Group";
    }

    /**
     * This will get you the map that stores all the data written into the job detail organized by trigger name.
     * @param jobName
     * @param groupName
     * @return
     */
    private static Optional<Map<String, Object>> getTriggerJobDetail(final String jobName,
            final String groupName) {
        final JobDetail jobDetail = Try
                .of(() -> QuartzUtils.getScheduler().getJobDetail(jobName, groupName))
                .getOrNull();
        if (null == jobDetail) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked") final Map<String, Object> dataMap = (Map<String, Object>) jobDetail
                .getJobDataMap()
                .get(TRIGGER_JOB_DETAIL);
        if (null == dataMap) {
            return Optional.empty();
        }
        return Optional.of(dataMap);
    }

    /**
     * This will get you the map that stores all the data written into the job detail organized by trigger name.
     * @param jobClass
     * @return
     */
    protected static Optional<Map<String, Object>> getTriggerJobDetail(final Class<? extends StatefulJob> jobClass) {
       return getTriggerJobDetail(getJobName(jobClass), getJobGroupName(jobClass));
    }

    /**
     * This extracts the execution data associated with a particular trigger if it isn't found an exception will be thrown
     * @param trigger
     * @param jobClass
     * @return
     */
    protected Map<String, Serializable> getExecutionData(final Trigger trigger, final Class<? extends StatefulJob> jobClass){

        final Optional<Map<String, Object>> triggerJobDetailOptional = getTriggerJobDetail(jobClass);
        if(triggerJobDetailOptional.isEmpty()){
            throw new IllegalArgumentException(
                    String.format("Unable to get job detail data `%s`. ", trigger.getName()));
        }
        final Map<String, Object> triggerJobDetail = triggerJobDetailOptional.get();
        @SuppressWarnings("unchecked")
        final Map<String, Serializable> executionData = (Map<String, Serializable>) triggerJobDetail.get(trigger.getName());
        if(null == executionData) {
            throw new IllegalArgumentException(
                    String.format("Unable to get trigger execution data for trigger `%s`. ", trigger.getName()));
        }
        return executionData;
    }

    /**
     * This will create a new trigger and associate the params data passed in nextExecutionData to the execution of such trigger.
     * The method expects the StatefulJob class so it will let Quartz the job type that needs to be instantiated and it'll generate a unique name used to guarantee that the job is executed as one single instance at the time.
     * @param nextExecutionData the execution data to be associated with the trigger
     * @param jobClass Stateful job class
     */
    protected static void enqueueTrigger(final Map<String, Serializable> nextExecutionData,
                                      final Class<? extends StatefulJob> jobClass)
            throws ParseException, SchedulerException, ClassNotFoundException {
            //Reading and writing to the jobDetail while other triggers are using it makes this a critical section.
            //For which we need to synchronize
        synchronized (DotStatefulJob.class) {
            //Job name must be unique. It doesn't matter if Our class is marked as stateful if we use it under different name we might end-up getting several instances.
            final String jobName = getJobName(jobClass);
            final String groupName = getJobGroupName(jobClass);
            final String description = getJobDescription(jobClass);
            final String nextTriggerName = nextTriggerName(jobClass);
            final String triggerGroup = getTriggerGroupName(jobClass);
            final Map<String, Object> triggersData = new HashMap<>();

            final Map<String, Object> jobProperties = new HashMap<>();
            //get the job detail so we dont lose any data already saved for other triggers.
            final Optional<Map<String, Object>> jobDetailOption = getTriggerJobDetail(jobName, groupName);
            //if there's stuff already saved from other triggers we need to save it again.
            jobDetailOption.ifPresent(triggersData::putAll);
            //Then add the data for our next execution and bind it to our new trigger name.
            triggersData.put(nextTriggerName, nextExecutionData);
            jobProperties.put(TRIGGER_JOB_DETAIL, triggersData);

            final ScheduledTask task = new SimpleScheduledTask(jobName,
                    groupName, description,
                    jobClass.getCanonicalName(),false,
                    nextTriggerName, triggerGroup, new Date(), null,
                    SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW, 10, false, jobProperties,0,0);

            task.setDurability(true); //must be durable to preserve the detail across triggers.

            QuartzUtils.scheduleTask(task);
            Logger.info(DotStatefulJob.class, String.format("New Task for job `%s` scheduled`.", jobName));

        }
    }

}
