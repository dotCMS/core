package com.dotcms.cube;

import com.dotcms.analytics.AnalyticsTestUtils;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.ResultSetItem;
import com.dotcms.analytics.model.TokenStatus;
import com.dotcms.cube.CubeJSQuery.Builder;
import com.dotcms.http.server.mock.MockHttpServer;
import com.dotcms.http.server.mock.MockHttpServerContext;
import com.dotcms.util.JsonUtil;
import com.dotcms.util.network.IPUtils;
import com.liferay.util.StringPool;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;

public class CubeJSClientTest {

    /**
     * Method to test: {@link CubeJSClient#send(CubeJSQuery)}
     * When: Send a request to Cube JS
     * Should: Return the right data end send the right Query
     */
    @Test
    public void sendAllOk() {

        final String cubeServerIp = "127.0.0.1";
        final int cubeJsServerPort = 5000;

        final MockHttpServer mockhttpServer = new MockHttpServer(cubeServerIp, cubeJsServerPort);

        try {
            IPUtils.disabledIpPrivateSubnet(true);

            final List<Map<String, String>> dataList = list(
                    Map.of(
                            "Events.experiment", "A",
                            "Events.variant", "B",
                            "Events.utcTime", "2022-09-20T15:24:21.000"
                    ),
                    Map.of(
                            "Events.experiment", "A",
                            "Events.variant", "C",
                            "Events.utcTime", "2022-09-20T15:24:21.000"
                    ),
                    Map.of(
                            "Events.experiment", "B",
                            "Events.variant", "C",
                            "Events.utcTime", "2022-09-20T15:24:21.000"
                    )
            );

            final Map<String, List<Map<String, String>>> dataExpected = Map.of("data", dataList);

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

            final CubeJSClient cubeClient =  new CubeJSClient(
                String.format("http://%s:%s", cubeServerIp, cubeJsServerPort),
                getAccessToken());
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
    public void http404() {

        final String cubeServerIp = "127.0.0.1";
        final int cubeJsServerPort = 8000;

        try {
            IPUtils.disabledIpPrivateSubnet(true);

            final CubeJSQuery cubeJSQuery = new Builder()
                    .dimensions("Events.experiment", "Events.variant")
                    .build();

            final CubeJSClient cubeClient = new CubeJSClient(
                    String.format("http://%s:%s", cubeServerIp, cubeJsServerPort),
                    getAccessToken());
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
    public void sendWrongQuery() {

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

            final CubeJSClient cubeClient =  new CubeJSClient(
                    String.format("http://%s:%s", cubeServerIp, cubeJsServerPort),
                    getAccessToken());

            try {
                CubeJSQuery query = null;
                cubeClient.send(query);
                throw new AssertionError("IllegalArgumentException Expected");
            }  catch (IllegalArgumentException e) {
                mockhttpServer.mustNeverCalled("/cubejs-api/v1/load");
            }

            try {
                CubeJSQuery query = null;
                cubeClient.send(query);
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
