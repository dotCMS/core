/**
 * 
 */
package com.dotmarketing.quartz.job;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;

/**
 * This job will clean up the binary folder created under the binary directory. It will cleanup
 * files older then 12 hours by default. This can be over ridden via the property
 * BINARY_CLEANUP_FILE_LIFE_HOURS The DotScheduler will also look for
 * BINARY_CLEANUP_JOB_CRON_EXPRESSION to see if it should start the job or not.
 * 
 * @author BayLogic
 * @since http://jira.dotmarketing.net/browse/DOTCMS-1073
 */
public class BinaryCleanupJob implements StatefulJob {

  /*
   * (non-Javadoc)
   * 
   * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
   */

  public BinaryCleanupJob() {

  }

  public void execute(JobExecutionContext ctx) throws JobExecutionException {

    final int hours = Config.getIntProperty("BINARY_CLEANUP_FILE_LIFE_HOURS", 3);

    Date olderThan = Date.from(Instant.now().minus(Duration.ofHours(hours)));

    
    
    Logger.info(this.getClass(), "Deleting tmp files older than " + olderThan + " from " + APILocator.getFileAssetAPI().getRealAssetPathTmpBinary());
    final File folder = new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary());
    FileUtil.cleanTree(folder, olderThan);

    
    

    // also delete bundles older than 2 days
    if (Config.getBooleanProperty("bundles.delete.enabled", true)) {

      olderThan =  Date.from(Instant.now().minus(Duration.ofMillis(Config.getIntProperty("bundles.delete.older.than.milliseconds", 1000 * 60 * 60 * 24 * 2))));
      Logger.info(this.getClass(), "Deleting bundle files older than " + olderThan + " from " + APILocator.getFileAssetAPI().getRealAssetPathTmpBinary());
      final File bundleFolder = new File(ConfigUtils.getBundlePath());
      FileUtil.cleanTree(bundleFolder, olderThan);
    }

  }

}
