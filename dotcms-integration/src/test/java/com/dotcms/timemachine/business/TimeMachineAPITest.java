package com.dotcms.timemachine.business;

import com.dotcms.util.IntegrationTestInitService;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TimeMachineAPITest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link TimeMachineAPIImpl#removeOldTimeMachineBackupsFiles()}
     * When: You have 10 folder with a lastModified older than the PRUNE_TIMEMACHINE_OLDER_THAN_DAYS default value (90)
     * and you have 1o folder with a lastModified younger than he PRUNE_TIMEMACHINE_OLDER_THAN_DAYS default value (90)
     * Should: Remove the folders with a older value
     */
    @Test
    public void removeOldTimeMachineBackupsFiles() throws IOException {

        final File tmTestingFolder = FileUtil.createTemporaryDirectory("tm_testing");
        final String timemachinePathPreviousValue = Config.getStringProperty("TIMEMACHINE_PATH", null);

        try {
            Config.setProperty("TIMEMACHINE_PATH", tmTestingFolder.toPath().toAbsolutePath().toString());

            final List<File> expireFolders = createFiles(tmTestingFolder, "timeMachineBundle_expired_",
                    91, 100);
            final List<File> notExpireFolders = createFiles(tmTestingFolder, "timeMachineBundle_not_expired_",
                    45, 89);

            final List<File> removedFiles = APILocator.getTimeMachineAPI().removeOldTimeMachineBackupsFiles();

            assertEquals(expireFolders.size(), removedFiles.size());
            assertTrue(removeAll(expireFolders, removedFiles).isEmpty());

            final File[] files = tmTestingFolder.listFiles();
            assertEquals(notExpireFolders.size(), files.length);
            assertTrue(removeAll(Arrays.asList(files), notExpireFolders).isEmpty());
        } finally {
            Config.setProperty("TIMEMACHINE_PATH", timemachinePathPreviousValue);
        }
    }

    @NotNull
    private static List<String> removeAll(final List<File> list1, final List<File> list2) {
        final List<String> list1Path = list1.stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());

        final List<String> list2Path = list2.stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());

        list1Path.removeAll(list2Path);
        return list1Path;
    }

    private static List<File> createFiles(final File parentFolder, final String preffix,
                                          final int olderThanFrom, final int olderThanTo )
            throws IOException {
        final List<File> files = new ArrayList();
        final Random random = new Random();
        long currentTimeMillis = System.currentTimeMillis();

        for (int i = 0; i < 10; i++) {
            final int expireDays = random.nextInt(olderThanTo - olderThanFrom) + olderThanFrom;
            final Calendar expireDate = Calendar.getInstance();
            expireDate.add(Calendar.DATE, (expireDays * -1));

            final File tmExpiredFolder = new File(parentFolder, preffix + (currentTimeMillis + i));
            tmExpiredFolder.mkdir();

            files.add(tmExpiredFolder);

            final File fileInside = new File(tmExpiredFolder, "file");
            fileInside.createNewFile();

            final File folderInside = new File(tmExpiredFolder, "folder");
            folderInside.mkdir();

            final File fileInsideFolderInside = new File(folderInside, "file");
            fileInsideFolderInside.createNewFile();

            tmExpiredFolder.setLastModified(expireDate.toInstant().toEpochMilli());
        }

        return files;
    }

    /**
     * Method to test: {@link TimeMachineAPIImpl#removeOldTimeMachineBackupsFiles()}
     * When: You have 10 folder with a lastModified older than the PRUNE_TIMEMACHINE_OLDER_THAN_DAYS default value (90)
     * but they are not Time Machine bundle
     * Should: Not remove any folder
     */
    @Test
    public void notRemoveNotTimeMachineFolder() throws IOException {
        final File tmTestingFolder = FileUtil.createTemporaryDirectory("tm_testing");
        final String timemachinePathPreviousValue = Config.getStringProperty("TIMEMACHINE_PATH", null);

        try {
            final List<File> expireFolders = createFiles(tmTestingFolder, "", 90, 100);
            final List<File> removedFiles = APILocator.getTimeMachineAPI().removeOldTimeMachineBackupsFiles();
            assertTrue(removedFiles.isEmpty());

            final File[] files = tmTestingFolder.listFiles();
            assertEquals(expireFolders.size(), files.length);
            assertTrue(removeAll(Arrays.asList(files), expireFolders).isEmpty());

        } finally {
            Config.setProperty("TIMEMACHINE_PATH", timemachinePathPreviousValue);
        }
    }

    /**
     * Method to test: {@link TimeMachineAPIImpl#setQuartzJobConfig(String, List, boolean, List, boolean)}
     * When: Called this method and any of this job are running: {@link com.dotmarketing.quartz.job.TimeMachineJob} and {@link com.dotmarketing.quartz.job.PruneTimeMachineBackupJob}
     * Should: start both
     */
    @Test
    public void startTimeMachineJob(){

    }

    /**
     * Method to test: {@link TimeMachineAPIImpl#setQuartzJobConfig(String, List, boolean, List, boolean)}
     * When: Called this method and the {@link com.dotmarketing.quartz.job.PruneTimeMachineBackupJob} is already running
     * Should: Restart just the {@link com.dotmarketing.quartz.job.TimeMachineJob}
     */
    @Test
    public void startTimeMachineJobWhenPruneJobIsAlreadyRunning(){

    }
}