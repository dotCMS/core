package com.dotmarketing.sitesearch.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotcms.publishing.PublishStatus;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.util.Logger;

public class SiteSearchJobProxy extends DotStatefulJob {
	
	private PublishStatus status = new PublishStatus();
	public SiteSearchJobProxy () {
		
	}
	
	public void run(JobExecutionContext jobContext) throws JobExecutionException {		
		SiteSearchJobImpl jobImpl = new SiteSearchJobImpl();
		jobImpl.setStatus(status);
		try{
			jobImpl.run(jobContext);
		}
		catch(Exception e){
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new JobExecutionException(e);
			
		}
	}
	
	@Override
	public void updateProgress(int currentProgress) {
		super.updateProgress(currentProgress);
	}
	
	@Override
	public void addMessage(String newMessage) {
		super.addMessage(newMessage);
	}

	public PublishStatus getStatus() {
		return status;
	}

	public void setStatus(PublishStatus status) {
		this.status = status;
	}
	
	
	
}
