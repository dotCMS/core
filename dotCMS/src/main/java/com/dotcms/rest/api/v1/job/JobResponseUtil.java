package com.dotcms.rest.api.v1.job;

import javax.servlet.http.HttpServletRequest;

public class JobResponseUtil {

    private JobResponseUtil() {
        // Utility class
    }

    /**
     * Builds a JobStatusResponse object with the job ID and status URL.
     *
     * @param jobId    The job ID
     * @param request  The HttpServletRequest to build the base URL
     * @return A JobStatusResponse object
     */
    public static JobStatusResponse buildJobStatusResponse(String jobId, HttpServletRequest request) {
        String statusUrl = buildBaseUrlFromRequest(request)
                + "/api/v1/content/_import/" + jobId;
        return JobStatusResponse.builder()
                .jobId(jobId)
                .statusUrl(statusUrl)
                .build();
    }

    /**
     * Builds the base URL from the HttpServletRequest.
     *
     * @param request The HttpServletRequest
     * @return The base URL as a string
     */
    private static String buildBaseUrlFromRequest(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();

        if (scheme == null || scheme.isBlank()) {
            scheme = "http";
        }

        boolean isDefaultPort = ("http".equalsIgnoreCase(scheme) && serverPort == 80) ||
                ("https".equalsIgnoreCase(scheme) && serverPort == 443);

        StringBuilder baseUrl = new StringBuilder();
        baseUrl.append(scheme).append("://").append(serverName);

        if (!isDefaultPort) {
            baseUrl.append(":").append(serverPort);
        }

        return baseUrl.toString();
    }
}