package com.dotcms.rest.api.v1.grafana.client;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrl.Method;
import com.dotcms.rest.api.v1.grafana.client.dto.DashboardDetail;
import com.dotcms.rest.api.v1.grafana.client.dto.DashboardSearchResult;
import com.dotcms.rest.api.v1.grafana.client.dto.GrafanaFolder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grafana REST API client implementation using dotCMS standard patterns.
 *
 * This client provides methods to interact with Grafana API endpoints including:
 * - Searching dashboards
 * - Getting dashboard details
 * - Managing folders
 *
 * Configuration properties:
 * - grafana.api.url: Base URL for Grafana API (default: http://localhost:3000)
 * - grafana.api.token: API token for authentication
 * - grafana.api.timeout: Request timeout in milliseconds (default: 30000)
 *
 * @author dotCMS
 */
public class GrafanaClient {

    private static final String GRAFANA_API_URL_KEY = "grafana.api.url";
    private static final String GRAFANA_API_TOKEN_KEY = "grafana.api.token";
    private static final String GRAFANA_API_TIMEOUT_KEY = "grafana.api.timeout";

    private static final String DEFAULT_API_URL = "http://localhost:3000";
    private static final int DEFAULT_TIMEOUT = 30000;
    private static final String BEARER_PREFIX = "Bearer ";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Gets the configured Grafana API base URL
     */
    private String getApiUrl() {
        return Config.getStringProperty(GRAFANA_API_URL_KEY, DEFAULT_API_URL);
    }

    /**
     * Gets the configured API token
     */
    private String getApiToken() {
        String token = Config.getStringProperty(GRAFANA_API_TOKEN_KEY, "");
        if (!UtilMethods.isSet(token)) {
            Logger.warn(this, "Grafana API token not configured. Set property: " + GRAFANA_API_TOKEN_KEY);
        }
        return token;
    }

    /**
     * Gets the configured request timeout
     */
    private int getTimeout() {
        return Config.getIntProperty(GRAFANA_API_TIMEOUT_KEY, DEFAULT_TIMEOUT);
    }

    /**
     * Builds authorization header with API token
     */
    private String getAuthHeader() {
        String token = getApiToken();
        return UtilMethods.isSet(token) ? BEARER_PREFIX + token : null;
    }

    /**
     * Search for dashboards based on various criteria
     */
    public List<DashboardSearchResult> searchDashboards(String query, String type, Boolean starred,
                                                       String folderIds, String tag, Integer limit) {
        try {
            Map<String, String> params = new HashMap<>();
            if (UtilMethods.isSet(query)) params.put("query", query);
            if (UtilMethods.isSet(type)) params.put("type", type);
            if (starred != null) params.put("starred", starred.toString());
            if (UtilMethods.isSet(folderIds)) params.put("folderIds", folderIds);
            if (UtilMethods.isSet(tag)) params.put("tag", tag);
            if (limit != null) params.put("limit", limit.toString());

            String url = buildUrl("/api/search", params);
            String response = executeRequest(url, Method.GET);

            return objectMapper.readValue(response, new TypeReference<List<DashboardSearchResult>>() {});

        } catch (Exception e) {
            Logger.error(this, "Error searching dashboards: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get dashboard details by UID
     */
    public DashboardDetail getDashboardByUid(String uid) {
        if (!UtilMethods.isSet(uid)) {
            throw new IllegalArgumentException("Dashboard UID cannot be empty");
        }

        try {
            String url = getApiUrl() + "/api/dashboards/uid/" + uid;
            String response = executeRequest(url, Method.GET);

            return objectMapper.readValue(response, DashboardDetail.class);

        } catch (Exception e) {
            Logger.error(this, "Error getting dashboard by UID [" + uid + "]: " + e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve dashboard", e);
        }
    }

    /**
     * Get all folders with optional limit
     */
    public List<GrafanaFolder> getFolders(Integer limit) {
        try {
            Map<String, String> params = new HashMap<>();
            if (limit != null) params.put("limit", limit.toString());

            String url = buildUrl("/api/folders", params);
            String response = executeRequest(url, Method.GET);

            return objectMapper.readValue(response, new TypeReference<List<GrafanaFolder>>() {});

        } catch (Exception e) {
            Logger.error(this, "Error getting folders: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get folder by UID
     */
    public GrafanaFolder getFolderByUid(String uid) {
        if (!UtilMethods.isSet(uid)) {
            throw new IllegalArgumentException("Folder UID cannot be empty");
        }

        try {
            String url = getApiUrl() + "/api/folders/" + uid;
            String response = executeRequest(url, Method.GET);

            return objectMapper.readValue(response, GrafanaFolder.class);

        } catch (Exception e) {
            Logger.error(this, "Error getting folder by UID [" + uid + "]: " + e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve folder", e);
        }
    }

    /**
     * Get dashboards in a specific folder
     */
    public List<DashboardSearchResult> getDashboardsInFolder(String folderUid) {
        if (!UtilMethods.isSet(folderUid)) {
            throw new IllegalArgumentException("Folder UID cannot be empty");
        }

        try {
            String url = getApiUrl() + "/api/folders/" + folderUid + "/dashboards";
            String response = executeRequest(url, Method.GET);

            return objectMapper.readValue(response, new TypeReference<List<DashboardSearchResult>>() {});

        } catch (Exception e) {
            Logger.error(this, "Error getting dashboards in folder [" + folderUid + "]: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Build URL with query parameters
     */
    private String buildUrl(String path, Map<String, String> params) {
        StringBuilder url = new StringBuilder(getApiUrl()).append(path);

        if (!params.isEmpty()) {
            url.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) url.append("&");
                url.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
        }

        return url.toString();
    }

    /**
     * Execute HTTP request using CircuitBreakerUrl
     */
    private String executeRequest(String url, Method method) throws IOException {
        Logger.debug(this, "Executing Grafana API request: " + method + " " + url);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CircuitBreakerUrl.builder()
                    .setUrl(url)
                    .setMethod(method)
                    .setAuthHeaders(getAuthHeader())
                    .setTimeout(getTimeout())
                    .setTryAgainAttempts(3)
                    .build()
                    .doOut(output);

            return output.toString(StandardCharsets.UTF_8);

        } catch (Exception e) {
            Logger.error(this, "Failed to execute Grafana API request: " + e.getMessage(), e);
            throw new IOException("Grafana API request failed", e);
        }
    }
}