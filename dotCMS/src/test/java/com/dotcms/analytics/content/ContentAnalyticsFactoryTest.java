package com.dotcms.analytics.content;

import com.dotcms.analytics.AnalyticsTestUtils;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.ResultSetItem;
import com.dotcms.analytics.model.TokenStatus;
import com.dotcms.analytics.query.AnalyticsQuery;
import com.dotcms.analytics.query.AnalyticsQueryParser;
import com.dotcms.cube.CubeJSClient;
import com.dotcms.cube.CubeJSClientFactory;
import com.dotcms.cube.CubeJSQuery;
import com.dotcms.http.server.mock.MockHttpServer;
import com.dotcms.http.server.mock.MockHttpServerContext;
import com.dotcms.util.JsonUtil;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.HttpURLConnection;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for the {@link ContentAnalyticsFactory} class.
 *
 * @author Jose Castro
 * @since sep 18th, 2024
 */
public class ContentAnalyticsFactoryTest {

    /**
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link ContentAnalyticsFactory#getReport(AnalyticsQuery, User)}
     *     </li>
     *     <li><b>Given Scenario: </b>Run a report for a given query.</li>
     *     <li><b>Expected Result: </b>The simulated results must adhere to the data specified in
     *     the query. A mock HTTP Server is created to simulate the data request.</li>
     * </ul>
     *
     * @throws DotDataException     Failed to create the CubeJS Client.
     * @throws DotSecurityException Failed to create the CubeJS Client.
     */
    @Test
    public void getContentAnalyticsReport() throws DotDataException, DotSecurityException {
        // ╔════════════════════════╗
        // ║  Generating Test Data  ║
        // ╚════════════════════════╝
        final List<Map<String, String>> dataList = List.of(
                Map.of(
                        "request.count", "4",
                        "request.pageId", "9c5f42da-31b1-4935-9df6-153f5de1bdf2",
                        "request.pageTitle", "Blogs",
                        "request.url", "/blog/"
                ),
                Map.of(
                        "request.count", "3",
                        "request.pageId", "44a076ad-affa-49d4-97b3-6caa3824e7e8",
                        "request.pageTitle", "Destinations",
                        "request.url", "/destinations/"
                ),
                Map.of(
                        "request.count", "2",
                        "request.pageId", "a9f30020-54ef-494e-92ed-645e757171c2",
                        "request.pageTitle", "home",
                        "request.url", "/"
                )
        );

        // ╔══════════════════╗
        // ║  Initialization  ║
        // ╚══════════════════╝
        final String cubeServerIp = "127.0.0.1";
        final int cubeJsServerPort = 5000;
        final MockHttpServer mockhttpServer = new MockHttpServer(cubeServerIp, cubeJsServerPort);
        IPUtils.disabledIpPrivateSubnet(true);
        final Map<String, List<Map<String, String>>> dataExpected = Map.of("data", dataList);

        final AnalyticsQuery analyticsQuery = new AnalyticsQuery.Builder()
                .measures(Set.of("request.count"))
                .orders("request.count DESC")
                .dimensions(Set.of("request.url", "request.pageId", "request.pageTitle"))
                .filters("request.whatAmI = ['PAGE']")
                .limit(100)
                .offset(1)
                .build();
        final AnalyticsQueryParser analyticsQueryParser = new AnalyticsQueryParser();
        final CubeJSQuery cubeJSQuery = analyticsQueryParser.parseQueryToCubeQuery(analyticsQuery);

        final MockHttpServerContext mockHttpServerContext = new MockHttpServerContext.Builder()
                .uri("/cubejs-api/v1/load")
                .requestCondition("CubeJS Query is not the expected one",
                        context -> context.getRequestParameter("query")
                                .orElse(StringPool.BLANK)
                                .equals(cubeJSQuery.toString()))
                .responseStatus(HttpURLConnection.HTTP_OK)
                .responseBody(JsonUtil.getJsonStringFromObject(dataExpected))
                .build();

        mockhttpServer.addContext(mockHttpServerContext);
        try {
            mockhttpServer.start();
            final CubeJSClient cubeClient = new CubeJSClient(
                    String.format("http://%s:%s", cubeServerIp, cubeJsServerPort),
                    getAccessToken());

            final User systemUser = new User();
            CubeJSClientFactory mockCubeJsClientFactory = Mockito.mock(CubeJSClientFactory.class);
            Mockito.when(mockCubeJsClientFactory.create(systemUser)).thenReturn(cubeClient);
            final ContentAnalyticsFactory contentAnalyticsFactory = new ContentAnalyticsFactoryImpl(new AnalyticsQueryParser(), mockCubeJsClientFactory);
            final ReportResponse report = contentAnalyticsFactory.getReport(analyticsQuery, systemUser);

            // ╔══════════════╗
            // ║  Assertions  ║
            // ╚══════════════╝
            assertNotNull("The result list must never be empty", report.getResults());
            assertEquals("There must be " + dataList.size() + " results in the list", dataList.size(), report.getResults().size());
            int i = 0;
            for (final ResultSetItem resultSetItem : report.getResults()) {
                final Map<String, String> objectMap = dataList.get(i);
                assertEquals("Value of request.count is not the expected one", objectMap.get("request.count"), resultSetItem.get("request.count").orElse(StringPool.BLANK));
                assertEquals("Value of request.pageId is not the expected one", objectMap.get("request.pageId"), resultSetItem.get("request.pageId").orElse(StringPool.BLANK));
                assertEquals("Value of request.pageTitle is not the expected one", objectMap.get("request.pageTitle"), resultSetItem.get("request.pageTitle").orElse(StringPool.BLANK));
                assertEquals("Value of request.url is not the expected one", objectMap.get("request.url"), resultSetItem.get("request.url").orElse(StringPool.BLANK));
                i++;
            }
        } finally {
            // ╔═══════════╗
            // ║  Cleanup  ║
            // ╚═══════════╝
            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link ContentAnalyticsFactory#getRawReport(String, User)}
     *     </li>
     *     <li><b>Given Scenario: </b>Run a report for a given query.</li>
     *     <li><b>Expected Result: </b>The simulated results must adhere to the data specified in
     *     the query. A mock HTTP Server is created to simulate the data request.</li>
     * </ul>
     *
     * @throws DotDataException     Failed to create the CubeJS Client.
     * @throws DotSecurityException Failed to create the CubeJS Client.
     */
    @Test
    public void getContentAnalyticsReportRaw() throws DotDataException, DotSecurityException {
        // ╔════════════════════════╗
        // ║  Generating Test Data  ║
        // ╚════════════════════════╝
        final List<Map<String, String>> dataList = List.of(
                Map.of(
                        "request.count", "4",
                        "request.pageId", "9c5f42da-31b1-4935-9df6-153f5de1bdf2",
                        "request.pageTitle", "Blogs",
                        "request.url", "/blog/"
                ),
                Map.of(
                        "request.count", "3",
                        "request.pageId", "44a076ad-affa-49d4-97b3-6caa3824e7e8",
                        "request.pageTitle", "Destinations",
                        "request.url", "/destinations/"
                ),
                Map.of(
                        "request.count", "2",
                        "request.pageId", "a9f30020-54ef-494e-92ed-645e757171c2",
                        "request.pageTitle", "home",
                        "request.url", "/"
                )
        );

        // ╔══════════════════╗
        // ║  Initialization  ║
        // ╚══════════════════╝
        final String cubeServerIp = "127.0.0.1";
        final int cubeJsServerPort = 5000;
        final MockHttpServer mockhttpServer = new MockHttpServer(cubeServerIp, cubeJsServerPort);
        IPUtils.disabledIpPrivateSubnet(true);
        final Map<String, List<Map<String, String>>> dataExpected = Map.of("data", dataList);

        final String analyticsQuery = "{\n" +
                "  \"order\": {\n" +
                "    \"request.createdAt\": \"asc\"\n" +
                "  },\n" +
                "  \"dimensions\": [\n" +
                "    \"request.requestId\"\n" +
                "  ],\n" +
                "  \"measures\": [\n" +
                "    \"request.count\"\n" +
                "  ],\n" +
                "  \"timeDimensions\": [\n" +
                "    {\n" +
                "      \"dimension\": \"request.createdAt\",\n" +
                "      \"granularity\": \"day\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";


        final MockHttpServerContext mockHttpServerContext = new MockHttpServerContext.Builder()
                .uri("/cubejs-api/v1/load")
                .responseStatus(HttpURLConnection.HTTP_OK)
                .responseBody(JsonUtil.getJsonStringFromObject(dataExpected))
                .build();

        mockhttpServer.addContext(mockHttpServerContext);
        try {
            mockhttpServer.start();
            final CubeJSClient cubeClient = new CubeJSClient(
                    String.format("http://%s:%s", cubeServerIp, cubeJsServerPort),
                    getAccessToken());

            final User systemUser = new User();
            CubeJSClientFactory mockCubeJsClientFactory = Mockito.mock(CubeJSClientFactory.class);
            Mockito.when(mockCubeJsClientFactory.create(systemUser)).thenReturn(cubeClient);
            final ContentAnalyticsFactory contentAnalyticsFactory = new ContentAnalyticsFactoryImpl(new AnalyticsQueryParser(), mockCubeJsClientFactory);
            final ReportResponse report = contentAnalyticsFactory.getRawReport(analyticsQuery, systemUser);

            // ╔══════════════╗
            // ║  Assertions  ║
            // ╚══════════════╝
            assertNotNull("The result list must never be empty", report.getResults());
            assertEquals("There must be " + dataList.size() + " results in the list", dataList.size(), report.getResults().size());
            int i = 0;
            for (final ResultSetItem resultSetItem : report.getResults()) {
                final Map<String, String> objectMap = dataList.get(i);
                assertEquals("Value of request.count is not the expected one", objectMap.get("request.count"), resultSetItem.get("request.count").orElse(StringPool.BLANK));
                assertEquals("Value of request.pageId is not the expected one", objectMap.get("request.pageId"), resultSetItem.get("request.pageId").orElse(StringPool.BLANK));
                assertEquals("Value of request.pageTitle is not the expected one", objectMap.get("request.pageTitle"), resultSetItem.get("request.pageTitle").orElse(StringPool.BLANK));
                assertEquals("Value of request.url is not the expected one", objectMap.get("request.url"), resultSetItem.get("request.url").orElse(StringPool.BLANK));
                i++;
            }
        } finally {
            // ╔═══════════╗
            // ║  Cleanup  ║
            // ╚═══════════╝
            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    private AccessToken getAccessToken() {
        return AnalyticsTestUtils.createAccessToken(
                "a1b2c3d4e5f6",
                "some-client",
                null,
                "some-scope",
                "some-token-type",
                TokenStatus.OK,
                Instant.now());
    }

}
