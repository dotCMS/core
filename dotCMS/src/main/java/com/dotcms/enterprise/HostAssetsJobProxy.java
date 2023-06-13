package com.dotcms.enterprise;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotcms.enterprise.priv.HostAssetsJobImpl;
import com.dotmarketing.quartz.DotStatefulJob;

public class HostAssetsJobProxy extends DotStatefulJob {
	
	public HostAssetsJobProxy () {
		
	}
	
	public void run(JobExecutionContext jobContext) throws JobExecutionException {		
		HostAssetsJobImpl jobImpl = new HostAssetsJobImpl(this);
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
