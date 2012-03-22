/**
 * 
 */
package com.dotmarketing.quartz.job;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.TrashUtils;
import com.liferay.util.FileUtil;

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
		
		File trashFolder = TrashUtils.getTrashFolder();
		if(!trashFolder.exists()){
			Logger.debug(this,"Trash folder "+ trashFolder.getPath() + " not found exiting job");
			return;
		}
		File[] files = trashFolder.listFiles();
		for (File file : files) {
			if(file.isDirectory()){
				FileUtil.deltree(file);
			}else{
				file.delete();
			}
		}
		
	}

}
