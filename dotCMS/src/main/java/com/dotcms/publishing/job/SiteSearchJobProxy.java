package com.dotcms.publishing.job;

import com.dotcms.business.CloseDBIfOpened;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotcms.enterprise.publishing.sitesearch.SiteSearchPublishStatus;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.TaskRuntimeValues;
import com.dotmarketing.util.Logger;

public class SiteSearchJobProxy extends DotStatefulJob {

	@Override
	@CloseDBIfOpened
	public void run(JobExecutionContext jobContext) throws JobExecutionException {
		SiteSearchJobImpl jobImpl = new SiteSearchJobImpl();
		SiteSearchPublishStatus status = getOrCreateStatus(jobContext);

		jobImpl.setStatus(status);

		try {
			jobImpl.run(jobContext);
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new JobExecutionException(e);
		}
	}

	private SiteSearchPublishStatus getOrCreateStatus(JobExecutionContext jobContext) {
		String jobName = jobContext.getJobDetail().getKey().getName();
		String jobGroup = jobContext.getJobDetail().getKey().getGroup();
		TaskRuntimeValues trv = QuartzUtils.getTaskRuntimeValues(jobName, jobGroup);

		if (trv == null || !(trv instanceof SiteSearchPublishStatus)) {
			SiteSearchPublishStatus status = new SiteSearchPublishStatus();
			QuartzUtils.setTaskRuntimeValues(jobName, jobGroup, status);
			return status;
		}

		return (SiteSearchPublishStatus) trv;
	}
}