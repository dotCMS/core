package com.dotmarketing.quartz;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import org.quartz.*;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import org.quartz.impl.matchers.GroupMatcher;

/**
 * This utility class let you schedule and check scheduled task through the two configured dotCMS schedulers:
 * - The standard scheduler
 * That let you run multiple task in parallel
 * - The sequential scheduler
 * That let you run only one task at a time, the order of the execution of the tasks is managed by quartz priorities
 *
 */
public class QuartzUtils {

    private static final Map<String, TaskRuntimeValues> runtimeTaskValues = new HashMap<>();

    /**
     * Lists all jobs scheduled through the standard scheduler, the standard scheduler
     * let you run multiple jobs in parallel
     *
     * @return
     */
    public static List<ScheduledTask> getScheduledTasks() throws SchedulerException {
        final Scheduler scheduler = getScheduler();
        return getScheduledTasks(scheduler, true, null);
    }

    /**
     * Want to get an instance of the scheduler? please use this method.
     *
     * @return
     */
    public static Scheduler getScheduler() {
        try {
            return DotSchedulerFactory.getInstance().getScheduler();
        } catch (Exception e) {
            Logger.warnAndDebug(QuartzUtils.class, e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Lists all jobs scheduled through the standard scheduler that belong to the given group
     *
     * @return
     */
    public static List<ScheduledTask> getScheduledTasks(final String group)
            throws SchedulerException {
        final Scheduler scheduler = getScheduler();
        return getScheduledTasks(scheduler, true, group);
    }

    /**
     * Scheduled tasks getter
     *
     * @param scheduler
     * @param sequential
     * @param group
     * @return
     * @throws SchedulerException
     */
    private static List<ScheduledTask> getScheduledTasks(
            final Scheduler scheduler, final boolean sequential, final String group)
            throws SchedulerException {
        final List<ScheduledTask> result = new ArrayList<>(100);

        for (final String groupName : scheduler.getJobGroupNames()) {
            if (group != null && !groupName.equals(group)) continue;

            for (final JobKey jobKey :
                    scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                final JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                final List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);

                for (Trigger t : triggers) {
                    ScheduledTask task;
                    if (t instanceof CronTrigger) {
                        task = new CronScheduledTask();
                        ((CronScheduledTask) task)
                                .setCronExpression(((CronTrigger) t).getCronExpression());
                    } else if (t instanceof SimpleTrigger) {
                        task = new SimpleScheduledTask();
                        ((SimpleScheduledTask) task)
                                .setRepeatCount(((SimpleTrigger) t).getRepeatCount());
                        ((SimpleScheduledTask) task)
                                .setRepeatInterval(((SimpleTrigger) t).getRepeatInterval());
                    } else {
                        continue;
                    }
                    task.setJobName(jobDetail.getKey().getName());
                    task.setJobGroup(jobDetail.getKey().getGroup());
                    task.setJobDescription(jobDetail.getDescription());
                    task.setProperties(new HashMap<>(jobDetail.getJobDataMap()));

                    task.setStartDate(t.getStartTime());
                    task.setEndDate(t.getEndTime());
                    task.setSequentialScheduled(sequential);
                    task.setJavaClassName(jobDetail.getJobClass().getName());

                    result.add(task);
                }
            }
        }
        return result;
    }

    /**
     * This method checks all jobs configured either in the standard or the sequential schedulers that match
     * the given jobName and jobGroup
     *
     * Even if the job is only configured in a single scheduler this method could return a list with multiple instances
     * because the same job could be configured with multiple triggers.
     *
     * @param jobName
     * @param jobGroup
     * @return
     * @throws SchedulerException
     */
    public static List<ScheduledTask> getScheduledTask(final String jobName, final String jobGroup)
            throws SchedulerException {
        final Scheduler scheduler = getScheduler();
        return getScheduledTask(jobName, jobGroup, scheduler, false);
    }

    /**
     * This method checks all jobs configured either in the standard scheduler that match
     * the given jobName and jobGroup
     *
     * This method could return a list with multiple instances
     * because the same job could be configured with multiple triggers.
     *
     * @param jobName
     * @param jobGroup
     * @return
     * @throws SchedulerException
     */
    public static List<ScheduledTask> getStandardScheduledTask(
            final String jobName, final String jobGroup) throws SchedulerException {
        final Scheduler scheduler = getScheduler();
        return getScheduledTask(jobName, jobGroup, scheduler, false);
    }

    /**
     * Scheduled task getter
     *
     * @param jobName
     * @param jobGroup
     * @param scheduler
     * @param sequential
     * @return
     * @throws SchedulerException
     */
    private static List<ScheduledTask> getScheduledTask(
            final String jobName,
            final String jobGroup,
            final Scheduler scheduler,
            final boolean sequential)
            throws SchedulerException {
        List<ScheduledTask> result = new ArrayList<>(1);

        final JobKey jobKey = new JobKey(jobName, jobGroup);
        final JobDetail jobDetail = scheduler.getJobDetail(jobKey);

        if (jobDetail != null) {
            final List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
            for (final Trigger t : triggers) {
                ScheduledTask task;
                if (t instanceof CronTrigger) {
                    task = new CronScheduledTask();
                    ((CronScheduledTask) task)
                            .setCronExpression(((CronTrigger) t).getCronExpression());
                } else if (t instanceof SimpleTrigger) {
                    task = new SimpleScheduledTask();
                    ((SimpleScheduledTask) task)
                            .setRepeatCount(((SimpleTrigger) t).getRepeatCount());
                    ((SimpleScheduledTask) task)
                            .setRepeatInterval(((SimpleTrigger) t).getRepeatInterval());
                } else {
                    continue;
                }
                task.setJobName(jobDetail.getKey().getName());
                task.setJobGroup(jobDetail.getKey().getGroup());
                task.setJobDescription(jobDetail.getDescription());
                task.setProperties(new HashMap<>(jobDetail.getJobDataMap()));
                task.setJavaClassName(jobDetail.getJobClass().getName());
                task.setStartDate(t.getStartTime());
                task.setEndDate(t.getEndTime());
                task.setSequentialScheduled(sequential);

                result.add(task);
            }
        }
        return result;
    }

    /**
     * Tells you whether the given jobName and jobGroup is set in the jobs DB to be executed or was executed and set with durability true
     * so still exists in the DB
     *
     * @param jobName
     * @param jobGroup
     * @return
     * @throws SchedulerException
     */
    public static boolean isJobScheduled(final String jobName, final String jobGroup)
            throws SchedulerException {
        final Scheduler scheduler = getScheduler();
        return isJobScheduled(jobName, jobGroup, scheduler);
    }

    /**
     * Tells you whether the given jobName and jobGroup is set in the jobs DB to be sequentially executed or was executed and set with durability true
     * so still exists in the DB
     *
     * @param jobName
     * @param jobGroup
     * @param scheduler
     * @return
     * @throws SchedulerException
     */
    private static boolean isJobScheduled(
            final String jobName, final String jobGroup, final Scheduler scheduler)
            throws SchedulerException {
        final JobKey jobKey = new JobKey(jobName, jobGroup);
        return scheduler.checkExists(jobKey);
    }

    /**
     * Returns the current task progress, it returns -1 if no task progress is found
     *
     * @param jobName
     * @param jobGroup
     * @return
     * @throws SchedulerException
     */
    public static int getTaskProgress(final String jobName, final String jobGroup) {
        TaskRuntimeValues runtimeValues = runtimeTaskValues.get(jobName + "-" + jobGroup);
        if (runtimeValues == null) return -1;
        return runtimeValues.currentProgress;
    }

    /**
     * Updates task progress
     *
     * @param jobName
     * @param jobGroup
     * @param progress
     */
    public static void updateTaskProgress(
            final String jobName, final String jobGroup, final int progress) {
        final TaskRuntimeValues runtimeValues = runtimeTaskValues.get(jobName + "-" + jobGroup);
        if (runtimeValues == null) return;
        runtimeValues.currentProgress = progress;
    }

    /**
     * Returns a task starting point of progress, by default 0 unless the task set it to something different
     *
     * @param jobName
     * @param jobGroup
     * @return
     *@throws SchedulerException
     */
    public static int getTaskStartProgress(final String jobName, final String jobGroup) {
        TaskRuntimeValues runtimeValues = runtimeTaskValues.get(jobName + "-" + jobGroup);
        if (runtimeValues == null) return -1;
        return runtimeValues.startProgress;
    }
    /**
     * Sets a task point of progress
     *
     * @param jobName
     * @param jobGroup
     * @param startProgress
     */

    public static void setTaskStartProgress(
            final String jobName, final String jobGroup, final int startProgress) {
        TaskRuntimeValues runtimeValues = runtimeTaskValues.get(jobName + "-" + jobGroup);
        if (runtimeValues == null) return;
        runtimeValues.startProgress = startProgress;
    }

    /**
     * Returns a task ending point to track progress, by default 100 unless the task set it to something different
     *
     * @param jobName
     * @param jobGroup
     * @return
     * @throws SchedulerException
     */
    public static int getTaskEndProgress(final String jobName, final String jobGroup) {
        TaskRuntimeValues runtimeValues = runtimeTaskValues.get(jobName + "-" + jobGroup);
        if (runtimeValues == null) return -1;
        return runtimeValues.endProgress;
    }

    /**
     * Task runtime value getter
     *
     * @param jobName
     * @param jobGroup
     * @return
     */
    public static TaskRuntimeValues getTaskRuntimeValues(
            final String jobName, final String jobGroup) {
        return runtimeTaskValues.get(jobName + "-" + jobGroup);
    }

    /**
     * Register a runtime task
     *
     * @param jobName
     * @param jobGroup
     * @param runtimeValues
     */
    public static void setTaskRuntimeValues(
            final String jobName, final String jobGroup, final TaskRuntimeValues runtimeValues) {
        runtimeTaskValues.put(jobName + "-" + jobGroup, runtimeValues);
    }

    /**
     * Task end-progress value setter
     *
     * @param jobName
     * @param jobGroup
     * @param endProgress
     */
    public static void setTaskEndProgress(
            final String jobName, final String jobGroup, final int endProgress) {
        TaskRuntimeValues runtimeValues = runtimeTaskValues.get(jobName + "-" + jobGroup);
        if (runtimeValues == null) return;
        runtimeValues.endProgress = endProgress;
    }

    /**
     * Shuts down all schedulers
     *
     * @throws SchedulerException
     */
    public static void stopSchedulers() throws SchedulerException {
        Collection<Scheduler> list = DotSchedulerFactory.getInstance().getAllSchedulers();
        for (Scheduler s : list) {
            s.shutdown();
        }
    }

    /**
     * Returns the current task progress, it returns -1 if no task progress is found
     *
     * @param jobName
     * @param jobGroup
     * @return
     * @throws SchedulerException
     */
    public static List<String> getTaskMessages(final String jobName, final String jobGroup) {
        final TaskRuntimeValues runtimeValues = runtimeTaskValues.get(jobName + "-" + jobGroup);
        if (runtimeValues == null) return null;
        return runtimeValues.messages;
    }

    /**
     * Add a task message
     *
     * @param jobName
     * @param jobGroup
     * @param newMessage
     */
    public static void addTaskMessage(
            final String jobName, final String jobGroup, final String newMessage) {
        TaskRuntimeValues runtimeValues = runtimeTaskValues.get(jobName + "-" + jobGroup);
        if (runtimeValues == null) return;
        runtimeValues.messages.add(newMessage);
    }

    /**
     * Initializer
     *
     * @param jobName
     * @param jobGroup
     */
    public static void initializeTaskRuntimeValues(final String jobName, final String jobGroup) {
        runtimeTaskValues.put(jobName + "-" + jobGroup, new TaskRuntimeValues());
    }

    /**
     * Removes from the map the Task
     *
     * @param jobName
     * @param jobGroup
     */
    public static void removeTaskRuntimeValues(final String jobName, final String jobGroup) {
        runtimeTaskValues.remove(jobName + "-" + jobGroup);
    }

    /**
     * This method schedules the given job in the quartz system, and depending on the sequentialScheduled property
     * it will use the sequential of the standard scheduler
     *
     * @param job
     * @return
     * @throws SchedulerException
     * @throws ParseException
     * @throws ClassNotFoundException
     */
    public static void scheduleTask(final ScheduledTask job)
            throws SchedulerException, ParseException, ClassNotFoundException {
        final Scheduler scheduler = getScheduler();

        boolean isNew = false;

        final String jobName = job.getJobName();
        final String jobGroup = job.getJobGroup();
        final String triggerName =
                job.getTriggerName() == null ? jobName + "_trigger" : job.getTriggerName();
        final String triggerGroup =
                job.getTriggerGroup() == null ? jobGroup : job.getTriggerGroup();
        final Date startDate = job.getStartDate();
        final Date endDate = job.getEndDate();

        JobDetail jobDetail = scheduler.getJobDetail(JobKey.jobKey(jobName, jobGroup));
        if (jobDetail == null) {
            jobDetail =
                    JobBuilder.newJob((Class<? extends Job>) Class.forName(job.getJavaClassName()))
                            .withIdentity(jobName, jobGroup)
                            .withDescription(job.getJobDescription())
                            .usingJobData(new JobDataMap(job.getProperties()))
                            .storeDurably(job.getDurability())
                            .build();
            isNew = true;
        } else {
            jobDetail =
                    jobDetail
                            .getJobBuilder()
                            .ofType((Class<? extends Job>) Class.forName(job.getJavaClassName()))
                            .withDescription(job.getJobDescription())
                            .usingJobData(new JobDataMap(job.getProperties()))
                            .storeDurably(job.getDurability())
                            .build();
        }

        Trigger trigger;
        if (job instanceof CronScheduledTask) {
            trigger =
                    TriggerBuilder.newTrigger()
                            .withIdentity(triggerName, triggerGroup)
                            .forJob(jobDetail)
                            .withSchedule(
                                    CronScheduleBuilder.cronSchedule(
                                            ((CronScheduledTask) job).getCronExpression()))
                            .startAt(startDate)
                            .endAt(endDate)
                            .build();
        } else {
            trigger =
                    TriggerBuilder.newTrigger()
                            .withIdentity(triggerName, triggerGroup)
                            .forJob(jobDetail)
                            .withSchedule(
                                    SimpleScheduleBuilder.simpleSchedule()
                                            .withRepeatCount(
                                                    ((SimpleScheduledTask) job).getRepeatCount())
                                            .withIntervalInMilliseconds(
                                                    ((SimpleScheduledTask) job)
                                                            .getRepeatInterval()))
                            .startAt(startDate)
                            .endAt(endDate)
                            .build();
        }

        trigger.getJobDataMap().putAll(job.getProperties());
        trigger = trigger.getTriggerBuilder().withPriority(job.getMisfireInstruction()).build();

        scheduler.addJob(jobDetail, true);

        if (isNew) {
            scheduler.scheduleJob(trigger);
        } else if (scheduler.checkExists(trigger.getKey())) {
            scheduler.rescheduleJob(trigger.getKey(), trigger);
        } else {
            try {
                scheduler.scheduleJob(trigger);
            } catch (Exception e) {
                scheduler.rescheduleJob(trigger.getKey(), trigger);
            }
        }

        QuartzUtils.initializeTaskRuntimeValues(jobName, jobGroup);
    }

    /**
     * Removes the job and all associated triggers from both schedulers the standard and the sequential
     *
     * @param jobName
     * @param jobGroup
     * @return true if the job is found and removed in at least one of the schedulers, false otherwise
     * @throws SchedulerException
     */
    public static boolean removeJob(final String jobName, final String jobGroup)
            throws SchedulerException {
        final Scheduler scheduler = getScheduler();
        return removeJob(jobName, jobGroup, scheduler);
    }

    /**
     * Removes the job and all associated triggers from the standard jobs scheduler
     *
     * @param jobName
     * @param jobGroup
     * @return true if the job is found and removed in at least one of the schedulers, false otherwise
     * @throws SchedulerException
     */
    public static boolean removeStandardJob(final String jobName, final String jobGroup)
            throws SchedulerException {
        final Scheduler scheduler = getScheduler();
        return removeJob(jobName, jobGroup, scheduler);
    }

    /**
     * This method avoids all the Quartz madness and just delete the job from the db
     *
     * @param jobName
     * @param jobGroup
     * @return
     * @throws DotDataException
     */
    @WrapInTransaction
    public static boolean deleteJobDB(final String jobName, final String jobGroup)
            throws DotDataException {
        final DotConnect db = new DotConnect();

        final List<Map<String, Object>> results =
                db.setSQL(
                                "select trigger_name,trigger_group from qrtz_excl_triggers where job_name=? and job_group=?")
                        .addParam(jobName)
                        .addParam(jobGroup)
                        .loadObjectResults();

        for (final Map<String, Object> map : results) {
            final String triggerName = map.get("trigger_name").toString();
            final String triggerGroup = map.get("trigger_group").toString();

            db.setSQL(
                            "delete from qrtz_excl_cron_triggers where trigger_name=? and trigger_group=?")
                    .addParam(triggerName)
                    .addParam(triggerGroup)
                    .loadResult();
            db.setSQL("delete from qrtz_excl_triggers where trigger_name=? and trigger_group=?")
                    .addParam(triggerName)
                    .addParam(triggerGroup)
                    .loadResult();

            db.setSQL("delete from qrtz_excl_paused_trigger_grps where trigger_group=?")
                    .addParam(triggerGroup)
                    .loadResult();

            db.setSQL(
                            "delete from qrtz_excl_trigger_listeners where trigger_name=? and trigger_group=?")
                    .addParam(triggerName)
                    .addParam(triggerGroup)
                    .loadResult();

            db.setSQL(
                            "delete from qrtz_excl_simple_triggers where trigger_name=? and trigger_group=?")
                    .addParam(triggerName)
                    .addParam(triggerGroup)
                    .loadResult();
        }

        db.setSQL("delete from qrtz_excl_job_details where job_name=? and job_group=?")
                .addParam(jobName)
                .addParam(jobGroup)
                .loadResult();

        db.setSQL("delete from qrtz_excl_job_listeners where job_name=? and job_group=?")
                .addParam(jobName)
                .addParam(jobGroup)
                .loadResult();

        return true;
    }

    /**
     * Removes the job and all associated triggers from the sequential jobs scheduler
     *
     * @param jobName
     * @param jobGroup
     * @return true if the job is found and removed in at least one of the schedulers, false otherwise
     * @throws SchedulerException
     */
    public static boolean removeSequentialJob(final String jobName, final String jobGroup)
            throws SchedulerException {
        final Scheduler scheduler = getScheduler();
        return removeJob(jobName, jobGroup, scheduler);
    }

    /**
     * Job removal utility
     *
     * @param jobName
     * @param jobGroup
     * @param scheduler
     * @return
     * @throws SchedulerException
     */
    private static boolean removeJob(
            final String jobName, final String jobGroup, final Scheduler scheduler)
            throws SchedulerException {
        final JobKey jobKey = new JobKey(jobName, jobGroup);
        return scheduler.deleteJob(jobKey);
    }

    /**
     * Pauses a job and all it associated triggers from all schedulers
     *
     * @param jobName
     * @param jobGroup
     * @return
     * @throws SchedulerException
     */
    public static void pauseJob(final String jobName, final String jobGroup)
            throws SchedulerException {
        final Scheduler scheduler = getScheduler();
        pauseJob(jobName, jobGroup, scheduler);
    }

    /**
     * Pauses a job and all it associated triggers from the standard schedulers
     *
     * @param jobName
     * @param jobGroup
     * @return
     * @throws SchedulerException
     */
    private static void pauseJob(
            final String jobName, final String jobGroup, final Scheduler scheduler)
            throws SchedulerException {
        final JobKey jobKey = new JobKey(jobName, jobGroup);
        scheduler.pauseJob(jobKey);
    }

    /**
     * Resumes a job and all it associated triggers from all schedulers
     *
     * @param jobName
     * @param jobGroup
     * @return
     * @throws SchedulerException
     */
    public static void resumeJob(String jobName, String jobGroup) throws SchedulerException {
        final Scheduler scheduler = getScheduler();
        resumeJob(jobName, jobGroup, scheduler);
    }

    /**
     * Resumes a job and all it associated triggers from a given schedulers
     *
     * @param jobName
     * @param jobGroup
     * @param scheduler
     * @return
     * @throws SchedulerException
     */
    private static void resumeJob(
            final String jobName, final String jobGroup, final Scheduler scheduler)
            throws SchedulerException {
        final JobKey jobKey = new JobKey(jobName, jobGroup);
        scheduler.resumeJob(jobKey);
    }

    /**
     * Pauses a trigger from all schedulers
     *
     * @param triggerName
     * @param triggerGroup
     * @return
     * @throws SchedulerException
     */
    public static void pauseTrigger(final String triggerName, final String triggerGroup)
            throws SchedulerException {
        final Scheduler scheduler = getScheduler();
        pauseTrigger(triggerName, triggerGroup, scheduler);
    }

    /**
     * Pause trigger for a given scheduler
     *
     * @param triggerName
     * @param triggerGroup
     * @param scheduler
     * @throws SchedulerException
     */
    private static void pauseTrigger(
            final String triggerName, final String triggerGroup, final Scheduler scheduler)
            throws SchedulerException {
        final TriggerKey triggerKey = new TriggerKey(triggerName, triggerGroup);
        scheduler.pauseTrigger(triggerKey);
    }

    /**
     * Trigger retrieval
     *
     * @param triggerName
     * @param triggerGroup
     * @return
     * @throws SchedulerException
     */
    public static Trigger getTrigger(final String triggerName, final String triggerGroup)
            throws SchedulerException {
        final TriggerKey triggerKey = new TriggerKey(triggerName, triggerGroup);
        return getScheduler().getTrigger(triggerKey);
    }

    /**
     * Resumes a trigger from all schedulers
     *
     * @param triggerName
     * @param triggerGroup
     * @return
     * @throws SchedulerException
     */
    public static void resumeTrigger(final String triggerName, final String triggerGroup)
            throws SchedulerException {
        final Scheduler scheduler = getScheduler();
        resumeTrigger(triggerName, triggerGroup, scheduler);
    }

    /**
     * Resumes a trigger from all schedulers
     *
     * @param triggerName
     * @param triggerGroup
     * @param scheduler
     * @return
     * @throws SchedulerException
     */
    private static void resumeTrigger(
            final String triggerName, final String triggerGroup, final Scheduler scheduler)
            throws SchedulerException {
        final TriggerKey triggerKey = new TriggerKey(triggerName, triggerGroup);
        scheduler.resumeTrigger(triggerKey);
    }

    /**
     * Temporarily pauses all schedulers from executing future triggers
     *
     * @throws SchedulerException
     */
    public static void pauseSchedulers() throws SchedulerException {
        final Scheduler scheduler = getScheduler();
        scheduler.standby();
    }

    /**
     * Temporarily pauses all schedulers from executing future triggers
     *
     * @throws SchedulerException
     */
    public static void pauseStandardSchedulers() throws SchedulerException {
        final Scheduler scheduler = getScheduler();
        scheduler.standby();
    }

    /**
     * Temporarily pauses all schedulers from executing future triggers
     *
     * @throws SchedulerException
     */
    public static void startSchedulers() throws SchedulerException {
        long start = System.currentTimeMillis();
        final Scheduler scheduler = getScheduler();
        scheduler.start();

        System.setProperty(
                WebKeys.DOTCMS_STARTUP_TIME_QUARTZ,
                String.valueOf(System.currentTimeMillis() - start));
    }

    /**
     * Verify if a job is running
     *
     * @param jobName
     * @param jobGroup
     * @return
     * @throws SchedulerException
     */
    public static boolean isJobRunning(final String jobName, final String jobGroup)
            throws SchedulerException {
        List<JobExecutionContext> currentlyExecutingJobs = new ArrayList<>();
        currentlyExecutingJobs.addAll(getScheduler().getCurrentlyExecutingJobs());

        JobKey jobKey = new JobKey(jobName, jobGroup);
        JobDetail existingJobDetail = getScheduler().getJobDetail(jobKey);

        if (existingJobDetail != null) {
            for (JobExecutionContext jec : currentlyExecutingJobs) {
                final JobDetail runningJobDetail = jec.getJobDetail();
                if (existingJobDetail.equals(runningJobDetail)
                        || isSameJob(existingJobDetail, runningJobDetail)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isJobRunning(JobKey jobKey) {
        try {
            for (JobExecutionContext jobExecutionContext : getScheduler().getCurrentlyExecutingJobs()) {
                if (jobExecutionContext.getJobDetail().getKey().equals(jobKey)) {
                    return true;
                }
            }
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to check if job is running", e);
        }
        return false;
    }

    /**
     * A more cluster-aware method to find out if a job is running since {@link #isJobRunning(String, String)} evaluates
     * for the current scheduler.
     *
     * @param scheduler scheduler to use
     * @param jobName   job name
     * @param jobGroup  job group
     * @param triggerName trigger name
     * @param triggerGroup trigger group
     * @return true if running job is detected, otherwise false
     */
	public static boolean isJobRunning(
			final Scheduler scheduler,
			final String jobName,
			final String jobGroup,
			final String triggerName,
			final String triggerGroup) {
		try {
			JobKey jobKey = new JobKey(jobName, jobGroup);
			List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);

			return triggers.stream()
					.anyMatch(
							trigger ->
									trigger.getKey().getName().equals(triggerName)
											&& trigger.getKey().getGroup().equals(triggerGroup));
		} catch (SchedulerException e) {
			return false;
		}
	}

    /**
     * Job comparison utility
     *
     * @param job1
     * @param job2
     * @return
     */
    private static boolean isSameJob(JobDetail job1, JobDetail job2) {
        try {
            Map<String, Object> m1 = job1.getJobDataMap();
            Map<String, Object> m2 = job2.getJobDataMap();

            for (String key : m1.keySet()) {
                if (m2.get(key) == null) {
                    return false;
                } else if (m1.get(key) instanceof String[] && m2.get(key) instanceof String[]) {
                    String[] x = (String[]) m1.get(key);
                    String[] y = (String[]) m2.get(key);
                    Arrays.sort(x);
                    Arrays.sort(y);
                    for (int i = 0; i < x.length; i++) {
                        if (!x[i].equals(y[i])) {
                            return false;
                        }
                    }
                } else if (!m1.get(key).equals(m2.get(key))) {
                    return false;
                }
            }

            return job1.getJobClass().equals(job2.getJobClass());
        } catch (Exception e) {
            return false;
        }
    }
}