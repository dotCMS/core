package com.dotcms.analytics.track.collectors;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Verifies that the {@link AsyncVanitiesCollector} is able to collect the necessary data.
 *
 * @author Jose Castro
 * @since Oct 21st, 2024
 */
@Ignore("Data Collectors have been disabled in favor of creating events via REST")
public class AsyncVanitiesCollectorTest extends IntegrationTestBase {

    private static final String TEST_PAGE_NAME = "index";
    private static final String TEST_PAGE_URL = "/" + TEST_PAGE_NAME;
    private static final String URI = "/my-test/vanity-url";

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
     *     </b>{@link AsyncVanitiesCollector#collect(CollectorContextMap, CollectorPayloadBean)}
     *     </li>
     *     <li><b>Given Scenario: </b> Calls the collector for a Vanity URL asynchronously.</li>
     *     <li><b>Expected Result: </b> The returned data must be equal to the expected one.</li>
     * </ul>
     */
    @Test
    public void collectAsyncVanityData() throws DotDataException, IOException,
            DotSecurityException {
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final String requestId = UUIDUtil.uuid();
        final Map<String, Object> requestAttrs = Map.of(
                Constants.CMS_FILTER_URI_OVERRIDE, TEST_PAGE_URL
        );
        final HttpServletRequest request = Util.mockHttpRequestObj(response, URI, requestId,
                APILocator.getUserAPI().getAnonymousUser(), requestAttrs, null);

        // Vanity URL will point to this HTML Page
        final HTMLPageAsset testHTMLPage = Util.createTestHTMLPage(testSite, TEST_PAGE_NAME);

        assertNotNull("Test HTML Page cannot be null", testHTMLPage);

        final Optional<CachedVanityUrl> resolvedVanity = Util.createAndResolveVanityURL(testSite,
                "My Test Vanity", URI, TEST_PAGE_URL);

        assertTrue("Resolved vanity url must be present", resolvedVanity.isPresent());

        final Map<String, Object> expectedDataMap = Map.of(
                Collector.EVENT_TYPE, EventType.PAGE_REQUEST.getType(),
                Collector.SITE_NAME, testSite.getHostname(),
                Collector.LANGUAGE, defaultLanguage.getIsoCode(),
                Collector.URL, TEST_PAGE_URL,
                Collector.OBJECT, Map.of(
                        Collector.ID, testHTMLPage.getIdentifier(),
                        Collector.TITLE, TEST_PAGE_NAME,
                        Collector.URL, TEST_PAGE_URL,
                        Collector.CONTENT_TYPE_ID, testHTMLPage.getContentType().id(),
                        Collector.CONTENT_TYPE_NAME, testHTMLPage.getContentType().name(),
                        Collector.CONTENT_TYPE_VAR_NAME, testHTMLPage.getContentType().variable(),
                        Collector.BASE_TYPE, testHTMLPage.getContentType().baseType().name(),
                        Collector.LIVE, testHTMLPage.isLive(),
                        Collector.WORKING, testHTMLPage.isWorking())
        );

        final Collector collector = new AsyncVanitiesCollector();
        final Map<String, Object> contextMap = Map.of(
                "uri", URI,
                "currentHost", testSite,
                Constants.VANITY_URL_OBJECT, resolvedVanity.get());
        final CollectorPayloadBean collectedData =
                Util.getRequestCharacterCollectorPayloadBean(request, collector, contextMap);

        assertTrue("Collected data map cannot be null or empty", UtilMethods.isSet(collectedData));

        Util.validateExpectedEntries(expectedDataMap, collectedData);
    }

}
