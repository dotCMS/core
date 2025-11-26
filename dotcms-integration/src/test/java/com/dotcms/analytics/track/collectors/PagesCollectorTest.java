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
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
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
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Verifies that the {@link PagesCollector} is able to collect the necessary data.
 *
 * @author Jose Castro
 * @since Oct 9th, 2024
 */
@ApplicationScoped
@RunWith(JUnit4WeldRunner.class)
@Ignore("Data Collectors have been disabled in favor of creating events via REST")
public class PagesCollectorTest extends IntegrationTestBase {

    private static final String TEST_PAGE_NAME = "index";
    private static final String TEST_PAGE_URL = "/" + TEST_PAGE_NAME;

    private static final String PARENT_FOLDER_1_NAME = "news";
    private static final String TEST_URL_MAP_PAGE_NAME = "news-detail";
    private static final String URL_MAP_PATTERN = "/testpattern/";
    private static final String TEST_URL_MAP_DETAIL_PAGE_URL = URL_MAP_PATTERN + "mynews";

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
     *     </b>{@link PagesCollector#collect(CollectorContextMap, CollectorPayloadBean)}</li>
     *     <li><b>Given Scenario: </b> Calls the collector for an HTML Page.</li>
     *     <li><b>Expected Result: </b> The returned data must be equal to the expected
     *     parameters.</li>
     * </ul>
     */
    @Test
    public void collectPageData() throws Exception {
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final String requestId = UUIDUtil.uuid();
        final HttpServletRequest request = Util.mockHttpRequestObj(response, TEST_PAGE_URL,
                requestId, APILocator.getUserAPI().getAnonymousUser());

        final HTMLPageAsset testPage = Util.createTestHTMLPage(testSite, TEST_PAGE_NAME);

        final Map<String, Object> expectedDataMap = Map.of(
                Collector.EVENT_TYPE, EventType.PAGE_REQUEST.getType(),
                Collector.SITE_NAME, testSite.getHostname(),
                Collector.LANGUAGE, APILocator.getLanguageAPI().getDefaultLanguage().getIsoCode(),
                Collector.URL, TEST_PAGE_URL,
                Collector.OBJECT, Map.of(
                        Collector.ID, testPage.getIdentifier(),
                        Collector.TITLE, testPage.getTitle(),
                        Collector.URL, TEST_PAGE_URL,
                        Collector.CONTENT_TYPE_ID, testPage.getContentType().id(),
                        Collector.CONTENT_TYPE_NAME, testPage.getContentType().name(),
                        Collector.CONTENT_TYPE_VAR_NAME, testPage.getContentType().variable(),
                        Collector.BASE_TYPE, testPage.getContentType().baseType().name(),
                        Collector.LIVE, testPage.isLive(),
                        Collector.WORKING, testPage.isWorking())
        );

        final Collector collector = new PagesCollector();
        final Map<String, Object> contextMap = Map.of(
                "uri", testPage.getURI(),
                "pageMode", PageMode.LIVE,
                "currentHost", testSite,
                "requestId", requestId);
        final CollectorPayloadBean collectedData = Util.getCollectorPayloadBean(request,
                collector, new PagesAndUrlMapsRequestMatcher(), contextMap);

        assertTrue("Collected data map cannot be null or empty", UtilMethods.isSet(collectedData));

        Util.validateExpectedEntries(expectedDataMap, collectedData);
    }

    /**
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link PagesCollector#collect(CollectorContextMap, CollectorPayloadBean)}</li>
     *     <li><b>Given Scenario: </b> Calls the collector for a URL Mapped HTML Page.</li>
     *     <li><b>Expected Result: </b>The collected data for a URL Mapped page must be equal to
     *     the expected attributes.</li>
     * </ul>
     */
    @Test
    public void collectUrlMapPageData() throws Exception {
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final String requestId = UUIDUtil.uuid();
        final HttpServletRequest request = Util.mockHttpRequestObj(response,
                TEST_URL_MAP_DETAIL_PAGE_URL, requestId,
                APILocator.getUserAPI().getAnonymousUser());

        final HTMLPageAsset testDetailPage = Util.createTestHTMLPage(testSite,
                TEST_URL_MAP_PAGE_NAME, PARENT_FOLDER_1_NAME);

        final String urlTitle = "mynews";
        final String urlMapPatternToUse = URL_MAP_PATTERN + "{urlTitle}";
        final long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();

        final ContentType urlMappedContentType = Util.getUrlMapLikeContentType(
                "News_" + System.currentTimeMillis(),
                testSite,
                testDetailPage.getIdentifier(),
                urlMapPatternToUse);

        assertNotNull("The test URL Map Content Type cannot be null", urlMappedContentType);

        final ContentletDataGen contentletDataGen = new ContentletDataGen(urlMappedContentType.id())
                .languageId(langId)
                .host(testSite)
                .setProperty("hostfolder", testSite)
                .setProperty("urlTitle", urlTitle)
                .setPolicy(IndexPolicy.WAIT_FOR);
        final Contentlet newsTestContent = contentletDataGen.nextPersisted();
        ContentletDataGen.publish(newsTestContent);

        final Map<String, Object> expectedDataMap = Map.of(
                Collector.EVENT_TYPE, EventType.URL_MAP.getType(),
                Collector.SITE_NAME, testSite.getHostname(),
                Collector.LANGUAGE, APILocator.getLanguageAPI().getDefaultLanguage().getIsoCode(),
                Collector.URL, TEST_URL_MAP_DETAIL_PAGE_URL,
                Collector.OBJECT, Map.of(
                        Collector.ID, newsTestContent.getIdentifier(),
                        Collector.TITLE, urlTitle,
                        Collector.URL, TEST_URL_MAP_DETAIL_PAGE_URL,
                        Collector.CONTENT_TYPE_ID, urlMappedContentType.id(),
                        Collector.CONTENT_TYPE_NAME, urlMappedContentType.name(),
                        Collector.CONTENT_TYPE_VAR_NAME, urlMappedContentType.variable(),
                        Collector.BASE_TYPE, urlMappedContentType.baseType().name(),
                        Collector.LIVE, newsTestContent.isLive(),
                        Collector.WORKING, newsTestContent.isWorking()
                        )
        );

        final Collector collector = new PagesCollector();
        final Map<String, Object> contextMap = new HashMap<>(Map.of(
                "uri", testDetailPage.getURI(),
                "pageMode", PageMode.LIVE,
                "currentHost", testSite,
                "requestId", requestId));
        final CollectorPayloadBean collectedData = Util.getCollectorPayloadBean(request,
                collector, new PagesAndUrlMapsRequestMatcher(), contextMap);

        assertTrue("Collected data map cannot be null or empty", UtilMethods.isSet(collectedData));

        Util.validateExpectedEntries(expectedDataMap, collectedData);
    }

}
