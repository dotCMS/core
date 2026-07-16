package com.dotcms.telemetry.collectors.theme;

import com.dotcms.IntegrationTestBase;
import com.dotcms.JUnit4WeldRunner;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.telemetry.MetricType;
import com.dotcms.telemetry.MetricValue;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.Dependent;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertTrue;

/**
 * Verifies that the Telemetry Metric called {@link TotalSizeOfFilesPerThemeMetricType} class is
 * working as expected.
 *
 * @author Jose Castro
 * @since Mar 25th, 2025
 */
@Dependent
@RunWith(JUnit4WeldRunner.class)
public class TotalSizeOfFilesPerThemeMetricTypeTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link TotalSizeOfFilesPerThemeMetricType#getStat()}</li>
     *     <li><b>Given Scenario: </b>Retrieve the size of all files under evey Theme in all Sites
     *     in the current repo.</li>
     *     <li><b>Expected Result: </b>Find the test Site created in this test inside the Map
     *     returned by the Metric Stat.</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testTotalFileSizeInTheme() throws DotDataException, DotSecurityException, IOException {
        // ╔══════════════════╗
        // ║  Initialization  ║
        // ╚══════════════════╝
        final long timestamp = System.currentTimeMillis();
        final String testSiteName = "www.mytestsite-" + timestamp + ".com";
        final String[] expectedFolderPathForThemes = {"application", "themes", "test-theme"};
        final String expectedThemeVtl = "template.vtl";
        final String dummyContent = "This is some text for the test VTL file";
        final String themeKeyInMap = "//" + testSiteName + "/" + expectedFolderPathForThemes[0] + "/" + expectedFolderPathForThemes[1] + "/" + expectedFolderPathForThemes[2] + "/";

        // ╔════════════════════════╗
        // ║  Generating Test data  ║
        // ╚════════════════════════╝
        final Host testSite = new SiteDataGen().name(testSiteName).nextPersisted();
        ContentletDataGen.publish(testSite);
        final Folder applicationFolder = new FolderDataGen()
                .name(expectedFolderPathForThemes[0])
                .site(testSite).nextPersisted();
        final Folder themesFolder = new FolderDataGen()
                .name(expectedFolderPathForThemes[1])
                .parent(applicationFolder).nextPersisted();
        final Folder testThemeFolder = new FolderDataGen()
                .name(expectedFolderPathForThemes[2])
                .parent(themesFolder).nextPersisted();
        final java.io.File file = java.io.File.createTempFile("template", ".vtl");
        FileUtil.write(file, dummyContent);
        final Contentlet testFile = new FileAssetDataGen(testThemeFolder, file)
                .title(expectedThemeVtl)
                .setProperty(FileAssetAPI.FILE_NAME_FIELD, expectedThemeVtl).nextPersisted();
        ContentletDataGen.publish(testFile);

        final MetricType metricType = new TotalSizeOfFilesPerThemeMetricType();
        final Optional<MetricValue> metricStatsOptional = metricType.getStat();

        // ╔══════════════╗
        // ║  Assertions  ║
        // ╚══════════════╝
        assertTrue("Stats object must NOT be empty", metricStatsOptional.isPresent());
        final MetricValue metricValue = metricStatsOptional.get();
        assertTrue("Stats object must be of type 'Map'", metricValue.getValue() instanceof Map);
        assertTrue("There must be an entry for the 'test-theme' in the results", ((Map<String, Long>) metricValue.getValue()).containsKey(themeKeyInMap));
    }

}
