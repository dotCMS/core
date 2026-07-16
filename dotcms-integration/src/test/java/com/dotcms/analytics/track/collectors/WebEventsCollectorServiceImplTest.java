package com.dotcms.analytics.track.collectors;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.model.AnalyticsAppProperty;
import com.dotcms.analytics.track.matchers.FilesRequestMatcher;
import com.dotcms.analytics.track.matchers.PagesAndUrlMapsRequestMatcher;
import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotcms.analytics.track.matchers.VanitiesRequestMatcher;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.jitsu.EventLogRunnable;
import com.dotcms.jitsu.EventLogSubmitter;
import com.dotcms.jitsu.EventsPayload;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.JsonUtil;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.init.DotInitScheduler;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDUtil;
import io.vavr.control.Try;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static com.dotcms.analytics.app.AnalyticsApp.ANALYTICS_APP_CONFIG_URL_KEY;
import static com.dotcms.analytics.app.AnalyticsApp.ANALYTICS_APP_READ_URL_KEY;
import static com.dotcms.analytics.app.AnalyticsApp.ANALYTICS_APP_WRITE_URL_KEY;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Verifies that the {@link WebEventsCollectorService} class is working as expected.
 *
 * @author Jose Castro
 * @since Oct 3rd, 2024
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
@Ignore("Data Collectors have been disabled in favor of creating events via REST")
public class WebEventsCollectorServiceImplTest extends IntegrationTestBase {

    private static final String PARENT_FOLDER_1_NAME = "parent-folder";
    private static final String TEST_PAGE_NAME = "index";
    private static final String TEST_PAGE_URL = "/" + TEST_PAGE_NAME;
    private static final String TEST_URL_MAP_PAGE_NAME = "news-detail";
    private static final String TEST_PATTERN = "/testpattern/";
    private static final String TEST_URL_MAP_DETAIL_PAGE_URL = TEST_PATTERN + "mynews";
    private static final String URI = "/my-test/vanity-url";

    private static final String CLIENT_ID = "analytics-customer-customer1";
    private static final String CLIENT_SECRET = "testsecret";

    private static Host testSite = null;
    private static HTMLPageAsset testPage = null;
    private static HTMLPageAsset testDetailPage = null;
    private static Optional<CachedVanityUrl> resolvedVanity = Optional.empty();

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
        DotInitScheduler.start();

        final long millis = System.currentTimeMillis();
        final String siteName = "www.myTestSite-" + millis + ".com";
        testSite = new SiteDataGen().name(siteName).nextPersisted();

        final AppSecrets appSecrets = new AppSecrets.Builder()
                .withKey(AnalyticsApp.ANALYTICS_APP_KEY)
                .withSecret(AnalyticsAppProperty.CLIENT_ID.getPropertyName(), CLIENT_ID)
                .withHiddenSecret(AnalyticsAppProperty.CLIENT_SECRET.getPropertyName(), CLIENT_SECRET)
                .withSecret(
                        AnalyticsAppProperty.ANALYTICS_CONFIG_URL.getPropertyName(),
                        Config.getStringProperty(
                                ANALYTICS_APP_CONFIG_URL_KEY,
                                "http://localhost:8080/c/customer1/cluster1/keys"))
                .withSecret(
                        AnalyticsAppProperty.ANALYTICS_WRITE_URL.getPropertyName(),
                        Config.getStringProperty(ANALYTICS_APP_WRITE_URL_KEY, "http://localhost"))
                .withSecret(
                        AnalyticsAppProperty.ANALYTICS_READ_URL.getPropertyName(),
                        Config.getStringProperty(ANALYTICS_APP_READ_URL_KEY, "http://localhost"))
                .withSecret(AnalyticsAppProperty.ANALYTICS_KEY.getPropertyName(), "123")
                .build();
        APILocator.getAppsAPI().saveSecrets(appSecrets, testSite, APILocator.systemUser());
    }

    /**
     * This version of the {@link EventLogSubmitter} was created with the only purpose of to
     * testing the {@link WebEventsCollectorService} class by overriding the behavior of the
     * logEvent method to run it synchronously.
     */
    static class TestEventLogSubmitter extends EventLogSubmitter {

        public EventsPayload analyticsEventsPayload;

        public void logEvent(final EventLogRunnable eventLogRunnable) {
            analyticsEventsPayload = eventLogRunnable.getEventPayload().orElse(null);
        }

    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link }</li>
     *     <li><b>Given Scenario: </b></li>
     *     <li><b>Expected Result: </b></li>
     * </ul>
     */
    @Test
    public void testBasicProfileCollector() throws DotDataException, IOException {
        testPage = null != testPage ? testPage : Util.createTestHTMLPage(testSite, TEST_PAGE_NAME);

        final Map<String, Object> expectedDataMap = Map.of(
                "event_type", EventType.PAGE_REQUEST.getType(),
                "host", testSite.getHostname(),
                "url", TEST_PAGE_URL,
                "language", APILocator.getLanguageAPI().getDefaultLanguage().getIsoCode(),
                "object", Map.of(
                        "id", testPage.getIdentifier(),
                        "title", testPage.getTitle(),
                        "url", testPage.getURI())
        );

        final TestEventLogSubmitter submitter = new TestEventLogSubmitter();

        final WebEventsCollectorService webEventsCollectorService =
                new WebEventsCollectorServiceFactory.WebEventsCollectorServiceImpl(submitter);
        webEventsCollectorService.addCollector(new BasicProfileCollector());

        final HttpServletResponse response = mock(HttpServletResponse.class);
        final Map<String, Object> requestParams = Map.of(
                "host_id", testSite.getIdentifier()
        );
        final HttpServletRequest request = Util.mockHttpRequestObj(response, TEST_PAGE_URL,
                UUIDUtil.uuid(), APILocator.getUserAPI().getAnonymousUser(), null, requestParams);

        final RequestMatcher requestMatcher = new PagesAndUrlMapsRequestMatcher();
        webEventsCollectorService.fireCollectors(request, response, requestMatcher);

        assertNotNull(submitter.analyticsEventsPayload, "");
        for (EventsPayload.EventPayload payload : submitter.analyticsEventsPayload.payloads()) {
            final Map<String, Object> payloadData = JsonUtil.getJsonFromString(payload.toString());
            Util.validateExpectedEntries(expectedDataMap, payloadData);
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link }</li>
     *     <li><b>Given Scenario: </b></li>
     *     <li><b>Expected Result: </b></li>
     * </ul>
     */
    @Test
    public void testPagesCollector() throws DotDataException, IOException, DotSecurityException {
        testPage = null != testPage ? testPage : Util.createTestHTMLPage(testSite, TEST_PAGE_NAME);

        final Map<String, Object> expectedDataMap = Map.of(
                Collector.EVENT_TYPE, EventType.PAGE_REQUEST.getType(),
                Collector.SITE_NAME, testSite.getHostname(),
                Collector.URL, TEST_PAGE_URL,
                Collector.LANGUAGE, APILocator.getLanguageAPI().getDefaultLanguage().getIsoCode(),
                Collector.OBJECT, Map.of(
                        Collector.ID, testPage.getIdentifier(),
                        Collector.TITLE, testPage.getTitle(),
                        Collector.URL, testPage.getURI(),
                        Collector.CONTENT_TYPE_ID, testPage.getContentType().id(),
                        Collector.CONTENT_TYPE_NAME, testPage.getContentType().name(),
                        Collector.CONTENT_TYPE_VAR_NAME, testPage.getContentType().variable(),
                        Collector.BASE_TYPE, testPage.getContentType().baseType().name(),
                        Collector.LIVE, testPage.isLive(),
                        Collector.WORKING, testPage.isWorking()
                        )

        );

        final TestEventLogSubmitter submitter = new TestEventLogSubmitter();

        final WebEventsCollectorService webEventsCollectorService =
                new WebEventsCollectorServiceFactory.WebEventsCollectorServiceImpl(submitter);
        webEventsCollectorService.addCollector(new PagesCollector());

        final HttpServletResponse response = mock(HttpServletResponse.class);
        final Map<String, Object> requestParams = Map.of(
                "host_id", testSite.getIdentifier()
        );
        final HttpServletRequest request = Util.mockHttpRequestObj(response, TEST_PAGE_URL,
                UUIDUtil.uuid(), APILocator.getUserAPI().getAnonymousUser(), null, requestParams);

        final RequestMatcher requestMatcher = new PagesAndUrlMapsRequestMatcher();
        webEventsCollectorService.fireCollectors(request, response, requestMatcher);

        assertNotNull(submitter.analyticsEventsPayload, "");
        for (EventsPayload.EventPayload payload : submitter.analyticsEventsPayload.payloads()) {
            final Map<String, Object> payloadData = JsonUtil.getJsonFromString(payload.toString());
            Util.validateExpectedEntries(expectedDataMap, payloadData);
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link }</li>
     *     <li><b>Given Scenario: </b></li>
     *     <li><b>Expected Result: </b></li>
     * </ul>
     */
    @Test
    public void testPageDetailCollector() throws Exception {
        testDetailPage = null != testDetailPage ? testDetailPage : Util.createTestHTMLPage(testSite, TEST_URL_MAP_PAGE_NAME, PARENT_FOLDER_1_NAME);

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
                Collector.LANGUAGE, language.getIsoCode(),
                Collector.URL, TEST_URL_MAP_DETAIL_PAGE_URL,
                Collector.OBJECT, Map.of(
                        Collector.ID, testDetailPage.getIdentifier(),
                        Collector.TITLE, testDetailPage.getTitle(),
                        Collector.URL, TEST_URL_MAP_DETAIL_PAGE_URL,
                        Collector.CONTENT_TYPE_ID, testDetailPage.getContentTypeId(),
                        Collector.CONTENT_TYPE_NAME, testDetailPage.getContentType().name(),
                        Collector.CONTENT_TYPE_VAR_NAME, testDetailPage.getContentType().variable(),
                        Collector.BASE_TYPE, urlMappedContentType.baseType().name(),
                        Collector.LIVE, testDetailPage.isLive(),
                        Collector.WORKING, testDetailPage.isWorking(),
                        Collector.DETAIL_PAGE_URL, testDetailPage.getURI()


                )
        );

        final TestEventLogSubmitter submitter = new TestEventLogSubmitter();

        final WebEventsCollectorService webEventsCollectorService =
                new WebEventsCollectorServiceFactory.WebEventsCollectorServiceImpl(submitter);
        webEventsCollectorService.addCollector(new PageDetailCollector());

        final HttpServletResponse response = mock(HttpServletResponse.class);
        final Map<String, Object> requestParams = Map.of(
                "host_id", testSite.getIdentifier()
        );
        final HttpServletRequest request = Util.mockHttpRequestObj(response, TEST_URL_MAP_DETAIL_PAGE_URL,
                UUIDUtil.uuid(), APILocator.getUserAPI().getAnonymousUser(), null, requestParams);

        final RequestMatcher requestMatcher = new PagesAndUrlMapsRequestMatcher();
        webEventsCollectorService.fireCollectors(request, response, requestMatcher);

        assertNotNull(submitter.analyticsEventsPayload, "");
        for (EventsPayload.EventPayload payload : submitter.analyticsEventsPayload.payloads()) {
            final Map<String, Object> payloadData = JsonUtil.getJsonFromString(payload.toString());
            Util.validateExpectedEntries(expectedDataMap, payloadData);
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link }</li>
     *     <li><b>Given Scenario: </b></li>
     *     <li><b>Expected Result: </b></li>
     * </ul>
     */
    @Test
    public void testSyncVanitiesCollector() throws DotDataException, IOException, DotSecurityException {
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        testPage = null != testPage ? testPage : Util.createTestHTMLPage(testSite, TEST_PAGE_NAME);
        resolvedVanity = Util.createAndResolveVanityURL(testSite, "My Test Vanity", URI, TEST_PAGE_URL);

        assertTrue(resolvedVanity.isPresent(), "Test resolved vanity url must be present");

        final Map<String, Object> expectedDataMap = Map.of(
                Collector.SITE_ID, testSite.getIdentifier(),
                Collector.EVENT_TYPE, EventType.VANITY_REQUEST.getType(),
                Collector.LANGUAGE, defaultLanguage.getIsoCode(),
                Collector.VANITY_URL_KEY, TEST_PAGE_URL,
                Collector.LANGUAGE_ID, (int) defaultLanguage.getId(),
                Collector.URL, URI,
                Collector.OBJECT, Map.of(
                        Collector.FORWARD_TO, TEST_PAGE_URL,
                        Collector.RESPONSE, "200",
                        Collector.ID, resolvedVanity.get().vanityUrlId,
                        Collector.URL, URI)
        );

        final TestEventLogSubmitter submitter = new TestEventLogSubmitter();

        final WebEventsCollectorService webEventsCollectorService =
                new WebEventsCollectorServiceFactory.WebEventsCollectorServiceImpl(submitter);
        webEventsCollectorService.addCollector(new SyncVanitiesCollector());

        final HttpServletResponse response = mock(HttpServletResponse.class);
        final Map<String, Object> requestParams = Map.of(
                "host_id", testSite.getIdentifier()
        );
        final Map<String, Object> requestAttrs = Map.of(
                Constants.CMS_FILTER_URI_OVERRIDE, TEST_PAGE_URL
        );
        final Map<String, Object> characterMap = Map.of(
                "uri", URI,
                "currentHost", testSite,
                Constants.VANITY_URL_OBJECT, resolvedVanity.get());
        final HttpServletRequest request = Util.mockHttpRequestObj(response, URI,
                UUIDUtil.uuid(), APILocator.getUserAPI().getAnonymousUser(), requestAttrs, requestParams, characterMap);

        final RequestMatcher requestMatcher = new VanitiesRequestMatcher();
        webEventsCollectorService.fireCollectors(request, response, requestMatcher);

        assertNotNull(submitter.analyticsEventsPayload, "");
        for (EventsPayload.EventPayload payload : submitter.analyticsEventsPayload.payloads()) {
            final Map<String, Object> payloadData = JsonUtil.getJsonFromString(payload.toString());
            Util.validateExpectedEntries(expectedDataMap, payloadData);
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link }</li>
     *     <li><b>Given Scenario: </b></li>
     *     <li><b>Expected Result: </b></li>
     * </ul>
     */
    @Test
    public void testAsyncVanitiesCollector() throws DotDataException, IOException, DotSecurityException {
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        testPage = null != testPage ? testPage : Util.createTestHTMLPage(testSite, TEST_PAGE_NAME);
        resolvedVanity = Util.createAndResolveVanityURL(testSite, "My Test Vanity", URI, TEST_PAGE_URL);

        assertTrue(resolvedVanity.isPresent(), "Test resolved vanity url must be present");

        final Map<String, Object> expectedDataMap = Map.of(
                Collector.EVENT_TYPE, EventType.PAGE_REQUEST.getType(),
                Collector.SITE_NAME, testSite.getHostname(),
                Collector.LANGUAGE, defaultLanguage.getIsoCode(),
                Collector.URL, TEST_PAGE_URL,
                Collector.OBJECT, Map.of(
                        Collector.ID, testPage.getIdentifier(),
                        Collector.TITLE, TEST_PAGE_NAME,
                        Collector.URL, TEST_PAGE_URL,
                        Collector.CONTENT_TYPE_ID, testPage.getContentType().id(),
                        Collector.CONTENT_TYPE_NAME, testPage.getContentType().name(),
                        Collector.CONTENT_TYPE_VAR_NAME, testPage.getContentType().variable(),
                        Collector.BASE_TYPE, testPage.getContentType().baseType().name(),
                        Collector.LIVE, testPage.isLive(),
                        Collector.WORKING, testPage.isWorking())
        );

        final TestEventLogSubmitter submitter = new TestEventLogSubmitter();

        final WebEventsCollectorService webEventsCollectorService =
                new WebEventsCollectorServiceFactory.WebEventsCollectorServiceImpl(submitter);
        webEventsCollectorService.addCollector(new AsyncVanitiesCollector());

        final HttpServletResponse response = mock(HttpServletResponse.class);
        final Map<String, Object> requestParams = Map.of(
                "host_id", testSite.getIdentifier()
        );
        final Map<String, Object> requestAttrs = Map.of(
                Constants.CMS_FILTER_URI_OVERRIDE, TEST_PAGE_URL
        );
        final Map<String, Object> characterMap = Map.of(
                "uri", URI,
                "currentHost", testSite,
                Constants.VANITY_URL_OBJECT, resolvedVanity.get());
        final HttpServletRequest request = Util.mockHttpRequestObj(response, URI,
                UUIDUtil.uuid(), APILocator.getUserAPI().getAnonymousUser(), requestAttrs, requestParams, characterMap);

        final RequestMatcher requestMatcher = new VanitiesRequestMatcher();
        webEventsCollectorService.fireCollectors(request, response, requestMatcher);

        assertNotNull(submitter.analyticsEventsPayload, "");
        for (EventsPayload.EventPayload payload : submitter.analyticsEventsPayload.payloads()) {
            final Map<String, Object> payloadData = JsonUtil.getJsonFromString(payload.toString());
            Util.validateExpectedEntries(expectedDataMap, payloadData);
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link }</li>
     *     <li><b>Given Scenario: </b></li>
     *     <li><b>Expected Result: </b></li>
     * </ul>
     */
    @Test
    public void testFilesCollector() throws DotDataException, IOException, DotSecurityException {
        final FileAsset testFileAsset = Util.createTestFileAsset("my-test-file_" + System.currentTimeMillis(),
                ".txt","Sample content for my test file", "parent-folder-for-file", testSite);

        final Map<String, Object> expectedDataMap = Map.of(
                Collector.SITE_NAME, testSite.getHostname(),
                Collector.SITE_ID, testSite.getIdentifier(),
                Collector.LANGUAGE, APILocator.getLanguageAPI().getDefaultLanguage().getIsoCode(),
                Collector.EVENT_TYPE, EventType.FILE_REQUEST.getType(),
                Collector.URL, testFileAsset.getURI(),
                Collector.OBJECT, Map.of(
                        Collector.ID, testFileAsset.getIdentifier(),
                        Collector.TITLE, testFileAsset.getTitle(),
                        Collector.URL, testFileAsset.getURI(),
                        Collector.CONTENT_TYPE_ID, testFileAsset.getContentType().id(),
                        Collector.CONTENT_TYPE_NAME, testFileAsset.getContentType().name(),
                        Collector.CONTENT_TYPE_VAR_NAME, testFileAsset.getContentType().variable(),
                        Collector.BASE_TYPE, testFileAsset.getContentType().baseType().name(),
                        Collector.LIVE,    String.valueOf(Try.of(()->testFileAsset.isLive()).getOrElse(false)),
                        Collector.WORKING, String.valueOf(Try.of(()->testFileAsset.isWorking()).getOrElse(false))
                        )

        );

        final TestEventLogSubmitter submitter = new TestEventLogSubmitter();

        final WebEventsCollectorService webEventsCollectorService =
                new WebEventsCollectorServiceFactory.WebEventsCollectorServiceImpl(submitter);
        webEventsCollectorService.addCollector(new FilesCollector());

        final HttpServletResponse response = mock(HttpServletResponse.class);
        final Map<String, Object> requestParams = Map.of(
                "host_id", testSite.getIdentifier()
        );
        final HttpServletRequest request = Util.mockHttpRequestObj(response, testFileAsset.getURI(), UUIDUtil.uuid(),
                APILocator.getUserAPI().getAnonymousUser(), null, requestParams);

        final RequestMatcher requestMatcher = new FilesRequestMatcher();
        webEventsCollectorService.fireCollectors(request, response, requestMatcher);

        assertNotNull(submitter.analyticsEventsPayload, "");
        for (EventsPayload.EventPayload payload : submitter.analyticsEventsPayload.payloads()) {
            final Map<String, Object> payloadData = JsonUtil.getJsonFromString(payload.toString());
            Util.validateExpectedEntries(expectedDataMap, payloadData);
        }
    }

}
