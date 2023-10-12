package com.dotcms.cube;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.analytics.AnalyticsAPI;
import com.dotcms.analytics.AnalyticsTestUtils;
import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.metrics.EventType;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.TokenStatus;
import com.dotcms.cube.CubeJSQuery.Builder;
import com.dotcms.cube.CubeJSResultSet.ResultSetItem;
import com.dotcms.exception.AnalyticsException;
import com.dotcms.http.server.mock.MockHttpServer;
import com.dotcms.http.server.mock.MockHttpServerContext;
import com.dotcms.util.JsonUtil;
import com.dotcms.util.network.IPUtils;

import com.liferay.util.StringPool;

import java.net.HttpURLConnection;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class CubeJSClientTest {

    /**
     * Method to test: {@link CubeJSClient#send(CubeJSQuery)}
     * When: Send a request to Cube JS
     * Should: Return the right data end send the right Query
     */
    @Test
    public void sendAllOk() throws AnalyticsException {
        final AccessToken accessToken = getAccessToken();
        final AnalyticsHelper analyticsHelper = mock(AnalyticsHelper.class);
        when(analyticsHelper.formatBearer(accessToken)).thenReturn(accessToken.toString());

        final String cubeServerIp = "127.0.0.1";
        final int cubeJsServerPort = 5000;

        final MockHttpServer mockhttpServer = new MockHttpServer(cubeServerIp, cubeJsServerPort);

        AnalyticsHelper.setMock(analyticsHelper);

        try {
            IPUtils.disabledIpPrivateSubnet(true);

            final List<Map<String, String>> dataList = list(
                    map(
                            "Events.experiment", "A",
                            "Events.variant", "B",
                            "Events.utcTime", "2022-09-20T15:24:21.000"
                    ),
                    map(
                            "Events.experiment", "A",
                            "Events.variant", "C",
                            "Events.utcTime", "2022-09-20T15:24:21.000"
                    ),
                    map(
                            "Events.experiment", "B",
                            "Events.variant", "C",
                            "Events.utcTime", "2022-09-20T15:24:21.000"
                    )
            );

            final Map<String, List<Map<String, String>>> dataExpected = map("data", dataList);

            final CubeJSQuery cubeJSQuery = new Builder()
                .dimensions("Events.experiment", "Events.variant")
                .build();

            final MockHttpServerContext mockHttpServerContext = new  MockHttpServerContext.Builder()
                    .uri("/cubejs-api/v1/load")
                    .requestCondition("Cube JS Query is not right",
                            context -> context.getRequestParameter("query")
                                    .orElse(StringPool.BLANK)
                                    .equals(cubeJSQuery.toString()))
                    .responseStatus(HttpURLConnection.HTTP_OK)
                    .responseBody(JsonUtil.getJsonStringFromObject(dataExpected))
                    .build();

            mockhttpServer.addContext(mockHttpServerContext);
            mockhttpServer.start();

            final AnalyticsApp analyticsAPP = mock(AnalyticsApp.class);

            final AnalyticsAPI analyticsAPI = mock(AnalyticsAPI.class);
            when(analyticsAPI.getAccessToken(analyticsAPP)).thenReturn(accessToken);


            final CubeJSClient cubeClient =  new CubeJSClient(String.format("http://%s:%s", cubeServerIp, cubeJsServerPort),
                    analyticsAPP, analyticsAPI);
            final CubeJSResultSet cubeJSResultSet = cubeClient.send(cubeJSQuery);

            mockhttpServer.validate();
            assertEquals(3, cubeJSResultSet.size());

            int i = 0;
            for (ResultSetItem resultSetItem : cubeJSResultSet) {
                final Map<String, String> objectMap = dataList.get(i);

                assertEquals(objectMap.get("Events.experiment"), resultSetItem.get("Events.experiment").get());
                assertEquals(objectMap.get("Events.variant"), resultSetItem.get("Events.variant").get());
                assertEquals(objectMap.get("Events.utcTime"), resultSetItem.get("Events.utcTime").get());
                i++;
            }
        } finally {
            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
            AnalyticsHelper.setMock(null);
        }
    }

    /**
     * Method to test: {@link CubeJSClient#send(CubeJSQuery)}
     * When: Paginated try to get 2500 events from CubeJS but the Token expired before get the lst page
     * Should: Use the newly token and get all the Events
     */
    @Test
    public void expireTokenPagination() throws AnalyticsException {
        final AnalyticsHelper analyticsHelper = mock(AnalyticsHelper.class);
        final AccessToken accessToken_1 = getAccessToken("accessToken_1");
        final AccessToken accessToken_2 = getAccessToken("accessToken_2");
        when(analyticsHelper.formatBearer(accessToken_1)).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) throws AnalyticsException {
                if (count < 2) {
                    count++;
                    return accessToken_1.toString();
                }

                throw new AnalyticsException("ACCESS_TOKEN for clientId analytics-customer-customer1 is EXPIRED");
            }
        });

        when(analyticsHelper.formatBearer(accessToken_2)).thenReturn(accessToken_2.toString());

        final String cubeServerIp = "127.0.0.1";
        final int cubeJsServerPort = 5000;

        final MockHttpServer mockhttpServer = new MockHttpServer(cubeServerIp, cubeJsServerPort);

        AnalyticsHelper.setMock(analyticsHelper);

        try {
            IPUtils.disabledIpPrivateSubnet(true);

            final List<Map<String, String>> dataList = new ArrayList<>();
            Instant nextEventTriggerTime = Instant.now();

            final DateTimeFormatter EVENTS_FORMATTER = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd'T'HH:mm:ss.n")
                    .withZone(ZoneId.systemDefault());

            for (int i = 0; i < 2500; i++) {
                dataList.add(map(
                        "Events.experiment", "A",
                        "Events.variant", "B",
                        "Events.utcTime", EVENTS_FORMATTER.format(nextEventTriggerTime)
                ));

                nextEventTriggerTime = nextEventTriggerTime.plus(1, ChronoUnit.MILLIS);
            }

            final CubeJSQuery cubeJSQuery = new Builder()
                    .dimensions("Events.experiment", "Events.variant")
                    .build();

            for (int i = 0; i < 3; i++) {
                final CubeJSQuery cubeJSQueryToPage = new Builder()
                        .offset(1000 * i)
                        .limit(1000)
                        .build();

                CubeJSQuery paginationQuery = Builder.merge(cubeJSQuery, cubeJSQueryToPage);

                int pageSize = i == 2 ? 500 : 1000;
                final int fromIndex = 1000 * i;

                final MockHttpServerContext mockHttpServerContext = new MockHttpServerContext.Builder()
                        .uri("/cubejs-api/v1/load")
                        .requestCondition("Cube JS Query is not right",
                                context -> context.getRequestParameter("query")
                                        .orElse(StringPool.BLANK)
                                        .equals(paginationQuery.toString()))
                        .responseStatus(HttpURLConnection.HTTP_OK)
                        .responseBody(JsonUtil.getJsonStringFromObject(map("data", dataList.subList(fromIndex, fromIndex + pageSize))))
                        .build();

                mockhttpServer.addContext(mockHttpServerContext);
            }

            mockhttpServer.start();

            final AnalyticsApp analyticsAPP = mock(AnalyticsApp.class);
            final AnalyticsAPI analyticsAPI = mock(AnalyticsAPI.class);
            when(analyticsAPI.getAccessToken(analyticsAPP)).thenAnswer(new Answer() {
                private int count = 0;

                public Object answer(InvocationOnMock invocation)  {
                    return count++ == 2 ? accessToken_2 : accessToken_1;
                }
            });

            final CubeJSClient cubeClient =  new CubeJSClient(String.format("http://%s:%s", cubeServerIp, cubeJsServerPort),
                    analyticsAPP, analyticsAPI);
            final CubeJSResultSet cubeJSResultSet = cubeClient.sendWithPagination(cubeJSQuery);

            mockhttpServer.validate();

            int howManyItems = 0;
            for (ResultSetItem resultSetItem : cubeJSResultSet) {
                howManyItems++;
            }
            assertEquals(2500, howManyItems);

        } finally {
            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
            AnalyticsHelper.setMock(null);
        }
    }

    /**
     * Method to test: {@link CubeJSClient#send(CubeJSQuery)}
     * When: Send a request to Cube JS
     * Should: Return the right data end send the right Query
     */
    @Test
    public void expireTokenOnTheMiddleOfRequest() throws AnalyticsException {
        final AnalyticsHelper analyticsHelper = mock(AnalyticsHelper.class);
        final AccessToken accessToken_1 = getAccessToken("accessToken_1");
        final AccessToken accessToken_2 = getAccessToken("accessToken_2");
        when(analyticsHelper.formatBearer(accessToken_1)).thenAnswer(new Answer() {
            private int count = 0;
            public Object answer(InvocationOnMock invocation) {
                return count++ == 0 ? accessToken_1.toString() : accessToken_2.toString();
            }
        });

        when(analyticsHelper.formatBearer(accessToken_2)).thenReturn(accessToken_2.toString());

        final String cubeServerIp = "127.0.0.1";
        final int cubeJsServerPort = 5000;

        final MockHttpServer mockhttpServer = new MockHttpServer(cubeServerIp, cubeJsServerPort);

        AnalyticsHelper.setMock(analyticsHelper);

        try {
            IPUtils.disabledIpPrivateSubnet(true);

            final List<Map<String, String>> dataList = new ArrayList<>();
            Instant nextEventTriggerTime = Instant.now();

            final DateTimeFormatter EVENTS_FORMATTER = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd'T'HH:mm:ss.n")
                    .withZone(ZoneId.systemDefault());

            dataList.add(map(
                    "Events.experiment", "A",
                    "Events.variant", "B",
                    "Events.utcTime", EVENTS_FORMATTER.format(nextEventTriggerTime)
            ));

            dataList.add(map(
                    "Events.experiment", "A",
                    "Events.variant", "B",
                    "Events.utcTime", EVENTS_FORMATTER.format(nextEventTriggerTime)
            ));

            final CubeJSQuery cubeJSQuery = new Builder()
                    .dimensions("Events.experiment", "Events.variant")
                    .offset(0)
                    .limit(1000)
                    .build();

            final MockHttpServerContext mockHttpServerContext_1 = new MockHttpServerContext.Builder()
                    .uri("/cubejs-api/v1/load")
                    .requestCondition("Cube JS Query is not right",
                            context -> context.getHeaders().get("Authorization").contains(accessToken_1.toString()))
                    .responseStatus(HttpURLConnection.HTTP_FORBIDDEN)
                    .build();

            mockhttpServer.addContext(mockHttpServerContext_1);

            final MockHttpServerContext mockHttpServerContext_2 = new MockHttpServerContext.Builder()
                    .uri("/cubejs-api/v1/load")
                    .requestCondition("Cube JS Query is not right",
                            context -> context.getHeaders().get("Authorization").contains(accessToken_2.toString()))
                    .responseStatus(HttpURLConnection.HTTP_OK)
                    .responseBody(JsonUtil.getJsonStringFromObject(map("data", dataList)))
                    .build();

            mockhttpServer.addContext(mockHttpServerContext_2);

            mockhttpServer.start();

            final AnalyticsApp analyticsAPP = mock(AnalyticsApp.class);
            final AnalyticsAPI analyticsAPI = mock(AnalyticsAPI.class);
            when(analyticsAPI.getAccessToken(analyticsAPP)).thenAnswer(new Answer() {
                private int count = 0;

                public Object answer(InvocationOnMock invocation)  {
                    return count++ == 0 ? accessToken_1 : accessToken_2;
                }
            });

            final CubeJSClient cubeClient =  new CubeJSClient(String.format("http://%s:%s", cubeServerIp, cubeJsServerPort),
                    analyticsAPP, analyticsAPI);
            final CubeJSResultSet cubeJSResultSet = cubeClient.sendWithPagination(cubeJSQuery);

            mockhttpServer.validate();

            int howManyItems = 0;
            for (ResultSetItem resultSetItem : cubeJSResultSet) {
                howManyItems++;
            }
            assertEquals(2, howManyItems);

        } finally {
            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
            AnalyticsHelper.setMock(null);
        }
    }

    /**
     * Method to test: {@link CubeJSClient#send(CubeJSQuery)}
     * When: Send a request to Cube JS but the CubeJS Server is down
     * Should: Return an empty {@lik CubeJSResultSet} Also it print in the console the follow:
     * <pre>
     * Connection attempts failed Connect to 127.0.0.1:8000 [/127.0.0.1] failed: Connection refused (Connection refused)
     * </pre>
     */
    @Test(expected = RuntimeException.class)
    public void http404() throws AnalyticsException {

        final String cubeServerIp = "127.0.0.1";
        final int cubeJsServerPort = 8000;

        try {
            IPUtils.disabledIpPrivateSubnet(true);

            final CubeJSQuery cubeJSQuery = new Builder()
                    .dimensions("Events.experiment", "Events.variant")
                    .build();

            final AnalyticsApp analyticsAPP = mock(AnalyticsApp.class);
            final AccessToken accessToken = getAccessToken();

            final AnalyticsAPI analyticsAPI = mock(AnalyticsAPI.class);
            when(analyticsAPI.getAccessToken(analyticsAPP)).thenReturn(accessToken);

            final CubeJSClient cubeClient = new CubeJSClient(
                    String.format("http://%s:%s", cubeServerIp, cubeJsServerPort), analyticsAPP, analyticsAPI);
            cubeClient.send(cubeJSQuery);
        } finally {
            IPUtils.disabledIpPrivateSubnet(false);
        }
    }

    /**
     * Method to test: {@link CubeJSClient#send(CubeJSQuery)}
     * When: Send a request with a Null {@link CubeJSQuery}
     * Should: throw {@link IllegalArgumentException}
     */
    @Test
    public void sendWrongQuery() throws AnalyticsException {

        final String cubeServerIp = "127.0.0.1";
        final int cubeJsServerPort = 6000;

        final MockHttpServer mockhttpServer = new MockHttpServer(cubeServerIp, cubeJsServerPort);

        try {
            IPUtils.disabledIpPrivateSubnet(true);

            final MockHttpServerContext mockHttpServerContext = new  MockHttpServerContext.Builder()
                    .uri("/cubejs-api/v1/load")
                    .responseStatus(HttpURLConnection.HTTP_OK)
                    .build();

            mockhttpServer.addContext(mockHttpServerContext);
            mockhttpServer.start();

            final AnalyticsApp analyticsAPP = mock(AnalyticsApp.class);
            final AccessToken accessToken = getAccessToken();

            final AnalyticsAPI analyticsAPI = mock(AnalyticsAPI.class);
            when(analyticsAPI.getAccessToken(analyticsAPP)).thenReturn(accessToken);

            final CubeJSClient cubeClient =  new CubeJSClient(
                    String.format("http://%s:%s", cubeServerIp, cubeJsServerPort),
                    analyticsAPP, analyticsAPI);

            try {
                cubeClient.send(null);
                throw new AssertionError("IllegalArgumentException Expected");
            }  catch (IllegalArgumentException e) {
                mockhttpServer.mustNeverCalled("/cubejs-api/v1/load");
            }

            try {
                cubeClient.sendWithPagination(null);
                throw new AssertionError("IllegalArgumentException Expected");
            }  catch (IllegalArgumentException e) {
                mockhttpServer.mustNeverCalled("/cubejs-api/v1/load");
            }
        } finally {
            IPUtils.disabledIpPrivateSubnet(false);
            mockhttpServer.stop();
        }
    }

    private static AccessToken getAccessToken() {
        return getAccessToken("a1b2c3d4e5f6");
    }

    private static AccessToken getAccessToken(final String token) {
        return AnalyticsTestUtils.createAccessToken(
                token,
            "some-client",
            null,
            "some-scope",
            "some-token-type",
            TokenStatus.OK,
            Instant.now());
    }

}
