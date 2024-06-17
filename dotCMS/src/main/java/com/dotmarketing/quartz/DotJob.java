package com.dotmarketing.quartz;

import java.util.List;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

public abstract class DotJob implements Job {

	private JobExecutionContext context;

	protected long getProgress() throws SchedulerException {
		return QuartzUtils.getTaskProgress(context.getJobDetail().getKey().getName(), context.getJobDetail().getKey().getGroup());
	}

	protected void updateProgress(int currentProgress) {
		QuartzUtils.updateTaskProgress(context.getJobDetail().getKey().getName(), context.getJobDetail().getKey().getGroup(), currentProgress);
	}

	protected long getStartProgress() throws SchedulerException {
		return QuartzUtils.getTaskStartProgress(context.getJobDetail().getKey().getName(), context.getJobDetail().getKey().getGroup());
	}

	protected void setStartProgress(int startProgress) {
		QuartzUtils.setTaskStartProgress(context.getJobDetail().getKey().getName(), context.getJobDetail().getKey().getGroup(), startProgress);
	}

	protected long getEndProgress() {
		return QuartzUtils.getTaskEndProgress(context.getJobDetail().getKey().getName(), context.getJobDetail().getKey().getGroup());
	}

	protected void setEndProgress(int endProgress) {
		QuartzUtils.setTaskEndProgress(context.getJobDetail().getKey().getName(), context.getJobDetail().getKey().getGroup(), endProgress);
	}

	protected List<String> getJobMessages() {
		return QuartzUtils.getTaskMessages(context.getJobDetail().getKey().getName(), context.getJobDetail().getKey().getGroup());
	}

	protected void addMessage(String newMessage) {
		QuartzUtils.addTaskMessage(context.getJobDetail().getKey().getName(), context.getJobDetail().getKey().getGroup(), newMessage);
	}

	@Override
	public void execute(JobExecutionContext jobContext) throws JobExecutionException {
		context = jobContext;
		QuartzUtils.initializeTaskRuntimeValues(context.getJobDetail().getKey().getName(), context.getJobDetail().getKey().getGroup());
		try {
			this.run(jobContext);
		} finally {
			QuartzUtils.removeTaskRuntimeValues(context.getJobDetail().getKey().getName(), context.getJobDetail().getKey().getGroup());
		}
	}

	public abstract void run(JobExecutionContext jobContext) throws JobExecutionException;
}