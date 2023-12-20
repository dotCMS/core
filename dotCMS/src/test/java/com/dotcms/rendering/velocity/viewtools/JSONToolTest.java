package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrlBuilder;
import com.dotmarketing.util.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that the {@link JSONTool} ViewTool works as expected.
 */
public class JSONToolTest {

    /**
     * Tests that the {@link JSONTool#post(String, Map, Object, boolean)} method works as expected
     * when the parameters map {@link Map} is sent as a json payload.
     * @throws IOException error executing the request
     */
    @Test
    public void test_PostJsonPayloadFromMap() throws IOException {

        final Map<String, Object> testPayloadMap = getTestPayloadMap();
        testPayloadMap.put("workflowAction", null);

        final CircuitBreakerUrlBuilder circuitBreakerUrlBuilder = mockCircuitBreakerUrlBuilder();
        when(circuitBreakerUrlBuilder.setRawData(anyString()))
                .thenThrow(new RuntimeException("Payload map does not match"));
        when(circuitBreakerUrlBuilder.setRawData(argThat(
                new PayloadMapMatcher(testPayloadMap)))).thenReturn(circuitBreakerUrlBuilder);

        final JSONTool jsonTool = new JSONTool(() -> circuitBreakerUrlBuilder);
        final Object result = jsonTool.post("http://localhost:8080/api/v1/content/publish/1",
                Map.of("Content-Type", "application/json"),
                testPayloadMap,
                true);

        assertNotNull(result);

    }

    /**
     * Tests that the {@link JSONTool#post(String, Map, Object, boolean)} method works as expected
     * when the parameters map {@link Map} is sent as the request parameters.
     * @throws IOException error executing the request
     */
    @Test
    public void test_PostParamMap() throws IOException {

        final Map<String, Object> testParamMap = Map.of(
                "param-1", "value-1",
                "param-2", 2,
                "param-3", true
        );

        final CircuitBreakerUrlBuilder circuitBreakerUrlBuilder = mockCircuitBreakerUrlBuilder();
        when(circuitBreakerUrlBuilder.setParams(anyMap()))
                .thenThrow(new RuntimeException("Param map does not match"));

        final Map<String, String> expectedParamMap = Map.of(
                "param-1", "value-1",
                "param-2", "2",
                "param-3", "true"
        );
        when(circuitBreakerUrlBuilder.setParams(eq(expectedParamMap))).thenReturn(circuitBreakerUrlBuilder);

        final JSONTool jsonTool = new JSONTool(() -> circuitBreakerUrlBuilder);
        final Object result = jsonTool.post("http://localhost:8080/api/v1/content/publish/1",
                Map.of("Accept", "application/json"),
                testParamMap,
                false);

        assertNotNull(result);

    }

    /**
     * Tests that the {@link JSONTool#put(String, Map, Object, boolean)} method works as expected
     * when the parameters map {@link Map} is sent as a json payload.
     * @throws IOException error executing the request
     */
    @Test
    public void test_PutJsonPayloadFromMap() throws IOException {

        final Map<String, Object> testPayloadMap = getTestPayloadMap();

        final CircuitBreakerUrlBuilder circuitBreakerUrlBuilder = mockCircuitBreakerUrlBuilder();
        when(circuitBreakerUrlBuilder.setRawData(anyString()))
                .thenThrow(new RuntimeException("Payload map does not match"));
        when(circuitBreakerUrlBuilder.setRawData(argThat(
                new PayloadMapMatcher(testPayloadMap)))).thenReturn(circuitBreakerUrlBuilder);

        final JSONTool jsonTool = new JSONTool(() -> circuitBreakerUrlBuilder);
        final Object result = jsonTool.put("http://localhost:8080/api/v1/content/publish/1",
                Map.of("Content-Type", "application/json"),
                testPayloadMap,
                true);

        assertNotNull(result);

    }

    /**
     * Tests that the {@link JSONTool#put(String, Map, Object, boolean)} method works as expected
     * when the parameters map {@link Map} is sent as the request parameters.
     * @throws IOException error executing the request
     */
    @Test
    public void test_PutParamMap() throws IOException {

        final Map<String, Object> testParamMap = Map.of(
                "param-1", "value-1",
                "param-2", 3,
                "param-3", true
        );

        final CircuitBreakerUrlBuilder circuitBreakerUrlBuilder = mockCircuitBreakerUrlBuilder();
        when(circuitBreakerUrlBuilder.setParams(anyMap()))
                .thenThrow(new RuntimeException("Param map does not match"));

        final Map<String, String> expectedParamMap = Map.of(
                "param-1", "value-1",
                "param-2", "3",
                "param-3", "true"
        );
        when(circuitBreakerUrlBuilder.setParams(eq(expectedParamMap))).thenReturn(circuitBreakerUrlBuilder);

        final JSONTool jsonTool = new JSONTool(() -> circuitBreakerUrlBuilder);
        final Object result = jsonTool.put("http://localhost:8080/api/v1/content/publish/1",
                Map.of("Accept", "application/json"),
                testParamMap,
                false);

        assertNotNull(result);

    }

    private static Map<String, Object> getTestPayloadMap() {
        final Map<String, Object> payloadMap = Map.of("languageId", 1,
                "hostId", "host-1",
                "system", true,
                "nested-obj", Map.of(
                        "first-nested-obj-key", "first-nested-obj-value",
                        "second-nested-obj-key", 2.25),
                "nested-list", List.of(
                        "nested-list-value-1",
                        "nested-list-value-2",
                        "nested-list-value-3"),
                "workflowAction", "action-1");
        return new HashMap<>(payloadMap);
    }

    private static CircuitBreakerUrlBuilder mockCircuitBreakerUrlBuilder() throws IOException {
        final CircuitBreakerUrlBuilder circuitBreakerUrlBuilder = mock(CircuitBreakerUrlBuilder.class);
        final CircuitBreakerUrl circuitBreakerUrl = mock(CircuitBreakerUrl.class);

        when(circuitBreakerUrlBuilder.setMethod(any())).thenReturn(circuitBreakerUrlBuilder);
        when(circuitBreakerUrlBuilder.setHeaders(anyMap())).thenReturn(circuitBreakerUrlBuilder);
        when(circuitBreakerUrlBuilder.setUrl(anyString())).thenReturn(circuitBreakerUrlBuilder);
        when(circuitBreakerUrlBuilder.setTimeout(anyLong())).thenReturn(circuitBreakerUrlBuilder);
        when(circuitBreakerUrlBuilder.build()).thenReturn(circuitBreakerUrl);
        when(circuitBreakerUrl.doString()).thenReturn("{\"success\": true}");
        return circuitBreakerUrlBuilder;
    }

    /**
     * Verifies that a given {@link Map} matches a given json string.
     */
    public static class PayloadMapMatcher implements ArgumentMatcher<String> {
        private final Map<String, Object> expectedMap;

        public PayloadMapMatcher(Map<String, Object> expectedMap) {
            this.expectedMap = expectedMap;
        }

        @Override
        public boolean matches(String mapAsString) {
            final Map<?, ?> actualMap = new JSONObject(mapAsString).getAsMap();
            if (actualMap.size() != expectedMap.size()) {
                return false;
            }
            for (Map.Entry<String, Object> entry : expectedMap.entrySet()) {
                if (!actualMap.containsKey(entry.getKey())) {
                    return false;
                }
                Object actualValue = actualMap.get(entry.getKey());
                if (JSONObject.NULL.equals(actualValue)) {
                    actualValue = null;
                }
                final Object expectedValue = entry.getValue();
                if (actualValue == null && expectedValue == null) {
                    continue;
                }
                if (actualValue == null || expectedValue == null) {
                    return false;
                } else if (actualValue instanceof Number && !(expectedValue instanceof Number)) {
                    return false;
                } else if (actualValue instanceof Map && !(expectedValue instanceof Map)) {
                    return false;
                } else if (actualValue instanceof Collection && !(expectedValue instanceof Collection)) {
                    return false;
                } else if (actualValue instanceof Boolean && !(expectedValue instanceof Boolean)) {
                    return false;
                } else if (actualValue instanceof String && !(expectedValue instanceof String)) {
                    return false;
                }
            }
            return true;
        }
    }

}
