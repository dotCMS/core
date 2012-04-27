package com.dotcms.publishing.sitesearch.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotcms.publishing.sitesearch.SiteSearchPublishStatus;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.TaskRuntimeValues;
import com.dotmarketing.util.Logger;

public class SiteSearchJobProxy extends DotStatefulJob {
	

	
	public void run(JobExecutionContext jobContext) throws JobExecutionException {		
		SiteSearchJobImpl jobImpl = new SiteSearchJobImpl();
		SiteSearchPublishStatus status = null;
		TaskRuntimeValues trv = QuartzUtils.getTaskRuntimeValues(jobContext.getJobDetail().getName(), jobContext.getJobDetail().getGroup());
		if(trv ==null || !(trv instanceof SiteSearchPublishStatus)){
			status = new SiteSearchPublishStatus();
			QuartzUtils.setTaskRuntimeValues(jobContext.getJobDetail().getName(), jobContext.getJobDetail().getGroup(), status);
		}
		status = (SiteSearchPublishStatus) QuartzUtils.getTaskRuntimeValues(jobContext.getJobDetail().getName(), jobContext.getJobDetail().getGroup());
		

		
		jobImpl.setStatus(status);
		try{
			jobImpl.run(jobContext);
		}
		catch(Exception e){
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new JobExecutionException(e);
			
		}
		finally{
			try{
				DbConnectionFactory.closeConnection();
			}
			catch(Exception e){
				
			}
		}
	}

	
}
