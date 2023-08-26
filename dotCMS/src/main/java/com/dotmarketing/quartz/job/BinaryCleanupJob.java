/**
 * 
 */
package com.dotmarketing.quartz.job;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import com.dotmarketing.util.*;
import jdk.jshell.execution.Util;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotmarketing.business.APILocator;
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


    Logger.info(this.getClass(), "STARTING TMP/TRASH FILE CLEANUP");

    cleanUpTmpUploadedFiles();
    cleanUpOldBundles();
    cleanUpTrashFolder();
    cleanUpLocalBackupDirectory();
    cleanUpJavaIoTmpFiles();

    Logger.info(this.getClass(), "ENDING TMP/TRASH FILE CLEANUP");

  }

  /**
   * Deletes from /assets/tmp_upload
   */
  void cleanUpTmpUploadedFiles(){

    final int hours = Config.getIntProperty("CLEANUP_TMP_FILES_OLDER_THAN_HOURS", 3);
    Date olderThan = Date.from(Instant.now().minus(Duration.ofHours(hours)));

    Logger.info(this.getClass(), "Deleting files older than " + olderThan + " from " + ConfigUtils.getAssetTempPath());
    final File folder = new File(ConfigUtils.getAssetTempPath());
    FileUtil.cleanTree(folder, olderThan);
  }

  /**
   * Deletes from /assets/bundles
   */
  void cleanUpOldBundles(){
      final int days = Config.getIntProperty("CLEANUP_BUNDLES_OLDER_THAN_DAYS", 3);
      if(days<1){
        return;
      }
      Date olderThan =  Date.from(Instant.now().minus(Duration.ofDays(days)));
      Logger.info(this.getClass(), "Deleting files older than " + olderThan + " from " + ConfigUtils.getBundlePath());
      final File bundleFolder = new File(ConfigUtils.getBundlePath());
      FileUtil.cleanTree(bundleFolder, olderThan);
  }

  /**
   * Deletes from /dotsecure/trash
   */
  void cleanUpTrashFolder() {
    final int hours = Config.getIntProperty("CLEANUP_TMP_FILES_OLDER_THAN_HOURS", 3);
    Date olderThan = Date.from(Instant.now().minus(Duration.ofHours(hours)));

    TrashUtils trash = new TrashUtils();
    Logger.info(this.getClass(), "Deleting files older than " + olderThan + " from " + trash.getTrashFolder());
    FileUtil.cleanTree(trash.getTrashFolder(), olderThan);
    new TrashUtils().emptyTrash();
  }


  /**
   * Deletes from /dotsecure/backup
   */
  void cleanUpLocalBackupDirectory(){

    final int days = Config.getIntProperty("CLEANUP_BACKUP_FILES_OLDER_THAN_DAYS", 3);
    if(days<1){
      return;
    }
    Date olderThan = Date.from(Instant.now().minus(Duration.ofDays(days)));

    Logger.info(this.getClass(), "Deleting files older than " + olderThan + " from " + ConfigUtils.getBackupPath());
    final File folder = new File(ConfigUtils.getBackupPath() );
    FileUtil.cleanTree(folder, olderThan);
  }



  /**
   * Deletes from java.io.tmpdir
   */
  void cleanUpJavaIoTmpFiles(){
    String javaTmpDir = System.getProperty("java.io.tmpdir");
    if(UtilMethods.isEmpty(javaTmpDir)) {
      return;
    }
    final int hours = Config.getIntProperty("CLEANUP_TMP_FILES_OLDER_THAN_HOURS", 3);
    Date olderThan = Date.from(Instant.now().minus(Duration.ofHours(hours)));

    Logger.info(this.getClass(), "Deleting files older than " + olderThan + " from " + javaTmpDir);
    final File folder = new File(javaTmpDir);
    FileUtil.cleanTree(folder, olderThan);
  }




}
