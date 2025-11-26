package com.dotcms.analytics.track.collectors;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.analytics.track.matchers.PagesAndUrlMapsRequestMatcher;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.telemetry.business.MetricsAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.UnknownHostException;
import java.util.Map;

import static com.dotcms.analytics.track.collectors.Collector.CUSTOMER_CATEGORY;
import static com.dotcms.analytics.track.collectors.Collector.CUSTOMER_NAME;
import static com.dotcms.analytics.track.collectors.Collector.ENVIRONMENT_NAME;
import static com.dotcms.analytics.track.collectors.Collector.ENVIRONMENT_VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Verifies that the {@link BasicProfileCollector} is able to collect the basic profile data and
 * works as expected.
 *
 * @author Jose Castro
 * @since Oct 9th, 2024
 */
@Ignore("Data Collectors have been disabled in favor of creating events via REST")
public class BasicProfileCollectorTest extends IntegrationTestBase {

    private static String CLUSTER_ID = null;
    private static String SERVER_ID = null;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();

        CLUSTER_ID = APILocator.getShortyAPI().shortify(ClusterFactory.getClusterId());
        SERVER_ID = APILocator.getShortyAPI().shortify(APILocator.getServerAPI().readServerId());
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b>
     *     {@link BasicProfileCollector#collect(CollectorContextMap, CollectorPayloadBean)}</li>
     *     <li><b>Given Scenario:</b> Simulate the collection of Basic Profile data, which is
     *     data that is ALWAYS collected for any kind of Data Collector, and compare it with an
     *     expected data map.</li>
     *     <li><b>Expected Result:</b> Both the collected data map and the expected data must
     *     match.</li>
     * </ul>
     */
    @Test
    public void collectBasicProfileData() throws DotDataException, UnknownHostException {
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final String requestId = UUIDUtil.uuid();
        final HttpServletRequest request = Util.mockHttpRequestObj(response, "/", requestId,
                APILocator.getUserAPI().getAnonymousUser());
        final Map<String, Object> expectedDataMap = new java.util.HashMap<>(Map.of(
                Collector.CLUSTER, CLUSTER_ID,
                Collector.SERVER, SERVER_ID,
                Collector.PERSONA, "dot:default",
                Collector.UTC_TIME, "2024-10-09T00:00:00.000000Z",
                Collector.SESSION_NEW, true,
                Collector.USER_AGENT, Util.USER_AGENT,
                Collector.SESSION_ID, "DAA3339CD687D9ABD4101CF9EDDD42DB",
                Collector.REQUEST_ID, requestId
        ));
        expectedDataMap.putAll(Map.of(
                Collector.EVENT_SOURCE, EventSource.DOT_CMS.getName(),
                Collector.IS_TARGET_PAGE, false,
                Collector.IS_EXPERIMENT_PAGE, false,
                Collector.USER_OBJECT, Map.of(
                        "identifier", "anonymous",
                        "email", "anonymous@dotcms.anonymoususer")));
        // The values returned when running the Integration Tests are random. So, in this case,
        // we'll just verify that the attributes are present, and add any values in here
        expectedDataMap.putAll(Map.of(
                CUSTOMER_NAME, "",
                CUSTOMER_CATEGORY, "",
                ENVIRONMENT_NAME, "",
                ENVIRONMENT_VERSION, 0
        ));
        final Collector collector = new BasicProfileCollector();
        final CollectorPayloadBean collectedData = Util.getCollectorPayloadBean(request, collector, new PagesAndUrlMapsRequestMatcher(), null);

        assertTrue("Collected data map cannot be null or empty", UtilMethods.isSet(collectedData));

        int counter = 0;
        for (final String key : expectedDataMap.keySet()) {
            if (collectedData.toMap().containsKey(key)) {
                final Object expectedValue = expectedDataMap.get(key);
                final Object collectedValue = collectedData.toMap().get(key);
                if (CUSTOMER_NAME.equalsIgnoreCase(key) || CUSTOMER_CATEGORY.equalsIgnoreCase(key) ||
                        ENVIRONMENT_NAME.equalsIgnoreCase(key) || ENVIRONMENT_VERSION.equalsIgnoreCase(key)) {
                    assertNotNull(String.format("Collected value '%s' cannot be null", key), collectedValue);
                } else if (!Collector.UTC_TIME.equalsIgnoreCase(key)) {
                    assertEquals("Collected value must be equal to expected value for key: " + key, expectedValue, collectedValue);
                }
                counter++;
            }
        }
        final MetricsAPI metricsAPI = APILocator.getMetricsAPI();
        final MetricsAPI.Client client = metricsAPI.getClient();
        // In local envs, the 'category_name' attribute maybe null, and is NOT added to the
        // collected data map, so the assertion below would fail. This hack is just to make this
        // test run locally without devs having to tweak it
        final boolean areAllAttrsPresent = client.getVersion() >= 0 && UtilMethods.isSet(client.getEnvironment()) &&
                UtilMethods.isSet(client.getCategory()) && UtilMethods.isSet(client.getClientName());
        if (areAllAttrsPresent) {
            assertEquals("Number of returned expected properties doesn't match", counter, expectedDataMap.size());
        }
    }

}
