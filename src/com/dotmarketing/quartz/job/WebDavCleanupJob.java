/**
 * 
 */
package com.dotmarketing.quartz.job;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.webdav.DotWebdavHelper;
import com.liferay.util.FileUtil;

/**
 * This job will clean up the webdav folder created under the temp directory.  It will cleanup files older then 12 hours by default.
 * This can be over ridden via the property WEBDAV_CLEANUP_FILE_LIFE_HOURS 
 * The DotScheduler will also look for WEBDAV_CLEANUP_JOB_CRON_EXPRESSION to see if it should start the job or not. 
 * @author Jason Tesser
 * @since 1.6.5
 *
 */
public class WebDavCleanupJob implements Job {
	
	/* (non-Javadoc)
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
	private DotWebdavHelper davHelper;
	
	public WebDavCleanupJob() {
		davHelper = new DotWebdavHelper();
	}
	
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		int hours = Config.getIntProperty("WEBDAV_CLEANUP_FILE_LIFE_HOURS",-1);
		if(hours < 0){
			hours = 12;
		}
		Calendar c = Calendar.getInstance();
		c.add(Calendar.HOUR_OF_DAY, -hours);
		Date dDate = c.getTime();
		File tempDir = null;
		try {
			tempDir = getTempWebDavDir();
		} catch (IOException e) {
			Logger.error(this,"Unable to get tempory file holder exiting job", e);
			return;
		}
		if(!tempDir.exists()){
			Logger.info(this,"Tempory Webdav Directory "+ tempDir.getPath() + " not found exiting job");
			return;
		}
		File[] files = tempDir.listFiles();
		for (File file : files) {
			if(file.isDirectory()){
				boolean deleteFolder = true;
				try {
					List<File> rFiles = FileUtil.listFilesRecursively(tempDir);
					for (File rFile : rFiles) {
						if(FileUtils.isFileNewer(rFile, dDate)){
							deleteFolder = false;
							break;
						}
					}
					if(deleteFolder){
						FileUtil.deltree(file);
					}
				} catch (FileNotFoundException e) {
					Logger.error(this, "Temp dir not found unable to list files for " + file.getPath(),e);
					continue;
				}
			}else{
				if(FileUtils.isFileNewer(file, dDate)){
					file.delete();
				}
			}
		}
		
	}

	private File getTempWebDavDir() throws IOException{
		return davHelper.getTempDir();
	}
}
