package com.dotcms.telemetry.collectors.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Util class to provide methods to calculate a Hash from a {@link HttpServletRequest}
 */
public class RequestHashCalculator {

    final ObjectMapper jsonMapper = new ObjectMapper();
    final MessageDigest digest;

    RequestHashCalculator() {
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calculate a hash from a {@link HttpServletRequest} using:
     * <ul>
     *     <li>The Query Parameters</li>
     *     <li>The body</li>
     *     <li>The Url Parameters</li>
     * </ul>
     * <p>The steps to calculate this hash are the follows:
     * <ul>
     *     <li>If the Request has Query parameters the these are sort alphabetically by the
     *     Parameter
     *      Name, then 'xxx?a=1&b=2' and 'xxx?b=2&a=1' are the same. If the Request does not has any
     *      Query parameters then the word 'NONE' is taking instead.</li>
     *      <li>If the Request has Url parameters the these are sort alphabetically by the
     *      Parameter Name,
     *      then 'xxx/a/1/b/2' and 'xxx/b/2/a/1' are the same. If the Request does not has any Url
     *      parameters then the word 'NONE' is taking instead.</li>
     *      <li>If the Request has any JSON Body it is sort by alphabetically by the attribute
     *      name, so:
     *      <pre>
     *          {@code { a: 1, b: 2 }}
     *      </pre>
     *      And:
     *      <pre>
     *          {@code { b: 2, a: 1 }}
     *      </pre>
     *      are the same.
     *      </li>
     *      <li>Finally all of them are concat and a hash is calculated with that String.</li>
     * </ul>
     *
     * @param apiMetricType
     * @param request
     *
     * @return
     */
    public String calculate(final ApiMetricType apiMetricType,
                            final ApiMetricWebInterceptor.RereadInputStreamRequest request) {

        final String queryParametersAsString =
                getQueryParameters(request).map(Map::toString).orElse("NONE");
        final String urlParametersAsString = getUrlParameters(apiMetricType, request)
                .map(Map::toString).orElse("NONE");
        final String bodyAsString = getBody(request).map(Map::toString).orElse("NONE");

        final String requestString = queryParametersAsString + urlParametersAsString + bodyAsString;


        final byte[] encodedHash = digest.digest(requestString.getBytes(StandardCharsets.UTF_8));
        return new String(encodedHash);
    }

    @SuppressWarnings("unchecked")
    private Optional<Map<String, Object>> getBody(ApiMetricWebInterceptor.RereadInputStreamRequest request) {
        final String requestContent = request.getReadContent();
        try {
            if (!requestContent.isEmpty()) {
                final Map<String, Object> bodyMap = jsonMapper.readValue(requestContent,
                        Map.class);

                return Optional.of(sort(bodyMap));
            }

            return Optional.empty();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    @SuppressWarnings("unchecked")
    private TreeMap<String, Object> sort(final Map<String, Object> map) {
        TreeMap<String, Object> treeMap = new TreeMap<>(map);

        for (Map.Entry<String, Object> bodyMapEntry : map.entrySet()) {
            final Object value = bodyMapEntry.getValue();

            if (value instanceof Map) {
                final TreeMap mapSorted = sort((Map) value);
                treeMap.put(bodyMapEntry.getKey(), mapSorted);
            }
        }
        return treeMap;
    }


    private Optional<Map<String, String>> getUrlParameters(final ApiMetricType apiMetricType,
                                                           final HttpServletRequest request) {
        String uri = request.getRequestURI();
        String apiUrl = apiMetricType.getAPIUrl();

        final String urlParameters = uri.substring(uri.indexOf("/es/search") + apiUrl.length());

        TreeMap<String, String> sortedParameters = new TreeMap<>();

        if (urlParameters != null) {
            final String[] paramsAndValues = urlParameters.split("/");
            final Pair pair = new Pair();

            for (String paramOrValue : paramsAndValues) {
                pair.add(paramOrValue);

                if (pair.isCompleted()) {
                    sortedParameters.put(pair.getKey(), pair.getValue());
                    pair.reset();
                }
            }
        }

        return sortedParameters.isEmpty() ? Optional.empty() : Optional.of(sortedParameters);
    }

    private Optional<Map<String, String>> getQueryParameters(final HttpServletRequest request) {
        String queryString = request.getQueryString();

        TreeMap<String, String> sortedParameters = new TreeMap<>();

        if (queryString != null) {
            String[] params = queryString.split("&");

            for (String param : params) {
                String[] keyValue = param.split("=");
                String key = keyValue[0];
                String value = keyValue.length > 1 ? keyValue[1] : "";

                sortedParameters.put(key, value);
            }
        }

        return sortedParameters.isEmpty() ? Optional.empty() : Optional.of(sortedParameters);
    }

    private static class Pair {
        private String key;
        private String value;

        void add(final String keyOrValue) {
            if (key == null) {
                key = keyOrValue;
            } else {
                value = keyOrValue;
            }
        }

        boolean isCompleted() {
            return key != null && value != null;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public void reset() {
            value = null;
            key = null;
        }
    }

}
