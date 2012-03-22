package com.dotmarketing.sitesearch.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotmarketing.quartz.DotStatefulJob;

public class SiteSearchJobProxy extends DotStatefulJob {
	
	public SiteSearchJobProxy () {
		
	}
	
	public void run(JobExecutionContext jobContext) throws JobExecutionException {		
		SiteSearchJobImpl jobImpl = new SiteSearchJobImpl();
		jobImpl.run(jobContext);
	}
	
	@Override
	public void updateProgress(int currentProgress) {
		super.updateProgress(currentProgress);
	}
	
	@Override
	public void addMessage(String newMessage) {
		super.addMessage(newMessage);
	}
	
}
