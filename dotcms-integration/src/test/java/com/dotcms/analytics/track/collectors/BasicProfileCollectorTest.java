package com.dotcms.analytics.track.collectors;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.analytics.track.matchers.PagesAndUrlMapsRequestMatcher;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.UnknownHostException;
import java.util.Map;

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

    @AfterClass
    public static void afterClass() {
        // Re-setting the Telemetry env vars
        Config.setProperty("TELEMETRY_CLIENT_NAME", null);
        Config.setProperty("TELEMETRY_CLIENT_CATEGORY", null);
        Config.setProperty("TELEMETRY_CLIENT_ENV", null);
        Config.setProperty("TELEMETRY_CLIENT_ENV", null);
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
                Collector.CUSTOMER_NAME, "",
                Collector.CUSTOMER_CATEGORY, "",
                Collector.ENVIRONMENT_NAME, "",
                Collector.ENVIRONMENT_VERSION, 0
        ));
        final Collector collector = new BasicProfileCollector();
        final CollectorPayloadBean collectedData = Util.getCollectorPayloadBean(request, collector, new PagesAndUrlMapsRequestMatcher(), null);

        assertTrue("Collected data map cannot be null or empty", UtilMethods.isSet(collectedData));

        int counter = 0;
        for (final String key : expectedDataMap.keySet()) {
            if (collectedData.toMap().containsKey(key)) {
                final Object expectedValue = expectedDataMap.get(key);
                final Object collectedValue = collectedData.toMap().get(key);
                if (!Collector.UTC_TIME.equalsIgnoreCase(key)) {
                    assertEquals("Collected value must be equal to expected value for key: " + key, expectedValue, collectedValue);
                }
                if ("TELEMETRY_CLIENT_NAME".equalsIgnoreCase(key) || "TELEMETRY_CLIENT_CATEGORY".equalsIgnoreCase(key) ||
                        "TELEMETRY_CLIENT_ENV".equalsIgnoreCase(key) || "TELEMETRY_CLIENT_VERSION".equalsIgnoreCase(key)) {
                    assertNotNull(String.format("Collected value '%s' cannot be null", key), collectedValue);
                }
                counter++;
            }
        }
        assertEquals("Number of returned expected properties doesn't match", counter, expectedDataMap.size());
    }

}
