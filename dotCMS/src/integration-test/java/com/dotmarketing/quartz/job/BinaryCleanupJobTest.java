package com.dotmarketing.quartz.job;

import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.google.common.collect.ImmutableList;
import com.liferay.util.FileUtil;

public class BinaryCleanupJobTest {

  @BeforeClass
  public static void prepare() throws Exception {
    // Setting web app environment
    IntegrationTestInitService.getInstance().init();
  }

  @Test
  public void test_job_deletes_tmp_assets() throws Exception {

    final Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MILLISECOND, 0);
    cal.set(Calendar.SECOND, 0);

    Config.setProperty("BINARY_CLEANUP_FILE_LIFE_HOURS", 3);

    final List<Date> hours = ImmutableList.of(cal.getTime(), // now
        new Date(cal.getTimeInMillis() - (60 * 60 * 1000)), // 1 hour ago
        new Date(cal.getTimeInMillis() - (2 * 60 * 60 * 1000)), // 2 hours ago
        new Date(cal.getTimeInMillis() - (5 * 60 * 60 * 1000)), // 5 hours ago
        new Date(cal.getTimeInMillis() - (10 * 60 * 60 * 1000))); // 10 hours ago

    final File tempDir = new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary());

    tempDir.mkdirs();
    FileUtil.deltree(tempDir, false);
    // a clean start
    assertTrue(FileUtil.listFilesRecursively(tempDir).size() == 0);

    for (int i = 0; i < 5; i++) {
      final File parent = new File(tempDir, "folder" + i);
      parent.mkdirs();
      final File child = new File(parent, "file" + i);
      new FileOutputStream(child).close();
      child.setLastModified(hours.get(i).getTime());
      parent.setLastModified(hours.get(i).getTime());
    }

    // create an old folder with a new file in it
    final File parent = new File(tempDir, "folderx");
    parent.mkdirs();

    final File child = new File(parent, "filex");
    new FileOutputStream(child).close();
    child.setLastModified(System.currentTimeMillis());
    parent.setLastModified(hours.get(4).getTime());

    assertEquals(12,FileUtil.listFilesRecursively(tempDir).size());

    new BinaryCleanupJob().execute(null);
    assertEquals(8,FileUtil.listFilesRecursively(tempDir).size());

  }

  @Test
  public void test_job_deletes_bundles() throws Exception {

    final Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MILLISECOND, 0);
    cal.set(Calendar.SECOND, 0);

    
    // deleting older than 2 days
    Config.setProperty("bundles.delete.older.than.milliseconds", 1000 * 60 * 60 * 24 * 2);

    final List<Date> hours = ImmutableList.of(cal.getTime(), // now
        Date.from(Instant.now().minus(Duration.ofHours(12))), // 12 hours ago
        Date.from(Instant.now().minus(Duration.ofDays(1))), // 1 day ago
        Date.from(Instant.now().minus(Duration.ofDays(2))), // 2 days ago
        Date.from(Instant.now().minus(Duration.ofDays(3)))); // 3 days ago

    final File tempDir = new File(ConfigUtils.getBundlePath());

    tempDir.mkdirs();
    FileUtil.deltree(tempDir, false);
    // a clean start
    assertEquals(0,FileUtil.listFilesRecursively(tempDir).size());

    for (int i = 0; i < 5; i++) {
      final File parent = new File(tempDir, "folder" + i);
      parent.mkdirs();
      final File child = new File(parent, "file" + i);
      new FileOutputStream(child).close();
      child.setLastModified(hours.get(i).getTime());
      parent.setLastModified(hours.get(i).getTime());
    }

    // create an old folder with a new file in it
    final File parent = new File(tempDir, "folderx");
    parent.mkdirs();

    final File child = new File(parent, "filex");
    new FileOutputStream(child).close();
    child.setLastModified(System.currentTimeMillis());
    parent.setLastModified(hours.get(4).getTime());

    assertEquals(12,FileUtil.listFilesRecursively(tempDir).size());

    new BinaryCleanupJob().execute(null);
    assertEquals(8,FileUtil.listFilesRecursively(tempDir).size());

  }
}
