/**
 * 
 */
package com.dotmarketing.quartz.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotmarketing.util.TrashUtils;

/**
 * This job will clean up the trash folder created under the dotsecure directory.  It will cleanup files older than 1 hour by default.
 * The DotScheduler will also look for THRASH_FILE_LIFESPAN to see if it should start the job or not. 
 * @author David Torres (2010)
 * @since 1.9 
 */
public class TrashCleanupJob implements Job {
	
	public TrashCleanupJob() {
		
	}
	
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		
		new TrashUtils().emptyTrash();
		
	}

}
