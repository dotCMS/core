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
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;

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
	

	
	private static final Map<String, TaskRuntimeValues> runtimeTaskValues = new HashMap<>();
	


	/**
	 * 
	 * Lists all jobs scheduled through the standard scheduler, the standard scheduler
	 * let you run multiple jobs in parallel
	 *
	 * @return
	 */
	public static List<ScheduledTask> getScheduledTasks() throws SchedulerException {
		final Scheduler scheduler = getScheduler();
		List<ScheduledTask> jobs = getScheduledTasks(scheduler, true, null);
		return jobs;
	}

	/**
	 * Want to get an instance of the scheduler?  please use this method.
	 * @return
	 */
	public static Scheduler getScheduler() {
	    try {
	    return DotSchedulerFactory.getInstance().getScheduler();
	    }
	    catch(Exception e) {
	        Logger.warnAndDebug(QuartzUtils.class, e);
	        throw new DotRuntimeException(e);
	    }
	}

	
	
	
	/**
	 * 
	 * Lists all jobs scheduled through the standard scheduler that belong to the given group
	 * 
	 * @return
	 */
	public static List<ScheduledTask> getScheduledTasks(final String group) throws SchedulerException {
		final Scheduler scheduler =getScheduler();
		List<ScheduledTask> jobs = getScheduledTasks(scheduler, true, group);

		return jobs;
	}

	/**
	 * sheduled tasks getter
	 * @param scheduler
	 * @param sequential
	 * @param group
	 * @return
	 * @throws SchedulerException
	 */
	@SuppressWarnings("unchecked")
	private static List<ScheduledTask> getScheduledTasks(final Scheduler scheduler, final boolean sequential, final String group) throws SchedulerException {
		
		final List<ScheduledTask> result = new ArrayList<>(100);

		final String[] groupNames = scheduler.getJobGroupNames();
		String[] jobNames;
		JobDetail jobDetail;

		for (final String groupName : groupNames) {
			
			if(group != null && !groupName.equals(group))
				continue;
			
			jobNames = scheduler.getJobNames(groupName);

			for (final String jobName : jobNames) {
				jobDetail = scheduler.getJobDetail(jobName, groupName);
				Trigger[] triggers = scheduler.getTriggersOfJob(jobName, groupName);

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
					task.setJavaClassName(jobDetail.getJobClass().getName());

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
	public static List<ScheduledTask> getScheduledTask(final String jobName, final String jobGroup) throws SchedulerException {
		final Scheduler scheduler = getScheduler();
		return getScheduledTask(jobName, jobGroup, scheduler, false);
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
	public static List<ScheduledTask> getStandardScheduledTask(final String jobName, final String jobGroup) throws SchedulerException {
		final Scheduler scheduler = getScheduler();
		return getScheduledTask(jobName, jobGroup, scheduler, false);
	}

	/**
	 * scheduled task getter
	 * @param jobName
	 * @param jobGroup
	 * @param scheduler
	 * @param sequential
	 * @return
	 * @throws SchedulerException
	 */
	@SuppressWarnings("unchecked")
	private static List<ScheduledTask> getScheduledTask(final String jobName, final String jobGroup, final Scheduler scheduler, final boolean sequential) throws SchedulerException {
		List<ScheduledTask> result = new ArrayList<>(1);

		final JobDetail jobDetail = scheduler.getJobDetail(jobName, jobGroup);

		if (jobDetail != null) {

			final Trigger[] triggers = scheduler.getTriggersOfJob(jobName, jobGroup);
			for (final Trigger t : triggers) {
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
	public static boolean isJobScheduled(final String jobName, final String jobGroup) throws SchedulerException {
		final Scheduler scheduler = getScheduler();
		return isJobScheduled(jobName, jobGroup, scheduler);
	
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
	@Deprecated
	public static boolean isJobSequentiallyScheduled(final String jobName, final String jobGroup) throws SchedulerException {

		return false;
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
	private static boolean isJobScheduled(final String jobName, final String jobGroup, final Scheduler scheduler) throws SchedulerException {
		return scheduler.getJobDetail(jobName, jobGroup) != null;
		
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
		if(runtimeValues == null) return -1;
		return runtimeValues.currentProgress;
	}

	/**
	 * updates task progress
	 * @param jobName
	 * @param jobGroup
	 * @param progress
	 */
	public static void updateTaskProgress(final String jobName, final String jobGroup, final int progress) {
		final TaskRuntimeValues runtimeValues = runtimeTaskValues.get(jobName + "-" + jobGroup);
		if(runtimeValues == null) return;
		runtimeValues.currentProgress = progress;
	}

	/**
	 * Returns a task starting point of progress, by default 0 unless the task set it to something different
	 * 
	 * @param jobName
	 * @param jobGroup
	 * @return
	 * @throws SchedulerException
	 */
	public static int getTaskStartProgress(final String jobName, final String jobGroup) {
		TaskRuntimeValues runtimeValues = runtimeTaskValues.get(jobName + "-" + jobGroup);
		if(runtimeValues == null) return -1;
		return runtimeValues.startProgress;
	}

	/**
	 * sets a task  point of progress
	 * @param jobName
	 * @param jobGroup
	 * @param startProgress
	 */
	public static void setTaskStartProgress(final String jobName, final String jobGroup, final int startProgress) {
		TaskRuntimeValues runtimeValues = runtimeTaskValues.get(jobName + "-" + jobGroup);
		if(runtimeValues == null) return;
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
		if(runtimeValues == null) return -1;
		return runtimeValues.endProgress;
	}

	/**
	 * Task runtime value getter
	 * @param jobName
	 * @param jobGroup
	 * @return
	 */
	public static  TaskRuntimeValues getTaskRuntimeValues(final String jobName, final String jobGroup) {

			
		return runtimeTaskValues.get(jobName + "-" + jobGroup);
	}

	/**
	 * register a runtime task
	 * @param jobName
	 * @param jobGroup
	 * @param runtimeValues
	 */
	public static void setTaskRuntimeValues(final String jobName, final String jobGroup, final TaskRuntimeValues runtimeValues) {

		 runtimeTaskValues.put(jobName + "-" + jobGroup , runtimeValues);
	}

	/**
	 * task end-porgress value setter
 	 * @param jobName
	 * @param jobGroup
	 * @param endProgress
	 */
	public static void setTaskEndProgress(final String jobName, final String jobGroup, final int endProgress) {
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
	 * @return
	 * @throws SchedulerException
	 */
	public static List<String> getTaskMessages(final String jobName, final String jobGroup) {
		final TaskRuntimeValues runtimeValues = runtimeTaskValues.get(jobName + "-" + jobGroup);
		if(runtimeValues == null) return null;
		return runtimeValues.messages;
	}

	/**
	 * add a task message
	 * @param jobName
	 * @param jobGroup
	 * @param newMessage
	 */
	public static void addTaskMessage(final String jobName, final String jobGroup,final String newMessage) {
		TaskRuntimeValues runtimeValues = runtimeTaskValues.get(jobName + "-" + jobGroup);
		if(runtimeValues == null) return;
		runtimeValues.messages.add(newMessage);
	}

	/**
	 * initializer
	 * @param jobName
	 * @param jobGroup
	 */
	public static void initializeTaskRuntimeValues(final String jobName, final String jobGroup) {
		runtimeTaskValues.put(jobName + "-" + jobGroup, new TaskRuntimeValues());
		
	}

	/**
	 * removes from the map the Task
	 * @param jobName
	 * @param jobGroup
	 */
	public static void removeTaskRuntimeValues(final String jobName, final String jobGroup) {
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
	public static void scheduleTask(final ScheduledTask job) throws SchedulerException, ParseException, ClassNotFoundException {

		final Scheduler scheduler = getScheduler();

		JobDetail jobDetail;
		Trigger trigger;
		boolean isNew;

		isNew = false;

		final String jobName = job.getJobName();
		final String jobGroup = job.getJobGroup();
		final String triggerName = job.getTriggerName() == null ? jobName + "_trigger" : job.getTriggerName();
		final String triggerGroup = job.getTriggerGroup() == null ? jobGroup : job.getTriggerGroup();
		final Date startDate = job.getStartDate();
		final Date endDate = job.getEndDate();

		if ((jobDetail = scheduler.getJobDetail(jobName, jobGroup)) == null) {
			jobDetail = new JobDetail(jobName, jobGroup, Class.forName(job.getJavaClassName()));
			isNew = true;
		} else {
			jobDetail.setJobClass(Class.forName(job.getJavaClassName()));
		}

		final JobDataMap dataMap = new JobDataMap(job.getProperties());
		
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

		scheduler.addJob(jobDetail, true);

		if (isNew) {
			scheduler.scheduleJob(trigger);
		} else if (scheduler.getTrigger(triggerName, triggerGroup) != null) {
			scheduler.rescheduleJob(triggerName, triggerGroup, trigger);
		} else {
			try {
				scheduler.scheduleJob(trigger);
			} catch (Exception e) {
				scheduler.rescheduleJob(triggerName, triggerGroup, trigger);
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
	public static boolean removeJob(final String jobName, final String jobGroup) throws SchedulerException {
		final Scheduler scheduler = getScheduler();




		return removeJob(jobName, jobGroup, scheduler);
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
	public static boolean removeStandardJob(final String jobName, final String jobGroup) throws SchedulerException {
		final Scheduler scheduler = getScheduler();
		return removeJob(jobName, jobGroup, scheduler);
	}


	/**
	 * This method avoids all the Quartz madness and just delete the job from the db
	 * @param jobName
	 * @param jobGroup
	 * @return
	 * @throws DotDataException
	 */
	@WrapInTransaction
	public static boolean deleteJobDB(final String jobName, final String jobGroup) throws DotDataException {
		final DotConnect db = new DotConnect();

		final List<Map<String, Object>> results = db.setSQL("select trigger_name,trigger_group from qrtz_excl_triggers  where job_name=? and job_group=?")
				.addParam(jobName)
				.addParam(jobGroup)
				.loadObjectResults();

		for (final Map<String,Object> map : results) {
			final String triggerName = map.get("trigger_name").toString();
			final String triggerGroup = map.get("trigger_group").toString();

			db.setSQL("delete from qrtz_excl_cron_triggers where trigger_name=? and trigger_group=?")
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


			db.setSQL("delete from qrtz_excl_trigger_listeners where trigger_name=? and trigger_group=?")
					.addParam(triggerName)
					.addParam(triggerGroup)
					.loadResult();

			db.setSQL("delete from qrtz_excl_simple_triggers where trigger_name=? and trigger_group=?")
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
	 * 
	 * Removes the job and all associated triggers from the sequential jobs scheduler
	 * 
	 * @param jobName
	 * @param jobGroup
	 * @return true if the job is found and removed in at least one of the schedulers, false otherwise 
	 * @throws SchedulerException 
	 * 
	 */
	public static boolean removeSequentialJob(final String jobName, final String jobGroup) throws SchedulerException {
		final Scheduler scheduler = getScheduler();
		return removeJob(jobName, jobGroup, scheduler);
	}

	/**
	 * job removal utility
	 * @param jobName
	 * @param jobGroup
	 * @param scheduler
	 * @return
	 * @throws SchedulerException
	 */
	private static boolean removeJob(final String jobName, final String jobGroup, final Scheduler scheduler) throws SchedulerException {
		return scheduler.deleteJob(jobName, jobGroup);
	}
	
	/**
	 * Pauses a job and all it associated triggers from all schedulers
	 * @param jobName
	 * @param jobGroup
	 * @return
	 * @throws SchedulerException 
	 */
	public static void pauseJob(final String jobName, final String jobGroup) throws SchedulerException {
		final Scheduler scheduler = getScheduler();
		pauseJob(jobName, jobGroup, scheduler);


	}
	
	/**
	 * Pauses a job and all it associated triggers from the standard schedulers
	 * @param jobName
	 * @param jobGroup
	 * @return
	 * @throws SchedulerException 
	 */

	private static void pauseJob(final String jobName, final String jobGroup, final Scheduler scheduler) throws SchedulerException {
		scheduler.pauseJob(jobName, jobGroup);
	}

	
	
	/**
	 * Pauses a job and all it associated triggers from all schedulers
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
	 * Pauses a job and all it associated triggers from a given schedulers
	 * @param jobName
	 * @param jobGroup
	 * @param scheduler
	 * @throws SchedulerException
	 */
	private static void resumeJob(final String jobName, final String jobGroup, final Scheduler scheduler) throws SchedulerException {
		scheduler.resumeJob(jobName, jobGroup);
	}
	
	/**
	 * Pauses a trigger from all schedulers
	 * @param triggerName
	 * @param triggerGroup
	 * @return
	 * @throws SchedulerException 
	 */
	public static void pauseTrigger(final String triggerName, final String triggerGroup) throws SchedulerException {
		final Scheduler scheduler = getScheduler();
		pauseTrigger(triggerName, triggerGroup, scheduler);

	}

	/**
	 * Pause trigger for a given scheduler
	 * @param triggerName
	 * @param triggerGroup
	 * @param scheduler
	 * @throws SchedulerException
	 */
	private static void pauseTrigger(final String triggerName, final String triggerGroup, final Scheduler scheduler) throws SchedulerException {
		scheduler.pauseTrigger(triggerName, triggerGroup);
	}

	/**
	 * Trigger retrieval
	 * @param triggerName
	 * @param triggerGroup
	 * @return
	 * @throws SchedulerException
	 */
	public static Trigger getTrigger(final String triggerName, final String triggerGroup) throws SchedulerException {
		return getScheduler() .getTrigger(triggerName, triggerGroup);
	}
	
	/**
	 * Resumes a trigger from all schedulers
	 * @param triggerName
	 * @param triggerGroup
	 * @return
	 * @throws SchedulerException 
	 */
	public static void resumeTrigger(final String triggerName, final String triggerGroup) throws SchedulerException {
		final Scheduler scheduler = getScheduler();
		resumeTrigger(triggerName, triggerGroup, scheduler);

	}

	/**
	 * Resumes a trigger from all schedulers
	 * @param triggerName
	 * @param triggerGroup
	 * @param scheduler
	 * @return
	 * @throws SchedulerException
	 */
	private static void resumeTrigger(final String triggerName, final String triggerGroup, final Scheduler scheduler) throws SchedulerException {
		scheduler.resumeTrigger(triggerName, triggerGroup);
	}
	
	/**
	 * Temporarily pauses all schedulers from executing future triggers
	 * @throws SchedulerException 
	 */
	public static void pauseSchedulers () throws SchedulerException {
		final Scheduler scheduler = getScheduler();
		scheduler.standby();

	}
	
	/**
	 * Temporarily pauses all schedulers from executing future triggers
	 * @throws SchedulerException 
	 */
	public static void pauseStandardSchedulers () throws SchedulerException {
		final Scheduler scheduler = getScheduler();
		scheduler.standby();
	}



	/**
	 * Temporarily pauses all schedulers from executing future triggers
	 * @throws SchedulerException 
	 */
	public static void startSchedulers () throws SchedulerException {
		long start = System.currentTimeMillis();
		final Scheduler scheduler = getScheduler();
		scheduler.start();

		System.setProperty(WebKeys.DOTCMS_STARTUP_TIME_QUARTZ, String.valueOf(System.currentTimeMillis() - start));
	}


	/**
	 * verify if a job is running
	 * @param jobName
	 * @param jobGroup
	 * @return
	 * @throws SchedulerException
	 */
	public static boolean isJobRunning(final String jobName, final String jobGroup) throws SchedulerException{

		List<JobExecutionContext> currentlyExecutingJobs = new ArrayList<>();
		currentlyExecutingJobs.addAll(getScheduler().getCurrentlyExecutingJobs());


		JobDetail existingJobDetail = getScheduler().getJobDetail(jobName, jobGroup);


		if (existingJobDetail != null) {
	        for (JobExecutionContext jec : currentlyExecutingJobs) {
	        	final JobDetail runningJobDetail = jec.getJobDetail();
	            if (existingJobDetail.equals(runningJobDetail) || isSameJob(existingJobDetail, runningJobDetail)) {
	                return true;
	            }
	        }
		}

		return false;
	}

	/**
	 * A more cluster aware method to find out if a job is running since {@link #isJobRunning(String, String) evaluates
	 * for the current scheduler. @param scheduler scheduler to use
	 * @param jobName job name
	 * @param jobGroup job group
	 * @param triggerName trigger name
	 * @param triggerGroup trigger group
	 * @return true if running job is detected, otherwise false
	 */
	public static boolean isJobRunning(final Scheduler scheduler,
									   final String jobName,
									   final String jobGroup,
									   final String triggerName,
									   final String triggerGroup) {
		try {
			return Arrays
					.stream(scheduler.getTriggersOfJob(jobName, jobGroup))
					.anyMatch(trigger -> trigger.getName().equals(triggerName) &&
							trigger.getGroup().equals(triggerGroup));
		} catch (SchedulerException e) {
			return false;
		}
	}
	
	/**
	 * job comparision util
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
