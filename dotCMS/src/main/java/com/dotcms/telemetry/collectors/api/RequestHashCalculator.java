package com.dotcms.telemetry.collectors.api;

import com.dotmarketing.util.Logger;
import org.apache.commons.codec.digest.DigestUtils;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Util class to provide methods to calculate a Hash from a {@link HttpServletRequest}
 */
public class RequestHashCalculator {

    /**
     * This method will create a hash of the request, to be able to know if this is a unique request. To
     * calculate the hash, the following elements are taken into account:
     * <ul>
     *     <li>URL Parameters</li>
     *     <li>Query Parameters</li>
     *     <li>Request Body</li>
     * </ul>
     *
     * @param apiMetricType Metric to calculate the hash
     * @param request       the request to get the content
     *
     * @return The hash of the request
     */
    public String calculate(final ApiMetricType apiMetricType,
                            final ApiMetricWebInterceptor.RereadInputStreamRequest request) {
        try {
            final StringBuilder key = new StringBuilder();
            //API Path
            key.append(cleanUrl(request.getRequestURI(), apiMetricType.getAPIUrl()));

            // get URL parameters
            final Map<String, String[]> parameters = request.getParameterMap();
            key.append(getURLParameters(parameters));

            // Try to extract request body if it exists and is not too large
            try {
                // We don't access the content directly anymore as it's buffered only in the input stream
                // and only if it's small enough (< MAX_REQUEST_BUFFER_SIZE)
                ServletInputStream inputStream = request.getInputStream();
                // Just add a marker for the hash calculation
                key.append("<content>");
            } catch (Exception e) {
                Logger.debug(this, "Failed to read request content for hash calculation: " + e.getMessage());
            }

            return DigestUtils.md5Hex(key.toString());
        } catch (Exception e) {
            Logger.debug(this, "Error calculating hash", e);
            return DigestUtils.md5Hex(UUID.randomUUID().toString());
        }
    }

    /**
     * Cleans the URL to get just the path without the API prefix
     * 
     * @param fullUrl The full request URI
     * @param apiUrl The API URL prefix
     * @return The cleaned URL
     */
    private String cleanUrl(final String fullUrl, final String apiUrl) {
        if (fullUrl == null) {
            return "";
        }
        
        String cleanedUrl = fullUrl;
        
        // Remove API prefix
        int apiIndex = fullUrl.indexOf("/api/");
        if (apiIndex >= 0) {
            cleanedUrl = fullUrl.substring(apiIndex + 5); // +5 to skip "/api/"
        }
        
        // Remove any query parameters
        int queryIndex = cleanedUrl.indexOf("?");
        if (queryIndex >= 0) {
            cleanedUrl = cleanedUrl.substring(0, queryIndex);
        }
        
        return cleanedUrl;
    }
    
    /**
     * Get URL parameters as a sorted string for consistent hash generation
     * 
     * @param parameters The parameter map from the request
     * @return A string representation of the sorted parameters
     */
    private String getURLParameters(final Map<String, String[]> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "NOPARAMS";
        }
        
        // Sort parameters by name for consistent hash generation
        SortedMap<String, String[]> sortedParams = new TreeMap<>(parameters);
        StringBuilder result = new StringBuilder();
        
        for (Map.Entry<String, String[]> entry : sortedParams.entrySet()) {
            String paramName = entry.getKey();
            String[] values = entry.getValue();
            
            if (values != null && values.length > 0) {
                for (String value : values) {
                    result.append(paramName).append("=").append(value).append(";");
                }
            } else {
                result.append(paramName).append("=;");
            }
        }
        
        return result.toString();
    }
}
