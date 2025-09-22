package com.dotcms.analytics.track.collectors;

import com.dotcms.IntegrationTestBase;
import com.dotcms.JUnit4WeldRunner;
import com.dotcms.LicenseTestUtil;
import com.dotcms.analytics.track.matchers.PagesAndUrlMapsRequestMatcher;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.UnknownHostException;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Verifies that the {@link PageDetailCollector} is able to collect the necessary data.
 *
 * @author Jose Castro
 * @since Oct 14th, 2024
 */

@ApplicationScoped
@RunWith(JUnit4WeldRunner.class)
@Ignore("Data Collectors have been disabled in favor of creating events via REST")
public class PageDetailCollectorTest extends IntegrationTestBase {

    private static final String PARENT_FOLDER_1_NAME = "news";
    private static final String TEST_URL_MAP_PAGE_NAME = "news-detail";
    private static final String TEST_PATTERN = "/testpattern/";
    private static final String TEST_URL_MAP_DETAIL_PAGE_URL = TEST_PATTERN + "mynews";

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
     *     <li><b>Method to test:
     *     </b>{@link PageDetailCollector#collect(CollectorContextMap, CollectorPayloadBean)}</li>
     *     <li><b>Given Scenario: </b> Calls the collector for a URL Mapped HTML Page.</li>
     *     <li><b>Expected Result: </b> The collected data for a URL Mapped page must be equal to
     *     the expected attributes.</li>
     * </ul>
     */
    @Test
    public void testPageDetailCollector() throws DotDataException, UnknownHostException, DotSecurityException {
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final String requestId = UUIDUtil.uuid();
        final HttpServletRequest request = Util.mockHttpRequestObj(response,
                TEST_URL_MAP_DETAIL_PAGE_URL, requestId,
                APILocator.getUserAPI().getAnonymousUser());

        final HTMLPageAsset testDetailPage = Util.createTestHTMLPage(testSite,
                TEST_URL_MAP_PAGE_NAME, PARENT_FOLDER_1_NAME);

        final String urlTitle = "mynews";
        final String urlMapPatternToUse = TEST_PATTERN + "{urlTitle}";
        final Language language = APILocator.getLanguageAPI().getDefaultLanguage();
        final long langId = language.getId();

        final ContentType urlMappedContentType = Util.getUrlMapLikeContentType(
                "News_" + System.currentTimeMillis(),
                testSite,
                testDetailPage.getIdentifier(),
                urlMapPatternToUse);
        final ContentletDataGen contentletDataGen = new ContentletDataGen(urlMappedContentType.id())
                .languageId(langId)
                .host(testSite)
                .setProperty("hostfolder", testSite)
                .setProperty("urlTitle", urlTitle)
                .setPolicy(IndexPolicy.WAIT_FOR);
        final Contentlet newsTestContent = contentletDataGen.nextPersisted();
        ContentletDataGen.publish(newsTestContent);

        final Map<String, Object> expectedDataMap = Map.of(
                Collector.EVENT_TYPE, EventType.PAGE_REQUEST.getType(),
                Collector.SITE_NAME, testSite.getHostname(),
                Collector.SITE_ID, testSite.getIdentifier(),
                Collector.LANGUAGE, language.getIsoCode(),
                Collector.URL, TEST_URL_MAP_DETAIL_PAGE_URL,
                Collector.OBJECT, Map.of(
                        Collector.ID, testDetailPage.getIdentifier(),
                        Collector.TITLE, testDetailPage.getTitle(),
                        Collector.URL, TEST_URL_MAP_DETAIL_PAGE_URL,
                        Collector.CONTENT_TYPE_ID, testDetailPage.getContentTypeId(),
                        Collector.CONTENT_TYPE_NAME, testDetailPage.getContentType().name(),
                        Collector.CONTENT_TYPE_VAR_NAME, testDetailPage.getContentType().variable(),
                        Collector.BASE_TYPE, newsTestContent.getContentType().baseType().name(),
                        Collector.LIVE, newsTestContent.isLive(),
                        Collector.WORKING, newsTestContent.isWorking(),
                        Collector.DETAIL_PAGE_URL, testDetailPage.getURI())
        );

        final Collector collector = new PageDetailCollector();
        final Map<String, Object> contextMap = Map.of(
                "uri", testDetailPage.getURI(),
                "pageMode", PageMode.LIVE,
                "currentHost", testSite,
                "requestId", requestId);
        final CollectorPayloadBean collectedData = Util.getCollectorPayloadBean(request,
                collector, new PagesAndUrlMapsRequestMatcher(), contextMap);

        assertTrue("Collected data map cannot be null or empty", UtilMethods.isSet(collectedData));

        Util.validateExpectedEntries(expectedDataMap, collectedData);
    }

}
