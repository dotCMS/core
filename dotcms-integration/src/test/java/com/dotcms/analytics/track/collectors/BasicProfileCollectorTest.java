package com.dotcms.analytics.track.collectors;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.analytics.track.matchers.PagesAndUrlMapsRequestMatcher;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.UnknownHostException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
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

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link }</li>
     *     <li><b>Given Scenario: </b></li>
     *     <li><b>Expected Result: </b></li>
     * </ul>
     */
    @Test
    public void collectBasicProfileData() throws DotDataException, UnknownHostException {
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final String requestId = UUIDUtil.uuid();
        final HttpServletRequest request = Util.mockHttpRequestObj(response, "/", requestId,
                APILocator.getUserAPI().getAnonymousUser());
        final Map<String, Object> expectedDataMap = Map.of(
                "renderMode", "LIVE",
                "cluster", CLUSTER_ID,
                "server", SERVER_ID,
                "persona", "dot:default",
                "utc_time", "2024-10-09T00:00:00.000000Z",
                "sessionNew", true,
                "userAgent", Util.USER_AGENT,
                "sessionId", "DAA3339CD687D9ABD4101CF9EDDD42DB",
                "request_id", requestId
        );
        final Collector collector = new BasicProfileCollector();
        final CollectorPayloadBean collectedData = Util.getCollectorPayloadBean(request, collector, new PagesAndUrlMapsRequestMatcher(), null);

        assertTrue("Collected data map cannot be null or empty", UtilMethods.isSet(collectedData));

        int counter = 0;
        for (final String key : expectedDataMap.keySet()) {
            if (collectedData.toMap().containsKey(key)) {
                final Object expectedValue = expectedDataMap.get(key);
                final Object collectedValue = collectedData.toMap().get(key);
                if (!"utc_time".equalsIgnoreCase(key)) {
                    assertEquals("Collected value must be equal to expected value for key: " + key, expectedValue, collectedValue);
                }
                counter++;
            }
        }
        assertEquals("Number of returned expected properties doesn't match", counter, expectedDataMap.size());
    }

}
