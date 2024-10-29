package com.dotcms.analytics.track.collectors;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.analytics.track.matchers.FilesRequestMatcher;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Verifies that the {@link FilesCollector} is able to collect the necessary data.
 *
 * @author Jose Castro
 * @since Oct 16th, 2024
 */
public class FilesCollectorTest extends IntegrationTestBase {

    private static final String PARENT_FOLDER_1_NAME = "parent-folder";

    private static Host testSite = null;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();

        final long millis = System.currentTimeMillis();
        final String siteName = "www.myTestSite-" + millis + ".com";
        testSite = new SiteDataGen().name(siteName).nextPersisted();
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link }</li>
     *     <li><b>Given Scenario: </b></li>
     *     <li><b>Expected Result: </b></li>
     * </ul>
     */
    @Test
    public void collectFileData() throws DotDataException, IOException, DotSecurityException {
        final FileAsset testFileAsset = Util.createTestFileAsset("my-test-file_" + System.currentTimeMillis(),
                ".txt","Sample content for my test file", PARENT_FOLDER_1_NAME, testSite);

        final HttpServletResponse response = mock(HttpServletResponse.class);
        final String requestId = UUIDUtil.uuid();
        final HttpServletRequest request = Util.mockHttpRequestObj(response, testFileAsset.getURI(), requestId,
                APILocator.getUserAPI().getAnonymousUser());

        final Map<String, Object> expectedDataMap = Map.of(
                "host", "localhost:8080",
                "site", testSite.getIdentifier(),
                "language", APILocator.getLanguageAPI().getDefaultLanguage().getIsoCode(),
                "event_type", EventType.FILE_REQUEST.getType(),
                "url", testFileAsset.getURI(),
                "object", Map.of(
                        "id", testFileAsset.getIdentifier(),
                        "title", testFileAsset.getTitle(),
                        "url", testFileAsset.getURI())
        );

        final Collector collector = new FilesCollector();
        final Map<String, Object> contextMap = Map.of(
                "uri", testFileAsset.getURI(),
                "pageMode", PageMode.LIVE,
                "currentHost", testSite,
                "requestId", requestId);
        final CollectorPayloadBean collectedData = Util.getCollectorPayloadBean(request, collector, new FilesRequestMatcher(), contextMap);

        assertTrue("Collected data map cannot be null or empty", UtilMethods.isSet(collectedData));

        Util.validateExpectedEntries(expectedDataMap, collectedData);
    }

}
