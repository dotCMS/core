package com.dotmarketing.quartz.job;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.TrashUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.FileUtil;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * This Quartz Job will clean up binary files that are either temporary, or can unnecessarily fill
 * up the disk space in dotCMS instances and can be safely deleted after a previously configured
 * amount of time. This Job will be able to purge the following folders:
 * <ul>
 *     <li>{@code /assets/tmp_upload/}</li>
 *     <li>{@code /assets/bundles}</li>
 *     <li>{@code /dotsecure/backup}</li>
 *     <li>{@code /dotsecure/trash}</li>
 *     <li>Java IO temp files located under the folder specified via the {@code "java.io.tmpdir"}
 *     system property, which is usually {@code ${TOMCAT_HOME}/temp/}.</li>
 * </ul>
 * The {@code BINARY_CLEANUP_JOB_CRON_EXPRESSION} allows you to set a CRON Expression to specify how
 * often this Job will run and, by default, files older than 3 hours compared to the current
 * execution time will be deleted. This behavior can be overridden through the following
 * configuration properties:
 * <ul>
 *     <li>{@code CLEANUP_TMP_FILES_OLDER_THAN_HOURS}: Files located under the {@code /assets
 *     /tmp_upload/}, {@code /dotsecure/trash}, and the folder specified via the {@code java.io
 *     .tmpdir} property will be deleted after the specified number of hours. Defaults to 3 hours
 *     .</li>
 *     <li>{@code CLEANUP_BUNDLES_OLDER_THAN_DAYS}: Files located under {@code /assets/bundles} will
 *     be deleted after the specified number of days. Defaults to 3 days.</li>
 *     <li>{@code CLEANUP_BACKUP_FILES_OLDER_THAN_DAYS}: Files located under
 *     {@code /dotsecure/backup} will be deleted after the specified number of days. Defaults to
 *     3 days.</li>
 * </ul>
 *
 * @author BayLogic
 * @author Will Ezell
 * @since Mar 22nd, 2012
 */
public class BinaryCleanupJob implements StatefulJob {

  public static final String CLEANUP_TMP_FILES_OLDER_THAN_HOURS = "CLEANUP_TMP_FILES_OLDER_THAN_HOURS";
  public static final String CLEANUP_BUNDLES_OLDER_THAN_DAYS = "CLEANUP_BUNDLES_OLDER_THAN_DAYS";
  public static final String CLEANUP_BACKUP_FILES_OLDER_THAN_DAYS = "CLEANUP_BACKUP_FILES_OLDER_THAN_DAYS";
  private static final String FILE_DELETION_STATUS_MSG = "Deleting files older than %d %s from %s";

  private static final String HOURS = "hours";
  private static final String DAYS = "days";

  public BinaryCleanupJob() {
    // Empty constructor
  }

  @Override
  public void execute(final JobExecutionContext ctx) throws JobExecutionException {
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
  void cleanUpTmpUploadedFiles() {
    final int hours = Config.getIntProperty(CLEANUP_TMP_FILES_OLDER_THAN_HOURS, 3);
    final Date olderThan = Date.from(Instant.now().minus(Duration.ofHours(hours)));

    Logger.info(this.getClass(), String.format(FILE_DELETION_STATUS_MSG, hours, HOURS, ConfigUtils.getAssetTempPath()));
    final File folder = new File(ConfigUtils.getAssetTempPath());
    FileUtil.cleanTree(folder, olderThan);
  }

  /**
   * Deletes from /assets/bundles
   */
  void cleanUpOldBundles() {
      final int days = Config.getIntProperty(CLEANUP_BUNDLES_OLDER_THAN_DAYS, 4);
      if (days < 1) {
        return;
      }
      final Date olderThan =  Date.from(Instant.now().minus(Duration.ofDays(days)));
      Logger.info(this.getClass(), String.format(FILE_DELETION_STATUS_MSG, days, DAYS, ConfigUtils.getBundlePath()));
      final File bundleFolder = new File(ConfigUtils.getBundlePath());
      FileUtil.cleanTree(bundleFolder, olderThan);
  }

  /**
   * Deletes from /dotsecure/trash
   */
  void cleanUpTrashFolder() {
    final int hours = Config.getIntProperty(CLEANUP_TMP_FILES_OLDER_THAN_HOURS, 3);
    final Date olderThan = Date.from(Instant.now().minus(Duration.ofHours(hours)));

    final TrashUtils trash = new TrashUtils();
    Logger.info(this.getClass(), String.format(FILE_DELETION_STATUS_MSG, hours, HOURS, trash.getTrashFolder()));
    FileUtil.cleanTree(trash.getTrashFolder(), olderThan);
    new TrashUtils().emptyTrash();
  }

  /**
   * Deletes from /dotsecure/backup
   */
  void cleanUpLocalBackupDirectory(){
    final int days = Config.getIntProperty(CLEANUP_BACKUP_FILES_OLDER_THAN_DAYS, 3);
    if (days < 1) {
      return;
    }
    final Date olderThan = Date.from(Instant.now().minus(Duration.ofDays(days)));

    Logger.info(this.getClass(), String.format(FILE_DELETION_STATUS_MSG, days, DAYS, ConfigUtils.getBackupPath()));
    final File folder = new File(ConfigUtils.getBackupPath());
    FileUtil.cleanTree(folder, olderThan);
  }

  /**
   * Deletes from java.io.tmpdir
   */
  void cleanUpJavaIoTmpFiles(){
    final String javaTmpDir = System.getProperty("java.io.tmpdir");
    if (UtilMethods.isEmpty(javaTmpDir)) {
      return;
    }
    final int hours = Config.getIntProperty(CLEANUP_TMP_FILES_OLDER_THAN_HOURS, 3);
    final Date olderThan = Date.from(Instant.now().minus(Duration.ofHours(hours)));

    Logger.info(this.getClass(), String.format(FILE_DELETION_STATUS_MSG, hours, HOURS, javaTmpDir));
    final File folder = new File(javaTmpDir);
    FileUtil.cleanTree(folder, olderThan);
  }

}
