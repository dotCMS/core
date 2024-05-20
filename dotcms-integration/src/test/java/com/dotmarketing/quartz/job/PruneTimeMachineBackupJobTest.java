package com.dotmarketing.quartz.job;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.stream.Collectors;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
public class PruneTimeMachineBackupJobTest {
    final PruneTimeMachineBackupJob pruneTimeMachineJob = new PruneTimeMachineBackupJob();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    private File createTimeMachineFolder(final File parentFolder, final String fileName,
                                         final int howLongWasModified) throws IOException {
        final Calendar expireDate = Calendar.getInstance();
        expireDate.add(Calendar.DATE, (howLongWasModified * -1));

        final File tmFolder = new File(parentFolder, fileName);
        tmFolder.mkdir();

        final File fileInside = new File(tmFolder, "file");
        fileInside.createNewFile();

        final File folderInside = new File(tmFolder, "folder");
        folderInside.mkdir();

        final File fileInsideFolderInside = new File(folderInside, "file");
        fileInsideFolderInside.createNewFile();

        tmFolder.setLastModified(expireDate.toInstant().toEpochMilli());

        return tmFolder;
    }

    /**
     * Method to test: {@link PruneTimeMachineBackupJob#run(JobExecutionContext)}
     * When: In the directory for the Time Machine, there are two folders with the following names and corresponding "Last Modified" days:
     *
     * Folder: timeMachineBundle_[current_millis]_1, Last Modified: 95 days ago
     * Folder: timeMachineBundle_[current_millis]_2, Last Modified: 45 days ago
     *
     * Should: Remove the folder named timeMachineBundle_1715638200156_1
     */
    @Test
    public void runRemoveFolder() throws IOException, JobExecutionException {
        final File tmTestingFolder = FileUtil.createTemporaryDirectory("tm_testing");
        final String timeMachinePathPreviousValue = Config.getStringProperty("TIMEMACHINE_PATH", null);

        try {
            Config.setProperty("TIMEMACHINE_PATH", tmTestingFolder.toPath().toAbsolutePath().toString());

            final String expireFolderName = "tm_" + System.currentTimeMillis() + "_1";
            final File expireFolder = createTimeMachineFolder(tmTestingFolder, expireFolderName, 95);

            final String noExpireFolderName = "tm_" + System.currentTimeMillis() + "_2";
            final File noExpireFolder = createTimeMachineFolder(tmTestingFolder, noExpireFolderName, 45);

            assertFileSystem(tmTestingFolder, noExpireFolderName, expireFolderName);

            pruneTimeMachineJob.run(getJobExecutionContext());

            assertFileSystem(tmTestingFolder, noExpireFolderName);
        } finally {
            cleanUp(timeMachinePathPreviousValue);
        }
    }

    private JobExecutionContext getJobExecutionContext() {
        return mock(JobExecutionContext.class);
    }

    private static void assertFileSystem(final File folder, String... folderNames) {
        final Collection<String> filesPath = Arrays.stream(folder.listFiles()).map(File::getName)
                .collect(Collectors.toList());
        assertEquals(folderNames.length, filesPath.size());

        Arrays.stream(folderNames).forEach(folderName -> assertTrue(filesPath.contains(folderName)));
    }

    /**
     * Method to test: {@link PruneTimeMachineBackupJob#run(JobExecutionContext)}
     * When: In the directory for the Time Machine, there are two folders with the following names and corresponding "Last Modified" days:
     *
     * Folder: timeMachineBundle_1715638200156_1, Last Modified: 95 days ago
     * Folder: timeMachineBundle_1715638200156_2, Last Modified: 97 days ago
     *
     * Should: Remove both folders
     */
    @Test
    public void runRemoveAll() throws IOException, JobExecutionException {
        final File tmTestingFolder = FileUtil.createTemporaryDirectory("tm_testing");
        final String timeMachinePathPreviousValue = Config.getStringProperty("TIMEMACHINE_PATH", null);

        try {
            Config.setProperty("TIMEMACHINE_PATH", tmTestingFolder.toPath().toAbsolutePath().toString());

            final String expireFolderName_1 = "tm_" + System.currentTimeMillis() + "_1";
            final File expireFolder_1 = createTimeMachineFolder(tmTestingFolder, expireFolderName_1, 95);

            final String expireFolderName_2 = "tm_" + System.currentTimeMillis() + "_2";
            final File expireFolder_2 = createTimeMachineFolder(tmTestingFolder, expireFolderName_2, 97);

            assertFileSystem(tmTestingFolder, expireFolderName_1, expireFolderName_2);

            pruneTimeMachineJob.run(getJobExecutionContext());
            assertEquals(0, tmTestingFolder.listFiles().length);
        } finally {
            cleanUp(timeMachinePathPreviousValue);
        }
    }

    private void cleanUp(String timeMachinePathPreviousValue) {
        Config.setProperty("TIMEMACHINE_PATH", timeMachinePathPreviousValue);
        APILocator.getTimeMachineAPI().removeQuartzJob();
    }

    /**
     * Method to test: {@link PruneTimeMachineBackupJob#run(JobExecutionContext)}
     * When: In the directory for the Time Machine, there are two folders with the following names and corresponding "Last Modified" days:
     *
     * Folder: timeMachineBundle_[current_millis]_1, Last Modified: 55 days ago
     * Folder: timeMachineBundle_[current_millis]_2, Last Modified: 45 days ago
     *
     * Should: Remove nothing
     */
    @Test
    public void runRemoveNothing() throws IOException, JobExecutionException {
        final File tmTestingFolder = FileUtil.createTemporaryDirectory("tm_testing");
        final String timeMachinePathPreviousValue = Config.getStringProperty("TIMEMACHINE_PATH", null);

        try {
            Config.setProperty("TIMEMACHINE_PATH", tmTestingFolder.toPath().toAbsolutePath().toString());
            
            final String noExpireFolderName_1 = "tm_" + System.currentTimeMillis() + "_1";
            final File noExpireFolder_1 = createTimeMachineFolder(tmTestingFolder, noExpireFolderName_1, 55);

            final String noExpireFolderName_2 = "tm_" + System.currentTimeMillis() + "_2";
            final File noExpireFolder_2 = createTimeMachineFolder(tmTestingFolder, noExpireFolderName_2, 45);

            assertFileSystem(tmTestingFolder, noExpireFolderName_1, noExpireFolderName_2);

            pruneTimeMachineJob.run(getJobExecutionContext());

            assertFileSystem(tmTestingFolder, noExpireFolderName_1, noExpireFolderName_2);
        } finally {
            cleanUp(timeMachinePathPreviousValue);
        }
    }
}
