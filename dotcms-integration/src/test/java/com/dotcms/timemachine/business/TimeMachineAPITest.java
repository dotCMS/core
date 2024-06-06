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

import com.dotmarketing.util.Config;

import com.dotmarketing.util.FileUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Calendar;
import java.util.stream.Collectors;

import java.util.stream.Stream;

import static com.dotcms.util.CollectionsUtils.list;
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

        final File assetFolder = new File(Config.getStringProperty("ASSET_REAL_PATH", null));
        final File bundleFolder = new File(assetFolder, "bundles");
        bundleFolder.mkdir();

        try {
            Config.setProperty("TIMEMACHINE_PATH", tmTestingFolder.toPath().toAbsolutePath().toString());

            final List<File> expireFoldersInTimeMachine = createFiles(tmTestingFolder, "tm_expired_",
                    91, 100);
            final List<File> notExpireFoldersInTimeMachine = createFiles(tmTestingFolder, "tm_not_expired_",
                    45, 89);

            final List<File> expireFoldersInBundle = createFiles(bundleFolder, "timeMachineBundle_expired_",
                    91, 100);
            final List<File> notExpireFoldersInBundle = createFiles(bundleFolder, "timeMachineBundle_not_expired_",
                    45, 89);

            final List<File> removedFiles = APILocator.getTimeMachineAPI().removeOldTimeMachineBackupsFiles();

            final List<File> expireFolders = Stream
                    .concat(expireFoldersInTimeMachine.stream(), expireFoldersInBundle.stream())
                    .collect(Collectors.toList());

            assertEquals(expireFolders.size(), removedFiles.size());
            assertTrue(removeAll(expireFolders, removedFiles).isEmpty());

            final List<File> noExpireFolders = Stream
                    .concat(notExpireFoldersInTimeMachine.stream(), notExpireFoldersInBundle.stream())
                    .collect(Collectors.toList());

            final List<File> files = Stream.concat(Arrays.stream(tmTestingFolder.listFiles()),
                            Arrays.stream(bundleFolder.listFiles()))
                    .collect(Collectors.toList());

            assertTrue(noExpireFolders.size() <= files.size());
            assertTrue(removeAll(noExpireFolders, files).isEmpty());
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

}