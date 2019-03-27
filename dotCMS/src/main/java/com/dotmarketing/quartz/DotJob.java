package com.dotmarketing.quartz;

import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

public abstract class DotJob implements Job {

	private JobExecutionContext context;
	
	protected long getProgress() throws SchedulerException {
		return QuartzUtils.getTaskProgress(context.getJobDetail().getName(), context.getJobDetail().getGroup());
	}
	protected void updateProgress(int currentProgress) {
		QuartzUtils.updateTaskProgress(context.getJobDetail().getName(), context.getJobDetail().getGroup(), currentProgress);
	}
	protected long getStartProgress() throws SchedulerException {
		return QuartzUtils.getTaskStartProgress(context.getJobDetail().getName(), context.getJobDetail().getGroup());
	}
	protected void setStartProgress(int startProgress) {
		QuartzUtils.setTaskStartProgress(context.getJobDetail().getName(), context.getJobDetail().getGroup(), startProgress);
	}
	protected long getEndProgress() {
		return QuartzUtils.getTaskEndProgress(context.getJobDetail().getName(), context.getJobDetail().getGroup());
	}
	protected void setEndProgress(int endProgress) {
		QuartzUtils.setTaskEndProgress(context.getJobDetail().getName(), context.getJobDetail().getGroup(), endProgress);
	}
	protected List<String> getJobMessages() {
		return QuartzUtils.getTaskMessages(context.getJobDetail().getName(), context.getJobDetail().getGroup());
	}
	protected void addMessage(String newMessage) {
		QuartzUtils.addTaskMessage(context.getJobDetail().getName(), context.getJobDetail().getGroup(), newMessage);
	}
	
	public void execute(JobExecutionContext jobContext) throws JobExecutionException {
		context = jobContext;
		QuartzUtils.initializeTaskRuntimeValues(context.getJobDetail().getName(), context.getJobDetail().getGroup());
		this.run(jobContext);
		QuartzUtils.removeTaskRuntimeValues(context.getJobDetail().getName(), context.getJobDetail().getGroup());
	}
	
	public abstract void run(JobExecutionContext jobContext) throws JobExecutionException;

}
