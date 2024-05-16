package com.dotcms.timemachine.business;

import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.util.IntegrationTestInitService;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.quartz.job.PruneTimeMachineBackupJob;
import com.dotmarketing.quartz.job.TimeMachineJob;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Calendar;
import java.util.stream.Collectors;
import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.*;
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

            final List<File> expireFolders = createFiles(tmTestingFolder, "tm_expired_",
                    91, 100);
            final List<File> notExpireFolders = createFiles(tmTestingFolder, "tm_not_expired_",
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
            Config.setProperty("TIMEMACHINE_PATH", tmTestingFolder.toPath().toAbsolutePath().toString());

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
     * Method to test: {@link TimeMachineAPIImpl#getAvailableTimeMachineFolder()}
     *
     * when: Does not exist any Time Machine folder on the Time Machine Path
     * should: return a empty list
     *
     * when: The {@link com.dotmarketing.quartz.job.TimeMachineJob run} the first time
     * should: Return a List with one element
     *
     * when: The {@link com.dotmarketing.quartz.job.TimeMachineJob run} the second time
     * should: Return a List with two elements
     *
     * when: The {@link com.dotmarketing.quartz.job.TimeMachineJob run} the third time
     * should: Return a List with three elements
     *
     * Note: A {@link com.dotmarketing.beans.Host} with a {@link com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset}
     * is created on the test to create not empty Time Machine folders
     */
    @Test
    public void getAvailableTimeMachineFolder() throws IOException {
        final File tmTestingFolder = FileUtil.createTemporaryDirectory("tm_testing");
        final String timemachinePathPreviousValue = Config.getStringProperty("TIMEMACHINE_PATH", null);

        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template)
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        try {
            Config.setProperty("TIMEMACHINE_PATH", tmTestingFolder.toPath().toAbsolutePath().toString());

            final List<File> availableTimeMachineFolder_1 = APILocator.getTimeMachineAPI().getAvailableTimeMachineFolder();
            assertTrue(availableTimeMachineFolder_1.isEmpty());

            APILocator.getTimeMachineAPI().startTimeMachine(list(host), list(defaultLanguage), false);

            final List<File> availableTimeMachineFolder_2 = APILocator.getTimeMachineAPI().getAvailableTimeMachineFolder();
            assertEquals(1, availableTimeMachineFolder_2.size());

            APILocator.getTimeMachineAPI().startTimeMachine(list(host), list(defaultLanguage), false);

            final List<File> availableTimeMachineFolder_3 = APILocator.getTimeMachineAPI().getAvailableTimeMachineFolder();
            assertEquals(2, availableTimeMachineFolder_3.size());

            APILocator.getTimeMachineAPI().startTimeMachine(list(host), list(defaultLanguage), false);

            final List<File> availableTimeMachineFolder_4 = APILocator.getTimeMachineAPI().getAvailableTimeMachineFolder();
            assertEquals(3, availableTimeMachineFolder_4.size());
        } finally {
            Config.setProperty("TIMEMACHINE_PATH", timemachinePathPreviousValue);
        }
    }

    /**
     * Method to test: {@link TimeMachineAPIImpl#setQuartzJobConfig(String, List, boolean, List, boolean)}
     * When: Called this method and any of this job are not running: {@link com.dotmarketing.quartz.job.TimeMachineJob} and
     * {@link com.dotmarketing.quartz.job.PruneTimeMachineBackupJob}
     * Should: start both
     */
    @Test
    public void startTimeMachineJob() throws SchedulerException {
        try {
            final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
            final Host host = new SiteDataGen().nextPersisted();
            final Template template = new TemplateDataGen().host(host).nextPersisted();
            final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template)
                    .languageId(defaultLanguage.getId())
                    .nextPersisted();

            assertTrue(QuartzUtils.getScheduledTasks("timemachine").isEmpty());

            final List<Host> hosts = list(host);
            final List<Language> langs = list(defaultLanguage);

            APILocator.getTimeMachineAPI().setQuartzJobConfig("0 0 0 ? * * *", hosts, false,
                    langs, false);

            assertJobsRunnings(hosts, langs, "0 0 0 ? * * *", "0 0 0 ? * SUN *");
        } finally {
            removeJobs();
        }
    }

    /**
     * Stop and remove the jobs: {@link TimeMachineJob} and {@link PruneTimeMachineBackupJob}
     */
    private void removeJobs() throws SchedulerException {
        QuartzUtils.pauseJob("timemachine","timemachine");
        QuartzUtils.removeTaskRuntimeValues("timemachine","timemachine");
        QuartzUtils.removeJob("timemachine","timemachine");

        QuartzUtils.pauseJob("prune-timemachine","timemachine");
        QuartzUtils.removeTaskRuntimeValues("prune-timemachine","timemachine");
        QuartzUtils.removeJob("prune-timemachine","timemachine");
    }

    /**
     * Check if {@link TimeMachineJob} and {@link PruneTimeMachineBackupJob} are running
     * @param hosts
     * @param langs
     */
    private static void assertJobsRunnings(final List<Host> hosts, final List<Language> langs, final String tmJonCron,
                                           final String pruneTMCron) throws SchedulerException {
        final List<ScheduledTask> taskList = QuartzUtils.getScheduledTasks("timemachine");

        assertEquals(2, taskList.size());

        for (final ScheduledTask scheduledTask : taskList) {
            if (scheduledTask.getJobName().equals("timemachine")) {
                assertEquals("timemachine", scheduledTask.getJobName());
                assertEquals("timemachine", scheduledTask.getJobGroup());
                assertEquals("Time Machine", scheduledTask.getJobDescription());
                assertEquals(TimeMachineJob.class.getName(), scheduledTask.getJavaClassName());
                assertEquals(tmJonCron, ((CronScheduledTask) scheduledTask).getCronExpression());

                final Map<String, Object> jobProperties = scheduledTask.getProperties();

                assertEquals(jobProperties.get("CRON_EXPRESSION"), tmJonCron);
                assertEquals(jobProperties.get("hosts"), hosts);
                assertEquals(jobProperties.get("langs"), langs);
                assertEquals(false, jobProperties.get("allhosts"));
                assertEquals(false, jobProperties.get("incremental"));
            } else if (scheduledTask.getJobName().equals("prune-timemachine")) {
                assertEquals("prune-timemachine", scheduledTask.getJobName());
                assertEquals("timemachine", scheduledTask.getJobGroup());
                assertEquals("Time Machine Prune Job", scheduledTask.getJobDescription());
                assertEquals(PruneTimeMachineBackupJob.class.getName(), scheduledTask.getJavaClassName());
                assertEquals(pruneTMCron, ((CronScheduledTask) scheduledTask).getCronExpression());

                final Map<String, Object> jobProperties = scheduledTask.getProperties();

                assertEquals("prune-timemachine", jobProperties.get("JOB_NAME").toString());
            } else {
                throw new AssertionError("Not expected Task called " + scheduledTask.getJobName());
            }
        }
    }

    /**
     * Method to test: {@link TimeMachineAPIImpl#startTimeMachine(List, List, boolean)}
     * When: Called this method and the {@link com.dotmarketing.quartz.job.PruneTimeMachineBackupJob} is already running
     * Should: Restart just the {@link com.dotmarketing.quartz.job.TimeMachineJob}
     */
    @Test
    public void startTimeMachineJobWhenPruneJobIsAlreadyRunning() throws SchedulerException {
        try {
            final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
            final Host host = new SiteDataGen().nextPersisted();
            final Template template = new TemplateDataGen().host(host).nextPersisted();
            final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template)
                    .languageId(defaultLanguage.getId())
                    .nextPersisted();

            assertTrue(QuartzUtils.getScheduledTasks("timemachine").isEmpty());

            final List<Host> hosts = list(host);
            final List<Language> langs = list(defaultLanguage);

            APILocator.getTimeMachineAPI().setQuartzJobConfig("0 0 0 ? * * *", hosts, false,
                    langs, false);

            APILocator.getTimeMachineAPI().removeQuartzJob();

            assertFalse(QuartzUtils.getScheduledTask("prune-timemachine", "timemachine").isEmpty());
            assertTrue(QuartzUtils.getScheduledTask("timemachine", "timemachine").isEmpty());

            APILocator.getTimeMachineAPI().setQuartzJobConfig("0 0 1 ? * * *", hosts, false,
                    langs, false);

            assertJobsRunnings(hosts, langs, "0 0 1 ? * * *", "0 0 0 ? * SUN *");
        } finally {
            removeJobs();
        }
    }
}