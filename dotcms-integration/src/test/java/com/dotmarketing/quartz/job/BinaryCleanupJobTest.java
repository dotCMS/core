package com.dotmarketing.quartz.job;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.liferay.util.FileUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobExecutionContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Verifies that the {@link BinaryCleanupJob} is working as expected.
 *
 * @author Will Ezell
 * @since Jul 8th, 2019
 */
public class BinaryCleanupJobTest {

  @BeforeClass
  public static void prepare() throws Exception {
    // Setting web app environment
    IntegrationTestInitService.getInstance().init();
  }

  @Test
  public void deleteTmpAssets() throws Exception {
    final Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MILLISECOND, 0);
    cal.set(Calendar.SECOND, 0);

    Config.setProperty(BinaryCleanupJob.CLEANUP_TMP_FILES_OLDER_THAN_HOURS, 3);

    final List<Date> hours = List.of(cal.getTime(), // now
        new Date(cal.getTimeInMillis() - (60 * 60 * 1000)), // 1 hour ago
        new Date(cal.getTimeInMillis() - (2 * 60 * 60 * 1000)), // 2 hours ago
        new Date(cal.getTimeInMillis() - (5 * 60 * 60 * 1000)), // 5 hours ago
        new Date(cal.getTimeInMillis() - (10 * 60 * 60 * 1000))); // 10 hours ago

    final File tempDir = new File(ConfigUtils.getAssetTempPath());
    if (!tempDir.mkdirs()) {
      // Delete the folder contents if any
      FileUtil.deltree(tempDir, false);
      assertEquals("The contents of the /tmp_upload/ folder were deleted, so it MUST have zero files" +
                      " in it", 0,
              FileUtil.listFilesRecursively(tempDir).size());
    }

    for (int i = 0; i < 5; i++) {
      createTestFolderWithFile(ConfigUtils.getAssetTempPath(), "folder" + i, "file" + i,
              hours.get(i));
    }

    // create an additional old folder with a new file in it
    createTestFolderWithFile(ConfigUtils.getAssetTempPath(), "folderx", "filex", hours.get(4));

    assertEquals("There MUST be 12 items total (including folders and files)", 12, FileUtil.listFilesRecursively(tempDir).size());

    new BinaryCleanupJob().execute(null);
    assertEquals("The number of files/folders that should remain is NOT the expected one.", 6, FileUtil.listFilesRecursively(tempDir).size());
  }

  /**
   * <ul>
   *     <li><b>Method to test: </b>{@link BinaryCleanupJob#execute(JobExecutionContext)}</li>
   *     <li><b>Given Scenario: </b>Create 6 test folders with a single file in them, with different
   *     modification dates, and call the Binary Cleanup Job which is supposed to delete all bundles
   *     that are older than 2 days.</li>
   *     <li><b>Expected Result: Three folders were created more than 2 days ago, so they must be
   *     deleted. The remaining folders must be 3, which totals 6 items including folders and files.
   *     </b></li>
   * </ul>
   *
   * @throws Exception An error occurred when creating the test files.
   */
  @Test
  public void deleteBundles() throws Exception {
    final Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MILLISECOND, 0);
    cal.set(Calendar.SECOND, 0);

    Config.setProperty(BinaryCleanupJob.CLEANUP_BUNDLES_OLDER_THAN_DAYS, 2);

    final List<Date> hours = List.of(cal.getTime(), // now
            Date.from(Instant.now().minus(Duration.ofHours(12))), // 12 hours ago
            Date.from(Instant.now().minus(Duration.ofDays(1))), // 1 day ago
            Date.from(Instant.now().minus(Duration.ofDays(2))), // 2 days ago
            Date.from(Instant.now().minus(Duration.ofDays(3)))); // 3 days ago

    final File bundlesDir = new File(ConfigUtils.getBundlePath());
    if (!bundlesDir.mkdirs()) {
      // Delete the folder contents if any
      FileUtil.deltree(bundlesDir, false);
      assertEquals("The contents of the /bundles/ folder were deleted, so it MUST have zero files" +
                      " in it", 0,
              FileUtil.listFilesRecursively(bundlesDir).size());
    }

    for (int i = 0; i < 5; i++) {
      createTestFolderWithFile(ConfigUtils.getBundlePath(), "folder" + i, "file" + i, hours.get(i));
    }
    // Create an additional old folder with a new file in it
    createTestFolderWithFile(ConfigUtils.getBundlePath(), "folderx", "filex", hours.get(4));

    assertEquals("There MUST be 12 items total (including folders and files)", 12,
            FileUtil.listFilesRecursively(bundlesDir).size());

    new BinaryCleanupJob().execute(null);
    assertEquals("The number of files/folders that should remain is NOT the expected one.", 6,
            FileUtil.listFilesRecursively(bundlesDir).size());
  }

  /**
   * Utility method that creates a folder and a single file in it, with a specific modification
   * date.
   *
   * @param parentPath The path to the parent folder where the new items will live in.
   * @param folderName The name of the folder.
   * @param fileName   The name of the child file.
   * @param date       The modification date for both items.
   *
   * @throws IOException An error occurred when adding the file to the folder.
   */
  private void createTestFolderWithFile(final String parentPath, final String folderName,
                                        final String fileName, final Date date) throws IOException {
    final File parent = new File(parentPath, folderName);
    if (!parent.mkdirs()) {
      fail("Could not create directory: " + parent.getAbsolutePath() + "/" + folderName);
    }
    final File child = new File(parent, fileName);
    new FileOutputStream(child).close();
    if (!child.setLastModified(date.getTime())) {
      fail("Could not set last modified date for file: " + child.getAbsolutePath());
    }
    if (!parent.setLastModified(date.getTime())) {
      fail("Could not set last modified date for folder: " + parent.getAbsolutePath());
    }
  }

}
