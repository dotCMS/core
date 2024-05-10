package com.dotcms.timemachine.business;

import com.dotcms.util.IntegrationTestInitService;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import junit.framework.TestCase;
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

        final Random random = new Random();

        final File tmTestingFolder = FileUtil.createTemporaryDirectory("tm_testing");
        final String timemachinePathPreviousValue = Config.getStringProperty("TIMEMACHINE_PATH", null);

        final Calendar notExpireDate = Calendar.getInstance();
        notExpireDate.add(Calendar.DATE, -45);


        try {
            final List<File> expireFolders = new ArrayList<>();
            final List<File> notExpireFolders = new ArrayList<>();

            Config.setProperty("TIMEMACHINE_PATH", tmTestingFolder.toPath().toAbsolutePath().toString());

            long currentTimeMillis = System.currentTimeMillis();

            for (int i = 0; i < 10; i++) {
                final int expireDays = random.nextInt(10) + 91;
                final Calendar expireDate = Calendar.getInstance();
                expireDate.add(Calendar.DATE, (expireDays * -1));

                final File tmExpiredFolder = new File(tmTestingFolder, "timeMachineBundle_expired_" + (currentTimeMillis + i));
                tmExpiredFolder.mkdir();
                tmExpiredFolder.setLastModified(expireDate.toInstant().toEpochMilli());

                expireFolders.add(tmExpiredFolder);
            }

            for (int i = 0; i < 10; i++) {
                final int days = random.nextInt(45);
                final Calendar expireDate = Calendar.getInstance();
                expireDate.add(Calendar.DATE, (days * -1));

                final File tmExpiredFolder = new File(tmTestingFolder, "timeMachineBundle_not_expired_" + (currentTimeMillis + i));
                tmExpiredFolder.mkdir();
                tmExpiredFolder.setLastModified(expireDate.toInstant().toEpochMilli());

                notExpireFolders.add(tmExpiredFolder);
            }

            final List<File> removedFiles = APILocator.getTimeMachineAPI().removeOldTimeMachineBackupsFiles();

            assertEquals(expireFolders.size(), removedFiles.size());

            final List<String> expectedExpireFilesPath = expireFolders.stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList());

            final List<String> notExpectedRemoveFiles = removedFiles.stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList());

            notExpectedRemoveFiles.removeAll(expectedExpireFilesPath);

            assertTrue(notExpectedRemoveFiles.isEmpty());

            final File[] files = tmTestingFolder.listFiles();

            assertEquals(notExpireFolders.size(), files.length);

            final List<String> expectedNotExpireFilesPath = notExpireFolders.stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList());

            final List<String> notExpectedNotRemoveFiles = Arrays.stream(files)
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList());

            notExpectedNotRemoveFiles.removeAll(expectedNotExpireFilesPath);

            assertTrue(notExpectedNotRemoveFiles.isEmpty());

        } finally {
            Config.setProperty("TIMEMACHINE_PATH", timemachinePathPreviousValue);
        }
    }

    /**
     * Method to test: {@link TimeMachineAPIImpl#removeOldTimeMachineBackupsFiles()}
     * When: You have 10 folder with a lastModified older than the PRUNE_TIMEMACHINE_OLDER_THAN_DAYS default value (90)
     * but they are not Time Machine bundle
     * Should: Not remove any folder
     */
    @Test
    public void notRemoveNotTimeMachineFolder() throws IOException {

        final Random random = new Random();

        final File tmTestingFolder = FileUtil.createTemporaryDirectory("tm_testing");
        final String timemachinePathPreviousValue = Config.getStringProperty("TIMEMACHINE_PATH", null);

        final Calendar notExpireDate = Calendar.getInstance();
        notExpireDate.add(Calendar.DATE, -45);

        try {
            final List<File> expireFolders = new ArrayList<>();

            Config.setProperty("TIMEMACHINE_PATH", tmTestingFolder.toPath().toAbsolutePath().toString());

            long currentTimeMillis = System.currentTimeMillis();

            for (int i = 0; i < 10; i++) {
                final int expireDays = random.nextInt(10) + 91;
                final Calendar expireDate = Calendar.getInstance();
                expireDate.add(Calendar.DATE, (expireDays * -1));

                final File tmExpiredFolder = new File(tmTestingFolder,  "" + (currentTimeMillis + i));
                tmExpiredFolder.mkdir();
                tmExpiredFolder.setLastModified(expireDate.toInstant().toEpochMilli());

                expireFolders.add(tmExpiredFolder);
            }


            final List<File> removedFiles = APILocator.getTimeMachineAPI().removeOldTimeMachineBackupsFiles();

            assertTrue(removedFiles.isEmpty());

            final File[] files = tmTestingFolder.listFiles();

            assertEquals(expireFolders.size(), files.length);

            final List<String> expectedNotExpireFilesPath = expireFolders.stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList());

            final List<String> notExpectedNotRemoveFiles = Arrays.stream(files)
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList());

            notExpectedNotRemoveFiles.removeAll(expectedNotExpireFilesPath);

            assertTrue(notExpectedNotRemoveFiles.isEmpty());

        } finally {
            Config.setProperty("TIMEMACHINE_PATH", timemachinePathPreviousValue);
        }
    }
}
