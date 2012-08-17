package com.dotmarketing.quartz;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

/**
 * 
 * This utility class let you schedule and check scheduled task through the two configured dotCMS schedulers: 
 * - The standard scheduler
 * 		That let you run multiple task in parallel 
 * - The sequential scheduler 
 * 		That let you run only one task at a time, the order of the execution of the tasks is managed by quartz priorities
 *  
 * @author David Torres
 *
 */
public class QuartzUtils {
	

	
	private static Map<String, TaskRuntimeValues> runtimeTaskValues = new HashMap<String, TaskRuntimeValues>();
	
	/**
	 * 
	 * Lists all jobs scheduled through the sequential scheduler, the sequential scheduler
	 * let you run only one job at a time
	 * 
	 * @return
	 */
	public static List<ScheduledTask> getSequentialScheduledTasks() throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getSequentialScheduler();
		return getScheduledTasks(sched, true, null);
	}
	
	/**
	 * 
	 * Lists all jobs scheduled through the standard scheduler, the standard scheduler
	 * let you run multiple jobs in parallel
	 * 
	 * @return
	 */
	public static List<ScheduledTask> getStandardScheduledTasks() throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
		return getScheduledTasks(sched, false, null);
	}

	/**
	 * 
	 * Lists all jobs scheduled through the standard scheduler, the standard scheduler
	 * let you run multiple jobs in parallel
	 * 
	 * @return
	 */
	public static List<ScheduledTask> getScheduledTasks() throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getSequentialScheduler();
		List<ScheduledTask> jobs = getScheduledTasks(sched, true, null);
		sched = DotSchedulerFactory.getInstance().getScheduler();
		jobs.addAll(getScheduledTasks(sched, false, null));
		return jobs;
	}
		

	/**
	 * 
	 * Lists all jobs scheduled through the sequential scheduler that belong to the given group
	 * 
	 * @return
	 */
	public static List<ScheduledTask> getSequentialScheduledTasks(String group) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getSequentialScheduler();
		return getScheduledTasks(sched, true, group);
	}
	
	/**
	 * 
	 * Lists all jobs scheduled through the standard scheduler that belong to the given group
	 * 
	 * @return
	 */
	public static List<ScheduledTask> getStandardScheduledTasks(String group) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
		return getScheduledTasks(sched, false, group);
	}

	/**
	 * 
	 * Lists all jobs scheduled through the standard scheduler that belong to the given group
	 * 
	 * @return
	 */
	public static List<ScheduledTask> getScheduledTasks(String group) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getSequentialScheduler();
		List<ScheduledTask> jobs = getScheduledTasks(sched, true, group);
		sched = DotSchedulerFactory.getInstance().getScheduler();
		jobs.addAll(getScheduledTasks(sched, false, group));
		return jobs;
	}
	
	@SuppressWarnings("unchecked")
	private static List<ScheduledTask> getScheduledTasks(Scheduler sched, boolean sequential, String group) throws SchedulerException {
		
		List<ScheduledTask> result = new ArrayList<ScheduledTask>(100);

		String[] groupNames = sched.getJobGroupNames();
		String[] jobNames;
		JobDetail jobDetail;

		for (String groupName : groupNames) {
			
			if(group != null && !groupName.equals(group))
				continue;
			
			jobNames = sched.getJobNames(groupName);

			for (String jobName : jobNames) {
				jobDetail = sched.getJobDetail(jobName, groupName);
				Trigger[] triggers = sched.getTriggersOfJob(jobName, groupName);

				for (Trigger t : triggers) {
					ScheduledTask task = null;
					if (t instanceof CronTrigger) {
						task = new CronScheduledTask();
						((CronScheduledTask) task).setCronExpression(((CronTrigger) t).getCronExpression());
					} else if (t instanceof SimpleTrigger) {
						task = new SimpleScheduledTask();
						((SimpleScheduledTask) task).setRepeatCount(((SimpleTrigger) t).getRepeatCount());
						((SimpleScheduledTask) task).setRepeatInterval(((SimpleTrigger) t).getRepeatInterval());
					} else {
						continue;
					}
					task.setJobName(jobDetail.getName());
					task.setJobGroup(jobDetail.getGroup());
					task.setJobDescription(jobDetail.getDescription());
					task.setProperties((HashMap<String, Object>) jobDetail.getJobDataMap().getWrappedMap());

					task.setStartDate(t.getStartTime());
					task.setEndDate(t.getStartTime());
					task.setSequentialScheduled(sequential);

					result.add(task);
				}

			}
		}

		return result;
	}
	
	/**
	 * This methods checks all jobs configured either in the standard or the sequential schedulers that match
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
	public static List<ScheduledTask> getScheduledTask(String jobName, String jobGroup) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
		List<ScheduledTask> jobs = getScheduledTask(jobName, jobGroup, sched, false);
		sched = DotSchedulerFactory.getInstance().getSequentialScheduler();
		jobs.addAll(getScheduledTask(jobName, jobGroup, sched, true));
		return jobs;
	}
	
	/**
	 * This methods checks all jobs configured either in the standard scheduler that match
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
	public static List<ScheduledTask> getStandardScheduledTask(String jobName, String jobGroup) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
		return getScheduledTask(jobName, jobGroup, sched, false);
	}
	
	/**
	 * This methods checks all jobs configured either in the sequential scheduler that match
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
	public static List<ScheduledTask> getSequentialScheduledTask(String jobName, String jobGroup) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getSequentialScheduler();
		return getScheduledTask(jobName, jobGroup, sched, true);
	}
	
	@SuppressWarnings("unchecked")
	private static List<ScheduledTask> getScheduledTask(String jobName, String jobGroup, Scheduler sched, boolean sequential) throws SchedulerException {
		List<ScheduledTask> result = new ArrayList<ScheduledTask>(1);

		JobDetail jobDetail = sched.getJobDetail(jobName, jobGroup);

		if (jobDetail != null) {

			Trigger[] triggers = sched.getTriggersOfJob(jobName, jobGroup);
			for (Trigger t : triggers) {
				ScheduledTask task = null;
				if (t instanceof CronTrigger) {
					task = new CronScheduledTask();
					((CronScheduledTask) task).setCronExpression(((CronTrigger) t).getCronExpression());
				} else if (t instanceof SimpleTrigger) {
					task = new SimpleScheduledTask();
					((SimpleScheduledTask) task).setRepeatCount(((SimpleTrigger) t).getRepeatCount());
					((SimpleScheduledTask) task).setRepeatInterval(((SimpleTrigger) t).getRepeatInterval());
				} else {
					continue;
				}
				task.setJobName(jobDetail.getName());
				task.setJobGroup(jobDetail.getGroup());
				task.setJobDescription(jobDetail.getDescription());
				task.setProperties((HashMap<String, Object>) jobDetail.getJobDataMap().getWrappedMap());
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
	public static boolean isJobScheduled(String jobName, String jobGroup) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
		return isJobScheduled(jobName, jobGroup, sched);
	
	}

	/**
	 * Tells you whether the given jobName and jobGroup is set in the jobs DB to be sequentially executed or was executed and set with durability true
	 * so still exists in the DB
	 * 
	 * @param jobName
	 * @param jobGroup
	 * @return
	 * @throws SchedulerException
	 */
	public static boolean isJobSequentiallyScheduled(String jobName, String jobGroup) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getSequentialScheduler();
		return isJobScheduled(jobName, jobGroup, sched);
	}
	
	private static boolean isJobScheduled(String jobName, String jobGroup, Scheduler sched) throws SchedulerException {
		return sched.getJobDetail(jobName, jobGroup) != null;
		
	}
	
	/**
	 * Returns the current task progress, it returns -1 if no task progress is found
	 * 
	 * @param jobName
	 * @param jobGroup
	 * @param triggerName
	 * @param triggerGroup
	 * @return
	 * @throws SchedulerException
	 */
	public static int getTaskProgress(String jobName, String jobGroup) {
		TaskRuntimeValues runtimeValues = runtimeTaskValues.get(jobName + "-" + jobGroup);
		if(runtimeValues == null) return -1;
		return runtimeValues.currentProgress;
	}
	
	public static void updateTaskProgress(String jobName, String jobGroup, int progress) {
		TaskRuntimeValues runtimeValues = runtimeTaskValues.get(jobName + "-" + jobGroup);
		if(runtimeValues == null) return;
		runtimeValues.currentProgress = progress;
	}

	/**
	 * Returns a task starting point of progress, by default 0 unless the task set it to something different
	 * 
	 * @param jobName
	 * @param jobGroup
	 * @param triggerName
	 * @param triggerGroup
	 * @return
	 * @throws SchedulerException
	 */
	public static int getTaskStartProgress(String jobName, String jobGroup) {
		TaskRuntimeValues runtimeValues = runtimeTaskValues.get(jobName + "-" + jobGroup);
		if(runtimeValues == null) return -1;
		return runtimeValues.startProgress;
	}
	
	
	public static void setTaskStartProgress(String jobName, String jobGroup, int startProgress) {
		TaskRuntimeValues runtimeValues = runtimeTaskValues.get(jobName + "-" + jobGroup);
		if(runtimeValues == null) return;
		runtimeValues.startProgress = startProgress;
	}
	

	/**
	 * Returns a task ending point to track progress, by default 100 unless the task set it to something different
	 * 
	 * @param jobName
	 * @param jobGroup
	 * @param triggerName
	 * @param triggerGroup
	 * @return
	 * @throws SchedulerException
	 */
	public static int getTaskEndProgress(String jobName, String jobGroup) {
		TaskRuntimeValues runtimeValues = runtimeTaskValues.get(jobName + "-" + jobGroup);
		if(runtimeValues == null) return -1;
		return runtimeValues.endProgress;
	}
	
	
	public static  TaskRuntimeValues getTaskRuntimeValues(String jobName, String jobGroup) {

			
		return runtimeTaskValues.get(jobName + "-" + jobGroup);
	}
	
	public static void setTaskRuntimeValues(String jobName, String jobGroup, TaskRuntimeValues runtimeValues) {

		 runtimeTaskValues.put(jobName + "-" + jobGroup , runtimeValues);
	}
	

	public static void setTaskEndProgress(String jobName, String jobGroup, int endProgress) {
		TaskRuntimeValues runtimeValues = runtimeTaskValues.get(jobName + "-" + jobGroup);
		if(runtimeValues == null) return;
		runtimeValues.endProgress = endProgress;
		
	}

	/**
	 * Shuts down all schedulers
	 * @throws SchedulerException 
	 */
	public static void stopSchedulers() throws SchedulerException {
		Collection<Scheduler> list= DotSchedulerFactory.getInstance().getAllSchedulers();
		for (Scheduler s:list) {
			s.shutdown();
		}
		
		
		
	}
	
	/**
	 * Returns the current task progress, it returns -1 if no task progress is found
	 * 
	 * @param jobName
	 * @param jobGroup
	 * @param triggerName
	 * @param triggerGroup
	 * @return
	 * @throws SchedulerException
	 */
	public static List<String> getTaskMessages(String jobName, String jobGroup) {
		TaskRuntimeValues runtimeValues = runtimeTaskValues.get(jobName + "-" + jobGroup);
		if(runtimeValues == null) return null;
		return runtimeValues.messages;
	}

	public static void addTaskMessage(String jobName, String jobGroup, String newMessage) {
		TaskRuntimeValues runtimeValues = runtimeTaskValues.get(jobName + "-" + jobGroup);
		if(runtimeValues == null) return;
		runtimeValues.messages.add(newMessage);
	}

	public static void initializeTaskRuntimeValues(String jobName, String jobGroup) {
		runtimeTaskValues.put(jobName + "-" + jobGroup, new TaskRuntimeValues());
		
	}

	public static void removeTaskRuntimeValues(String jobName, String jobGroup) {
		runtimeTaskValues.remove(jobName + "-" + jobGroup);
	}	
	
	/**
	 * 
	 * This methods schedules the given job in the quartz system, and depending on the sequentialScheduled property
	 * it will use the sequential of the standard scheduler
	 * 
	 * @param job
	 * @return
	 * @throws SchedulerException 
	 * @throws ParseException 
	 * @throws ClassNotFoundException 
	 */
	public static void scheduleTask(ScheduledTask job) throws SchedulerException, ParseException, ClassNotFoundException {

		Scheduler sched;
		if (job.isSequentialScheduled())
			sched = DotSchedulerFactory.getInstance().getSequentialScheduler();
		else
			sched = DotSchedulerFactory.getInstance().getScheduler();

		JobDetail jobDetail;
		Trigger trigger;
		boolean isNew;

		isNew = false;

		String jobName = job.getJobName();
		String jobGroup = job.getJobGroup();
		String triggerName = job.getTriggerName() == null ? jobName + "_trigger" : job.getTriggerName();
		String triggerGroup = job.getTriggerGroup() == null ? jobGroup : job.getTriggerGroup();
		Date startDate = job.getStartDate();
		Date endDate = job.getEndDate();

		if ((jobDetail = sched.getJobDetail(jobName, jobGroup)) == null) {
			jobDetail = new JobDetail(jobName, jobGroup, Class.forName(job.getJavaClassName()));
			isNew = true;
		} else {
			jobDetail.setJobClass(Class.forName(job.getJavaClassName()));
		}

		JobDataMap dataMap = new JobDataMap(job.getProperties());
		
		jobDetail.setDescription(job.getJobDescription());
		jobDetail.setJobDataMap(dataMap);
		jobDetail.setDurability(job.getDurability());
		
		if (job instanceof CronScheduledTask) {
			trigger = new CronTrigger(triggerName, triggerGroup, jobName, jobGroup, startDate, endDate, ((CronScheduledTask) job).getCronExpression());			
		} else {
			trigger = new SimpleTrigger(triggerName, triggerGroup, jobName, jobGroup, startDate, endDate, ((SimpleScheduledTask) job).getRepeatCount(),
					((SimpleScheduledTask) job).getRepeatInterval());
		}
		trigger.setMisfireInstruction(job.getMisfireInstruction());

		sched.addJob(jobDetail, true);

		if (isNew)
			sched.scheduleJob(trigger);
		else if (sched.getTrigger(triggerName, triggerGroup) != null)
			sched.rescheduleJob(triggerName, triggerGroup, trigger);
		else {
			try {
				sched.scheduleJob(trigger);
			} catch (Exception e) {
				sched.rescheduleJob(triggerName, triggerGroup, trigger);
			}
		}
		
		QuartzUtils.initializeTaskRuntimeValues(jobName, jobGroup);


	}

	/**
	 * 
	 * Removes the job and all associated triggers from both schedulers the standard and the sequential
	 * 
	 * @param jobName
	 * @param jobGroup
	 * @return true if the job is found and removed in at least one of the schedulers, false otherwise 
	 * @throws SchedulerException 
	 * 
	 */
	public static boolean removeJob(String jobName, String jobGroup) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getSequentialScheduler();
		boolean result1 = removeJob(jobName, jobGroup, sched);
		sched = DotSchedulerFactory.getInstance().getScheduler();
		boolean result2 = removeJob(jobName, jobGroup, sched);
		return result1 | result2;
	}

	/**
	 * 
	 * Removes the job and all associated triggers from the standard jobs scheduler
	 * 
	 * @param jobName
	 * @param jobGroup
	 * @return true if the job is found and removed in at least one of the schedulers, false otherwise 
	 * @throws SchedulerException 
	 * 
	 */
	public static boolean removeStandardJob(String jobName, String jobGroup) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
		return removeJob(jobName, jobGroup, sched);
	}

	/**
	 * 
	 * Removes the job and all associated triggers from the sequential jobs scheduler
	 * 
	 * @param jobName
	 * @param jobGroup
	 * @return true if the job is found and removed in at least one of the schedulers, false otherwise 
	 * @throws SchedulerException 
	 * 
	 */
	public static boolean removeSequentialJob(String jobName, String jobGroup) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
		return removeJob(jobName, jobGroup, sched);
	}
	
	private static boolean removeJob(String jobName, String jobGroup, Scheduler sched) throws SchedulerException {
		return sched.deleteJob(jobName, jobGroup);
	}
	
	/**
	 * Pauses a job and all it associated triggers from all schedulers
	 * @param jobName
	 * @param jobGroup
	 * @return
	 * @throws SchedulerException 
	 */
	public static void pauseJob(String jobName, String jobGroup) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
		pauseJob(jobName, jobGroup, sched);
		sched = DotSchedulerFactory.getInstance().getSequentialScheduler();
		pauseJob(jobName, jobGroup, sched);
	}
	
	/**
	 * Pauses a job and all it associated triggers from the standard schedulers
	 * @param jobName
	 * @param jobGroup
	 * @return
	 * @throws SchedulerException 
	 */
	public static void pauseStandardJob(String jobName, String jobGroup) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
		pauseJob(jobName, jobGroup, sched);
	}
	
	/**
	 * Pauses a job and all it associated triggers from the sequential schedulers
	 * @param jobName
	 * @param jobGroup
	 * @return
	 * @throws SchedulerException 
	 */
	public static void pauseSequentialJob(String jobName, String jobGroup) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getSequentialScheduler();
		pauseJob(jobName, jobGroup, sched);
	}
	
	private static void pauseJob(String jobName, String jobGroup, Scheduler sched) throws SchedulerException {
		sched.pauseJob(jobName, jobGroup);
	}

	
	
	/**
	 * Pauses a job and all it associated triggers from all schedulers
	 * @param jobName
	 * @param jobGroup
	 * @return
	 * @throws SchedulerException 
	 */
	public static void resumeJob(String jobName, String jobGroup) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
		resumeJob(jobName, jobGroup, sched);
		sched = DotSchedulerFactory.getInstance().getSequentialScheduler();
		resumeJob(jobName, jobGroup, sched);
	}
	
	/**
	 * Pauses a job and all it associated triggers from the standard schedulers
	 * @param jobName
	 * @param jobGroup
	 * @return
	 * @throws SchedulerException 
	 */
	public static void resumeStandardJob(String jobName, String jobGroup) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
		resumeJob(jobName, jobGroup, sched);
	}
	
	/**
	 * Pauses a job and all it associated triggers from the sequential schedulers
	 * @param jobName
	 * @param jobGroup
	 * @return
	 * @throws SchedulerException 
	 */
	public static void resumeSequentialJob(String jobName, String jobGroup) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getSequentialScheduler();
		resumeJob(jobName, jobGroup, sched);
	}
	
	private static void resumeJob(String jobName, String jobGroup, Scheduler sched) throws SchedulerException {
		sched.resumeJob(jobName, jobGroup);
	}
	
	/**
	 * Pauses a trigger from all schedulers
	 * @param triggerName
	 * @param triggerGroup
	 * @return
	 * @throws SchedulerException 
	 */
	public static void pauseTrigger(String triggerName, String triggerGroup) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
		pauseTrigger(triggerName, triggerGroup, sched);
		sched = DotSchedulerFactory.getInstance().getSequentialScheduler();
		pauseTrigger(triggerName, triggerGroup, sched);
	}
	
	/**
	 * Pauses a trigger from the standard scheduler
	 * @param triggerName
	 * @param triggerGroup
	 * @return
	 * @throws SchedulerException 
	 */
	public static void pauseStandardTrigger(String triggerName, String triggerGroup) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
		pauseTrigger(triggerName, triggerGroup, sched);
	}
	
	/**
	 * Pauses a trigger from the sequential scheduler
	 * @param triggerName
	 * @param triggerGroup
	 * @return
	 * @throws SchedulerException 
	 */
	public static void pauseSequentialTrigger(String triggerName, String triggerGroup) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getSequentialScheduler();
		pauseTrigger(triggerName, triggerGroup, sched);
	}
	
	private static void pauseTrigger(String triggerName, String triggerGroup, Scheduler sched) throws SchedulerException {
		sched.pauseTrigger(triggerName, triggerGroup);
	}

	/**
	 * 
	 * @param triggerName
	 * @param triggerGroup
	 * @return
	 * @throws SchedulerException
	 */
	public static Trigger getTrigger(String triggerName, String triggerGroup) throws SchedulerException {
		Trigger t = getSequentialScheduler ().getTrigger(triggerName, triggerGroup);
		if(t==null){
			t = getStandardScheduler () .getTrigger(triggerName, triggerGroup);
			
		}
		return t;
	}
	
	/**
	 * Resumes a trigger from all schedulers
	 * @param triggerName
	 * @param triggerGroup
	 * @return
	 * @throws SchedulerException 
	 */
	public static void resumeTrigger(String triggerName, String triggerGroup) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
		resumeTrigger(triggerName, triggerGroup, sched);
		sched = DotSchedulerFactory.getInstance().getSequentialScheduler();
		resumeTrigger(triggerName, triggerGroup, sched);
	}
	
	/**
	 * Resumes a trigger from the standard scheduler
	 * @param triggerName
	 * @param triggerGroup
	 * @return
	 * @throws SchedulerException 
	 */
	public static void resumeStandardTrigger(String triggerName, String triggerGroup) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
		resumeTrigger(triggerName, triggerGroup, sched);
	}
	
	/**
	 * Resumes a trigger from the sequential scheduler
	 * @param triggerName
	 * @param triggerGroup
	 * @return
	 * @throws SchedulerException 
	 */
	public static void resumeSequentialTrigger(String triggerName, String triggerGroup) throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getSequentialScheduler();
		resumeTrigger(triggerName, triggerGroup, sched);
	}
	
	private static void resumeTrigger(String triggerName, String triggerGroup, Scheduler sched) throws SchedulerException {
		sched.resumeTrigger(triggerName, triggerGroup);
	}
	
	/**
	 * Temporarily pauses all schedulers from executing future triggers
	 * @throws SchedulerException 
	 */
	public static void pauseSchedulers () throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
		sched.standby();
		sched = DotSchedulerFactory.getInstance().getSequentialScheduler();
		sched.standby();
	}
	
	/**
	 * Temporarily pauses all schedulers from executing future triggers
	 * @throws SchedulerException 
	 */
	public static void pauseStandardSchedulers () throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
		sched.standby();
	}

	/**
	 * Temporarily pauses all schedulers from executing future triggers
	 * @throws SchedulerException 
	 */
	public static void pauseSequentialSchedulers () throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getSequentialScheduler();
		sched.standby();
	}
	
	
	/**
	 * Temporarily pauses all schedulers from executing future triggers
	 * @throws SchedulerException 
	 */
	public static void startSchedulers () throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
		sched.start();
		sched = DotSchedulerFactory.getInstance().getSequentialScheduler();
		sched.start();
	}
	
	/**
	 * Temporarily pauses all schedulers from executing future triggers
	 * @throws SchedulerException 
	 */
	public static void startStandardSchedulers () throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
		sched.start();
	}

	/**
	 * Temporarily pauses all schedulers from executing future triggers
	 * @throws SchedulerException 
	 */
	public static void startSequentialSchedulers () throws SchedulerException {
		Scheduler sched = DotSchedulerFactory.getInstance().getSequentialScheduler();
		sched.start();
	}	

	/**
	 * Returns you the standard quartz scheduler class that let have more control over jobs and triggers
	 * @return
	 * @throws SchedulerException 
	 */
	public static Scheduler getStandardScheduler () throws SchedulerException {
		return DotSchedulerFactory.getInstance().getScheduler();
	}

	
	/**
	 * Returns you the sequential quartz scheduler class that let have more control over jobs and triggers
	 * @return
	 * @throws SchedulerException 
	 */
	public static Scheduler getSequentialScheduler () throws SchedulerException {
		return DotSchedulerFactory.getInstance().getSequentialScheduler();
	}
	
	public static boolean isJobRunning(String jobName, String jobGroup) throws SchedulerException{
		
		List<JobExecutionContext> currentlyExecutingJobs = new ArrayList<JobExecutionContext>();
		currentlyExecutingJobs.addAll(getSequentialScheduler().getCurrentlyExecutingJobs());
		currentlyExecutingJobs.addAll(getStandardScheduler().getCurrentlyExecutingJobs());

		JobDetail existingJobDetail = getSequentialScheduler().getJobDetail(jobName, jobGroup);

		if (existingJobDetail == null) {
			existingJobDetail = getStandardScheduler().getJobDetail(jobName, jobGroup);
		}
		if (existingJobDetail != null) {
	        for (JobExecutionContext jec : currentlyExecutingJobs) {
	        	JobDetail runningJobDetail = jec.getJobDetail();
	            if(existingJobDetail.equals(runningJobDetail) || isSameJob(existingJobDetail, runningJobDetail)) {
	                return true;
	            }
	        }
		}


		return false;
		

	}
	
	/**
	 * 
	 * @param job1
	 * @param job2
	 * @return
	 */
	private static boolean isSameJob(JobDetail job1, JobDetail job2){
		
		
		
		
		
		
		try{
			Map<String, Object> m1 = job1.getJobDataMap();
			Map<String, Object> m2 = job2.getJobDataMap();
			
			for(String key : m1.keySet()){
				if(m2.get(key) == null ){
					return false;
					
				}
				else if (m1.get(key) instanceof String[] && m2.get(key) instanceof String[]){
					
					String[] x = (String[]) m1.get(key);
					String[] y = (String[]) m2.get(key);
					Arrays.sort(x);
					Arrays.sort(y);
					
					
					
					for(int i=0;i<x.length;i++){
						if(!x[i].equals(y[i])){
							return false;
						}
						
					}
					
					
				}
				else if(! m1.get(key).equals(m2.get(key))){
					

					return false;
				}
				
			}
			
			if(!job1.getJobClass().equals(job2.getJobClass())){
				return false;
			}
			
			//if(job1.
			
			return true;
		}
		catch(Exception e){
			return false;
		}
		
	}
	
	

}