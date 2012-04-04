package com.dotmarketing.quartz;

import org.quartz.Job;

import com.dotmarketing.util.Logger;

public class TestJob implements Job {
	public void execute(org.quartz.JobExecutionContext arg0) throws org.quartz.JobExecutionException {
		
		int i = 0;
		while(i < 10) {
			Logger.info(this, "Executing job " + arg0.getJobDetail().getJobDataMap().get("jobid") + ", cycle = " + i);
			try {
				Thread.sleep(1000);
			} catch (Exception e) {}
			i++;
		}
	}
}
